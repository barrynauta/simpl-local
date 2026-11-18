package eu.simpl.vocabularymanager.error;

import java.util.List;

/**
 * The one exception class of the component (CODE-43). The error code gives the HTTP status and problem type; the
 * detail and the issues are returned to the client in the problem body.
 */
public class VocabularyManagerException extends RuntimeException {

    private final transient ErrorCode code;
    private final String detail;
    private final transient List<Issue> issues;

    public VocabularyManagerException(ErrorCode code, String detail) {
        this(code, detail, List.of(), null);
    }

    public VocabularyManagerException(ErrorCode code, String detail, List<Issue> issues) {
        this(code, detail, issues, null);
    }

    public VocabularyManagerException(ErrorCode code, String detail, List<Issue> issues, Throwable cause) {
        super(code + ": " + detail, cause);
        this.code = code;
        this.detail = detail;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public ErrorCode code() {
        return code;
    }

    public String detail() {
        return detail;
    }

    public List<Issue> issues() {
        return issues;
    }
}
