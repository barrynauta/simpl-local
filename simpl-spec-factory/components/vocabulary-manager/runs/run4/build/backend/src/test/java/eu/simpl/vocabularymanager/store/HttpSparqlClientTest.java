package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpSparqlClientTest {

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String serve(int status, String body, long delayMillis) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ds", exchange -> {
            calls.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/sparql-results+json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        return "http://localhost:" + server.getAddress().getPort() + "/ds";
    }

    private static HttpSparqlClient client(String url, Duration responseTimeout) {
        return new HttpSparqlClient(new VocabularyManagerProperties(
                null,
                0,
                new VocabularyManagerProperties.Store(
                        url, Duration.ofSeconds(2), responseTimeout, 2, Duration.ofSeconds(2)),
                null));
    }

    private static int status(Throwable e) {
        return ((VocabularyManagerException) e).code().status();
    }

    @Test
    void parsesSelectResults() throws Exception {
        String url = serve(
                200,
                "{\"head\":{\"vars\":[\"name\"]},\"results\":{\"bindings\":[{\"name\":{\"type\":\"literal\",\"value\":\"A\"}}]}}",
                0);
        HttpSparqlClient client = client(url, Duration.ofSeconds(2));
        assertThat(client.select("SELECT ?name {}"))
                .singleElement()
                .satisfies(row -> assertThat(row.getLiteral("name").getString()).isEqualTo("A"));
        client.update("INSERT DATA {}");
        assertThat(client.ping(Duration.ofSeconds(1))).isTrue();
    }

    @Test
    void unreachableStoreGives503AfterRetries() {
        HttpSparqlClient client = client("http://localhost:1/ds", Duration.ofSeconds(1));
        assertThatThrownBy(() -> client.select("SELECT * {}"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(503));
        assertThatThrownBy(() -> client.update("INSERT DATA {}"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(503));
        assertThat(client.ping(Duration.ofMillis(200))).isFalse();
    }

    @Test
    void readsAreRetriedTwiceOnTimeoutUpdatesNever() throws Exception {
        String url = serve(200, "{}", 700);
        HttpSparqlClient client = client(url, Duration.ofMillis(200));
        assertThatThrownBy(() -> client.select("SELECT * {}"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(503));
        assertThat(calls.get()).isEqualTo(3);
        calls.set(0);
        assertThatThrownBy(() -> client.update("INSERT DATA {}"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(503));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void storeErrorThatIsNotATimeoutGives500() throws Exception {
        String url = serve(500, "boom", 0);
        HttpSparqlClient client = client(url, Duration.ofSeconds(2));
        assertThatThrownBy(() -> client.select("SELECT * {}"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(500));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void storeAnswering503GivesServiceUnavailable() throws Exception {
        String url = serve(503, "", 0);
        HttpSparqlClient client = client(url, Duration.ofSeconds(2));
        assertThatThrownBy(() -> client.update("INSERT DATA {}"))
                .satisfies(e -> assertThat(status(e)).isEqualTo(503));
    }
}
