package eu.simpl.vocabularymanager.domain;

/** The service cannot take the request now and the client should retry (D43, D44, D92). */
public class ServiceBusyException extends RuntimeException {

    /** Detail when the store is down, slow, or the write budget ran out (D44). */
    public static final String STORE_DETAIL = "The vocabulary store is not available. Retry later.";

    /** Detail when all upload slots are taken (D92). */
    public static final String UPLOADS_DETAIL = "Too many uploads in progress. Retry later.";

    /** Detail when the realm keys cannot be fetched (D44). */
    public static final String IDENTITY_DETAIL = "The identity provider is not available. Retry later.";

    /** @param detail one of the decided detail texts */
    public ServiceBusyException(String detail) {
        super(detail);
    }

    /**
     * @param detail one of the decided detail texts
     * @param cause the underlying failure
     */
    public ServiceBusyException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
