package eu.simpl.vocabularymanager.security;

import java.util.Set;

/**
 * The caller, from a validated access token.
 *
 * @param subject token subject
 * @param creator value stored as creator: email claim, else preferred_username (D27)
 * @param roles realm roles of D16 the caller holds
 */
public record AuthenticatedUser(String subject, String creator, Set<Role> roles) {

    /** True when the caller holds at least one of the roles. */
    public boolean hasAnyRole(Set<Role> wanted) {
        return roles.stream().anyMatch(wanted::contains);
    }
}
