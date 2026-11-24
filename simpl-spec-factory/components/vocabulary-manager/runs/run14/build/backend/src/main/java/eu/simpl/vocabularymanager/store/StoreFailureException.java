package eu.simpl.vocabularymanager.store;

/**
 * The store answered with an error that is not a timeout or a connection failure: 500 for the client (D44). A
 * store 5xx counts for the circuit breaker, a 4xx does not (D134, D201).
 */
public class StoreFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean serverError;

    public StoreFailureException(String message, Throwable cause, boolean serverError) {
        super(message, cause);
        this.serverError = serverError;
    }

    /** True for a store 5xx. */
    public boolean serverError() {
        return serverError;
    }
}
