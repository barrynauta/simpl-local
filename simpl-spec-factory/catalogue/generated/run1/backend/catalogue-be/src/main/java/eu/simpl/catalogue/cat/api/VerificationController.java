package eu.simpl.catalogue.cat.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.crypto.SignatureVerifier;
import eu.simpl.catalogue.cat.service.VcChecks;
import eu.simpl.catalogue.cat.store.RdRepository;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Json;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** POST /verification (D25) and the HTML page of GET /verification (D87). Nothing is stored. */
@RestController
public class VerificationController {
    private final VcChecks checks;
    private final RdRepository repo;

    public VerificationController(VcChecks checks, RdRepository repo) {
        this.checks = checks;
        this.repo = repo;
    }

    private static final String PAGE = """
            <!DOCTYPE html>
            <html lang="en"><head><meta charset="utf-8"><title>Verify a self-description</title></head>
            <body>
            <h1>Verify (portions of) a signed self-description</h1>
            <p>POST the signed verifiable credential as JSON to <code>/verification</code>.
            Query flags: verifySemantics, verifySchema, verifyVPSignature, verifyVCSignature.</p>
            <form><label for="vc">Self-description</label><br>
            <textarea id="vc" name="vc" rows="12" cols="80"></textarea></form>
            </body></html>
            """;

    /** D87: a valid token of the realm, no role (D67), as for POST /verification. */
    @GetMapping("/verification")
    public ResponseEntity<String> page() {
        Caller.current();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html;charset=UTF-8").body(PAGE);
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
