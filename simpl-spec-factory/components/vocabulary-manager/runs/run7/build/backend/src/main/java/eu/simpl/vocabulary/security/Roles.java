package eu.simpl.vocabulary.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Realm roles (D16, E1) and the user facts the service needs (D27, D80). */
public final class Roles {

    public static final String ADMIN = "GA_VOCABULARY_ADMIN";
    public static final String VIEWER = "GA_VOCABULARY_VIEWER";

    private Roles() {}

    /**
     * Reads the realm roles from realm_access.roles.
     *
     * @param jwt the token
     * @return one authority per realm role
     */
    public static Collection<GrantedAuthority> authorities(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object roles = map.get("roles");
        if (!(roles instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .filter(String.class::isInstance)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority((String) role))
                .toList();
    }

    /**
     * Tells whether the authentication holds the role.
     *
     * @param authentication the authentication
     * @param role the role
     * @return true when present
     */
    public static boolean has(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    /**
     * The creator of a resource: email claim, else preferred_username (D27).
     *
     * @return the creator, or "unknown" without a token
     */
    public static String creator() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return "unknown";
        }
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username == null || username.isBlank() ? jwt.getSubject() : username;
    }

    /**
     * The user id for logs: the token subject, never a name or email (D80).
     *
     * @return the subject or "anonymous"
     */
    public static String userId() {
        Jwt jwt = currentJwt();
        return jwt == null || jwt.getSubject() == null ? "anonymous" : jwt.getSubject();
    }

    private static Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        return null;
    }
}
