package eu.simpl.vocabularymanager.store;

/** The store did not answer: connection failure or timeout (decision D44, gives 503). */
public class StoreUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what failed
     * @param cause the transport failure
     */
    public StoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
