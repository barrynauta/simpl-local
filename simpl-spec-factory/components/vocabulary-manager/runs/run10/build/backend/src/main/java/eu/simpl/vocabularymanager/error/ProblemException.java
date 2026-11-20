package eu.simpl.vocabularymanager.error;

import java.util.List;
import java.util.Map;

/**
 * The one exception base class of the component (D104). It carries the problem type, title and status that the
 * single handler maps to application/problem+json. Use the factories in {@link Problems}.
 */
public class ProblemException extends RuntimeException {

    private final int status;
    private final String type;
    private final String href;
    private final String title;
    private final String detail;
    private final List<Issue> issues;
    private final Map<String, String> headers;

    public ProblemException(
            int status,
            String type,
            String href,
            String title,
            String detail,
            List<Issue> issues,
            Map<String, String> headers) {
        super(title + ": " + detail);
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

    public String href() {
        return href;
    }

    public String title() {
        return title;
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
}
