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
import java.util.concurrent.atomic.AtomicInteger;

/** A stand-in for the Keycloak realm: serves a JWKS document and signs tokens with its key. */
public final class TestIdentityProvider implements AutoCloseable {

    public static final String ISSUER = "http://idp.test/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";

    private final RSAKey key;
    private final HttpServer server;
    private final AtomicInteger fetches = new AtomicInteger();
    private volatile boolean available = true;

    public TestIdentityProvider() {
        try {
            key = new RSAKeyGenerator(2048).keyID("test-key").generate();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (JOSEException | IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/certs", exchange -> {
            fetches.incrementAndGet();
            if (!available) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    public String jwksUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/certs";
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int fetchCount() {
        return fetches.get();
    }

    public String admin() {
        return token("admin-sub", "ga-admin@example.com", List.of("GA_VOCABULARY_ADMIN"));
    }

    public String viewer() {
        return token("viewer-sub", "ga-viewer@example.com", List.of("GA_VOCABULARY_VIEWER"));
    }

    public String noRole() {
        return token("norole-sub", "ga-norole@example.com", List.of());
    }

    public String token(String subject, String email, List<String> roles) {
        return sign(claims(subject, email, roles).build(), key.getKeyID(), key);
    }

    public JWTClaimsSet.Builder claims(String subject, String email, List<String> roles) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE, "account"))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("preferred_username", subject)
                .claim("realm_access", Map.of("roles", roles));
        if (email != null) {
            builder.claim("email", email);
        }
        return builder;
    }

    public String sign(JWTClaimsSet claims, String keyId, RSAKey signingKey) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(), claims);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public RSAKey key() {
        return key;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
