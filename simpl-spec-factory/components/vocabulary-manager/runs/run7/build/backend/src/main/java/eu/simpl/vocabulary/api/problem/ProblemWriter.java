package eu.simpl.vocabulary.api.problem;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes problem responses, both from servlet filters and from controllers. */
@Component
public class ProblemWriter {

    public static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private final JsonMapper mapper;

    public ProblemWriter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Writes the problem directly to a servlet response (used before the DispatcherServlet).
     *
     * @param response the servlet response
     * @param exception the problem to write
     * @throws IOException when the response cannot be written
     */
    public void write(HttpServletResponse response, ApiException exception) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(exception.status());
        response.setContentType(PROBLEM_JSON.toString());
        for (Map.Entry<String, String> header : exception.headers().entrySet()) {
            response.setHeader(header.getKey(), header.getValue());
        }
        byte[] body = mapper.writeValueAsBytes(exception.toProblem(newInstance()));
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }

    /**
     * Builds a response entity for the problem (used inside the DispatcherServlet).
     *
     * @param exception the problem
     * @return the response entity
     */
    public ResponseEntity<Problem> entity(ApiException exception) {
        HttpHeaders headers = new HttpHeaders();
        exception.headers().forEach(headers::set);
        headers.setContentType(PROBLEM_JSON);
        return ResponseEntity.status(exception.status()).headers(headers).body(exception.toProblem(newInstance()));
    }

    private static String newInstance() {
        return "urn:uuid:" + UUID.randomUUID();
    }
}
