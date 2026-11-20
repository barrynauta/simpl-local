package eu.simpl.vocabularymanager.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

/** Writes a problem straight to the servlet response; used by filters that run before Spring MVC. */
public final class ProblemWriter {

    public static final String PROBLEM_JSON = "application/problem+json";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ProblemWriter() {}

    /**
     * Writes the problem as application/problem+json.
     *
     * @throws IOException when the response cannot be written
     */
    public static void write(HttpServletResponse response, ProblemException problem) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(problem.status());
        problem.headers().forEach(response::setHeader);
        response.setContentType(PROBLEM_JSON);
        byte[] body = MAPPER.writeValueAsBytes(ProblemBody.of(problem));
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }

    public static String toJson(ProblemException problem) {
        return new String(MAPPER.writeValueAsBytes(ProblemBody.of(problem)), StandardCharsets.UTF_8);
    }
}
