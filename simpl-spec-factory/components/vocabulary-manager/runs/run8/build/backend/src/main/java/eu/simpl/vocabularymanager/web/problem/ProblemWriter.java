package eu.simpl.vocabularymanager.web.problem;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes problems straight to the servlet response as application/problem+json. Used by filters, security handlers and
 * the exception handler, so error bodies never depend on the Accept header of the request.
 */
@Component
public class ProblemWriter {

    public static final String PROBLEM_JSON = "application/problem+json";

    private final JsonMapper jsonMapper;

    /**
     * @param jsonMapper the JSON mapper of the application
     */
    public ProblemWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * @param response the response, not yet committed
     * @param problem the problem to write
     * @param headers extra headers
     * @throws IOException when the client connection fails
     */
    public void write(HttpServletResponse response, Problem problem, Map<String, String> headers) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(problem.status());
        headers.forEach(response::setHeader);
        response.setContentType(PROBLEM_JSON);
        byte[] body = jsonMapper.writeValueAsBytes(problem);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }

    /**
     * @param response the response, not yet committed
     * @param problem the problem to write
     * @throws IOException when the client connection fails
     */
    public void write(HttpServletResponse response, Problem problem) throws IOException {
        write(response, problem, Map.of());
    }
}
