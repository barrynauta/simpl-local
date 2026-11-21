package eu.simpl.vocabularymanager.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes RFC 9457 problem details. Used by the exception handler, by the request gate (before routing)
 * and by the Tomcat error valve, so every answer of the service has the same shape (D48, D158).
 */
public final class ProblemWriter {

    public static final String PROBLEM_JSON = "application/problem+json";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ProblemWriter() {}

    /** The problem body as a map, with absent values left out (BLG-jsn-null). */
    public static Map<String, Object> body(
            ProblemType type, String detail, List<ProblemIssue> issues, boolean truncated) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type.type());
        if (type.href() != null) {
            body.put("href", type.href());
        }
        body.put("title", type.title());
        body.put("status", type.status());
        body.put("detail", detail);
        body.put("instance", "urn:uuid:" + UUID.randomUUID());
        if (!issues.isEmpty()) {
            body.put("issues", issues);
        }
        if (truncated) {
            body.put("truncated", true);
        }
        return body;
    }

    public static byte[] bytes(ApiException exception) {
        return MAPPER.writeValueAsBytes(
                body(exception.problemType(), exception.detail(), exception.issues(), exception.truncated()));
    }

    /** Writes the problem to a servlet response that has not been committed yet. */
    public static void write(HttpServletResponse response, ApiException exception) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(exception.problemType().status());
        response.setContentType(PROBLEM_JSON);
        exception.headers().forEach(response::setHeader);
        byte[] payload = bytes(exception);
        response.setContentLength(payload.length);
        response.getOutputStream().write(payload);
        response.flushBuffer();
    }
}
