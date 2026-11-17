package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.ApiException;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.common.web.Ids;
import eu.simpl.cm.common.web.JsonBody;
import eu.simpl.cm.common.web.Problems;
import eu.simpl.cm.common.web.Times;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offer table, accept and reject (D23), expiry (D44, B.4). */
@Service
public class OfferService {
    private static final Logger log = LoggerFactory.getLogger(OfferService.class);
    public static final String CREATED = "CREATED";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String TERMINATED = "TERMINATED";

    private final OfferRepository offers;
    private final CmClient cm;
    private final UibeSettings settings;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;

    public OfferService(OfferRepository offers, CmClient cm, UibeSettings settings, TransactionTemplate tx, ObjectMapper mapper) {
        this.offers = offers;
        this.cm = cm;
        this.settings = settings;
        this.tx = tx;
        this.mapper = mapper;
    }

    /** D9 response names. */
    public static Map<String, Object> view(Offer o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractNegotiationId", o.contractNegotiationId);
        m.put("humanReadableData", o.humanReadable);
        m.put("contractData", null);
        m.put("hashData", o.hash);
        m.put("status", o.status);
        m.put("createdDate", Times.local(o.createdDate));
        m.put("lastUpdatedDate", Times.local(o.lastUpdatedDate));
        return m;
    }

    /** B.5: create the offer row, or replace html and hash keeping the status. */
    public Map<String, Object> upsert(String nid, JsonNode body) {
        Ids.pathId("contractNegotiationId", nid);
        String html = JsonBody.text(body, "humanReadable");
        String hash = JsonBody.text(body, "hash");
        if (html == null || html.isBlank() || hash == null || hash.isBlank()) {
            throw ApiException.validation("humanReadable and hash are required");
        }
        Offer saved = tx.execute(s -> {
            Offer o = offers.findById(nid).orElse(null);
            Instant now = Times.now();
            if (o == null) {
                o = new Offer();
                o.contractNegotiationId = nid;
                o.status = CREATED;
                o.createdDate = now;
            }
            o.humanReadable = html;
            o.hash = hash;
            o.lastUpdatedDate = now;
            return offers.save(o);
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("offerStatus", saved.status);
        out.put("offerUrl", settings.uiPublicUrl + "/contractOffers/" + nid);
        return out;
    }

    public List<Map<String, Object>> all() {
        return offers.findAll().stream()
                .sorted((a, b) -> b.createdDate.compareTo(a.createdDate))
                .map(OfferService::view).toList();
    }

    public Map<String, Object> one(String nid) {
        return view(find(nid));
    }

    public Offer find(String nid) {
        Ids.pathId("contractNegotiationId", nid);
        return offers.findById(nid)
                .orElseThrow(() -> ApiException.notFound("Contract offer not found for contractNegotiationId = " + nid));
    }

    public Offer findOrNull(String nid) {
        return offers.findById(nid).orElse(null);
    }

    /** D23: UIBE checks its offer status, then CM decides; CM failure keeps CREATED. */
    public Map<String, Object> decide(String nid, String operation, String actor) {
        if (!"accept".equals(operation) && !"reject".equals(operation)) {
            throw ApiException.typeMismatch("operation must be accept or reject");
        }
        Offer o = find(nid);
        if (!CREATED.equals(o.status)) {
            throw new ApiException(400, Problems.OFFER_NOT_PENDING, "The offer is not pending, status " + o.status);
        }
        HttpJson.Response r = cm.call("PATCH", "/contractOffers/" + nid + "/" + operation, null, actor);
        if (!r.ok()) {
            throw passThrough(r);
        }
        Offer saved = tx.execute(s -> {
            Offer cur = offers.findById(nid).orElseThrow();
            cur.status = "accept".equals(operation) ? ACCEPTED : REJECTED;
            cur.lastUpdatedDate = Times.now();
            return offers.save(cur);
        });
        return view(saved);
    }

    public ApiException passThrough(HttpJson.Response r) {
        if (r.status() < 0) {
            return new ApiException(500, Problems.INTERNAL_SERVER_ERROR, "The contract manager is not reachable");
        }
        JsonNode body = r.json(mapper);
        if (body != null && body.isObject()) {
            return new ApiException(r.status(), Problems.INTERNAL_SERVER_ERROR, "Contract manager error", body);
        }
        return new ApiException(r.status(), r.status() >= 500 ? Problems.INTERNAL_SERVER_ERROR : Problems.VALIDATION_FAILURE,
                "Contract manager error");
    }

    /** D44, B.4: one expiry run. */
    public Map<String, Object> expire(Instant asOf, List<String> ids) {
        Instant cutoff = asOf.minus(settings.offerExpiryTimeout);
        List<String> expired = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<Offer> candidates;
        if (ids == null) {
            candidates = offers.findByStatus(CREATED);
        } else {
            candidates = new ArrayList<>();
            for (String id : ids) {
                Offer o = offers.findById(id).orElse(null);
                if (o == null) {
                    skipped.add(id);
                } else {
                    candidates.add(o);
                }
            }
        }
        for (Offer o : candidates) {
            if (!CREATED.equals(o.status) || !o.createdDate.isBefore(cutoff)) {
                skipped.add(o.contractNegotiationId);
                continue;
            }
            HttpJson.Response r = cm.call("PATCH", "/contractOffers/" + o.contractNegotiationId + "/reject?reason=EXPIRED", null, null);
            if (r.status() != 200) {
                log.warn("Expiry of offer {} failed at CM: {}", o.contractNegotiationId, r.status());
                failed.add(o.contractNegotiationId);
                continue;
            }
            tx.executeWithoutResult(s -> {
                Offer cur = offers.findById(o.contractNegotiationId).orElseThrow();
                cur.status = TERMINATED;
                cur.lastUpdatedDate = Times.now();
                offers.save(cur);
            });
            expired.add(o.contractNegotiationId);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("expired", expired);
        out.put("skipped", skipped);
        out.put("failed", failed);
        return out;
    }
}
