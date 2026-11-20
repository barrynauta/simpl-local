package eu.simpl.vocabularymanager.security;

import org.springframework.security.core.AuthenticationException;

/** The token could not be checked because the realm keys cannot be fetched (D44): 503, never accepted. */
public class IdentityProviderUnavailableException extends AuthenticationException {

    /**
     * @param message what failed
     * @param cause the key fetch failure
     */
    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
