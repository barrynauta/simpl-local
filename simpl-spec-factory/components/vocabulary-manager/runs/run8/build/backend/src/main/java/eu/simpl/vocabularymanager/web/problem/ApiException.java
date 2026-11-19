package eu.simpl.vocabularymanager.web.problem;

import java.util.Map;

/**
 * Base application exception: carries the problem returned to the client. Every request path that ends in an error
 * throws one of these (or a framework exception that {@link ApiExceptionHandler} maps), so all errors are answered the
 * same way.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Problem problem;
    private final transient Map<String, String> headers;

    /**
     * @param problem the problem to return
     */
    public ApiException(Problem problem) {
        this(problem, Map.of(), null);
    }

    /**
     * @param problem the problem to return
     * @param headers extra response headers
     * @param cause the cause, or null
     */
    public ApiException(Problem problem, Map<String, String> headers, Throwable cause) {
        super(problem.title() + ": " + problem.detail(), cause);
        this.problem = problem;
        this.headers = Map.copyOf(headers);
    }

    public Problem problem() {
        return problem;
    }

    public Map<String, String> headers() {
        return headers;
    }
}
