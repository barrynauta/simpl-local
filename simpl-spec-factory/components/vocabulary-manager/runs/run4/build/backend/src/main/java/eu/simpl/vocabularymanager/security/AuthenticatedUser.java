package eu.simpl.vocabularymanager.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/** A caller whose bearer token passed validation. The name is the creator value of decision D27. */
public class AuthenticatedUser extends AbstractAuthenticationToken {

    private final String userId;
    private final String subject;

    public AuthenticatedUser(String userId, String subject, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.subject = subject;
        setAuthenticated(true);
    }

    /** The opaque token subject, used in logs instead of the e-mail address (CODE-32). */
    public String subject() {
        return subject;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public String getName() {
        return userId;
    }
}
