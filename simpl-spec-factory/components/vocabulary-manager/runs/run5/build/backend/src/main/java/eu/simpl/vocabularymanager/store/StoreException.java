package eu.simpl.vocabularymanager.store;

/** The store answered with an error that is not a timeout or a connection failure (D44, gives 500). */
public class StoreException extends RuntimeException {

    public StoreException(String message) {
        super(message);
    }

    public StoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
