package eu.simpl.vocabularymanager.store;

/** The store did not answer in time or could not be reached (D44, gives 503). */
public class StoreUnavailableException extends RuntimeException {

    public StoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
