package eu.simpl.vocabularymanager.error;

/** Errors caused by the service or its dependencies (decision D44). */
public enum TechnicalErrorCode implements ErrorCode {
    INTERNAL(
            500,
            "urn:problem-type:belgif:internalServerError",
            Problems.BELGIF_PROBLEMS + "internalServerError.html",
            "Internal Server Error"),
    SERVICE_UNAVAILABLE(
            503,
            "urn:problem-type:belgif:serviceUnavailable",
            Problems.BELGIF_PROBLEMS + "serviceUnavailable.html",
            "Service Unavailable");

    private final int status;
    private final String type;
    private final String href;
    private final String title;

    TechnicalErrorCode(int status, String type, String href, String title) {
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
