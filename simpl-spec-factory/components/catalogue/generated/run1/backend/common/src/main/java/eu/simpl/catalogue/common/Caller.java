package eu.simpl.catalogue.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** The authenticated caller, read from the validated JWT (claims of D65). */
public record Caller(String token, String issuer, String azp, String participantId,
                     List<String> roles, List<String> identityAttributes) {

    public static Caller current() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (!(a instanceof JwtAuthenticationToken jat)) {
            throw new ApiException(ProblemTypes.UNAUTHORIZED, "The request lacks valid authentication credentials.");
        }
        return of(jat.getToken());
    }

    public static Caller of(Jwt jwt) {
        List<String> roles = new ArrayList<>();
        Object ra = jwt.getClaims().get("realm_access");
        if (ra instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> c) {
            c.forEach(r -> roles.add(String.valueOf(r)));
        }
        List<String> attrs = new ArrayList<>();
        Object ia = jwt.getClaims().get("identity_attributes");
        if (ia instanceof Collection<?> c) {
            c.forEach(x -> attrs.add(String.valueOf(x)));
        } else if (ia instanceof String s && !s.isBlank()) {
            for (String p : s.split(",")) if (!p.isBlank()) attrs.add(p.trim());
        }
        Object pid = jwt.getClaims().get("participant_id");
        String iss = jwt.getIssuer() == null ? jwt.getClaimAsString("iss") : jwt.getIssuer().toString();
        return new Caller(jwt.getTokenValue(), iss, jwt.getClaimAsString("azp"),
                pid == null ? null : String.valueOf(pid), roles, attrs);
    }

    public boolean hasAny(List<String> wanted) {
        return wanted.stream().anyMatch(roles::contains);
    }

    /** "Any one of" role check (D66). 403 when none is present. */
    public Caller requireAny(List<String> wanted) {
        if (!hasAny(wanted)) {
            throw new ApiException(ProblemTypes.FORBIDDEN,
                    "The authenticated user does not have permission to perform this operation.");
        }
        return this;
    }

    public String bearer() { return "Bearer " + token; }

    /** Comma separated attribute codes, the User-Attributes format of D69. */
    public String attributesHeader() { return String.join(",", identityAttributes); }
}
