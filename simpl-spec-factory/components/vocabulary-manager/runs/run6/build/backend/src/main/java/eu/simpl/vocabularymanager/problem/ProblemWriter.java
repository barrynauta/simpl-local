package eu.simpl.vocabularymanager.problem;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Writes problem bodies as application/problem+json, from controllers and from servlet filters alike. */
@Component
public class ProblemWriter {

    /** Media type of every error body (API-45). */
    public static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private final ObjectMapper objectMapper;

    /**
     * Creates the writer.
     *
     * @param objectMapper the JSON mapper of the application
     */
    public ProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the response entity of a problem. The content type is preset, so content negotiation does not
     * reject a problem body when the client asked for another type.
     *
     * @param problem the problem
     * @param headers extra headers
     * @return the response entity
     */
    public ResponseEntity<Problem> entity(Problem problem, Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);
        httpHeaders.setContentType(PROBLEM_JSON);
        return ResponseEntity.status(problem.status()).headers(httpHeaders).body(problem);
    }

    /**
     * Writes a problem straight to the servlet response (used outside Spring MVC).
     *
     * @param response the servlet response
     * @param problem the problem
     * @param headers extra headers
     * @throws IOException when the response cannot be written
     */
    public void write(HttpServletResponse response, Problem problem, Map<String, String> headers) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(problem.status());
        headers.forEach(response::setHeader);
        response.setContentType(PROBLEM_JSON.toString());
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(objectMapper.writeValueAsBytes(problem));
        response.flushBuffer();
    }
}
