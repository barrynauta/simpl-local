package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.stubs.crypto.DataIntegrity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Signer + VC issuer and verifier (C.3). */
@RestController
public class IssuerController {

    private final Participants participants;
    private final StubState state;
    private final ObjectMapper mapper;

    public IssuerController(Participants participants, StubState state, ObjectMapper mapper) {
        this.participants = participants;
        this.state = state;
        this.mapper = mapper;
    }

    @PostMapping("/vc-issuer/v1/credentials/issue")
    public ResponseEntity<Object> issue(@RequestBody JsonNode body,
                                        @RequestHeader(value = "X-Contract-Negotiation-Id", required = false)
                                        String nidHeader) {
        String nid = nidHeader != null ? nidHeader : body.at("/credentialSubject/contractNegotiationId").asText(null);
        if (state.hasFault(nid, "SIGNER:FAIL")) {
            return error(500, "Signer failure");
        }
        Participants.Participant p = participants.get(body.path("participantId").asText(null));
        if (p == null) {
            return error(400, "Unknown participant");
        }
        JsonNode subject = body.get("credentialSubject");
        if (subject == null || !subject.isObject()) {
            return error(400, "credentialSubject is required");
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String validFrom = body.path("validFrom").isTextual() ? body.get("validFrom").asText() : now.toString();
        String validUntil = body.path("validUntil").isTextual() ? body.get("validUntil").asText()
                : now.plus(365, ChronoUnit.DAYS).toString();
        ObjectNode vc = mapper.createObjectNode();
        vc.set("@context", mapper.valueToTree(List.of("https://www.w3.org/ns/credentials/v2")));
        vc.put("id", "urn:uuid:" + UUID.randomUUID());
        vc.set("type", mapper.valueToTree(List.of("VerifiableCredential", "UsageContractCredential")));
        vc.put("issuer", p.did());
        vc.put("validFrom", validFrom);
        vc.put("validUntil", validUntil);
        vc.set("credentialSubject", subject);
        return ResponseEntity.status(201).body(DataIntegrity.sign(vc, p.key(), now.toString()));
    }

    @PostMapping("/vc-issuer/v1/credentials/verify")
    public ResponseEntity<Object> verify(@RequestBody JsonNode body,
                                         @RequestHeader(value = "X-Contract-Negotiation-Id", required = false)
                                         String nidHeader) {
        JsonNode vc = body.get("credential");
        String nid = nidHeader;
        if (nid == null && vc != null) {
            nid = vc.at("/credentialSubject/contractNegotiationId").asText(null);
        }
        if (state.hasFault(nid, "VERIFIER:FAIL")) {
            return error(500, "Verifier failure");
        }
        String problem = check(vc, nid);
        ObjectNode out = mapper.createObjectNode();
        out.put("verified", problem == null);
        Participants.Participant issuer = vc != null && vc.path("issuer").isTextual()
                ? participants.byDid(vc.get("issuer").asText()) : null;
        if (issuer != null) {
            out.put("issuerParticipantId", issuer.id());
        } else {
            out.putNull("issuerParticipantId");
        }
        ArrayNode problems = out.putArray("problems");
        if (problem != null) {
            problems.add(problem);
        }
        return ResponseEntity.ok(out);
    }

    private String check(JsonNode vc, String nid) {
        if (vc == null || !vc.isObject()) {
            return "MALFORMED";
        }
        for (String f : List.of("@context", "type", "issuer", "credentialSubject", "proof")) {
            if (vc.get(f) == null || vc.get(f).isNull()) {
                return "MALFORMED";
            }
        }
        JsonNode proof = vc.get("proof");
        if (!proof.isObject() || !DataIntegrity.PROOF_TYPE.equals(proof.path("type").asText())
                || !DataIntegrity.CRYPTOSUITE.equals(proof.path("cryptosuite").asText())) {
            return "MALFORMED";
        }
        String issuer = vc.get("issuer").asText("");
        String vm = proof.path("verificationMethod").asText("");
        int hash = vm.indexOf('#');
        if (hash < 0 || !vm.substring(0, hash).equals(issuer)) {
            return "MALFORMED";
        }
        if (!DataIntegrity.verifySignature(vc)) {
            return "INVALID_SIGNATURE";
        }
        Participants.Participant p = participants.byDid(issuer);
        if (p == null || !p.trusted()) {
            return "UNKNOWN_ISSUER";
        }
        if (state.hasFault(nid, "VERIFIER:REVOKED")) {
            return "REVOKED_ISSUER";
        }
        return null;
    }

    private ResponseEntity<Object> error(int status, String detail) {
        return ResponseEntity.status(status).header("Content-Type", "application/problem+json")
                .body(Map.of("type", "urn:problem-type:simpl:stubError", "title", "Stub error", "status", status,
                        "detail", detail));
    }
}
