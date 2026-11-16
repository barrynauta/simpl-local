package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Control endpoints for tests (C.6). No authentication. */
@RestController
public class ControlController {

    private static final Set<String> FAULTS = Set.of("SIGNER:FAIL", "VERIFIER:FAIL", "VERIFIER:REVOKED",
            "WALLET:UNAVAILABLE", "WALLET:FULL", "WALLET:CORRUPTED", "DIRECTORY:NO_CONTACT");
    private static final Set<String> STATES = Set.of(Choreography.STARTED, Choreography.EXISTENCE_CHECKED,
            Choreography.OFFERED, Choreography.CONSUMER_SIGNED, Choreography.PROVIDER_SIGNED,
            Choreography.CONSUMER_VERIFIED, Choreography.FINALIZED, Choreography.TERMINATED);

    private final Choreography choreography;
    private final StubState state;
    private final ObjectMapper mapper;

    public ControlController(Choreography choreography, StubState state, ObjectMapper mapper) {
        this.choreography = choreography;
        this.state = state;
        this.mapper = mapper;
    }

    private static String hex8() {
        byte[] b = new byte[4];
        ThreadLocalRandom.current().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static String text(JsonNode body, String field, String fallback) {
        JsonNode v = body.get(field);
        return v != null && v.isTextual() && !v.asText().isBlank() ? v.asText() : fallback;
    }

    @PostMapping("/_control/negotiations")
    public ResponseEntity<Object> start(@RequestBody JsonNode body) {
        String providerId = text(body, "providerId", null);
        String consumerId = text(body, "consumerId", null);
        String assetId = text(body, "assetId", null);
        if (providerId == null || consumerId == null || assetId == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "providerId, consumerId and assetId are required"));
        }
        String stopAfter = text(body, "stopAfter", null);
        if (stopAfter != null && !STATES.contains(stopAfter)) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Unknown stopAfter state"));
        }
        String nid = text(body, "contractNegotiationId", UUID.randomUUID().toString());
        if (choreography.get(nid) != null) {
            return ResponseEntity.status(409).body(Map.of("detail", "Negotiation already exists"));
        }
        String aid = text(body, "contractAgreementId", UUID.randomUUID().toString());
        String did = text(body, "contractDefinitionId", "def-" + hex8());
        String offerId = text(body, "contractOfferId", "offer-" + hex8());
        JsonNode faults = body.get("faults");
        if (faults != null && faults.isArray()) {
            for (JsonNode f : faults) {
                String key = f.path("service").asText() + ":" + f.path("mode").asText();
                if (!FAULTS.contains(key)) {
                    return ResponseEntity.badRequest().body(Map.of("detail", "Unknown fault " + key));
                }
                state.addFault(nid, f.path("service").asText(), f.path("mode").asText());
            }
        }
        if (body.has("consumerVcOverride")) {
            state.setConsumerVcOverride(nid, body.get("consumerVcOverride"));
        }
        if (body.has("providerVcOverride")) {
            state.setProviderVcOverride(nid, body.get("providerVcOverride"));
        }
        Negotiation n = choreography.start(new Negotiation(nid, aid, did, offerId, providerId, consumerId, assetId,
                stopAfter));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractNegotiationId", nid);
        out.put("contractAgreementId", aid);
        out.put("contractDefinitionId", did);
        out.put("contractOfferId", offerId);
        out.put("state", n.state);
        return ResponseEntity.status(201).body(out);
    }

    @GetMapping("/_control/negotiations/{nid}")
    public ResponseEntity<Object> get(@PathVariable String nid) {
        Negotiation n = choreography.get(nid);
        if (n == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Unknown negotiation"));
        }
        return ResponseEntity.ok(choreography.describe(n));
    }

    @PostMapping("/_control/negotiations/{nid}/advance")
    public ResponseEntity<Object> advance(@PathVariable String nid) {
        Negotiation n = choreography.get(nid);
        if (n == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Unknown negotiation"));
        }
        n.advance();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/_control/faults")
    public ResponseEntity<Object> fault(@RequestBody JsonNode body) {
        String nid = text(body, "contractNegotiationId", null);
        String service = text(body, "service", null);
        String mode = text(body, "mode", null);
        if (nid == null || service == null || mode == null || !FAULTS.contains(service + ":" + mode)) {
            return ResponseEntity.badRequest().body(Map.of("detail", "contractNegotiationId and a known service:mode "
                    + "are required"));
        }
        state.addFault(nid, service, mode);
        return ResponseEntity.status(201).body(Map.of("contractNegotiationId", nid, "service", service, "mode", mode));
    }

    @DeleteMapping("/_control/faults")
    public ResponseEntity<Object> clearFaults(@RequestParam("contractNegotiationId") String nid) {
        state.clearFaults(nid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/_control/negotiations/{nid}/overrides")
    public ResponseEntity<Object> overrides(@PathVariable String nid, @RequestBody JsonNode body) {
        if (body.has("consumerVcOverride")) {
            state.setConsumerVcOverride(nid, body.get("consumerVcOverride"));
        }
        if (body.has("providerVcOverride")) {
            state.setProviderVcOverride(nid, body.get("providerVcOverride"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractNegotiationId", nid);
        out.put("consumerVcOverride", state.consumerVcOverride(nid));
        out.put("providerVcOverride", state.providerVcOverride(nid));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/_control/calls")
    public List<StubState.Call> calls(@RequestParam(value = "contractNegotiationId", required = false) String nid) {
        return state.calls(nid);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
