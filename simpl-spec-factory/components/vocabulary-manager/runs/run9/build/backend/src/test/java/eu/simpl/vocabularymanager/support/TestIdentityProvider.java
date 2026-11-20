package eu.simpl.vocabularymanager.support;

import com.nimbusds.jose.JOSEObjectType;
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
import java.util.UUID;

/** A stand-in for the Keycloak realm: serves a JWKS and signs tokens like Keycloak does (E1, E5). */
public final class TestIdentityProvider {

    /** The accepted issuer in the tests. */
    public static final String ISSUER = "http://localhost:8193/realms/simpl";

    private static TestIdentityProvider instance;

    private final RSAKey key;
    private final RSAKey otherKey;
    private final HttpServer server;
    private volatile boolean available = true;

    private TestIdentityProvider() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        otherKey = new RSAKeyGenerator(2048).keyID("kid-unknown").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/certs", exchange -> {
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

    /** @return the shared instance */
    public static synchronized TestIdentityProvider get() {
        if (instance == null) {
            try {
                instance = new TestIdentityProvider();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        return instance;
    }

    /** @return the JWKS URL */
    public String jwkSetUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/certs";
    }

    /** Makes the key endpoint answer 503 or work again. */
    public void setAvailable(boolean value) {
        available = value;
    }

    /** A token of a test user with roles. */
    public String token(String username, String... roles) {
        return sign(key, ISSUER, List.of("vocabulary-manager-be"), username, roles);
    }

    /** A token without the audience (E7). */
    public String tokenWithoutAudience(String username, String... roles) {
        return sign(key, ISSUER, List.of("account"), username, roles);
    }

    /** A token of another realm (E7). */
    public String tokenOtherIssuer(String username, String... roles) {
        return sign(key, "http://localhost:8193/realms/simpl-other", List.of("vocabulary-manager-be"), username, roles);
    }

    /** A token signed with a key the realm does not publish. */
    public String tokenUnknownKey(String username, String... roles) {
        return sign(otherKey, ISSUER, List.of("vocabulary-manager-be"), username, roles);
    }

    private static String sign(
            RSAKey signingKey, String issuer, List<String> audience, String username, String... roles) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8))
                            .toString())
                    .audience(audience)
                    .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                    .issueTime(new Date())
                    .claim("preferred_username", username)
                    .claim("email", username + "@example.com")
                    .claim("realm_access", Map.of("roles", List.of(roles)))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(signingKey.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Closes nothing; the server lives for the JVM. */
    public static void noop() throws IOException {}
}
