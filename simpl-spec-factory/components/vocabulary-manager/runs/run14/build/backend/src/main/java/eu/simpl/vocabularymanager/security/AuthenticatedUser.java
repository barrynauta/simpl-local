package eu.simpl.vocabularymanager.security;

import java.util.Set;

/**
 * The caller of a request.
 *
 * @param subject the token subject, the only user id written to logs (D80)
 * @param creator the creator value of D27: email, else preferred_username (personal data, D165)
 * @param roles the realm roles
 */
public record AuthenticatedUser(String subject, String creator, Set<String> roles) {

    public static final String ADMIN_ROLE = "GA_VOCABULARY_ADMIN";
    public static final String VIEWER_ROLE = "GA_VOCABULARY_VIEWER";

    /** True for a user with either vocabulary management role. */
    public boolean canRead() {
        return roles.contains(ADMIN_ROLE) || roles.contains(VIEWER_ROLE);
    }

    /** True for a user with the admin role. */
    public boolean canWrite() {
        return roles.contains(ADMIN_ROLE);
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[subject=" + subject + "]";
    }
}
