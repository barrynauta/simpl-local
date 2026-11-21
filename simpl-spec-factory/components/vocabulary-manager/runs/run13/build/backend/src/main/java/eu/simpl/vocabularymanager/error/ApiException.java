package eu.simpl.vocabularymanager.error;

import java.util.List;
import java.util.Map;

/**
 * The one exception base class of the component (D104, CODE-43). It carries the problem type, the detail
 * text, the issues and optional response headers. One handler maps it to application/problem+json.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ProblemType problemType;
    private final String detail;
    private final transient List<ProblemIssue> issues;
    private final transient Map<String, String> headers;
    private final boolean truncated;

    public ApiException(ProblemType problemType, String detail) {
        this(problemType, detail, List.of(), Map.of(), false);
    }

    public ApiException(ProblemType problemType, String detail, List<ProblemIssue> issues) {
        this(problemType, detail, issues, Map.of(), false);
    }

    public ApiException(
            ProblemType problemType,
            String detail,
            List<ProblemIssue> issues,
            Map<String, String> headers,
            boolean truncated) {
        super(problemType.title() + ": " + detail);
        this.problemType = problemType;
        this.detail = detail;
        this.issues = List.copyOf(issues);
        this.headers = Map.copyOf(headers);
        this.truncated = truncated;
    }

    public ProblemType problemType() {
        return problemType;
    }

    public String detail() {
        return detail;
    }

    public List<ProblemIssue> issues() {
        return issues;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public boolean truncated() {
        return truncated;
    }
}
