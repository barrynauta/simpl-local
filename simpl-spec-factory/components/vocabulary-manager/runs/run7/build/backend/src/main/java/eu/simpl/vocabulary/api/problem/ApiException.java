package eu.simpl.vocabulary.api.problem;

import java.util.List;
import java.util.Map;

/**
 * Base application exception. Each instance carries the full problem response it maps to, so every layer
 * handles errors the same way (CODE-34, CODE-36). Create instances through {@link Problems}.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String type;
    private final String href;
    private final String title;
    private final String detail;
    private final transient List<Issue> issues;
    private final transient Map<String, String> headers;

    ApiException(
            int status,
            String type,
            String href,
            String title,
            String detail,
            List<Issue> issues,
            Map<String, String> headers) {
        super(title + ": " + detail, null, false, false);
        this.status = status;
        this.type = type;
        this.href = href;
        this.title = title;
        this.detail = detail;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public int status() {
        return status;
    }

    public String type() {
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

    /**
     * Converts the exception to the response body.
     *
     * @param instance the occurrence id
     * @return the problem body
     */
    public Problem toProblem(String instance) {
        return new Problem(type, href, title, status, detail, instance, issues);
    }
}
