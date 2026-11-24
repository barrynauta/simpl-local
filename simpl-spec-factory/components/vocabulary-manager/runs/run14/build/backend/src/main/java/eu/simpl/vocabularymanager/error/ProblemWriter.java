package eu.simpl.vocabularymanager.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes problem+json bodies directly to a servlet response. Used where no Spring MVC handler runs: the
 * gatekeeper filter, the Tomcat error valve and the exception handler.
 */
public final class ProblemWriter {

    public static final String PROBLEM_JSON = "application/problem+json";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ProblemWriter() {}

    /**
     * Writes the problem of the exception, with its headers.
     *
     * @param response the response, not yet committed
     * @param exception the exception to render
     * @throws IOException when the client connection fails
     */
    public static void write(HttpServletResponse response, ApiException exception) throws IOException {
        for (Map.Entry<String, String> header : exception.headers().entrySet()) {
            response.setHeader(header.getKey(), header.getValue());
        }
        if (exception.type() == ProblemType.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        writeBody(response, exception.type().status(), MAPPER.writeValueAsBytes(exception.toProblem()));
    }

    /** Serialises any problem body, used by the Tomcat valve. */
    public static byte[] toBytes(Problem problem) {
        return MAPPER.writeValueAsBytes(problem);
    }

    /**
     * Writes raw problem bytes with the given status.
     *
     * @param response the response
     * @param status the status
     * @param body the problem body
     * @throws IOException when the client connection fails
     */
    public static void writeBody(HttpServletResponse response, int status, byte[] body) throws IOException {
        response.setStatus(status);
        response.setContentType(PROBLEM_JSON);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }
}
