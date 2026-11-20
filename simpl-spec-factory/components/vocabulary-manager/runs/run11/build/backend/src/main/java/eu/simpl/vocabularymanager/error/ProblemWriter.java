package eu.simpl.vocabularymanager.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes RFC 9457 problem details. Used by filters (outside Spring MVC) and by the advice. */
@Component
public class ProblemWriter {

    public static final String PROBLEM_JSON = "application/problem+json";

    private final JsonMapper mapper;

    public ProblemWriter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /** Builds the problem body as an ordered map. */
    public Map<String, Object> body(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        ProblemType type = ex.problemType();
        body.put("type", type.type());
        if (type.href() != null) {
            body.put("href", type.href());
        }
        body.put("title", type.title());
        body.put("status", type.status());
        body.put("detail", ex.detail());
        body.put("instance", "urn:uuid:" + UUID.randomUUID());
        List<Issue> issues = ex.issues();
        if (!issues.isEmpty()) {
            body.put("issues", issues);
        }
        if (ex.truncated()) {
            body.put("truncated", Boolean.TRUE);
        }
        return body;
    }

    /**
     * Writes the problem straight to the servlet response.
     *
     * @throws IOException when the response cannot be written
     */
    public void write(HttpServletResponse response, ApiException ex) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(ex.problemType().status());
        response.setContentType(PROBLEM_JSON);
        ex.headers().forEach(response::setHeader);
        response.getOutputStream().write(mapper.writeValueAsBytes(body(ex)));
        response.flushBuffer();
    }
}
