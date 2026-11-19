package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.support.TestProperties;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Decision D44 for the identity provider, and token validation (D17, OWT-A07). */
class RealmKeySourceTest {

    private HttpServer server;
    private final AtomicReference<String> jwks = new AtomicReference<>();
    private RSAKey key;

    @BeforeEach
    void start() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("k1").generate();
        jwks.set(new JWKSet(key.toPublicJWK()).toString());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/certs", exchange -> {
            String body = jwks.get();
            if (body == null) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private String certsUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/certs";
    }

    private AppProperties properties(String jwkSetUri, Duration cooldown) {
        AppProperties base = TestProperties.properties();
        return new AppProperties(
                base.contentBaseUrl(),
                base.store(),
                new AppProperties.Identity(
                        "http://issuer", "vocabulary-manager-be", jwkSetUri, Duration.ofSeconds(2), cooldown),
                base.limits());
    }

    private String token(RSAKey signingKey, String issuer, String audience, Instant expiry) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("user")
                .issueTime(new Date())
                .expirationTime(Date.from(expiry))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(signingKey.getKeyID())
                        .build(),
                claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static JWKSelector selector(String kid) {
        return new JWKSelector(new JWKMatcher.Builder().keyID(kid).build());
    }

    @Test
    void fetchesKeysAndValidatesTokens() throws Exception {
        AppProperties props = properties(certsUrl(), Duration.ofSeconds(5));
        RealmKeySource source = new RealmKeySource(props.identity(), Clock.systemUTC());
        assertThat(source.hasKeys()).isFalse();
        JwtDecoder decoder = new SecurityConfig().jwtDecoder(source, props);
        Instant later = Instant.now().plusSeconds(300);
        assertThat(decoder.decode(token(key, "http://issuer", "vocabulary-manager-be", later))
                        .getSubject())
                .isEqualTo("user");
        assertThat(source.hasKeys()).isTrue();
        assertThatThrownBy(() -> decoder.decode(token(key, "http://other", "vocabulary-manager-be", later)))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(token(key, "http://issuer", "other-audience", later)))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(token(
                        key,
                        "http://issuer",
                        "vocabulary-manager-be",
                        Instant.now().minusSeconds(600))))
                .isInstanceOf(JwtException.class);
        RSAKey stranger = new RSAKeyGenerator(2048).keyID("k1").generate();
        assertThatThrownBy(() -> decoder.decode(token(stranger, "http://issuer", "vocabulary-manager-be", later)))
                .isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> decoder.decode("not-a-token")).isInstanceOf(BadJwtException.class);
    }

    @Test
    void cachedKeysKeepWorkingWhileTheIdentityProviderIsDown() throws Exception {
        AppProperties props = properties(certsUrl(), Duration.ZERO);
        RealmKeySource source = new RealmKeySource(props.identity(), Clock.systemUTC());
        assertThat(source.refresh()).isTrue();
        jwks.set(null);
        assertThat(source.get(selector("k1"), null)).hasSize(1);
    }

    @Test
    void unknownKeyIdFailsClosedWhenKeysCannotBeFetched() {
        AppProperties props = properties("http://127.0.0.1:1/certs", Duration.ZERO);
        RealmKeySource source = new RealmKeySource(props.identity(), Clock.systemUTC());
        assertThatThrownBy(() -> source.get(selector("unknown"), null))
                .isInstanceOf(RealmKeySource.IdentityProviderUnavailableException.class);
        assertThat(source.refresh()).isFalse();
    }

    @Test
    void unknownKeyIdAfterARecentSuccessfulFetchIsNotFound() throws Exception {
        AppProperties props = properties(certsUrl(), Duration.ofMinutes(5));
        RealmKeySource source = new RealmKeySource(props.identity(), Clock.systemUTC());
        assertThat(source.refresh()).isTrue();
        assertThat(source.get(selector("other"), null)).isEmpty();
    }

    @Test
    void realmRolesAreReadFromRealmAccess() {
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue(
                        "t")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", List.of("GA_VOCABULARY_ADMIN", 7)))
                .build();
        assertThat(SecurityConfig.realmRoles(jwt)).extracting(Object::toString).containsExactly("GA_VOCABULARY_ADMIN");
        org.springframework.security.oauth2.jwt.Jwt none = org.springframework.security.oauth2.jwt.Jwt.withTokenValue(
                        "t")
                .header("alg", "none")
                .claim("sub", "x")
                .build();
        assertThat(SecurityConfig.realmRoles(none)).isEmpty();
    }
}
