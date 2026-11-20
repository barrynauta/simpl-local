package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.ProblemException.Problem;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

/** Writes problem responses as application/problem+json, from filters and from controllers. */
public final class ProblemWriter {

    /** Media type of problem responses. */
    public static final String PROBLEM_JSON = "application/problem+json";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ProblemWriter() {}

    /** Serialises a problem. */
    public static byte[] body(Problem problem) {
        return MAPPER.writeValueAsBytes(Problems.toMap(problem));
    }

    /** As a controller response. */
    public static ResponseEntity<byte[]> entity(Problem problem) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON);
        problem.headers().forEach(headers::set);
        return ResponseEntity.status(problem.status()).headers(headers).body(body(problem));
    }

    /**
     * Directly on the servlet response (filters, security handlers).
     *
     * @param response the response, not yet committed
     * @param problem the problem
     * @throws IOException when writing fails
     */
    public static void write(HttpServletResponse response, Problem problem) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(problem.status());
        response.setHeader(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON);
        problem.headers().forEach(response::setHeader);
        byte[] body = body(problem);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }
}
