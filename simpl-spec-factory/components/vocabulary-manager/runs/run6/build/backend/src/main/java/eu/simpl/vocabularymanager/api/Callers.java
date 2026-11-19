package eu.simpl.vocabularymanager.api;

import org.springframework.security.oauth2.jwt.Jwt;

/** Who sent a request (decision D27). */
public final class Callers {

    private Callers() {}

    /**
     * The creator: the email claim of the token, else preferred_username, else the subject.
     *
     * @param jwt the validated token
     * @return the creator
     */
    public static String creator(Jwt jwt) {
        if (jwt == null) {
            return "unknown";
        }
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return jwt.getSubject() == null ? "unknown" : jwt.getSubject();
    }
}
