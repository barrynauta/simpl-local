package eu.simpl.vocabularymanager.api;

import java.util.List;
import java.util.Map;

/** A request that ends with a decided problem response (RFC 9457, Belgif problem types). */
public class ProblemException extends RuntimeException {

    private final transient Problem problem;

    /** @param problem the response to send */
    public ProblemException(Problem problem) {
        super(problem.title() + ": " + problem.detail());
        this.problem = problem;
    }

    /** @return the response to send */
    public Problem problem() {
        return problem;
    }

    /**
     * A problem response.
     *
     * @param status HTTP status
     * @param type problem type URN
     * @param href documentation link or null
     * @param title title
     * @param detail detail
     * @param issues issues, possibly empty
     * @param headers extra headers
     */
    public record Problem(
            int status,
            String type,
            String href,
            String title,
            String detail,
            List<Issue> issues,
            Map<String, String> headers) {}
}
