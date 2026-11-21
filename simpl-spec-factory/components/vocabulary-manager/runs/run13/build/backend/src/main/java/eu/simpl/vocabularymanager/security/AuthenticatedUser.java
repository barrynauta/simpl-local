package eu.simpl.vocabularymanager.security;

import java.util.Set;

/**
 * The caller of a request, taken from a verified token.
 *
 * @param subject the token subject, the only user identifier that goes into logs (D80)
 * @param creator the creator value stored with new data: email, else preferred_username (D27)
 * @param roles the realm roles of the token (D16)
 */
public record AuthenticatedUser(String subject, String creator, Set<String> roles) {

    public static final String ADMIN_ROLE = "GA_VOCABULARY_ADMIN";
    public static final String VIEWER_ROLE = "GA_VOCABULARY_VIEWER";
    public static final String REQUEST_ATTRIBUTE = AuthenticatedUser.class.getName();

    public boolean canRead() {
        return roles.contains(ADMIN_ROLE) || roles.contains(VIEWER_ROLE);
    }

    public boolean canWrite() {
        return roles.contains(ADMIN_ROLE);
    }
}
