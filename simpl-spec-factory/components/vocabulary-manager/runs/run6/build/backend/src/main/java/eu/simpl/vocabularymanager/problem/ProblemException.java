package eu.simpl.vocabularymanager.problem;

import java.util.List;
import java.util.Map;

/**
 * Base application exception. Every expected failure of an operation is a ProblemException with a defined
 * RFC 9457 problem body; the exception handler turns it into the response (CODE-36).
 */
public class ProblemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Problem problem;
    private final transient Map<String, String> headers;

    /**
     * Creates the exception.
     *
     * @param problem the problem body
     * @param headers extra response headers (for example Retry-After or Allow)
     */
    public ProblemException(Problem problem, Map<String, String> headers) {
        super(problem.title() + ": " + problem.detail());
        this.problem = problem;
        this.headers = Map.copyOf(headers);
    }

    /**
     * Creates the exception without extra headers.
     *
     * @param problem the problem body
     */
    public ProblemException(Problem problem) {
        this(problem, Map.of());
    }

    public Problem problem() {
        return problem;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public int status() {
        return problem.status();
    }

    public List<Issue> issues() {
        return problem.issues();
    }
}
