package eu.simpl.vocabularymanager.store;

/**
 * A failed store call. {@link #unavailable()} is true for connection failures, timeouts and an open
 * circuit breaker (503, decision D44); otherwise the store answered with an error (500).
 */
public class StoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean unavailable;
    private final boolean serverError;

    public StoreException(String message, boolean unavailable, boolean serverError, Throwable cause) {
        super(message, cause);
        this.unavailable = unavailable;
        this.serverError = serverError;
    }

    public static StoreException unavailable(String message, Throwable cause) {
        return new StoreException(message, true, false, cause);
    }

    public static StoreException serverError(String message) {
        return new StoreException(message, false, true, null);
    }

    public static StoreException clientError(String message) {
        return new StoreException(message, false, false, null);
    }

    public boolean unavailable() {
        return unavailable;
    }

    /** True when the failure counts for the circuit breaker (timeouts, connection, store 5xx). */
    public boolean countsAsFailure() {
        return unavailable || serverError;
    }
}
