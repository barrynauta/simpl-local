package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.ApiException;
import org.springframework.http.HttpMethod;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** UIBE offer table, offer decision (D23) and offer expiry (D44, B.4). */
@Service
public class OfferService {

    public static final String CREATED = "CREATED";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECTED = "REJECTED";
    public static final String TERMINATED = "TERMINATED";

    private final OfferRepository offers;
    private final CmClient cm;
    private final UibeProperties props;

    public OfferService(OfferRepository offers, CmClient cm, UibeProperties props) {
        this.offers = offers;
        this.cm = cm;
        this.props = props;
    }

    /** D15: UTC local date-time without offset. */
    static String local(Instant i) {
        return i == null ? null : LocalDateTime.ofInstant(i, ZoneOffset.UTC).toString();
    }

    public static Map<String, Object> view(Offer o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractNegotiationId", o.contractNegotiationId);
        m.put("humanReadableData", o.humanReadable);
        m.put("contractData", null);
        m.put("hashData", o.hash);
        m.put("status", o.status);
        m.put("createdDate", local(o.createdDate));
        m.put("lastUpdatedDate", local(o.lastUpdatedDate));
        return m;
    }

    public String offerUrl(String nid) {
        return props.uiPublicUrl() + "/contractOffers/" + nid;
    }

    @Transactional
    public Map<String, Object> upsert(String nid, String humanReadable, String hash) {
        Instant now = Instant.now();
        Offer o = offers.findById(nid).orElse(null);
        if (o == null) {
            o = new Offer();
            o.contractNegotiationId = nid;
            o.status = CREATED;
            o.createdDate = now;
        }
        o.humanReadable = humanReadable;
        o.hash = hash;
        o.lastUpdatedDate = now;
        offers.save(o);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("offerStatus", o.status);
        out.put("offerUrl", offerUrl(nid));
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return offers.findAllByOrderByCreatedDateDesc().stream().map(OfferService::view).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Offer> find(String nid) {
        return offers.findById(nid);
    }

    public Map<String, Object> get(String nid) {
        return view(find(nid).orElseThrow(() -> ApiException.notFound(
                "Contract offer not found for contractNegotiationId = " + nid)));
    }

    /** D23: UIBE status first, then CM; on CM failure the offer stays CREATED. */
    public Map<String, Object> decide(String nid, String operation, String actor) {
        if (!"accept".equals(operation) && !"reject".equals(operation)) {
            throw ApiException.argumentTypeMismatch("Invalid request param operation");
        }
        Offer o = find(nid).orElseThrow(() -> ApiException.notFound(
                "Contract offer not found for contractNegotiationId = " + nid));
        if (!CREATED.equals(o.status)) {
            throw ApiException.offerNotPending("The offer is not pending: status " + o.status);
        }
        cm.require(HttpMethod.PATCH, cm.uri("/contractOffers/" + enc(nid) + "/" + operation).build(true).toUri(),
                null, actor);
        return setStatus(nid, "accept".equals(operation) ? ACCEPTED : REJECTED, CREATED);
    }

    @Transactional
    public Map<String, Object> setStatus(String nid, String status, String expected) {
        Offer o = offers.findById(nid).orElseThrow();
        if (expected != null && !expected.equals(o.status)) {
            throw ApiException.offerNotPending("The offer is not pending: status " + o.status);
        }
        o.status = status;
        o.lastUpdatedDate = Instant.now();
        try {
            return view(offers.saveAndFlush(o));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.offerNotPending("The offer changed concurrently");
        }
    }

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** One expiry run (D44, B.4). */
    public Map<String, Object> expire(Instant asOf, List<String> nids) {
        Instant cutoff = asOf.minus(props.offerExpiryTimeout());
        List<String> expired = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<Offer> candidates;
        if (nids == null) {
            candidates = offers.findAll();
        } else {
            candidates = new ArrayList<>();
            for (String nid : nids) {
                Optional<Offer> o = offers.findById(nid);
                if (o.isPresent()) {
                    candidates.add(o.get());
                } else {
                    skipped.add(nid);
                }
            }
        }
        for (Offer o : candidates) {
            if (!CREATED.equals(o.status) || !o.createdDate.isBefore(cutoff)) {
                skipped.add(o.contractNegotiationId);
                continue;
            }
            CmClient.Result r = cm.call(HttpMethod.PATCH, cm.uri("/contractOffers/" + enc(o.contractNegotiationId)
                    + "/reject").queryParam("reason", "EXPIRED").build(true).toUri(), null, null);
            if (r.status() == 200) {
                try {
                    setStatus(o.contractNegotiationId, TERMINATED, CREATED);
                    expired.add(o.contractNegotiationId);
                } catch (ApiException e) {
                    failed.add(o.contractNegotiationId);
                }
            } else {
                failed.add(o.contractNegotiationId);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("expired", expired);
        out.put("skipped", skipped);
        out.put("failed", failed);
        return out;
    }

}
