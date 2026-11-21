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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** A stand-in identity provider for the tests: realm keys over HTTP and signed tokens. */
public final class TestIdp implements AutoCloseable {

    public static final String ISSUER = "http://idp.test/realms/simpl";
    public static final String AUDIENCE = "vocabulary-manager-be";

    private final RSAKey key;
    private final HttpServer server;
    public final AtomicBoolean failing = new AtomicBoolean(false);
    public final AtomicInteger fetches = new AtomicInteger();

    public TestIdp() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/certs", ex -> {
            fetches.incrementAndGet();
            if (failing.get()) {
                ex.sendResponseHeaders(500, -1);
                ex.close();
                return;
            }
            byte[] body = new JWKSet(key.toPublicJWK()).toString(true).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream o = ex.getResponseBody()) {
                o.write(body);
            }
        });
        server.start();
    }

    public String jwksUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/certs";
    }

    public String token(String user, String... roles) throws Exception {
        return token(user, ISSUER, AUDIENCE, key, Instant.now().plusSeconds(300), roles);
    }

    public String token(String user, String issuer, String audience, RSAKey signer, Instant exp, String... roles)
            throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sub-" + user)
                .issuer(issuer)
                .audience(audience)
                .expirationTime(Date.from(exp))
                .issueTime(new Date())
                .claim("email", user + "@example.com")
                .claim("preferred_username", user)
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(signer.getKeyID())
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims);
        jwt.sign(new RSASSASigner(signer));
        return jwt.serialize();
    }

    public RSAKey key() {
        return key;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
