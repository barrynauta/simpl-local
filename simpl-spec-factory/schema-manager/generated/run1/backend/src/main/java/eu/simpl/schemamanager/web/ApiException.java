package eu.simpl.schemamanager.web;

import java.util.List;
import org.springframework.http.HttpStatus;

/** An error that maps to one problem+json response (D63). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<Issue> issues;

    public ApiException(HttpStatus status, String detail) {
        this(status, detail, null);
    }

    public ApiException(HttpStatus status, String detail, List<Issue> issues) {
        super(detail);
        this.status = status;
        this.issues = issues;
    }

    public HttpStatus status() {
        return status;
    }

    public String detail() {
        return getMessage();
    }

    public List<Issue> issues() {
        return issues;
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, detail);
    }

    public static ApiException notAcceptable() {
        return new ApiException(HttpStatus.NOT_ACCEPTABLE, Problems.NOT_ACCEPTABLE_DETAIL);
    }

    public static ApiException unsupportedMediaType() {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, Problems.UNSUPPORTED_DETAIL);
    }
}
