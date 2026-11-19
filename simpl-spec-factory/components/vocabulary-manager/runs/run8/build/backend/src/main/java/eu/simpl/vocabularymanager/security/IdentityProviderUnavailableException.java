package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.KeySourceException;

/** The realm keys are needed and cannot be fetched (D44): the request fails closed with 503. */
public class IdentityProviderUnavailableException extends KeySourceException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what failed
     * @param cause the cause, or null
     */
    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
