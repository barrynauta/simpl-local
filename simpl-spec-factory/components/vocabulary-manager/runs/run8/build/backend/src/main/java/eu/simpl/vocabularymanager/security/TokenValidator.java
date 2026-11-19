package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates bearer tokens with Nimbus JOSE+JWT (REUSE-01): signature against the realm keys, issuer, audience, expiry
 * and not-before (OWT-A07, E5). Never accepts a token without a signature check.
 */
public class TokenValidator {

    private static final Set<JWSAlgorithm> ALGORITHMS = Set.of(
            JWSAlgorithm.RS256,
            JWSAlgorithm.RS384,
            JWSAlgorithm.RS512,
            JWSAlgorithm.PS256,
            JWSAlgorithm.ES256,
            JWSAlgorithm.ES384);
    private static final int CLOCK_SKEW_SECONDS = 30;

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    /**
     * @param keySource the realm keys
     * @param issuer the accepted issuer
     * @param audience the required audience
     */
    public TokenValidator(RealmKeySource keySource, String issuer, String audience) {
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(ALGORITHMS, keySource));
        DefaultJWTClaimsVerifier<SecurityContext> verifier = new DefaultJWTClaimsVerifier<>(
                Set.of(audience),
                new JWTClaimsSet.Builder().issuer(issuer).build(),
                Set.of("sub", "exp", "iss", "aud"),
                Set.of());
        verifier.setMaxClockSkew(CLOCK_SKEW_SECONDS);
        jwtProcessor.setJWTClaimsSetVerifier(verifier);
        this.processor = jwtProcessor;
    }

    /**
     * @param token the compact JWT
     * @return the caller
     * @throws InvalidTokenException when the token is not acceptable
     * @throws IdentityProviderUnavailableException when the keys are needed and cannot be fetched
     */
    public AuthenticatedUser validate(String token) throws InvalidTokenException, IdentityProviderUnavailableException {
        JWTClaimsSet claims;
        try {
            claims = processor.process(token, null);
        } catch (IdentityProviderUnavailableException e) {
            throw e;
        } catch (ParseException | BadJOSEException | JOSEException e) {
            throw new InvalidTokenException(e.getMessage(), e);
        }
        return toUser(claims);
    }

    private static AuthenticatedUser toUser(JWTClaimsSet claims) throws InvalidTokenException {
        try {
            String email = claims.getStringClaim("email");
            String username = claims.getStringClaim("preferred_username");
            String creator = email != null && !email.isBlank() ? email : username;
            if (creator == null || creator.isBlank()) {
                creator = claims.getSubject();
            }
            return new AuthenticatedUser(claims.getSubject(), creator, realmRoles(claims));
        } catch (ParseException e) {
            throw new InvalidTokenException("Token claims have unexpected types", e);
        }
    }

    private static Set<String> realmRoles(JWTClaimsSet claims) {
        Object realmAccess = claims.getClaim("realm_access");
        Set<String> roles = new LinkedHashSet<>();
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> values) {
            for (Object value : values) {
                if (value instanceof String role) {
                    roles.add(role);
                }
            }
        }
        return Set.copyOf(roles.isEmpty() ? List.of() : roles);
    }

    /** The token is not acceptable: 401. */
    public static class InvalidTokenException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * @param message why
         * @param cause the cause
         */
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
