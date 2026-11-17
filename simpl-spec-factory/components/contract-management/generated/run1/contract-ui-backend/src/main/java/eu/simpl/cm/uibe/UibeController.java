package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.common.web.ApiException;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.common.web.Ids;
import eu.simpl.cm.common.web.JsonBody;
import eu.simpl.cm.common.web.Problems;
import eu.simpl.cm.common.web.Times;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** UIBE operations of the OpenAPI and Appendix B, under /contract-ui/v1. */
@RestController
public class UibeController {
    private static final String JSON = "application/json";
    private static final String CT = "Content-Type";
    private final OfferService offers;
    private final CmClient cm;
    private final UibeSettings settings;
    private final ObjectMapper mapper;

    public UibeController(OfferService offers, CmClient cm, UibeSettings settings, ObjectMapper mapper) {
        this.offers = offers;
        this.cm = cm;
        this.settings = settings;
        this.mapper = mapper;
    }

    private UserContext user(Jwt jwt) {
        return UserContext.of(jwt, settings);
    }

    // ---- machine operations (API key)
    @PostMapping(value = "/contractOffers/{nid}", produces = JSON)
    public ResponseEntity<Map<String, Object>> createOffer(@PathVariable String nid, @RequestBody(required = false) byte[] body,
                                                           @RequestHeader(value = CT, required = false) String ct) {
        Ids.pathId("contractNegotiationId", nid);
        return ResponseEntity.status(201).body(offers.upsert(nid, JsonBody.readObject(mapper, body, ct, true)));
    }

    @PostMapping(value = "/admin/offerExpiry/run", produces = JSON)
    public Map<String, Object> expiry(@RequestBody(required = false) byte[] body,
                                      @RequestHeader(value = CT, required = false) String ct) {
        JsonNode b = JsonBody.readObject(mapper, body, ct, false);
        Instant asOf = Instant.now();
        List<String> ids = null;
        if (b != null) {
            String asOfText = JsonBody.text(b, "asOf");
            if (asOfText != null) {
                try {
                    asOf = Times.parse(asOfText);
                } catch (Exception e) {
                    throw ApiException.validation("asOf must be an RFC 3339 date-time");
                }
            }
            JsonNode list = b.get("contractNegotiationIds");
            if (list != null && !list.isNull()) {
                if (!list.isArray()) {
                    throw ApiException.validation("contractNegotiationIds must be an array");
                }
                ids = new ArrayList<>();
                for (JsonNode n : list) {
                    if (!n.isTextual()) {
                        throw ApiException.validation("contractNegotiationIds must contain strings");
                    }
                    ids.add(n.asText());
                }
            }
        }
        return offers.expire(asOf, ids);
    }

    // ---- user operations (Bearer token, contract_manager)
    @GetMapping(value = "/contractOffers", produces = JSON)
    public List<Map<String, Object>> allOffers(@AuthenticationPrincipal Jwt jwt) {
        user(jwt);
        return offers.all();
    }

    @GetMapping(value = "/contractOffers/{nid}", produces = JSON)
    public Map<String, Object> offer(@AuthenticationPrincipal Jwt jwt, @PathVariable String nid) {
        user(jwt);
        return offers.one(nid);
    }

    @PatchMapping(value = "/contractOffers/{nid}/{operation}", produces = JSON)
    public Map<String, Object> decide(@AuthenticationPrincipal Jwt jwt, @PathVariable String nid, @PathVariable String operation) {
        UserContext u = user(jwt);
        return offers.decide(nid, operation, u.username());
    }

    @GetMapping(value = "/contracts", produces = JSON)
    public ResponseEntity<JsonNode> contracts(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam(required = false) String contractNegotiationId,
                                              @RequestParam(required = false) String resourceName,
                                              @RequestParam(required = false) String providerName,
                                              @RequestParam(required = false) String consumerName,
                                              @RequestParam(required = false) String counterpartyName,
                                              @RequestParam(required = false) String status) {
        user(jwt);
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/contracts");
        add(b, "contractNegotiationId", contractNegotiationId);
        add(b, "resourceName", resourceName);
        add(b, "providerName", providerName);
        add(b, "consumerName", consumerName);
        add(b, "status", status);
        HttpJson.Response r = cm.call("GET", b.encode().build().toUriString(), null, null);
        JsonNode body = forward(r);
        if (counterpartyName != null && !counterpartyName.isEmpty() && body.path("items").isArray()) {
            String needle = counterpartyName.toLowerCase(Locale.ROOT);
            ArrayNode kept = mapper.createArrayNode();
            for (JsonNode item : body.get("items")) {
                String cp = "CONSUMER".equals(item.path("partyRole").asText()) ? item.path("providerName").asText(null)
                        : item.path("consumerName").asText(null);
                if (cp != null && cp.toLowerCase(Locale.ROOT).contains(needle)) {
                    kept.add(item);
                }
            }
            ((ObjectNode) body).set("items", kept);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/contracts/{nid}", produces = JSON)
    public JsonNode contract(@AuthenticationPrincipal Jwt jwt, @PathVariable String nid) {
        UserContext u = user(jwt);
        Ids.pathId("contractNegotiationId", nid);
        return enrich(forward(cm.call("GET", "/contracts/" + nid, null, null)), nid, u);
    }

    @PostMapping(value = "/contracts/{nid}/lifecycleActions", produces = JSON)
    public JsonNode lifecycle(@AuthenticationPrincipal Jwt jwt, @PathVariable String nid,
                              @RequestBody(required = false) byte[] body, @RequestHeader(value = CT, required = false) String ct) {
        UserContext u = user(jwt);
        Ids.pathId("contractNegotiationId", nid);
        JsonNode b = JsonBody.readObject(mapper, body, ct, true);
        ObjectNode out = mapper.createObjectNode();
        out.set("action", b.get("action"));
        out.set("reason", b.get("reason"));
        out.put("actor", u.username());
        return enrich(forward(cm.call("POST", "/contracts/" + nid + "/lifecycleActions", out, u.username())), nid, u);
    }

    private static void add(UriComponentsBuilder b, String name, String value) {
        if (value != null) {
            b.queryParam(name, value);
        }
    }

    private JsonNode forward(HttpJson.Response r) {
        if (!r.ok()) {
            throw offers.passThrough(r);
        }
        JsonNode body = r.json(mapper);
        if (body == null) {
            throw new ApiException(500, Problems.INTERNAL_SERVER_ERROR, "Invalid answer from the contract manager");
        }
        return body;
    }

    /** B.2: offer fields, role filter on UPDATE_CONSENT and the consent manager link. */
    private JsonNode enrich(JsonNode details, String nid, UserContext u) {
        ObjectNode d = (ObjectNode) details;
        Offer o = offers.findOrNull(nid);
        d.put("offerStatus", o == null ? null : o.status);
        d.put("humanReadableData", o == null ? null : o.humanReadable);
        d.put("hashData", o == null ? null : o.hash);
        ArrayNode actions = mapper.createArrayNode();
        boolean consent = false;
        for (JsonNode a : d.path("allowedActions")) {
            if ("UPDATE_CONSENT".equals(a.asText())) {
                if (!u.has(UserContext.CONSENT_MANAGER)) {
                    continue;
                }
                consent = true;
            }
            actions.add(a);
        }
        d.set("allowedActions", actions);
        if (consent) {
            d.put("consentManagerUrl", settings.consentManagerUrl + "?contractNegotiationId=" + nid);
        }
        return d;
    }
}
