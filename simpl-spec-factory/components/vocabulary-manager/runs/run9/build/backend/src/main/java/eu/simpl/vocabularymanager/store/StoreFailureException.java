package eu.simpl.vocabularymanager.store;

/** The store answered with an error that is not a timeout or a connection failure (D44). Mapped to 500. */
public class StoreFailureException extends RuntimeException {

    /** @param message what failed */
    public StoreFailureException(String message) {
        super(message);
    }

    /**
     * @param message what failed
     * @param cause the cause
     */
    public StoreFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
