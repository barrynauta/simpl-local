package eu.simpl.catalogue.cat.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.crypto.SignatureVerifier;
import eu.simpl.catalogue.cat.service.VcChecks;
import eu.simpl.catalogue.cat.store.RdRepository;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Json;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** POST /verification (D25). Nothing is stored. */
@RestController
public class VerificationController {
    private final VcChecks checks;
    private final RdRepository repo;

    public VerificationController(VcChecks checks, RdRepository repo) {
        this.checks = checks;
        this.repo = repo;
    }

    @PostMapping(value = "/verification", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> verify(@RequestBody(required = false) String body,
                                         @RequestParam(required = false) String verifySemantics,
                                         @RequestParam(required = false) String verifySchema,
                                         @RequestParam(required = false) String verifyVPSignature,
                                         @RequestParam(required = false) String verifyVCSignature) {
        Caller.current();
        boolean semantics = Params.bool(verifySemantics, "verifySemantics", true);
        boolean schema = Params.bool(verifySchema, "verifySchema", true);
        Params.bool(verifyVPSignature, "verifyVPSignature", true);
        boolean vcSig = Params.bool(verifyVCSignature, "verifyVCSignature", true);
        JsonNode vc = checks.structure(body == null ? "" : body);
        if (semantics) checks.semantics(vc);
        if (vcSig) checks.signature(vc);
        if (schema) checks.shacl(vc, checks.schema(vc));
        String id = vc.path("credentialSubject").path("@id").asText();
        boolean revoked = repo.byId(id).map(r -> "revoked".equals(r.status())).orElse(false);
        ObjectNode out = Json.object();
        out.put("verificationTimestamp", Instant.now().toString());
        out.put("lifecycleStatus", revoked ? "revoked" : "active");
        out.put("issuer", SignatureVerifier.issuerOf(vc));
        out.put("issuedDateTime", vc.path("issuanceDate").asText());
        out.putArray("validatorDids");
        return SelfDescriptionController.json(200, out);
    }
}
