package eu.simpl.vocabularymanager.store;

/** The store answered with an error that is not a timeout or connection failure (decision D44, gives 500). */
public class StoreFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what failed
     */
    public StoreFailureException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param message what failed
     * @param cause the cause
     */
    public StoreFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
