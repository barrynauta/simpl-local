package eu.simpl.vocabularymanager.error;

/** Errors caused by the form of the request (client errors before any business rule). */
public enum RequestErrorCode implements ErrorCode {
    BAD_REQUEST(400, "urn:problem-type:belgif:badRequest", Problems.BELGIF_PROBLEMS + "badRequest.html", "Bad Request"),
    PAYLOAD_TOO_LARGE(413, "urn:problem-type:simpl:payloadTooLarge", null, "Payload Too Large"),
    UNSUPPORTED_MEDIA_TYPE(415, "urn:problem-type:simpl:unsupportedMediaType", null, "Unsupported Media Type"),
    NOT_ACCEPTABLE(406, "urn:problem-type:simpl:notAcceptable", null, "Not Acceptable"),
    METHOD_NOT_ALLOWED(405, "urn:problem-type:simpl:methodNotAllowed", null, "Method Not Allowed");

    private final int status;
    private final String type;
    private final String href;
    private final String title;

    RequestErrorCode(int status, String type, String href, String title) {
        this.status = status;
        this.type = type;
        this.href = href;
        this.title = title;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String href() {
        return href;
    }

    @Override
    public String title() {
        return title;
    }
}
