package eu.simpl.vocabularymanager.error;

/**
 * The problem types of the service (decisions D48, D104). Each type is the error code: type URN,
 * title, status and, for the Belgif types, the documentation href.
 */
public enum ProblemType {
    BAD_REQUEST("urn:problem-type:belgif:badRequest", "Bad Request", 400, "badRequest"),
    UNAUTHORIZED("urn:problem-type:simpl:unauthorized", "Unauthorized", 401, null),
    FORBIDDEN("urn:problem-type:simpl:forbidden", "Forbidden", 403, null),
    NOT_FOUND("urn:problem-type:belgif:resourceNotFound", "Resource Not Found", 404, "resourceNotFound"),
    METHOD_NOT_ALLOWED("urn:problem-type:simpl:methodNotAllowed", "Method Not Allowed", 405, null),
    NOT_ACCEPTABLE("urn:problem-type:simpl:notAcceptable", "Not Acceptable", 406, null),
    CONFLICT("urn:problem-type:simpl:conflict", "Conflict", 409, null),
    PAYLOAD_TOO_LARGE("urn:problem-type:simpl:payloadTooLarge", "Payload Too Large", 413, null),
    URI_TOO_LONG("urn:problem-type:simpl:uriTooLong", "URI Too Long", 414, null),
    UNSUPPORTED_MEDIA_TYPE("urn:problem-type:simpl:unsupportedMediaType", "Unsupported Media Type", 415, null),
    HEADERS_TOO_LARGE(
            "urn:problem-type:simpl:requestHeaderFieldsTooLarge", "Request Header Fields Too Large", 431, null),
    INTERNAL_ERROR("urn:problem-type:belgif:internalServerError", "Internal Server Error", 500, "internalServerError"),
    SERVICE_UNAVAILABLE("urn:problem-type:belgif:serviceUnavailable", "Service Unavailable", 503, "serviceUnavailable");

    private static final String BELGIF_PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";

    private final String type;
    private final String title;
    private final int status;
    private final String hrefName;

    ProblemType(String type, String title, int status, String hrefName) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.hrefName = hrefName;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public int status() {
        return status;
    }

    /** Documentation link of a Belgif problem type, or null for the Simpl types. */
    public String href() {
        return hrefName == null ? null : BELGIF_PROBLEMS + hrefName + ".html";
    }

    /** The problem type for an HTTP status the server itself produced. */
    public static ProblemType forStatus(int status) {
        for (ProblemType p : values()) {
            if (p.status == status) {
                return p;
            }
        }
        return status >= 500 ? INTERNAL_ERROR : BAD_REQUEST;
    }
}
