package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static eu.simpl.cm.stubs.StubController.problem;

/** C.6 control endpoints (tests only, no auth). */
@RestController
@RequestMapping("/_control")
public class ControlController {
    private final StubState state;
    private final Choreography choreography;

    public ControlController(StubState state, Choreography choreography) {
        this.state = state;
        this.choreography = choreography;
    }

    private static String hex8() {
        byte[] b = new byte[4];
        ThreadLocalRandom.current().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v != null && v.isTextual() && !v.asText().isBlank() ? v.asText() : null;
    }

    @PostMapping("/catalogue/assets")
    public ResponseEntity<Object> addAsset(@RequestBody JsonNode body) {
        if (!body.isObject() || text(body, "assetId") == null) {
            return problem(400, "assetId is required");
        }
        ObjectNode a = ((ObjectNode) body).deepCopy();
        String assetId = a.get("assetId").asText();
        if (!a.hasNonNull("resourceDescriptionId")) {
            a.put("resourceDescriptionId", "rd-" + assetId);
        }
        if (!a.hasNonNull("consentRequired")) {
            a.put("consentRequired", false);
        }
        if (!a.hasNonNull("resourceName")) {
            a.put("resourceName", assetId);
        }
        if (!a.has("description")) {
            a.put("description", "");
        }
        if (!a.hasNonNull("providerId")) {
            a.put("providerId", "provider-01");
        }
        if (!a.hasNonNull("providerName")) {
            StubState.Participant p = state.directory.get(a.get("providerId").asText());
            a.put("providerName", p == null ? a.get("providerId").asText() : p.name());
        }
        if (!a.has("policies")) {
            a.putArray("policies");
        }
        if (!a.has("contractClauses")) {
            a.putArray("contractClauses");
        }
        state.assets.put(assetId, a);
        return ResponseEntity.status(201).body(a);
    }

    @PostMapping("/negotiations")
    public ResponseEntity<Object> start(@RequestBody JsonNode body) {
        if (!body.isObject() || text(body, "providerId") == null || text(body, "consumerId") == null
                || text(body, "assetId") == null) {
            return problem(400, "providerId, consumerId and assetId are required");
        }
        Negotiation n = new Negotiation();
        n.contractNegotiationId = text(body, "contractNegotiationId") != null ? text(body, "contractNegotiationId") : UUID.randomUUID().toString();
        n.contractAgreementId = text(body, "contractAgreementId") != null ? text(body, "contractAgreementId") : UUID.randomUUID().toString();
        n.contractDefinitionId = text(body, "contractDefinitionId") != null ? text(body, "contractDefinitionId") : "def-" + hex8();
        n.contractOfferId = text(body, "contractOfferId") != null ? text(body, "contractOfferId") : "offer-" + hex8();
        n.providerId = text(body, "providerId");
        n.consumerId = text(body, "consumerId");
        n.assetId = text(body, "assetId");
        n.stopAfter = text(body, "stopAfter");
        if (n.stopAfter != null && !Negotiation.STATES.contains(n.stopAfter)) {
            return problem(400, "Unknown stopAfter state");
        }
        if (!state.cms.containsKey(n.providerId) || !state.cms.containsKey(n.consumerId)) {
            return problem(400, "providerId and consumerId must be parties with a contract manager");
        }
        if (state.negotiations.containsKey(n.contractNegotiationId)) {
            return problem(409, "Negotiation already exists");
        }
        JsonNode faults = body.get("faults");
        if (faults != null && faults.isArray()) {
            for (JsonNode f : faults) {
                String key = f.path("service").asText() + ":" + f.path("mode").asText();
                if (!StubState.FAULTS.contains(key)) {
                    return problem(400, "Unknown fault " + key);
                }
            }
            for (JsonNode f : faults) {
                state.addFault(n.contractNegotiationId, f.path("service").asText() + ":" + f.path("mode").asText());
            }
        }
        if (body.path("consumerVcOverride").isObject()) {
            state.consumerVcOverride.put(n.contractNegotiationId, (ObjectNode) body.get("consumerVcOverride"));
        }
        if (body.path("providerVcOverride").isObject()) {
            state.providerVcOverride.put(n.contractNegotiationId, (ObjectNode) body.get("providerVcOverride"));
        }
        n.enter("STARTED");
        state.negotiations.put(n.contractNegotiationId, n);
        choreography.start(n);
        ObjectNode out = state.mapper.createObjectNode();
        out.put("contractNegotiationId", n.contractNegotiationId);
        out.put("contractAgreementId", n.contractAgreementId);
        out.put("contractDefinitionId", n.contractDefinitionId);
        out.put("contractOfferId", n.contractOfferId);
        out.put("state", n.state);
        return ResponseEntity.status(201).body(out);
    }

    @GetMapping("/negotiations/{nid}")
    public ResponseEntity<Object> get(@PathVariable String nid) {
        Negotiation n = state.negotiations.get(nid);
        if (n == null) {
            return problem(404, "Unknown negotiation");
        }
        ObjectNode out = state.mapper.createObjectNode();
        out.put("contractNegotiationId", n.contractNegotiationId);
        out.put("contractAgreementId", n.contractAgreementId);
        out.put("contractDefinitionId", n.contractDefinitionId);
        out.put("contractOfferId", n.contractOfferId);
        out.put("state", n.state);
        out.put("terminationReason", n.terminationReason);
        ArrayNode steps = out.putArray("steps");
        n.steps.forEach(s -> steps.addObject().put("state", s.state()).put("at", s.at().toString()));
        ArrayNode cbs = out.putArray("callbacks");
        state.callbacks(nid).forEach(c -> {
            ObjectNode o = cbs.addObject().put("participantId", c.participantId()).put("endpoint", c.endpoint());
            if (c.value() == null) {
                o.putNull("value");
            } else {
                o.put("value", c.value());
            }
            o.put("at", c.at().toString());
        });
        return ResponseEntity.ok(out);
    }

    @PostMapping("/negotiations/{nid}/advance")
    public ResponseEntity<Object> advance(@PathVariable String nid) {
        Negotiation n = state.negotiations.get(nid);
        if (n == null) {
            return problem(404, "Unknown negotiation");
        }
        n.advance.release();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/faults")
    public ResponseEntity<Object> fault(@RequestBody JsonNode body) {
        String nid = text(body, "contractNegotiationId");
        String key = body.path("service").asText() + ":" + body.path("mode").asText();
        if (nid == null || !StubState.FAULTS.contains(key)) {
            return problem(400, "contractNegotiationId and a known service:mode are required");
        }
        state.addFault(nid, key);
        return ResponseEntity.status(201).body(Map.of("contractNegotiationId", nid, "service", body.path("service").asText(),
                "mode", body.path("mode").asText()));
    }

    @DeleteMapping("/faults")
    public ResponseEntity<Object> clearFaults(@RequestParam String contractNegotiationId) {
        state.faults.remove(contractNegotiationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/negotiations/{nid}/overrides")
    public ResponseEntity<Object> overrides(@PathVariable String nid, @RequestBody JsonNode body) {
        if (body.path("consumerVcOverride").isObject()) {
            state.consumerVcOverride.put(nid, (ObjectNode) body.get("consumerVcOverride"));
        } else if (body.has("consumerVcOverride") && body.get("consumerVcOverride").isNull()) {
            state.consumerVcOverride.remove(nid);
        }
        if (body.path("providerVcOverride").isObject()) {
            state.providerVcOverride.put(nid, (ObjectNode) body.get("providerVcOverride"));
        } else if (body.has("providerVcOverride") && body.get("providerVcOverride").isNull()) {
            state.providerVcOverride.remove(nid);
        }
        ObjectNode out = state.mapper.createObjectNode();
        out.put("contractNegotiationId", nid);
        out.set("consumerVcOverride", state.consumerVcOverride.get(nid));
        out.set("providerVcOverride", state.providerVcOverride.get(nid));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/calls")
    public Object calls(@RequestParam(required = false) String contractNegotiationId) {
        return state.calls(contractNegotiationId).stream().map(c -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("at", c.at().toString());
            m.put("method", c.method());
            m.put("path", c.path());
            m.put("status", c.status());
            m.put("body", c.body());
            return m;
        }).toList();
    }
}
