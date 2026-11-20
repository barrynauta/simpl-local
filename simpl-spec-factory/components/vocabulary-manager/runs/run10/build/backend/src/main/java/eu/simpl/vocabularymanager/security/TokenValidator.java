package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ProblemException;
import eu.simpl.vocabularymanager.error.Problems;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates a bearer token on every call (D17, E5, OWT-A07): signature with a realm key (Nimbus), algorithm,
 * issuer, audience, expiry and not-before. Only asymmetric algorithms are accepted.
 */
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
    private static final String INVALID = "The access token is not valid.";

    private final AppProperties.Security settings;
    private final RealmKeyProvider keys;
    private final Clock clock;
    private final DefaultJWSVerifierFactory verifierFactory = new DefaultJWSVerifierFactory();

    public TokenValidator(AppProperties.Security settings, RealmKeyProvider keys, Clock clock) {
        this.settings = settings;
        this.keys = keys;
        this.clock = clock;
    }

    /**
     * Validates the token and returns the caller.
     *
     * @throws ProblemException 401 for an invalid token, 503 when the realm keys cannot be fetched
     */
    public AuthenticatedUser validate(String token) {
        SignedJWT jwt = parse(token);
        JWSHeader header = jwt.getHeader();
        if (!ALLOWED.contains(header.getAlgorithm()) || header.getKeyID() == null) {
            throw Problems.unauthorized(INVALID);
        }
        JWK key = keys.keyFor(header.getKeyID());
        verifySignature(jwt, header, key);
        JWTClaimsSet claims = claims(jwt);
        checkClaims(claims);
        return toUser(claims);
    }

    private static SignedJWT parse(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException | RuntimeException e) {
            throw Problems.unauthorized(INVALID);
        }
    }

    private void verifySignature(SignedJWT jwt, JWSHeader header, JWK key) {
        if (!(key instanceof AsymmetricJWK asymmetric)) {
            throw Problems.unauthorized(INVALID);
        }
        try {
            JWSVerifier verifier = verifierFactory.createJWSVerifier(header, asymmetric.toPublicKey());
            if (!jwt.verify(verifier)) {
                throw Problems.unauthorized(INVALID);
            }
        } catch (JOSEException | RuntimeException e) {
            if (e instanceof ProblemException problem) {
                throw problem;
            }
            throw Problems.unauthorized(INVALID);
        }
    }

    private static JWTClaimsSet claims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw Problems.unauthorized(INVALID);
        }
    }

    private void checkClaims(JWTClaimsSet claims) {
        Instant now = clock.instant();
        Date expiry = claims.getExpirationTime();
        if (expiry == null || now.minus(settings.clockSkew()).isAfter(expiry.toInstant())) {
            throw Problems.unauthorized(INVALID);
        }
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && now.plus(settings.clockSkew()).isBefore(notBefore.toInstant())) {
            throw Problems.unauthorized(INVALID);
        }
        if (!settings.issuerUri().equals(claims.getIssuer())) {
            throw Problems.unauthorized(INVALID);
        }
        List<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(settings.audience())) {
            throw Problems.unauthorized(INVALID);
        }
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw Problems.unauthorized(INVALID);
        }
    }

    private static AuthenticatedUser toUser(JWTClaimsSet claims) {
        String subject = claims.getSubject();
        String creator = stringClaim(claims, "email");
        if (creator == null) {
            creator = stringClaim(claims, "preferred_username");
        }
        if (creator == null) {
            creator = subject;
        }
        return new AuthenticatedUser(subject, creator, realmRoles(claims));
    }

    private static String stringClaim(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        return value instanceof String text && !text.isBlank() ? text : null;
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
        return roles;
    }
}
