package eu.simpl.vocabularymanager.error;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one exception base of the service (decision D104): it carries the problem type, the detail,
 * the issues and extra response headers. One handler maps it to application/problem+json.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";
    public static final String IDP_UNAVAILABLE = "The identity provider is not available. Retry later.";
    public static final String TOO_MANY_UPLOADS = "Too many uploads in progress. Retry later.";
    public static final String FILE_TOO_LARGE =
            "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. Please"
                    + " upload a smaller Turtle file.";
    public static final String JSON_TOO_LARGE = "The request body is larger than 1 MB.";
    public static final String BAD_INPUT = "The input message is incorrect";

    private final ProblemType problemType;
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
        super(detail);
        this.problemType = problemType;
        this.issues = List.copyOf(issues);
        this.headers = new LinkedHashMap<>(headers);
        this.truncated = truncated;
    }

    public ProblemType problemType() {
        return problemType;
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

    public static ApiException badRequest(List<Issue> issues) {
        return new ApiException(ProblemType.BAD_REQUEST, BAD_INPUT, issues);
    }

    public static ApiException badRequest(Issue issue) {
        return badRequest(List.of(issue));
    }

    public static ApiException conflict(String detail) {
        return new ApiException(ProblemType.CONFLICT, detail);
    }

    public static ApiException notFound(String detail, Issue issue) {
        return new ApiException(ProblemType.NOT_FOUND, detail, issue == null ? List.of() : List.of(issue));
    }

    public static ApiException unavailable(String detail) {
        return new ApiException(ProblemType.SERVICE_UNAVAILABLE, detail, List.of(), Map.of("Retry-After", "5"), false);
    }

    public static ApiException fileTooLarge() {
        return new ApiException(
                ProblemType.PAYLOAD_TOO_LARGE,
                FILE_TOO_LARGE,
                List.of(Issue.invalidInput("body", "vocabularyFile", FILE_TOO_LARGE, null)));
    }

    public static ApiException jsonTooLarge() {
        return new ApiException(ProblemType.PAYLOAD_TOO_LARGE, JSON_TOO_LARGE);
    }

    public static ApiException internal() {
        return new ApiException(ProblemType.INTERNAL_ERROR, "Internal Server Error");
    }
}
