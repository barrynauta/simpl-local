package eu.simpl.vocabularymanager.error;

/**
 * The problem types of the component (D48, D104). They serve as error codes: one value per error
 * category, with the HTTP status, the type URN, the title and the Belgif documentation link.
 */
public enum ProblemType {
    BAD_REQUEST(
            400,
            "urn:problem-type:belgif:badRequest",
            "Bad Request",
            "https://www.belgif.be/specification/rest/api-guide/problems/badRequest.html"),
    UNAUTHORIZED(401, "urn:problem-type:simpl:unauthorized", "Unauthorized", null),
    FORBIDDEN(403, "urn:problem-type:simpl:forbidden", "Forbidden", null),
    NOT_FOUND(
            404,
            "urn:problem-type:belgif:resourceNotFound",
            "Resource Not Found",
            "https://www.belgif.be/specification/rest/api-guide/problems/resourceNotFound.html"),
    METHOD_NOT_ALLOWED(405, "urn:problem-type:simpl:methodNotAllowed", "Method Not Allowed", null),
    NOT_ACCEPTABLE(406, "urn:problem-type:simpl:notAcceptable", "Not Acceptable", null),
    CONFLICT(409, "urn:problem-type:simpl:conflict", "Conflict", null),
    PAYLOAD_TOO_LARGE(413, "urn:problem-type:simpl:payloadTooLarge", "Payload Too Large", null),
    UNSUPPORTED_MEDIA_TYPE(415, "urn:problem-type:simpl:unsupportedMediaType", "Unsupported Media Type", null),
    INTERNAL_SERVER_ERROR(
            500,
            "urn:problem-type:belgif:internalServerError",
            "Internal Server Error",
            "https://www.belgif.be/specification/rest/api-guide/problems/internalServerError.html"),
    SERVICE_UNAVAILABLE(
            503,
            "urn:problem-type:belgif:serviceUnavailable",
            "Service Unavailable",
            "https://www.belgif.be/specification/rest/api-guide/problems/serviceUnavailable.html");

    private final int status;
    private final String type;
    private final String title;
    private final String href;

    ProblemType(int status, String type, String title, String href) {
        this.status = status;
        this.type = type;
        this.title = title;
        this.href = href;
    }

    public int status() {
        return status;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String href() {
        return href;
    }
}
