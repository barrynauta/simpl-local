package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates bearer tokens locally with the realm keys (D17, D44, D132): signature with Nimbus, then issuer,
 * audience, expiry and not-before with the Nimbus claims verifier, without clock skew.
 */
@Component
public class JwtAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticator.class);

    private static final Set<JWSAlgorithm> ALGORITHMS = Set.of(
            JWSAlgorithm.RS256,
            JWSAlgorithm.RS384,
            JWSAlgorithm.RS512,
            JWSAlgorithm.PS256,
            JWSAlgorithm.PS384,
            JWSAlgorithm.PS512,
            JWSAlgorithm.ES256,
            JWSAlgorithm.ES384,
            JWSAlgorithm.ES512);
    private static final String IDP_DOWN = "The identity provider is not available. Retry later.";

    private final RealmKeyCache keyCache;
    private final DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier;

    public JwtAuthenticator(AppProperties properties, RealmKeyCache keyCache) {
        this.keyCache = keyCache;
        this.claimsVerifier = new DefaultJWTClaimsVerifier<>(
                properties.identity().audience(),
                new JWTClaimsSet.Builder()
                        .issuer(properties.identity().issuer())
                        .build(),
                Set.of("exp", "iss"));
        this.claimsVerifier.setMaxClockSkew(0);
    }

    /**
     * Authenticates the Authorization header value.
     *
     * @param authorization the header value, may be null
     * @return the user
     * @throws ApiException 401 for a missing or invalid token, 503 when the keys cannot be fetched
     */
    public AuthenticatedUser authenticate(String authorization) {
        String token = bearerToken(authorization);
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (ParseException e) {
            throw unauthorized();
        }
        RealmKeyCache.Lookup lookup = keyCache.lookup(jwt.getHeader().getKeyID());
        if (lookup == RealmKeyCache.Lookup.UNAVAILABLE) {
            throw ApiException.unavailable(IDP_DOWN, null);
        }
        if (lookup == RealmKeyCache.Lookup.NOT_FOUND) {
            throw unauthorized();
        }
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(ALGORITHMS, new ImmutableJWKSet<>(keyCache.keys())));
        processor.setJWTClaimsSetVerifier(claimsVerifier);
        try {
            JWTClaimsSet claims = processor.process(jwt, null);
            return toUser(claims);
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    private static AuthenticatedUser toUser(JWTClaimsSet claims) throws ParseException {
        String subject = claims.getSubject() == null ? "unknown" : claims.getSubject();
        String email = claims.getStringClaim("email");
        String username = claims.getStringClaim("preferred_username");
        String creator = email != null && !email.isBlank() ? email : username;
        List<String> roles = new ArrayList<>();
        Map<String, Object> realmAccess = claims.getJSONObjectClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> list) {
            for (Object role : list) {
                if (role instanceof String name) {
                    roles.add(name);
                }
            }
        }
        return new AuthenticatedUser(subject, creator == null ? subject : creator, Set.copyOf(roles));
    }

    private static String bearerToken(String authorization) {
        if (authorization == null) {
            throw unauthorized();
        }
        String value = authorization.trim();
        if (value.length() < 8
                || !value.substring(0, 7).toLowerCase(Locale.ROOT).equals("bearer ")) {
            throw unauthorized();
        }
        String token = value.substring(7).trim();
        if (token.isEmpty()) {
            throw unauthorized();
        }
        return token;
    }

    private static ApiException unauthorized() {
        LOG.warn("Authentication failed: no valid bearer token");
        return new ApiException(ProblemType.UNAUTHORIZED, null, List.of());
    }
}
