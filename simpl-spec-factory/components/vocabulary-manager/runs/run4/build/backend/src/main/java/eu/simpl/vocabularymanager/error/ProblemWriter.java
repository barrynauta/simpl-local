package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Writes RFC 9457 problem bodies straight to the servlet response, so error output does not depend on content
 * negotiation and is the same from filters and controllers.
 */
@Component
public class ProblemWriter {

    public static final String PROBLEM_JSON = "application/problem+json";
    private final ObjectMapper objectMapper;

    public ProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes the problem for the given error code.
     *
     * @throws IOException when the response cannot be written
     */
    public void write(HttpServletResponse response, ErrorCode code, String detail, List<Issue> issues)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(code.status());
        response.setContentType(PROBLEM_JSON);
        response.setCharacterEncoding("UTF-8");
        if (code.status() == 503) {
            response.setHeader("Retry-After", "5");
        }
        if (code.status() == 401) {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"simpl\"");
        }
        objectMapper.writeValue(response.getOutputStream(), body(code, detail, issues));
    }

    /** Builds the problem body. */
    public ObjectNode body(ErrorCode code, String detail, List<Issue> issues) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", code.type());
        if (code.href() != null) {
            node.put("href", code.href());
        }
        node.put("title", code.title());
        node.put("status", code.status());
        node.put("detail", detail);
        node.put("instance", "urn:uuid:" + UUID.randomUUID());
        if (issues != null && !issues.isEmpty()) {
            ArrayNode array = node.putArray("issues");
            issues.forEach(issue -> array.add(objectMapper.valueToTree(issue)));
        }
        return node;
    }
}
