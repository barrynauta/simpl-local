package eu.simpl.vocabulary.store;

/** The store did not answer: connection failure or timeout (D44). Maps to 503. */
public class StoreUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
