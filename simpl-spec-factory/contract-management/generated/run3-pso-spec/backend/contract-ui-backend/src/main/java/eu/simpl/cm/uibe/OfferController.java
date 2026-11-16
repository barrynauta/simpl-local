package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.ApiException;
import eu.simpl.cm.common.Ids;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The UIBE OpenAPI offer operations and the expiry trigger (D8, D9, D23, B.4, B.5). */
@RestController
public class OfferController {

    private final OfferService offers;

    public OfferController(OfferService offers) {
        this.offers = offers;
    }

    @PostMapping(value = "/contractOffers/{nid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> create(@PathVariable("nid") String nid,
                                                      @RequestBody(required = false) JsonNode body) {
        Ids.requirePathId("contractNegotiationId", nid);
        if (body == null || !body.isObject() || !body.path("humanReadable").isTextual()
                || !body.path("hash").isTextual() || body.get("humanReadable").asText().isBlank()
                || body.get("hash").asText().isBlank()) {
            throw ApiException.validation("humanReadable and hash are required");
        }
        return ResponseEntity.status(201).body(offers.upsert(nid, body.get("humanReadable").asText(),
                body.get("hash").asText()));
    }

    @GetMapping(value = "/contractOffers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> list() {
        return offers.list();
    }

    @GetMapping(value = "/contractOffers/{nid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> get(@PathVariable("nid") String nid) {
        Ids.requirePathId("contractNegotiationId", nid);
        return offers.get(nid);
    }

    @PatchMapping(value = "/contractOffers/{nid}/{operation}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> decide(@PathVariable("nid") String nid, @PathVariable("operation") String operation,
                                      Authentication auth) {
        Ids.requirePathId("contractNegotiationId", nid);
        return offers.decide(nid, operation, auth.getName());
    }

    @PostMapping(value = "/admin/offerExpiry/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> expiry(@RequestBody(required = false) JsonNode body) {
        Instant asOf = Instant.now();
        List<String> nids = null;
        if (body != null && !body.isNull()) {
            if (!body.isObject()) {
                throw ApiException.validation("Body must be a JSON object");
            }
            JsonNode a = body.get("asOf");
            if (a != null && !a.isNull()) {
                try {
                    asOf = java.time.OffsetDateTime.parse(a.asText()).toInstant();
                } catch (Exception e) {
                    throw ApiException.validation("asOf must be an RFC 3339 date-time");
                }
            }
            JsonNode ids = body.get("contractNegotiationIds");
            if (ids != null && !ids.isNull()) {
                if (!ids.isArray()) {
                    throw ApiException.validation("contractNegotiationIds must be an array");
                }
                nids = new ArrayList<>();
                for (JsonNode id : ids) {
                    nids.add(id.asText());
                }
            }
        }
        return offers.expire(asOf, nids);
    }
}
