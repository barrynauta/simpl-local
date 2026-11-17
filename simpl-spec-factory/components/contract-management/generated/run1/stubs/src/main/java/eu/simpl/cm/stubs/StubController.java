package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.common.proof.DataIntegrity;
import eu.simpl.cm.common.proof.DidKey;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** C.1 connector endpoints, C.3 issuer and verifier, C.4 wallet, C.5 catalogue, directory, consent page. */
@RestController
public class StubController {
    private static final String NID = "X-Contract-Negotiation-Id";
    private final StubState state;
    private final CmClient cm;

    public StubController(StubState state, CmClient cm) {
        this.state = state;
        this.cm = cm;
    }

    static ResponseEntity<Object> problem(int status, String detail) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Map.of("type", "urn:problem-type:stub:error", "title", "Stub error", "status", status, "detail", detail));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    // ---- C.1 connector callbacks
    private boolean authorised(String key) {
        return state.edcApiKey.equals(key);
    }

    @PostMapping("/edc/{p}/{endpoint:isPresent|isValid|signed}/{nid}/{value}")
    public ResponseEntity<Object> callback(@PathVariable String p, @PathVariable String endpoint, @PathVariable String nid,
                                           @PathVariable String value,
                                           @RequestHeader(value = "X-Api-Key", required = false) String key) {
        if (!authorised(key)) {
            return problem(401, "Invalid API Key");
        }
        if (!value.equals("true") && !value.equals("false")) {
            return problem(400, "value must be true or false");
        }
        state.addCallback(nid, new StubState.Callback(p, endpoint, Boolean.valueOf(value), Instant.now()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/edc/{p}/vcExchange/{nid}/request")
    public ResponseEntity<Object> vcExchange(@PathVariable String p, @PathVariable String nid,
                                             @RequestHeader(value = "X-Api-Key", required = false) String key) {
        if (!authorised(key)) {
            return problem(401, "Invalid API Key");
        }
        state.addCallback(nid, new StubState.Callback(p, "vcExchange", null, Instant.now()));
        String other = state.counterparty(nid, p);
        // The provider asks for the consumer VC and the consumer for the provider VC.
        boolean callerIsProvider = isProviderOf(nid, p);
        ObjectNode override = callerIsProvider ? state.consumerVcOverride.get(nid) : state.providerVcOverride.get(nid);
        if (override != null) {
            if (override.path("notFound").asBoolean(false)) {
                return problem(404, "No VC for " + nid);
            }
            return ResponseEntity.ok(override);
        }
        CmClient.Result r = cm.call(other, "GET", "/verifications/" + nid + "/vc", null, nid);
        if (r.status() == 200) {
            return ResponseEntity.ok(r.body());
        }
        if (r.status() == 404) {
            return problem(404, "No VC for " + nid);
        }
        return problem(502, "Counterparty connector did not answer");
    }

    private boolean isProviderOf(String nid, String p) {
        Negotiation n = state.negotiations.get(nid);
        if (n != null) {
            return p.equals(n.providerId);
        }
        return p.startsWith("provider");
    }

    // ---- C.3 issuer and verifier
    @PostMapping("/vc-issuer/v1/credentials/issue")
    public ResponseEntity<Object> issue(@RequestBody JsonNode body, @RequestHeader(value = NID, required = false) String nidHeader) {
        JsonNode subject = body.get("credentialSubject");
        String nid = nidHeader != null ? nidHeader : subject == null ? null : subject.path("contractNegotiationId").asText(null);
        if (state.hasFault(nid, "SIGNER:FAIL")) {
            return problem(500, "Signer failure");
        }
        StubState.Participant participant = state.directory.get(body.path("participantId").asText(""));
        if (participant == null) {
            return problem(400, "Unknown participant");
        }
        if (subject == null || !subject.isObject()) {
            return problem(400, "credentialSubject is required");
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String validFrom = body.hasNonNull("validFrom") ? body.get("validFrom").asText() : now.toString();
        String validUntil = body.hasNonNull("validUntil") ? body.get("validUntil").asText() : now.plus(365, ChronoUnit.DAYS).toString();
        ObjectNode vc = state.mapper.createObjectNode();
        vc.putArray("@context").add("https://www.w3.org/ns/credentials/v2");
        vc.put("id", "urn:uuid:" + UUID.randomUUID());
        vc.putArray("type").add("VerifiableCredential").add("UsageContractCredential");
        vc.put("issuer", participant.did());
        vc.put("validFrom", validFrom);
        vc.put("validUntil", validUntil);
        vc.set("credentialSubject", subject.deepCopy());
        ObjectNode signed = DataIntegrity.sign(vc, participant.seed(), now.toString());
        return ResponseEntity.status(201).body(signed);
    }

    @PostMapping("/vc-issuer/v1/credentials/verify")
    public ResponseEntity<Object> verify(@RequestBody JsonNode body, @RequestHeader(value = NID, required = false) String nidHeader) {
        JsonNode vc = body.get("credential");
        String nid = nidHeader != null ? nidHeader
                : vc == null ? null : vc.path("credentialSubject").path("contractNegotiationId").asText(null);
        if (state.hasFault(nid, "VERIFIER:FAIL")) {
            return problem(500, "Verifier failure");
        }
        String problemCode = verifyProblem(vc, nid);
        String issuer = vc != null && vc.path("issuer").isTextual() ? vc.get("issuer").asText() : null;
        StubState.Participant p = issuer == null ? null : state.byDid(issuer);
        ObjectNode out = state.mapper.createObjectNode();
        out.put("verified", problemCode == null);
        out.put("issuerParticipantId", p == null ? null : p.participantId());
        ArrayNode problems = out.putArray("problems");
        if (problemCode != null) {
            problems.add(problemCode);
        }
        return ResponseEntity.ok(out);
    }

    private String verifyProblem(JsonNode vc, String nid) {
        if (vc == null || !vc.isObject() || !vc.has("@context") || !vc.has("type") || !vc.path("issuer").isTextual()
                || !vc.path("credentialSubject").isObject() || !vc.path("proof").isObject()) {
            return "MALFORMED";
        }
        JsonNode proof = vc.get("proof");
        if (!DataIntegrity.PROOF_TYPE.equals(proof.path("type").asText()) || !DataIntegrity.CRYPTOSUITE.equals(proof.path("cryptosuite").asText())
                || !proof.path("verificationMethod").isTextual()
                || !DidKey.didOf(proof.get("verificationMethod").asText()).equals(vc.get("issuer").asText())) {
            return "MALFORMED";
        }
        DataIntegrity.Outcome outcome = DataIntegrity.verify(vc);
        if (outcome == DataIntegrity.Outcome.MALFORMED) {
            return "MALFORMED";
        }
        if (outcome == DataIntegrity.Outcome.INVALID_SIGNATURE) {
            return "INVALID_SIGNATURE";
        }
        StubState.Participant p = state.byDid(vc.get("issuer").asText());
        if (p == null || !state.trusted(p.participantId())) {
            return "UNKNOWN_ISSUER";
        }
        if (state.hasFault(nid, "VERIFIER:REVOKED")) {
            return "REVOKED_ISSUER";
        }
        return null;
    }

    // ---- C.4 wallet
    @PutMapping("/wallet/v1/{p}/credentials/{credentialId}")
    public ResponseEntity<Object> store(@PathVariable String p, @PathVariable String credentialId, @RequestBody JsonNode vc,
                                        @RequestHeader(value = NID, required = false) String nidHeader) {
        String nid = nidHeader != null ? nidHeader : vc.path("credentialSubject").path("contractNegotiationId").asText(null);
        if (state.hasFault(nid, "WALLET:UNAVAILABLE")) {
            return problem(503, "Secure storage location unavailable");
        }
        if (state.hasFault(nid, "WALLET:FULL")) {
            return problem(507, "Secure storage location is full");
        }
        if (state.hasFault(nid, "WALLET:CORRUPTED")) {
            return problem(500, "Secure storage location is corrupted");
        }
        if (!vc.isObject()) {
            return problem(400, "A VC object is expected");
        }
        state.wallets.computeIfAbsent(p, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(credentialId, (ObjectNode) vc);
        return ResponseEntity.status(201).body(Map.of("credentialId", credentialId, "participantId", p));
    }

    @GetMapping("/wallet/v1/{p}/credentials")
    public List<ObjectNode> wallet(@PathVariable String p, @RequestParam(required = false) String contractNegotiationId) {
        return state.wallets.getOrDefault(p, Map.of()).values().stream()
                .filter(vc -> contractNegotiationId == null
                        || contractNegotiationId.equals(vc.path("credentialSubject").path("contractNegotiationId").asText()))
                .toList();
    }

    // ---- C.5 catalogue, directory, consent page
    @GetMapping("/catalogue/v1/assets/{assetId}")
    public ResponseEntity<Object> asset(@PathVariable String assetId) {
        ObjectNode a = state.assets.get(assetId);
        return a == null ? problem(404, "Asset not found") : ResponseEntity.ok(a);
    }

    @GetMapping("/directory/v1/participants/{p}")
    public ResponseEntity<Object> participant(@PathVariable String p, @RequestHeader(value = NID, required = false) String nid) {
        StubState.Participant participant = state.directory.get(p);
        if (participant == null) {
            return problem(404, "Participant not found");
        }
        ObjectNode out = state.mapper.createObjectNode();
        out.put("participantId", participant.participantId());
        out.put("name", participant.name());
        out.put("did", participant.did());
        out.put("contactEmail", state.hasFault(nid, "DIRECTORY:NO_CONTACT") ? null : participant.contactEmail());
        return ResponseEntity.ok(out);
    }

    @GetMapping(value = "/consent", produces = MediaType.TEXT_HTML_VALUE)
    public String consent(@RequestParam(required = false) String contractNegotiationId) {
        String nid = contractNegotiationId == null ? "" : contractNegotiationId.replaceAll("[^A-Za-z0-9:-]", "");
        return "<!DOCTYPE html><html><head><title>Consent manager stub</title></head><body><h1>Consent manager stub</h1>"
                + "<p>Contract negotiation: <span id=\"nid\">" + nid + "</span></p></body></html>";
    }
}
