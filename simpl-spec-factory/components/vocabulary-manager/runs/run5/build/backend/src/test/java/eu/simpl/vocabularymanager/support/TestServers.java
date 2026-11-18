package eu.simpl.vocabularymanager.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.DatasetFactory;

/** An embedded Fuseki with the 'vocabulary' dataset and a JWKS endpoint, shared by the integration tests. */
public final class TestServers {

    public static final TestTokens TOKENS = new TestTokens("it-key");
    private static FusekiServer fuseki;
    private static HttpServer jwks;

    private TestServers() {}

    public static synchronized void start() {
        if (fuseki != null) {
            return;
        }
        fuseki = FusekiServer.create()
                .port(0)
                .loopback(true)
                .add("/vocabulary", DatasetFactory.createTxnMem(), true)
                .build()
                .start();
        try {
            jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        byte[] body = TOKENS.publicKeys().toString().getBytes(StandardCharsets.UTF_8);
        jwks.createContext("/certs", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        jwks.start();
    }

    public static String storeUrl() {
        start();
        return "http://127.0.0.1:" + fuseki.getHttpPort() + "/vocabulary";
    }

    public static String jwksUrl() {
        start();
        return "http://127.0.0.1:" + jwks.getAddress().getPort() + "/certs";
    }
}
