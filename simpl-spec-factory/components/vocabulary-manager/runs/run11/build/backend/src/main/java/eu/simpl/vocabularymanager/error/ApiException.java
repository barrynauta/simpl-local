package eu.simpl.vocabularymanager.error;

import java.util.List;
import java.util.Map;

/**
 * The one exception class of the component (D104, CODE-43). It carries the problem type (the
 * error code), the detail text, the issues and optional extra headers.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ProblemType problemType;
    private final String detail;
    private final transient List<Issue> issues;
    private final transient Map<String, String> headers;
    private final boolean truncated;

    public ApiException(ProblemType problemType, String detail) {
        this(problemType, detail, List.of(), Map.of(), false);
    }

    public ApiException(ProblemType problemType, String detail, List<Issue> issues) {
        this(problemType, detail, issues, Map.of(), false);
    }

    public ApiException(
            ProblemType problemType,
            String detail,
            List<Issue> issues,
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

    public List<Issue> issues() {
        return issues;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public boolean truncated() {
        return truncated;
    }

    /** 400 with one or more request shape issues. */
    public static ApiException badRequest(List<Issue> issues) {
        return new ApiException(ProblemType.BAD_REQUEST, "The input message is incorrect", issues);
    }

    /** 400 with a single schema violation. */
    public static ApiException badRequest(String in, String name, String detail) {
        return badRequest(List.of(Issue.schema(in, name, detail)));
    }

    /** 503 as decided in D44, with Retry-After. */
    public static ApiException unavailable(String detail) {
        return new ApiException(ProblemType.SERVICE_UNAVAILABLE, detail, List.of(), Map.of("Retry-After", "5"), false);
    }

    /** 409 with the conflict text. */
    public static ApiException conflict(String detail) {
        return new ApiException(ProblemType.CONFLICT, detail);
    }

    /** 413 for an upload or body above the limit. */
    public static ApiException tooLarge(String name, String detail) {
        return new ApiException(ProblemType.PAYLOAD_TOO_LARGE, detail, List.of(Issue.invalidInput(name, detail, null)));
    }
}
