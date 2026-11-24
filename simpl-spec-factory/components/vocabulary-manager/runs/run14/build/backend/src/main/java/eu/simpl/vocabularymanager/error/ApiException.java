package eu.simpl.vocabularymanager.error;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The one base exception of the component (D104). It carries the problem type, the detail, the issues and
 * extra response headers; {@link ProblemHandler} maps it to problem+json.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final int MAX_ISSUES = 1000;

    private final ProblemType type;
    private final String detail;
    private final transient List<Issue> issues;
    private final boolean truncated;
    private final transient Map<String, String> headers;

    public ApiException(ProblemType type, String detail, List<Issue> issues) {
        this(type, detail, issues, Map.of(), null);
    }

    public ApiException(
            ProblemType type, String detail, List<Issue> issues, Map<String, String> headers, Throwable cause) {
        super(detail == null ? type.defaultDetail() : detail, cause);
        this.type = type;
        this.detail = detail == null ? type.defaultDetail() : detail;
        List<Issue> all = issues == null ? List.of() : issues;
        this.truncated = all.size() > MAX_ISSUES;
        this.issues = truncated ? List.copyOf(all.subList(0, MAX_ISSUES)) : List.copyOf(all);
        this.headers = new LinkedHashMap<>(headers);
    }

    public ProblemType type() {
        return type;
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

    /** Builds the response body of this exception. */
    public Problem toProblem() {
        return new Problem(
                type.uri(),
                type.href(),
                type.title(),
                type.status(),
                detail,
                "urn:uuid:" + UUID.randomUUID(),
                issues.isEmpty() ? null : issues,
                truncated ? Boolean.TRUE : null);
    }

    /** 400 with schema issues. */
    public static ApiException badRequest(List<Issue> issues) {
        return new ApiException(ProblemType.BAD_REQUEST, null, issues);
    }

    /** 400 with one schema violation issue. */
    public static ApiException badRequest(String in, String name, String detail) {
        return badRequest(List.of(Issue.schemaViolation(in, name, detail)));
    }

    /** 404 with one issue. */
    public static ApiException notFound(String detail, String in, String name) {
        return new ApiException(ProblemType.NOT_FOUND, detail, List.of(Issue.notFound(in, name, detail)));
    }

    /** 409 with the given detail. */
    public static ApiException conflict(String detail) {
        return new ApiException(ProblemType.CONFLICT, detail, List.of());
    }

    /** 503 with Retry-After: 5 (D44). */
    public static ApiException unavailable(String detail, Throwable cause) {
        return new ApiException(ProblemType.SERVICE_UNAVAILABLE, detail, List.of(), Map.of("Retry-After", "5"), cause);
    }

    /** 500 without internal details. */
    public static ApiException internal(Throwable cause) {
        return new ApiException(ProblemType.INTERNAL_SERVER_ERROR, null, List.of(), Map.of(), cause);
    }
}
