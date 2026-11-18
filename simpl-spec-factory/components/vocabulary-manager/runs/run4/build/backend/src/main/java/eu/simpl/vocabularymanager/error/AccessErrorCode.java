package eu.simpl.vocabularymanager.error;

/** Authentication and authorisation errors (decision D17). */
public enum AccessErrorCode implements ErrorCode {
    UNAUTHORIZED(401, "urn:problem-type:simpl:unauthorized", null, "Unauthorized"),
    FORBIDDEN(403, "urn:problem-type:simpl:forbidden", null, "Forbidden");

    private final int status;
    private final String type;
    private final String href;
    private final String title;

    AccessErrorCode(int status, String type, String href, String title) {
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
