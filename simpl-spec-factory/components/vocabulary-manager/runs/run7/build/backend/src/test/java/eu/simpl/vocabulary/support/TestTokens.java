package eu.simpl.vocabulary.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Test realm: an RSA key and signed tokens shaped like Keycloak access tokens (E1, E5). */
public final class TestTokens {

    public static final String ISSUER = "http://issuer.test/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";
    private static final RSAKey KEY = generate("test-key");
    private static final RSAKey OTHER_KEY = generate("unknown-key");

    private TestTokens() {}

    private static RSAKey generate(String kid) {
        try {
            return new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JWKSet publicKeys() {
        return new JWKSet(KEY.toPublicJWK());
    }

    public static String admin() {
        return token("admin-sub", "ga-admin@example.com", List.of("GA_VOCABULARY_ADMIN"), AUDIENCE, ISSUER, KEY);
    }

    public static String viewer() {
        return token("viewer-sub", "ga-viewer@example.com", List.of("GA_VOCABULARY_VIEWER"), AUDIENCE, ISSUER, KEY);
    }

    public static String noRole() {
        return token("norole-sub", "ga-norole@example.com", List.of(), AUDIENCE, ISSUER, KEY);
    }

    public static String wrongAudience() {
        return token("admin-sub", "ga-admin@example.com", List.of("GA_VOCABULARY_ADMIN"), "other", ISSUER, KEY);
    }

    public static String wrongIssuer() {
        return token("admin-sub", "ga-admin@example.com", List.of("GA_VOCABULARY_ADMIN"), AUDIENCE, "http://evil", KEY);
    }

    public static String unknownKey() {
        return token("admin-sub", "ga-admin@example.com", List.of("GA_VOCABULARY_ADMIN"), AUDIENCE, ISSUER, OTHER_KEY);
    }

    public static String withoutEmail() {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .audience(AUDIENCE)
                    .subject("plain-sub")
                    .claim("preferred_username", "plain-user")
                    .claim("realm_access", Map.of("roles", List.of("GA_VOCABULARY_ADMIN")))
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                    .build();
            return sign(claims, KEY);
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String token(
            String subject, String email, List<String> roles, String audience, String issuer, RSAKey key) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject(subject)
                    .claim("email", email)
                    .claim("preferred_username", email.substring(0, email.indexOf('@')))
                    .claim("realm_access", Map.of("roles", roles))
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                    .build();
            return sign(claims, key);
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sign(JWTClaimsSet claims, RSAKey key) throws JOSEException {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
