package eu.simpl.vocabularymanager.it;

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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

/**
 * Shared test infrastructure: an embedded Fuseki store (E3) and a fake identity provider that
 * serves realm keys (E1); tokens are signed with Nimbus like Keycloak would.
 */
public final class TestEnvironment {

    public static final String ISSUER = "http://localhost:8193/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";

    public static final DatasetGraph DATASET = DatasetGraphFactory.createTxnMem();
    public static final FusekiServer FUSEKI;
    public static final RSAKey KEY;
    public static final HttpServer IDP;
    public static volatile boolean idpUp = true;

    static {
        try {
            FUSEKI = FusekiServer.create()
                    .port(0)
                    .add("/vocabulary", DATASET, true)
                    .build()
                    .start();
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            IDP = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            IDP.createContext("/certs", exchange -> {
                if (!idpUp) {
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                    return;
                }
                byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            IDP.start();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private TestEnvironment() {}

    public static String storeUrl() {
        return "http://localhost:" + FUSEKI.getPort() + "/vocabulary";
    }

    public static String jwksUrl() {
        return "http://127.0.0.1:" + IDP.getAddress().getPort() + "/certs";
    }

    public static String token(String user, List<String> roles) {
        return token(user, roles, ISSUER, AUDIENCE, KEY, Instant.now().plusSeconds(300));
    }

    public static String token(
            String user, List<String> roles, String issuer, String audience, RSAKey key, Instant expiry) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject("sub-" + user)
                    .expirationTime(Date.from(expiry))
                    .issueTime(new Date())
                    .claim("preferred_username", user)
                    .claim("email", user + "@example.com")
                    .claim("realm_access", Map.of("roles", roles))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(key.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String admin() {
        return token("ga-admin", List.of("GA_VOCABULARY_ADMIN"));
    }

    public static String viewer() {
        return token("ga-viewer", List.of("GA_VOCABULARY_VIEWER"));
    }

    public static String norole() {
        return token("ga-norole", List.of());
    }
}
