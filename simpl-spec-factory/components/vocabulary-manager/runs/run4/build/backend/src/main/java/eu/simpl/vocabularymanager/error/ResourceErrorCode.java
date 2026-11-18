package eu.simpl.vocabularymanager.error;

/** Errors about the state of resources: not found and conflicts. */
public enum ResourceErrorCode implements ErrorCode {
    NOT_FOUND(
            404,
            "urn:problem-type:belgif:resourceNotFound",
            Problems.BELGIF_PROBLEMS + "resourceNotFound.html",
            "Resource Not Found"),
    CONFLICT(409, "urn:problem-type:simpl:conflict", null, "Conflict");

    private final int status;
    private final String type;
    private final String href;
    private final String title;

    ResourceErrorCode(int status, String type, String href, String title) {
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
