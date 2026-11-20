package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import eu.simpl.vocabularymanager.config.AppProperties;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates bearer tokens (D17, D44, D88, D132, OWT-A07): signature with the realm keys, issuer,
 * audience, expiry and not-before through the Nimbus library. No own cryptography (REUSE-01).
 */
@Component
public class TokenVerifier {

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

    private final RealmKeys realmKeys;
    private final DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier;

    public TokenVerifier(AppProperties properties, RealmKeys realmKeys) {
        this.realmKeys = realmKeys;
        this.claimsVerifier = new DefaultJWTClaimsVerifier<>(
                properties.security().audience(),
                new JWTClaimsSet.Builder()
                        .issuer(properties.security().issuerUri())
                        .build(),
                Set.of("exp", "sub", "iss"));
    }

    /** The result of a token check. */
    public sealed interface Result permits Valid, Invalid, KeysUnavailable {}

    /** A valid token and its user. */
    public record Valid(AuthenticatedUser user) implements Result {}

    /** An invalid token (401) with the reason for the log. */
    public record Invalid(String reason) implements Result {}

    /** The realm keys could not be fetched (503, D44). */
    public record KeysUnavailable() implements Result {}

    public Result verify(String token) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (ParseException | RuntimeException ex) {
            return new Invalid("not a signed JWT");
        }
        if (!ALGORITHMS.contains(jwt.getHeader().getAlgorithm())) {
            return new Invalid("algorithm not allowed");
        }
        RealmKeys.Lookup lookup = realmKeys.lookup(jwt.getHeader().getKeyID());
        if (lookup instanceof RealmKeys.Unavailable) {
            return new KeysUnavailable();
        }
        if (!(lookup instanceof RealmKeys.Found found)) {
            return new Invalid("unknown key id");
        }
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(ALGORITHMS, new ImmutableJWKSet<>(new JWKSet(found.key()))));
        processor.setJWTClaimsSetVerifier(claimsVerifier);
        try {
            JWTClaimsSet claims = processor.process(jwt, null);
            return new Valid(user(claims));
        } catch (BadJOSEException | com.nimbusds.jose.JOSEException ex) {
            return new Invalid(ex.getClass().getSimpleName());
        } catch (ParseException | RuntimeException ex) {
            return new Invalid("claims could not be read");
        }
    }

    private static AuthenticatedUser user(JWTClaimsSet claims) throws ParseException {
        List<String> roles = new ArrayList<>();
        Map<String, Object> realmAccess = claims.getJSONObjectClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> list) {
            for (Object role : list) {
                if (role instanceof String text) {
                    roles.add(text);
                }
            }
        }
        String email = claims.getStringClaim("email");
        String username = claims.getStringClaim("preferred_username");
        String creator = email != null && !email.isBlank() ? email : username;
        return new AuthenticatedUser(claims.getSubject(), creator == null ? claims.getSubject() : creator, roles);
    }
}
