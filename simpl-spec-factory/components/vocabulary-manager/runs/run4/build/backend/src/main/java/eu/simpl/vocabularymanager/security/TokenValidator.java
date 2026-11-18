package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.AccessErrorCode;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Validates Keycloak access tokens locally (decisions D17, D44, OWT-A07): signature with the realm keys, issuer,
 * audience, expiry. Fails closed.
 */
@Component
public class TokenValidator {

    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);
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

    private final RealmKeys realmKeys;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    public TokenValidator(RealmKeys realmKeys, VocabularyManagerProperties properties, Clock clock) {
        this.realmKeys = realmKeys;
        this.issuer = properties.security().issuer();
        this.audience = properties.security().audience();
        this.clock = clock;
    }

    /**
     * Validates the token.
     *
     * @throws VocabularyManagerException 401 for an invalid token, 503 when the keys cannot be fetched
     */
    public AuthenticatedUser validate(String token) {
        SignedJWT jwt = parse(token);
        if (!ALLOWED.contains(jwt.getHeader().getAlgorithm())) {
            throw invalid("Token algorithm is not accepted.");
        }
        String keyId = jwt.getHeader().getKeyID();
        if (keyId == null) {
            throw invalid("Token has no key id.");
        }
        JWK key = realmKeys.key(keyId);
        if (key == null) {
            throw invalid("Token is signed with an unknown key.");
        }
        verifySignature(jwt, key);
        JWTClaimsSet claims = claims(jwt);
        checkClaims(claims);
        return new AuthenticatedUser(userId(claims), claims.getSubject(), authorities(claims));
    }

    private static SignedJWT parse(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw invalid("Token is not a signed JWT.");
        }
    }

    private static void verifySignature(SignedJWT jwt, JWK key) {
        try {
            JWSVerifier verifier;
            if (key instanceof RSAKey rsaKey) {
                verifier = new RSASSAVerifier(rsaKey);
            } else if (key instanceof ECKey ecKey) {
                verifier = new ECDSAVerifier(ecKey);
            } else {
                throw invalid("Token key type is not supported.");
            }
            if (!jwt.verify(verifier)) {
                throw invalid("Token signature is not valid.");
            }
        } catch (JOSEException e) {
            throw invalid("Token signature could not be verified.");
        }
    }

    private static JWTClaimsSet claims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw invalid("Token claims are not valid.");
        }
    }

    private void checkClaims(JWTClaimsSet claims) {
        Instant now = clock.instant();
        Date expiry = claims.getExpirationTime();
        if (expiry == null || expiry.toInstant().plus(CLOCK_SKEW).isBefore(now)) {
            throw invalid("Token has expired.");
        }
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && notBefore.toInstant().minus(CLOCK_SKEW).isAfter(now)) {
            throw invalid("Token is not yet valid.");
        }
        if (!issuer.equals(claims.getIssuer())) {
            throw invalid("Token issuer is not accepted.");
        }
        if (!audience.isEmpty()
                && (claims.getAudience() == null || !claims.getAudience().contains(audience))) {
            throw invalid("Token audience is not accepted.");
        }
    }

    private static String userId(JWTClaimsSet claims) {
        Object email = claims.getClaim("email");
        if (email instanceof String text && !text.isBlank()) {
            return text;
        }
        Object username = claims.getClaim("preferred_username");
        if (username instanceof String text && !text.isBlank()) {
            return text;
        }
        return claims.getSubject() == null ? "unknown" : claims.getSubject();
    }

    private static Collection<SimpleGrantedAuthority> authorities(JWTClaimsSet claims) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Object realmAccess = claims.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> roles) {
            for (Object role : roles) {
                if (Roles.ADMIN.equals(role) || Roles.VIEWER.equals(role)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
        }
        return authorities;
    }

    private static VocabularyManagerException invalid(String detail) {
        return new VocabularyManagerException(AccessErrorCode.UNAUTHORIZED, detail);
    }
}
