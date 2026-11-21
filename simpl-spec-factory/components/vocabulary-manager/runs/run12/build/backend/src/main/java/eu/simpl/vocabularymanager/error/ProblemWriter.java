package eu.simpl.vocabularymanager.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes application/problem+json (RFC 9457) directly to the servlet response, so filters, the
 * security layer and the exception handler all produce the same body without content negotiation.
 */
@Component
public class ProblemWriter {

    public static final String MEDIA_TYPE = "application/problem+json";

    private final JsonMapper mapper;

    public ProblemWriter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse response, ApiException ex) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(ex.problemType().status());
        ex.headers().forEach(response::setHeader);
        response.setContentType(MEDIA_TYPE);
        byte[] body = mapper.writeValueAsBytes(body(ex));
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }

    /** The problem body as an ordered map (field order follows the OpenAPI examples). */
    public static Map<String, Object> body(ApiException ex) {
        ProblemType p = ex.problemType();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", p.type());
        if (p.href() != null) {
            m.put("href", p.href());
        }
        m.put("title", p.title());
        m.put("status", p.status());
        m.put("detail", ex.getMessage());
        m.put("instance", "urn:uuid:" + UUID.randomUUID());
        List<Issue> issues = ex.issues();
        if (!issues.isEmpty()) {
            m.put("issues", issues);
        }
        if (ex.truncated()) {
            m.put("truncated", true);
        }
        return m;
    }
}
