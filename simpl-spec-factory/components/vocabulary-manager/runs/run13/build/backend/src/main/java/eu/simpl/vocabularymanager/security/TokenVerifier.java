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
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Problems;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies bearer tokens with Nimbus (D17, D108, D132): signature with a realm key, then issuer, audience,
 * expiry and not-before with the Nimbus claims verifier. Never accepts a token without a signature check.
 */
public class TokenVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(TokenVerifier.class);
    private static final Set<JWSAlgorithm> ALLOWED_ALGORITHMS = Set.of(
            JWSAlgorithm.RS256,
            JWSAlgorithm.RS384,
            JWSAlgorithm.RS512,
            JWSAlgorithm.PS256,
            JWSAlgorithm.PS384,
            JWSAlgorithm.PS512,
            JWSAlgorithm.ES256,
            JWSAlgorithm.ES384,
            JWSAlgorithm.ES512);

    /** The token is missing, malformed or fails a check (401). */
    public static final class InvalidTokenException extends Exception {
        private static final long serialVersionUID = 1L;

        public InvalidTokenException(String message) {
            super(message);
        }
    }

    private final RealmKeyProvider keys;
    private final DefaultJWTClaimsVerifier<?> claimsVerifier;
    private final DefaultJWSVerifierFactory verifierFactory = new DefaultJWSVerifierFactory();

    public TokenVerifier(VocabularyManagerProperties.Identity identity, RealmKeyProvider keys) {
        this.keys = keys;
        this.claimsVerifier = new DefaultJWTClaimsVerifier<>(
                Set.of(identity.audience()),
                new JWTClaimsSet.Builder().issuer(identity.issuer()).build(),
                Set.of("sub", "exp", "iat"),
                Set.of());
    }

    /**
     * Verifies a compact serialised token.
     *
     * @throws InvalidTokenException when the token is not valid (401)
     * @throws ApiException 503 when the realm keys cannot be fetched (D44)
     */
    public AuthenticatedUser verify(String token) throws InvalidTokenException {
        SignedJWT jwt = parse(token);
        JWSHeader header = jwt.getHeader();
        if (!ALLOWED_ALGORITHMS.contains(header.getAlgorithm()) || header.getKeyID() == null) {
            throw new InvalidTokenException("Unsupported algorithm or missing key id");
        }
        JWK key;
        try {
            key = keys.keyFor(header.getKeyID());
        } catch (RealmKeyProvider.UnknownKeyException e) {
            throw new InvalidTokenException("Unknown key id");
        } catch (RealmKeyProvider.KeysUnavailableException e) {
            throw Problems.identityProviderUnavailable();
        }
        checkSignature(jwt, header, key);
        JWTClaimsSet claims = claims(jwt);
        try {
            claimsVerifier.verify(claims, null);
        } catch (BadJWTException e) {
            throw new InvalidTokenException("Claims rejected: " + e.getMessage());
        }
        return toUser(claims);
    }

    private static SignedJWT parse(String token) throws InvalidTokenException {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new InvalidTokenException("Malformed token");
        }
    }

    private void checkSignature(SignedJWT jwt, JWSHeader header, JWK key) throws InvalidTokenException {
        if (!(key instanceof AsymmetricJWK asymmetric) || key.isPrivate()) {
            throw new InvalidTokenException("Unsupported key");
        }
        try {
            JWSVerifier verifier = verifierFactory.createJWSVerifier(header, asymmetric.toPublicKey());
            if (!jwt.verify(verifier)) {
                throw new InvalidTokenException("Bad signature");
            }
        } catch (JOSEException | IllegalStateException e) {
            LOG.debug("Signature check failed: {}", e.getClass().getSimpleName());
            throw new InvalidTokenException("Signature check failed");
        }
    }

    private static JWTClaimsSet claims(SignedJWT jwt) throws InvalidTokenException {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new InvalidTokenException("Malformed claims");
        }
    }

    private static AuthenticatedUser toUser(JWTClaimsSet claims) {
        String creator = stringClaim(claims, "email");
        if (creator == null || creator.isBlank()) {
            creator = stringClaim(claims, "preferred_username");
        }
        if (creator == null || creator.isBlank()) {
            creator = claims.getSubject();
        }
        return new AuthenticatedUser(claims.getSubject(), creator, realmRoles(claims));
    }

    private static String stringClaim(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        return value instanceof String text ? text : null;
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
        return Set.copyOf(roles);
    }
}
