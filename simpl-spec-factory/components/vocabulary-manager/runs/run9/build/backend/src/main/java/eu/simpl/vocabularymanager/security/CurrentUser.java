package eu.simpl.vocabularymanager.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The user of the current request. */
public final class CurrentUser {

    private CurrentUser() {}

    /** Creator of D27: the email claim, else preferred_username, else the subject. */
    public static String creator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            String email = token.getToken().getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            String username = token.getToken().getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) {
                return username;
            }
            return token.getToken().getSubject();
        }
        return authentication == null ? "unknown" : authentication.getName();
    }

    /** The token subject, the only user id in logs (D80). */
    public static String subject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken().getSubject();
        }
        return null;
    }
}
