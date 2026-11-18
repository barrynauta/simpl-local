package eu.simpl.vocabularymanager.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Signs test tokens like Keycloak does and serves the public key as a JWKS endpoint. */
public final class TestTokens {

    public static final String ISSUER = "http://keycloak.test/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";
    public static final RSAKey KEY = generate("test-key-1");
    public static final RSAKey OTHER_KEY = generate("test-key-2");
    public static final AtomicBoolean JWKS_DOWN = new AtomicBoolean(false);
    public static final AtomicInteger JWKS_CALLS = new AtomicInteger();
    private static final HttpServer SERVER = start();

    private TestTokens() {}

    public static String jwksUrl() {
        return "http://localhost:" + SERVER.getAddress().getPort() + "/certs";
    }

    public static String admin() {
        return token("ga-admin@example.com", List.of("GA_VOCABULARY_ADMIN"));
    }

    public static String viewer() {
        return token("ga-viewer@example.com", List.of("GA_VOCABULARY_VIEWER"));
    }

    public static String noRole() {
        return token("ga-norole@example.com", List.of("offline_access"));
    }

    public static String token(String email, List<String> roles) {
        return sign(KEY, claims(email, roles).build());
    }

    public static JWTClaimsSet.Builder claims(String email, List<String> roles) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE, "account"))
                .subject("subject-" + email)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("email", email)
                .claim("preferred_username", email.split("@")[0])
                .claim("realm_access", Map.of("roles", roles));
    }

    public static String sign(RSAKey key, JWTClaimsSet claims) {
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

    private static RSAKey generate(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/certs", exchange -> {
                JWKS_CALLS.incrementAndGet();
                if (JWKS_DOWN.get()) {
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                    return;
                }
                byte[] body =
                        new JWKSet(List.of(KEY.toPublicJWK())).toString(true).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
