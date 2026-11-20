package eu.simpl.vocabularymanager.store;

/**
 * Failure of one store call. Internal to the store package: the client maps it to the problem of
 * D44 (503 for UNAVAILABLE) or 500. UNAVAILABLE and SERVER_ERROR count as circuit breaker failures
 * (D134); CLIENT_ERROR does not.
 */
public class StoreCallException extends Exception {

    private static final long serialVersionUID = 1L;

    /** What went wrong. */
    public enum Kind {
        UNAVAILABLE,
        SERVER_ERROR,
        CLIENT_ERROR
    }

    private final Kind kind;

    public StoreCallException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
