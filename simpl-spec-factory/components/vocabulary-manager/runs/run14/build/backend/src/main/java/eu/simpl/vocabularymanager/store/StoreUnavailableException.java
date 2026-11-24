package eu.simpl.vocabularymanager.store;

/** The store could not be reached or timed out (D44): a breaker failure, 503 for the client. */
public class StoreUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
