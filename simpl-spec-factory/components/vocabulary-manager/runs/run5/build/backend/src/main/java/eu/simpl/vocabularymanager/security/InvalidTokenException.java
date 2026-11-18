package eu.simpl.vocabularymanager.security;

/** The bearer token is missing, malformed, unsigned, expired or for another issuer or audience (gives 401). */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
