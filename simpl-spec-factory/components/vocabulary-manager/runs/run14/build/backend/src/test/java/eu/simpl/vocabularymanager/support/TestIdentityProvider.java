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
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** A tiny identity provider for tests: serves the realm keys and signs tokens like Keycloak. */
public final class TestIdentityProvider {

    public static final String ISSUER = "http://localhost:8193/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";

    private static TestIdentityProvider shared;

    private final HttpServer server;
    private final RSAKey key;
    private final RSAKey otherKey;
    private final AtomicInteger fetches = new AtomicInteger();
    private volatile boolean down;

    private TestIdentityProvider() {
        try {
            key = new RSAKeyGenerator(2048).keyID("key-1").generate();
            otherKey = new RSAKeyGenerator(2048).keyID("key-unknown").generate();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/certs", exchange -> {
                fetches.incrementAndGet();
                if (down) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public static synchronized TestIdentityProvider shared() {
        if (shared == null) {
            shared = new TestIdentityProvider();
        }
        return shared;
    }

    public String jwksUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/certs";
    }

    public int fetches() {
        return fetches.get();
    }

    public void setDown(boolean value) {
        down = value;
    }

    /** A token for a user with roles. */
    public String token(String user, String... roles) {
        return sign(key, claims(user, ISSUER, AUDIENCE, Instant.now().plusSeconds(300), roles));
    }

    /** A token with custom claims. */
    public String token(String issuer, String audience, Instant expiry, String... roles) {
        return sign(key, claims("ga-admin", issuer, audience, expiry, roles));
    }

    /** A token signed by a key the realm does not publish. */
    public String tokenWithUnknownKey(String... roles) {
        return sign(otherKey, claims("ga-admin", ISSUER, AUDIENCE, Instant.now().plusSeconds(300), roles));
    }

    private static JWTClaimsSet claims(String user, String issuer, String audience, Instant expiry, String... roles) {
        return new JWTClaimsSet.Builder()
                .subject("sub-" + user)
                .issuer(issuer)
                .audience(audience)
                .expirationTime(Date.from(expiry))
                .issueTime(new Date())
                .claim("preferred_username", user)
                .claim("email", user + "@example.com")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
    }

    private static String sign(RSAKey signingKey, JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(signingKey.getKeyID())
                            .build(),
                    claims);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
