package eu.simpl.vocabularymanager.support;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

/**
 * Shared test infrastructure: an embedded Fuseki with an in-memory dataset 'vocabulary' (E3) and a local key set
 * endpoint that stands in for the Keycloak realm (E1), with tokens signed by a key of that set.
 */
public final class TestEnvironment {

    public static final String ISSUER = "http://idp.test/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";
    public static final String ADMIN_ROLE = "GA_VOCABULARY_ADMIN";
    public static final String VIEWER_ROLE = "GA_VOCABULARY_VIEWER";

    private static final TestEnvironment INSTANCE = new TestEnvironment();

    private final DatasetGraph dataset = DatasetGraphFactory.createTxnMem();
    private final FusekiServer fuseki;
    private final HttpServer jwksServer;
    private final RSAKey key;
    private final RSAKey otherKey;
    private final AtomicBoolean identityProviderUp = new AtomicBoolean(true);
    private final int managementPort;

    private TestEnvironment() {
        try {
            fuseki = FusekiServer.create().port(0).loopback(true).add("/vocabulary", dataset).build();
            fuseki.start();
            key = new RSAKeyGenerator(2048).keyID("test-key").keyUse(KeyUse.SIGNATURE).generate();
            otherKey = new RSAKeyGenerator(2048).keyID("unknown-key").keyUse(KeyUse.SIGNATURE).generate();
            jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            jwksServer.createContext("/certs", exchange -> {
                byte[] body;
                int status;
                if (identityProviderUp.get()) {
                    body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                    status = 200;
                } else {
                    body = "down".getBytes(StandardCharsets.UTF_8);
                    status = 503;
                }
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            jwksServer.start();
            try (ServerSocket socket = new ServerSocket(0)) {
                managementPort = socket.getLocalPort();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Test environment could not start", e);
        }
    }

    /**
     * @return the shared environment
     */
    public static TestEnvironment get() {
        return INSTANCE;
    }

    /**
     * @return the dataset behind the embedded Fuseki
     */
    public DatasetGraph dataset() {
        return dataset;
    }

    /**
     * @return base URL of the dataset
     */
    public String storeUrl() {
        return "http://127.0.0.1:" + fuseki.getHttpPort() + "/vocabulary";
    }

    /**
     * @return URL of the key set
     */
    public String jwksUrl() {
        return "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/certs";
    }

    /**
     * @return a free port for the management server
     */
    public int managementPort() {
        return managementPort;
    }

    /**
     * @param up whether the key set endpoint answers
     */
    public void identityProviderUp(boolean up) {
        identityProviderUp.set(up);
    }

    /**
     * @param user the user name
     * @param roles realm roles
     * @return a valid token
     */
    public String token(String user, String... roles) {
        return token(user, user + "@example.com", ISSUER, AUDIENCE, Instant.now().plusSeconds(300), key, roles);
    }

    /**
     * @return a token for ga-admin
     */
    public String admin() {
        return token("ga-admin", ADMIN_ROLE);
    }

    /**
     * @return a token for ga-viewer
     */
    public String viewer() {
        return token("ga-viewer", VIEWER_ROLE);
    }

    /**
     * @return a token for ga-norole
     */
    public String noRole() {
        return token("ga-norole");
    }

    /**
     * @return a token signed by a key the realm does not publish
     */
    public String tokenWithUnknownKey() {
        return token("ga-admin", "ga-admin@example.com", ISSUER, AUDIENCE, Instant.now().plusSeconds(300), otherKey,
                ADMIN_ROLE);
    }

    /**
     * @param user user name
     * @param email email claim, or null
     * @param issuer issuer
     * @param audience audience
     * @param expiry expiry
     * @param signingKey signing key
     * @param roles realm roles
     * @return the signed token
     */
    public String token(
            String user, String email, String issuer, String audience, Instant expiry, RSAKey signingKey,
            String... roles) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject("sub-" + user)
                    .expirationTime(Date.from(expiry))
                    .issueTime(new Date())
                    .claim("preferred_username", user)
                    .claim("realm_access", Map.of("roles", List.of(roles)));
            if (email != null) {
                claims.claim("email", email);
            }
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(signingKey.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims.build());
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return the realm key
     */
    public RSAKey key() {
        return key;
    }

    /**
     * Closes nothing: the servers live for the whole test JVM.
     *
     * @throws IOException never
     */
    void close() throws IOException {
        fuseki.stop();
        jwksServer.stop(0);
    }
}
