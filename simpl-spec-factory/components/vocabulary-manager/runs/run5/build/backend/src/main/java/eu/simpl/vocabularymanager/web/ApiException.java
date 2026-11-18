package eu.simpl.vocabularymanager.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** An outcome that ends a request with a problem response. Thrown by controllers and services. */
public class ApiException extends RuntimeException {

    private static final String BELGIF_PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";

    private final transient Problem problem;
    private final transient Map<String, String> headers;

    public ApiException(Problem problem, Map<String, String> headers) {
        super(problem.title() + ": " + problem.detail());
        this.problem = problem;
        this.headers = headers;
    }

    public Problem problem() {
        return problem;
    }

    public Map<String, String> headers() {
        return headers;
    }

    private static String instance() {
        return "urn:uuid:" + UUID.randomUUID();
    }

    /** 400 with input validation issues (belgif badRequest). */
    public static ApiException badRequest(List<Issue> issues) {
        return new ApiException(
                new Problem(
                        "urn:problem-type:belgif:badRequest",
                        BELGIF_PROBLEMS + "badRequest.html",
                        "Bad Request",
                        400,
                        "The input message is incorrect",
                        instance(),
                        issues),
                Map.of());
    }

    /** 400 with one input validation issue. */
    public static ApiException badRequest(Issue issue) {
        return badRequest(List.of(issue));
    }

    /** 404 with one issue pointing at the path parameter (VocabularyNotFoundResponse). */
    public static ApiException notFound(String detail, String parameter) {
        return new ApiException(
                new Problem(
                        "urn:problem-type:belgif:resourceNotFound",
                        BELGIF_PROBLEMS + "resourceNotFound.html",
                        "Resource Not Found",
                        404,
                        detail,
                        instance(),
                        parameter == null ? null : List.of(Issue.pointer("path", parameter, detail))),
                Map.of());
    }

    /** 404 for a path that has no operation (D55). */
    public static ApiException noSuchResource() {
        return new ApiException(
                new Problem(
                        "urn:problem-type:belgif:resourceNotFound",
                        BELGIF_PROBLEMS + "resourceNotFound.html",
                        "Resource Not Found",
                        404,
                        "The requested resource does not exist.",
                        instance(),
                        null),
                Map.of());
    }

    /** 409 (ConflictResponse). */
    public static ApiException conflict(String detail) {
        return simpl(409, "conflict", "Conflict", detail, Map.of());
    }

    /** 401 (D17). */
    public static ApiException unauthorized(String detail) {
        return simpl(401, "unauthorized", "Unauthorized", detail, Map.of("WWW-Authenticate", "Bearer"));
    }

    /** 403 (D17). */
    public static ApiException forbidden() {
        return simpl(403, "forbidden", "Forbidden", "You do not have the role this operation needs.", Map.of());
    }

    /** 413 (UploadTooLargeResponse, D15). */
    public static ApiException payloadTooLarge(String fieldName) {
        String detail = "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB."
                + " Please upload a smaller Turtle file.";
        Problem p = new Problem(
                "urn:problem-type:simpl:payloadTooLarge",
                null,
                "Payload Too Large",
                413,
                detail,
                instance(),
                List.of(Issue.invalidInput(fieldName, detail, null)));
        return new ApiException(p, Map.of());
    }

    /** 415 (D15). */
    public static ApiException unsupportedMediaType(String expected) {
        return simpl(
                415,
                "unsupportedMediaType",
                "Unsupported Media Type",
                "Request content type is not supported. Use " + expected + ".",
                Map.of());
    }

    /** 406. */
    public static ApiException notAcceptable() {
        return simpl(406, "notAcceptable", "Not Acceptable", "Requested media type cannot be served.", Map.of());
    }

    /** 405 (D55). */
    public static ApiException methodNotAllowed(String allow) {
        return simpl(
                405,
                "methodNotAllowed",
                "Method Not Allowed",
                "The method is not allowed on this resource.",
                allow == null ? Map.of() : Map.of("Allow", allow));
    }

    /** 503 (D44). */
    public static ApiException serviceUnavailable(String detail) {
        return new ApiException(
                new Problem(
                        "urn:problem-type:belgif:serviceUnavailable",
                        BELGIF_PROBLEMS + "serviceUnavailable.html",
                        "Service Unavailable",
                        503,
                        detail,
                        instance(),
                        null),
                Map.of("Retry-After", "5"));
    }

    /** 500 (InternalServerErrorResponse), without internal details (BLG-err-sanit). */
    public static ApiException internal() {
        return new ApiException(
                new Problem(
                        "urn:problem-type:belgif:internalServerError",
                        BELGIF_PROBLEMS + "internalServerError.html",
                        "Internal Server Error",
                        500,
                        "Internal Server Error",
                        instance(),
                        null),
                Map.of());
    }

    private static ApiException simpl(
            int status, String type, String title, String detail, Map<String, String> headers) {
        return new ApiException(
                new Problem("urn:problem-type:simpl:" + type, null, title, status, detail, instance(), null), headers);
    }
}
