package eu.simpl.vocabularymanager.security;

import java.util.List;

/**
 * The caller.
 *
 * @param subject token subject, the only user id written to logs (D80)
 * @param creator email claim or preferred_username, stored as createdBy (D27)
 * @param roles realm roles (E1)
 */
public record AuthenticatedUser(String subject, String creator, List<String> roles) {

    public AuthenticatedUser {
        roles = List.copyOf(roles);
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[subject=" + subject + "]";
    }
}
