package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.simpl.vocabularymanager.config.VocabularyProperties;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates Keycloak access tokens locally with the realm keys (D17, D44, OWT-A07): signature, issuer, audience,
 * expiry and not-before. Only asymmetric signatures are accepted.
 */
@Component
public class TokenValidator {

    private static final Set<JWSAlgorithm> ALLOWED = Set.of(
            JWSAlgorithm.RS256,
            JWSAlgorithm.RS384,
            JWSAlgorithm.RS512,
            JWSAlgorithm.PS256,
            JWSAlgorithm.PS384,
            JWSAlgorithm.PS512,
            JWSAlgorithm.ES256,
            JWSAlgorithm.ES384,
            JWSAlgorithm.ES512);

    private final RealmKeys keys;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final Duration clockSkew;
    private final DefaultJWSVerifierFactory verifiers = new DefaultJWSVerifierFactory();

    public TokenValidator(RealmKeys keys, Clock clock, VocabularyProperties properties) {
        this.keys = keys;
        this.clock = clock;
        this.issuer = properties.identity().issuer();
        this.audience = properties.identity().audience();
        this.clockSkew = properties.identity().clockSkew();
    }

    /**
     * Validates a compact JWT.
     *
     * @param token the bearer token
     * @return the caller
     * @throws InvalidTokenException when the token is not acceptable
     * @throws IdentityProviderUnavailableException when the key is unknown and cannot be fetched
     */
    public AuthenticatedUser validate(String token) {
        SignedJWT jwt;
        JWTClaimsSet claims;
        try {
            jwt = SignedJWT.parse(token);
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new InvalidTokenException("Token is not a signed JWT");
        }
        JWSAlgorithm algorithm = jwt.getHeader().getAlgorithm();
        if (!ALLOWED.contains(algorithm)) {
            throw new InvalidTokenException("Token algorithm is not accepted");
        }
        JWK key = keys.find(jwt.getHeader().getKeyID())
                .orElseThrow(() -> new InvalidTokenException("Token key is unknown"));
        verifySignature(jwt, key);
        checkClaims(claims);
        return toUser(claims);
    }

    private void verifySignature(SignedJWT jwt, JWK key) {
        if (key.getKeyUse() != null && !KeyUse.SIGNATURE.equals(key.getKeyUse())) {
            throw new InvalidTokenException("Key is not a signing key");
        }
        if (!(key instanceof AsymmetricJWK asymmetric)) {
            throw new InvalidTokenException("Key is not asymmetric");
        }
        try {
            JWSVerifier verifier = verifiers.createJWSVerifier(jwt.getHeader(), asymmetric.toPublicKey());
            if (!jwt.verify(verifier)) {
                throw new InvalidTokenException("Token signature is not valid");
            }
        } catch (JOSEException e) {
            throw new InvalidTokenException("Token signature could not be checked");
        }
    }

    private void checkClaims(JWTClaimsSet claims) {
        Instant now = clock.instant();
        Date expiry = claims.getExpirationTime();
        if (expiry == null || now.isAfter(expiry.toInstant().plus(clockSkew))) {
            throw new InvalidTokenException("Token has expired");
        }
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && now.isBefore(notBefore.toInstant().minus(clockSkew))) {
            throw new InvalidTokenException("Token is not valid yet");
        }
        if (!issuer.equals(claims.getIssuer())) {
            throw new InvalidTokenException("Token issuer is not accepted");
        }
        List<String> audiences = claims.getAudience();
        if (audiences == null || !audiences.contains(audience)) {
            throw new InvalidTokenException("Token audience is not accepted");
        }
    }

    private static AuthenticatedUser toUser(JWTClaimsSet claims) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        Object realmAccess = claims.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> list) {
            for (Object value : list) {
                for (Role role : Role.values()) {
                    if (role.name().equals(value)) {
                        roles.add(role);
                    }
                }
            }
        }
        String creator = stringClaim(claims, "email");
        if (creator == null || creator.isBlank()) {
            creator = stringClaim(claims, "preferred_username");
        }
        if (creator == null || creator.isBlank()) {
            creator = claims.getSubject();
        }
        return new AuthenticatedUser(claims.getSubject(), creator, roles);
    }

    private static String stringClaim(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        return value instanceof String s ? s : null;
    }
}
