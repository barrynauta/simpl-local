package eu.simpl.catalogue.cat.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.cat.crypto.SignatureVerifier;
import eu.simpl.catalogue.cat.rdf.JsonLdSupport;
import eu.simpl.catalogue.cat.rdf.ShapeSupport;
import eu.simpl.catalogue.cat.store.ShapeRepository;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.ProblemTypes;
import org.apache.jena.graph.Graph;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Steps 2, 3, 5 and 6 of the publication pipeline (D14), shared with POST /verification (D25). */
@Component
public class VcChecks {
    public static final String CREDENTIALS_V1 = "https://www.w3.org/2018/credentials/v1";

    private final ShapeRepository shapes;
    private final ConcurrentHashMap<String, Graph> parsed = new ConcurrentHashMap<>();

    public VcChecks(ShapeRepository shapes) {
        this.shapes = shapes;
    }

    /** Step 2 without JSON-LD: JSON object, VC structure. */
    public JsonNode structure(String body) {
        JsonNode vc = Json.parseOrNull(body);
        if (vc == null || !vc.isObject()) throw ApiException.badRequest("The request body is not a JSON object.");
        if (vc.path("type").asText("").equals("VerifiablePresentation")
                || Json.textList(vc.get("type")).contains("VerifiablePresentation")) {
            throw ApiException.badRequest("A Verifiable Presentation is not accepted; send a Verifiable Credential.");
        }
        JsonNode ctx = vc.get("@context");
        String first = ctx == null ? null : ctx.isArray() ? (ctx.isEmpty() ? null : ctx.get(0).asText(null)) : ctx.asText(null);
        if (!CREDENTIALS_V1.equals(first)) {
            throw ApiException.badRequest("The first @context entry must be " + CREDENTIALS_V1 + ".");
        }
        if (!Json.textList(vc.get("type")).contains("VerifiableCredential")) {
            throw ApiException.badRequest("type must contain VerifiableCredential.");
        }
        if (SignatureVerifier.issuerOf(vc) == null) throw ApiException.badRequest("issuer is required.");
        if (!vc.path("issuanceDate").isTextual()) throw ApiException.badRequest("issuanceDate is required.");
        JsonNode cs = vc.get("credentialSubject");
        if (cs == null || !cs.isObject() || !cs.path("@id").isTextual() || cs.get("@id").asText().isBlank()) {
            throw ApiException.badRequest("credentialSubject with an @id is required.");
        }
        if (!vc.path("proof").isObject()) throw ApiException.badRequest("proof is required.");
        return vc;
    }

    /** Step 2, JSON-LD part: expansion with bundled contexts only. */
    public void semantics(JsonNode vc) {
        try {
            JsonLdSupport.checkExpands(vc);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }

    /** Step 3. Returns the public key x of the signer. */
    public String signature(JsonNode vc) {
        SignatureVerifier.Result r = SignatureVerifier.verify(vc);
        if (!r.valid()) throw new ApiException(ProblemTypes.INVALID_SIGNATURE, "The signature is not valid: " + r.reason() + ".");
        return r.publicX();
    }

    /** Step 5. Returns the stored shape the RD conforms to. */
    public ShapeRepository.Shape schema(JsonNode vc) {
        JsonNode ct = vc.path("credentialSubject").path("dct:conformsTo");
        if (ct.isArray()) ct = ct.path(0);
        String name = Json.literal(ct.get("dct:schemaName"));
        String version = Json.literal(ct.get("dct:hasVersion"));
        if (name == null || version == null) {
            throw new ApiException(ProblemTypes.SCHEMA_NOT_SUPPORTED,
                    "credentialSubject.dct:conformsTo with dct:schemaName and dct:hasVersion is required.");
        }
        ShapeRepository.Shape shape = shapes.byId(name).orElseThrow(() -> new ApiException(
                ProblemTypes.SCHEMA_NOT_SUPPORTED, "Schema '" + name + "' is not supported by this catalogue."));
        if ("REVOKED".equals(shape.status())) {
            throw new ApiException(ProblemTypes.SCHEMA_NOT_SUPPORTED, "Schema '" + name + "' is revoked.");
        }
        if (!shape.version().equals(version)) {
            throw new ApiException(ProblemTypes.SCHEMA_NOT_SUPPORTED, "Schema '" + name + "' version " + version
                    + " is not supported; the supported version is " + shape.version() + ".");
        }
        return shape;
    }

    /** Step 6. */
    public void shacl(JsonNode vc, ShapeRepository.Shape shape) {
        Graph data;
        try {
            data = JsonLdSupport.subjectGraph(vc);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
        Graph sg = parsed.computeIfAbsent(shape.schemaId() + "#" + shape.content().hashCode(),
                k -> ShapeSupport.parseTurtle(shape.content()));
        List<String> violations = ShapeSupport.validate(sg, shape.schemaId(), data,
                vc.path("credentialSubject").path("@id").asText());
        if (!violations.isEmpty()) {
            throw new ApiException(ProblemTypes.SHACL_VIOLATION,
                    "The resource description violates shape " + shape.schemaId() + ": " + String.join("; ", violations));
        }
    }
}
