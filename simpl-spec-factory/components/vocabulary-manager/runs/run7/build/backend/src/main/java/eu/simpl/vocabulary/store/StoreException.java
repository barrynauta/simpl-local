package eu.simpl.vocabulary.store;

/** The store answered with an error that is not a timeout or a connection failure (D44). Maps to 500. */
public class StoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StoreException(String message) {
        super(message);
    }

    public StoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
