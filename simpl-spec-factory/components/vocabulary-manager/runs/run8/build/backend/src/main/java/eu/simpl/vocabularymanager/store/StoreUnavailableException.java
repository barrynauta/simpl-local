package eu.simpl.vocabularymanager.store;

/** The store is unreachable or does not answer in time: 503 with Retry-After (D44). */
public class StoreUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what failed
     * @param cause the cause, or null
     */
    public StoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
