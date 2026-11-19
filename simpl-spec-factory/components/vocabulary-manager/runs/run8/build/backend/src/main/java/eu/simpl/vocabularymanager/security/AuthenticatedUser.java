package eu.simpl.vocabularymanager.security;

import java.util.Set;

/**
 * The caller, from a validated token.
 *
 * @param subject the token subject (sub), the only user id written to logs (D80)
 * @param creator the email claim, or preferred_username when there is no email (D27)
 * @param roles the realm roles (realm_access.roles)
 */
public record AuthenticatedUser(String subject, String creator, Set<String> roles) {

    public static final String ADMIN_ROLE = "GA_VOCABULARY_ADMIN";
    public static final String VIEWER_ROLE = "GA_VOCABULARY_VIEWER";

    /**
     * @return true when the user may read (D16)
     */
    public boolean canRead() {
        return roles.contains(ADMIN_ROLE) || roles.contains(VIEWER_ROLE);
    }

    /**
     * @return true when the user may write (D16)
     */
    public boolean canWrite() {
        return roles.contains(ADMIN_ROLE);
    }
}
