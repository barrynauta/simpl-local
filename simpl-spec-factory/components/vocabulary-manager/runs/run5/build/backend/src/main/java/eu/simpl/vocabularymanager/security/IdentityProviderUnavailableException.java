package eu.simpl.vocabularymanager.security;

/** The realm keys could not be fetched while a token named an unknown key id (D44, gives 503). */
public class IdentityProviderUnavailableException extends RuntimeException {

    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
