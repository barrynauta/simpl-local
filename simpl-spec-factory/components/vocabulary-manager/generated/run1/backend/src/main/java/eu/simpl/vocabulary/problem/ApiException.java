package eu.simpl.vocabulary.problem;

import java.util.List;
import org.springframework.http.HttpStatus;

/** An error that maps one to one on a Problem Details response. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String href;
    private final String title;
    private final List<ProblemIssue> issues;
    private final List<?> warnings;

    public ApiException(HttpStatus status, String type, String href, String title, String detail,
                        List<ProblemIssue> issues, List<?> warnings) {
        super(detail);
        this.status = status;
        this.type = type;
        this.href = href;
        this.title = title;
        this.issues = issues == null ? List.of() : List.copyOf(issues);
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static ApiException badRequest(List<ProblemIssue> issues) {
        return badRequest(issues, List.of());
    }

    public static ApiException badRequest(List<ProblemIssue> issues, List<?> warnings) {
        return new ApiException(HttpStatus.BAD_REQUEST, "urn:problem-type:belgif:badRequest",
                "https://www.belgif.be/specification/rest/api-guide/problems/badRequest.html", "Bad Request",
                "The input message is incorrect", issues, warnings);
    }

    public static ApiException badRequest(ProblemIssue issue) {
        return badRequest(List.of(issue));
    }

    public static ApiException notFound(String detail, ProblemIssue issue) {
        return new ApiException(HttpStatus.NOT_FOUND, "urn:problem-type:belgif:resourceNotFound",
                "https://www.belgif.be/specification/rest/api-guide/problems/resourceNotFound.html",
                "Resource Not Found", detail, issue == null ? List.of() : List.of(issue), null);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, "urn:problem-type:simpl:conflict", null, "Conflict", detail,
                null, null);
    }

    public static ApiException unsupportedMediaType(String detail, ProblemIssue issue) {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "urn:problem-type:simpl:unsupportedMediaType",
                null, "Unsupported Media Type", detail, issue == null ? List.of() : List.of(issue), null);
    }

    public static ApiException payloadTooLarge(long maxBytes) {
        String mb = (maxBytes % (1024 * 1024) == 0) ? (maxBytes / (1024 * 1024)) + " MB" : maxBytes + " bytes";
        String detail = "Uploaded vocabulary file is too large. The maximum allowed upload size is " + mb
                + ". Please upload a smaller Turtle file.";
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "urn:problem-type:simpl:payloadTooLarge", null,
                "Payload Too Large", detail,
                List.of(new ProblemIssue(ProblemIssue.INVALID_INPUT, ProblemIssue.INVALID_INPUT_HREF, "Invalid input",
                        detail, "body", "vocabularyFile", null, null)), null);
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String href() { return href; }
    public String title() { return title; }
    public List<ProblemIssue> issues() { return issues; }
    public List<?> warnings() { return warnings; }
}
