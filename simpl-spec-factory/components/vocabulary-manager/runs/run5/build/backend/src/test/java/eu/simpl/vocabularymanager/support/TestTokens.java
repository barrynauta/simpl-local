package eu.simpl.vocabularymanager.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Signs test access tokens like the Keycloak realm of E1 would. */
public final class TestTokens {

    public static final String ISSUER = "http://issuer.test/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";

    private final RSAKey key;

    public TestTokens(String keyId) {
        try {
            this.key = new RSAKeyGenerator(2048)
                    .keyID(keyId)
                    .keyUse(KeyUse.SIGNATURE)
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public JWKSet publicKeys() {
        return new JWKSet(key.toPublicJWK());
    }

    public String admin() {
        return token("ga-admin", "ga-admin@example.com", List.of("GA_VOCABULARY_ADMIN"));
    }

    public String viewer() {
        return token("ga-viewer", "ga-viewer@example.com", List.of("GA_VOCABULARY_VIEWER"));
    }

    public String noRole() {
        return token("ga-norole", "ga-norole@example.com", List.of());
    }

    public String token(String username, String email, List<String> roles) {
        return sign(claims(username, email, roles).build());
    }

    public JWTClaimsSet.Builder claims(String username, String email, List<String> roles) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject("id-" + username)
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE, "account"))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", roles));
        if (email != null) {
            builder.claim("email", email);
        }
        return builder;
    }

    public String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(key.getKeyID())
                            .build(),
                    claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
