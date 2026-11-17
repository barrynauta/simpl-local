package eu.simpl.cm.uibe;

import eu.simpl.cm.common.web.ApiException;
import eu.simpl.cm.common.web.Problems;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

/** D68, D69: role and participant checks on the token. */
public record UserContext(String username, List<String> roles) {
    public static final String CONTRACT_MANAGER = "contract_manager";
    public static final String CONSENT_MANAGER = "consent_manager";

    @SuppressWarnings("unchecked")
    public static UserContext of(Jwt jwt, UibeSettings settings) {
        if (jwt == null) {
            throw new ApiException(401, Problems.UNAUTHORIZED, "A valid bearer token is required");
        }
        Object pid = jwt.getClaims().get("participant_id");
        if (pid == null || !settings.participantId.equals(pid.toString())) {
            throw new ApiException(403, Problems.FORBIDDEN, "The token belongs to another participant");
        }
        List<String> roles = List.of();
        Object ra = jwt.getClaims().get("realm_access");
        if (ra instanceof Map<?, ?> m && m.get("roles") instanceof List<?> l) {
            roles = (List<String>) l.stream().map(Object::toString).toList();
        }
        UserContext u = new UserContext(jwt.getClaimAsString("preferred_username"), roles);
        if (!u.has(CONTRACT_MANAGER)) {
            throw new ApiException(403, Problems.FORBIDDEN, "The role contract_manager is required");
        }
        return u;
    }

    public boolean has(String role) {
        return roles.contains(role);
    }
}
