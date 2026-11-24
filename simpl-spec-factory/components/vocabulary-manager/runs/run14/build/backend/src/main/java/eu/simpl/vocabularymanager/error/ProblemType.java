package eu.simpl.vocabularymanager.error;

/**
 * The problem types of the component (D48, D104). Each type is the error code of one outcome.
 */
public enum ProblemType {
    BAD_REQUEST(
            400,
            "urn:problem-type:belgif:badRequest",
            "Bad Request",
            "https://www.belgif.be/specification/rest/api-guide/problems/badRequest.html",
            "The input message is incorrect"),
    UNAUTHORIZED(401, "urn:problem-type:simpl:unauthorized", "Unauthorized", null, "No valid access token."),
    FORBIDDEN(
            403,
            "urn:problem-type:simpl:forbidden",
            "Forbidden",
            null,
            "The access token does not grant the role this operation needs."),
    NOT_FOUND(
            404,
            "urn:problem-type:belgif:resourceNotFound",
            "Resource Not Found",
            "https://www.belgif.be/specification/rest/api-guide/problems/resourceNotFound.html",
            "The requested resource was not found."),
    METHOD_NOT_ALLOWED(
            405,
            "urn:problem-type:simpl:methodNotAllowed",
            "Method Not Allowed",
            null,
            "The method is not allowed on this resource."),
    NOT_ACCEPTABLE(
            406,
            "urn:problem-type:simpl:notAcceptable",
            "Not Acceptable",
            null,
            "Requested media type cannot be served."),
    CONFLICT(409, "urn:problem-type:simpl:conflict", "Conflict", null, "Conflict."),
    PAYLOAD_TOO_LARGE(
            413,
            "urn:problem-type:simpl:payloadTooLarge",
            "Payload Too Large",
            null,
            "The request is larger than the maximum allowed size of 6 MB."),
    URI_TOO_LONG(414, "urn:problem-type:simpl:uriTooLong", "URI Too Long", null, "The request URI is too long."),
    UNSUPPORTED_MEDIA_TYPE(
            415,
            "urn:problem-type:simpl:unsupportedMediaType",
            "Unsupported Media Type",
            null,
            "Request content type is not supported."),
    HEADERS_TOO_LARGE(
            431,
            "urn:problem-type:simpl:requestHeaderFieldsTooLarge",
            "Request Header Fields Too Large",
            null,
            "The request headers are too large."),
    INTERNAL_SERVER_ERROR(
            500,
            "urn:problem-type:belgif:internalServerError",
            "Internal Server Error",
            "https://www.belgif.be/specification/rest/api-guide/problems/internalServerError.html",
            "Internal Server Error"),
    SERVICE_UNAVAILABLE(
            503,
            "urn:problem-type:belgif:serviceUnavailable",
            "Service Unavailable",
            "https://www.belgif.be/specification/rest/api-guide/problems/serviceUnavailable.html",
            "The vocabulary store is not available. Retry later.");

    private final int status;
    private final String uri;
    private final String title;
    private final String href;
    private final String defaultDetail;

    ProblemType(int status, String uri, String title, String href, String defaultDetail) {
        this.status = status;
        this.uri = uri;
        this.title = title;
        this.href = href;
        this.defaultDetail = defaultDetail;
    }

    public int status() {
        return status;
    }

    public String uri() {
        return uri;
    }

    public String title() {
        return title;
    }

    public String href() {
        return href;
    }

    public String defaultDetail() {
        return defaultDetail;
    }

    /** Finds the type for an HTTP status, falling back to the generic 400 or 500 type. */
    public static ProblemType forStatus(int status) {
        for (ProblemType type : values()) {
            if (type.status == status) {
                return type;
            }
        }
        return status >= 500 ? INTERNAL_SERVER_ERROR : BAD_REQUEST;
    }
}
