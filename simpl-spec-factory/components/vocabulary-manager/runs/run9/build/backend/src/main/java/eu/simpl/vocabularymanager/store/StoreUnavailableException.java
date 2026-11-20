package eu.simpl.vocabularymanager.store;

/** The store did not answer: connection failure or timeout (D44). Mapped to 503. */
public class StoreUnavailableException extends RuntimeException {

    /**
     * @param message what failed
     * @param cause the transport failure
     */
    public StoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
