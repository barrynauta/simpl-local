package eu.simpl.vocabulary.security;

import com.nimbusds.jose.RemoteKeySourceException;

/** The realm keys were needed and could not be fetched (D44): the request fails closed with 503. */
public class IdpUnavailableException extends RemoteKeySourceException {

    private static final long serialVersionUID = 1L;

    public IdpUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
