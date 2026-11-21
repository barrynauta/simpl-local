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

/** A realm key pair, a JWKS endpoint on a local port and token minting, for tests only. */
public final class TestKeys {

    public static final String ISSUER = "http://localhost:8193/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";

    private final RSAKey key;
    private final HttpServer server;
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final AtomicInteger fetches = new AtomicInteger();

    public TestKeys(String keyId) {
        try {
            key = new RSAKeyGenerator(2048).keyID(keyId).generate();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (JOSEException | IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/certs", exchange -> {
            fetches.incrementAndGet();
            if (!available.get()) {
                exchange.sendResponseHeaders(500, -1);
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

    public void setAvailable(boolean value) {
        available.set(value);
    }

    public int fetches() {
        return fetches.get();
    }

    public void stop() {
        server.stop(0);
    }

    public String token(String user, List<String> roles) {
        return token(user, roles, ISSUER, AUDIENCE, Instant.now().plusSeconds(600));
    }

    public String token(String user, List<String> roles, String issuer, String audience, Instant expiry) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub-" + user)
                .issuer(issuer)
                .audience(audience)
                .issueTime(new Date())
                .expirationTime(Date.from(expiry))
                .claim("email", user + "@example.com")
                .claim("preferred_username", user)
                .claim("realm_access", Map.of("roles", roles))
                .build();
        return sign(claims, key);
    }

    public static String sign(JWTClaimsSet claims, RSAKey signingKey) {
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
