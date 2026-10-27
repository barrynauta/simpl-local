package eu.simpl.vocabulary.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Writes Problem Details straight to the servlet response. Writing directly (instead of through Spring's
 * message converters) means an error can always be sent as application/problem+json, even when the client
 * asked for text/turtle.
 */
@Component
public class ProblemWriter {

    public static final String PROBLEM_JSON = "application/problem+json";

    private final ObjectMapper objectMapper;

    public ProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ApiException ex) throws IOException {
        write(response, ex.status().value(), ex.type(), ex.href(), ex.title(), ex.getMessage(), ex.issues(),
                ex.warnings());
    }

    public void write(HttpServletResponse response, int status, String type, String href, String title,
                      String detail, List<ProblemIssue> issues, List<?> warnings) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType(PROBLEM_JSON);
        response.setCharacterEncoding("UTF-8");
        Problem problem = new Problem(type, href, title, status, detail, "urn:uuid:" + UUID.randomUUID(), issues,
                warnings);
        objectMapper.writeValue(response.getOutputStream(), problem);
        response.flushBuffer();
    }
}
