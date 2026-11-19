package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import eu.simpl.vocabularymanager.support.TestProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Decision D44: timeouts, read retries after 200 and 400 ms, no update retries, 500 for store errors. */
class SparqlClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String serve(int status, String body, AtomicInteger calls) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/sparql-results+json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/vocabulary";
    }

    @Test
    void readsAreRetriedTwiceWithBackoffThenUnavailable() {
        List<Long> pauses = new ArrayList<>();
        SparqlClient client = new SparqlClient(TestProperties.properties().store(), pauses::add);
        assertThatThrownBy(() -> client.select("SELECT * WHERE {}")).isInstanceOf(StoreUnavailableException.class);
        assertThat(pauses).containsExactly(200L, 400L);
        assertThat(client.ping()).isFalse();
    }

    @Test
    void updatesAreNeverRetried() {
        List<Long> pauses = new ArrayList<>();
        SparqlClient client = new SparqlClient(TestProperties.properties().store(), pauses::add);
        assertThatThrownBy(() -> client.update("INSERT DATA {}")).isInstanceOf(StoreUnavailableException.class);
        assertThat(pauses).isEmpty();
    }

    @Test
    void storeErrorIsAFailureNotUnavailable() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        String url = serve(500, "", calls);
        SparqlClient client = new SparqlClient(TestProperties.properties(url).store(), millis -> {});
        assertThatThrownBy(() -> client.select("SELECT * WHERE {}")).isInstanceOf(StoreFailureException.class);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(client.ping()).isFalse();
    }

    @Test
    void unreadableAnswerIsAFailure() throws IOException {
        String url = serve(200, "not json", new AtomicInteger());
        SparqlClient client = new SparqlClient(TestProperties.properties(url).store(), millis -> {});
        assertThatThrownBy(() -> client.select("SELECT * WHERE {}")).isInstanceOf(StoreFailureException.class);
    }

    @Test
    void readsSolutions() throws IOException {
        String url = serve(
                200,
                "{\"head\":{\"vars\":[\"x\"]},\"results\":{\"bindings\":[{\"x\":{\"type\":\"literal\",\"value\":\"1\"}}]}}",
                new AtomicInteger());
        SparqlClient client = new SparqlClient(TestProperties.properties(url).store(), millis -> {});
        assertThat(client.select("SELECT ?x WHERE {}")).hasSize(1);
        assertThat(client.ping()).isTrue();
    }
}
