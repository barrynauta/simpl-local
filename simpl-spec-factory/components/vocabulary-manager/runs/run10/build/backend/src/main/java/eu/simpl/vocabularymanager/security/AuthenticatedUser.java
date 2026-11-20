package eu.simpl.vocabularymanager.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The caller after token validation: the token subject (used in logs, D80), the creator value (D27) and the realm
 * roles as authorities.
 */
public class AuthenticatedUser extends AbstractAuthenticationToken {

    private final String subject;
    private final String creator;
    private final Set<String> roles;

    public AuthenticatedUser(String subject, String creator, Set<String> roles) {
        super(roles.stream().map(SimpleGrantedAuthority::new).toList());
        this.subject = subject;
        this.creator = creator;
        this.roles = Set.copyOf(roles);
        setAuthenticated(true);
    }

    public String subject() {
        return subject;
    }

    /** The email claim, else preferred_username, else the subject (D27). */
    public String creator() {
        return creator;
    }

    public Set<String> roles() {
        return roles;
    }

    public boolean hasAnyRole(Collection<String> wanted) {
        return wanted.stream().anyMatch(roles::contains);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return subject;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
