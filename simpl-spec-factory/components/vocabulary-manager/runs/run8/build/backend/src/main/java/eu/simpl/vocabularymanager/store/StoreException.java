package eu.simpl.vocabularymanager.store;

/** A store error that is not a timeout or a connection failure: 500 (D44). */
public class StoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what failed
     * @param cause the cause, or null
     */
    public StoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
