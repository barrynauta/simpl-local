package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Builds Problem bodies and responses. The content type is preset so Accept negotiation never blocks an error.
 * D89 (run 1.2): every Problem also carries the members of the older {@code Error {code, message}} shape as
 * RFC 9457 extension members, so one body meets both shapes the OpenAPI uses.
 */
public final class Problems {
    public static final String PROBLEM_JSON = "application/problem+json";

    private Problems() {}

    public static ObjectNode body(ProblemTypes type, int status, String detail) {
        ObjectNode n = Json.object();
        n.put("type", type.uri);
        n.put("title", type.title);
        n.put("status", status);
        n.put("detail", detail == null ? type.title : detail);
        n.put("instance", "urn:uuid:" + UUID.randomUUID());
        return n;
    }

    public static ResponseEntity<String> response(ApiException e) {
        ObjectNode body = e.body() != null ? e.body() : body(e.type(), e.status(), e.getMessage());
        return response(e.status(), body);
    }

    public static ResponseEntity<String> response(ProblemTypes type, String detail) {
        return response(type.status, body(type, type.status, detail));
    }

    /** The publication pipeline's verification failures share one code (PSO scenario TCC11, D89). */
    private static final java.util.Set<String> VERIFICATION_TYPES = java.util.Set.of(
            ProblemTypes.INVALID_SIGNATURE.uri, ProblemTypes.SCHEMA_NOT_SUPPORTED.uri, ProblemTypes.SHACL_VIOLATION.uri);

    /**
     * D89: adds {@code message} (the detail) when absent, and {@code code} only for the verification failures,
     * where PSO checks it. A body with both {@code code} and {@code message} matches both branches of PSO's
     * {@code oneOf} error schema and fails it, so {@code code} is not added elsewhere.
     */
    public static ObjectNode withErrorMembers(ObjectNode body) {
        if (!body.has("message")) body.put("message", body.path("detail").asText(body.path("title").asText("")));
        if (!body.has("code") && VERIFICATION_TYPES.contains(body.path("type").asText(""))) {
            body.put("code", "verification_error");
        }
        return body;
    }

    public static ResponseEntity<String> response(int status, ObjectNode body) {
        withErrorMembers(body);
        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON)
                .body(Json.write(body));
    }

    public static void write(HttpServletResponse res, ProblemTypes type, String detail) throws IOException {
        res.setStatus(type.status);
        res.setContentType(PROBLEM_JSON);
        res.setCharacterEncoding("UTF-8");
        res.getOutputStream().write(Json.write(withErrorMembers(body(type, type.status, detail))).getBytes(StandardCharsets.UTF_8));
    }
}
