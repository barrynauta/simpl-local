package eu.simpl.vocabularymanager.error;

/**
 * The problem types of the API (decisions D48, D104). Each type is an error code: it fixes the URN, the
 * title and the HTTP status of the problem detail (RFC 9457) that the client receives.
 */
public enum ProblemType {
    BAD_REQUEST("urn:problem-type:belgif:badRequest", "Bad Request", 400, belgifProblem("badRequest")),
    UNAUTHORIZED("urn:problem-type:simpl:unauthorized", "Unauthorized", 401, null),
    FORBIDDEN("urn:problem-type:simpl:forbidden", "Forbidden", 403, null),
    NOT_FOUND("urn:problem-type:belgif:resourceNotFound", "Resource Not Found", 404, belgifProblem("resourceNotFound")),
    METHOD_NOT_ALLOWED("urn:problem-type:simpl:methodNotAllowed", "Method Not Allowed", 405, null),
    NOT_ACCEPTABLE("urn:problem-type:simpl:notAcceptable", "Not Acceptable", 406, null),
    CONFLICT("urn:problem-type:simpl:conflict", "Conflict", 409, null),
    PAYLOAD_TOO_LARGE("urn:problem-type:simpl:payloadTooLarge", "Payload Too Large", 413, null),
    URI_TOO_LONG("urn:problem-type:simpl:uriTooLong", "URI Too Long", 414, null),
    UNSUPPORTED_MEDIA_TYPE("urn:problem-type:simpl:unsupportedMediaType", "Unsupported Media Type", 415, null),
    HEADERS_TOO_LARGE(
            "urn:problem-type:simpl:requestHeaderFieldsTooLarge", "Request Header Fields Too Large", 431, null),
    INTERNAL_SERVER_ERROR(
            "urn:problem-type:belgif:internalServerError",
            "Internal Server Error",
            500,
            belgifProblem("internalServerError")),
    SERVICE_UNAVAILABLE(
            "urn:problem-type:belgif:serviceUnavailable",
            "Service Unavailable",
            503,
            belgifProblem("serviceUnavailable"));

    private final String type;
    private final String title;
    private final int status;
    private final String href;

    ProblemType(String type, String title, int status, String href) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.href = href;
    }

    private static String belgifProblem(String name) {
        return "https://www.belgif.be/specification/rest/api-guide/problems/" + name + ".html";
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

    /** Link to the problem type documentation, or null when there is none. */
    public String href() {
        return href;
    }

    /** The problem type for an HTTP status, used for answers that no handler produced. */
    public static ProblemType forStatus(int status) {
        for (ProblemType candidate : values()) {
            if (candidate.status == status) {
                return candidate;
            }
        }
        return status >= 500 ? INTERNAL_SERVER_ERROR : BAD_REQUEST;
    }
}
