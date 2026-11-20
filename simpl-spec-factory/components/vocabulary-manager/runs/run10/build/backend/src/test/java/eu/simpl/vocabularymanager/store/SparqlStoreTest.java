package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ProblemException;
import eu.simpl.vocabularymanager.web.OutboundHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** Store failure handling (D44, D103): 500 for store errors, retries on reads only, breaker gives 503 at once. */
class SparqlStoreTest {

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private SparqlStore storeAnswering(int status, String body, Duration delay) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ds", exchange -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/sparql-results+json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        return store("http://127.0.0.1:" + server.getAddress().getPort() + "/ds", CircuitBreaker.ofDefaults("t"));
    }

    private static SparqlStore store(String url, CircuitBreaker breaker) {
        AppProperties.Store settings = new AppProperties.Store(
                url,
                "r",
                "p",
                "w",
                "p",
                Duration.ofMillis(500),
                Duration.ofMillis(300),
                Duration.ofMillis(300),
                Duration.ofMillis(300));
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        OutboundHeaders headers = new OutboundHeaders(
                beans.getBeanProvider(io.micrometer.tracing.Tracer.class),
                beans.getBeanProvider(io.micrometer.tracing.propagation.Propagator.class));
        return new SparqlStore(settings, SparqlStore.httpClient(Duration.ofMillis(500)), breaker, headers);
    }

    @Test
    void readsResults() throws IOException {
        SparqlStore store = storeAnswering(
                200,
                "{\"head\":{\"vars\":[\"a\"]},\"results\":{\"bindings\":[{\"a\":{\"type\":\"literal\",\"value\":\"x\"}}]}}",
                Duration.ZERO);
        assertThat(store.select("SELECT * {}")).hasSize(1);
        assertThat(store.ping()).isTrue();
    }

    @Test
    void storeErrorIs500AndNotRetried() throws IOException {
        SparqlStore store = storeAnswering(500, "", Duration.ZERO);
        assertThatThrownBy(() -> store.select("SELECT * {}"))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(500));
        assertThat(calls.get()).isEqualTo(1);
        assertThatThrownBy(() -> store.update("INSERT DATA {}"))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(500));
    }

    @Test
    void unreadableResultIs500() throws IOException {
        SparqlStore store = storeAnswering(200, "not json", Duration.ZERO);
        assertThatThrownBy(() -> store.select("SELECT * {}"))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(500));
    }

    @Test
    void readTimeoutIsRetriedTwiceThen503AndUpdatesAreNotRetried() throws IOException {
        SparqlStore store = storeAnswering(200, "{}", Duration.ofMillis(800));
        assertThatThrownBy(() -> store.select("SELECT * {}")).isInstanceOfSatisfying(ProblemException.class, e -> {
            assertThat(e.status()).isEqualTo(503);
            assertThat(e.headers()).containsEntry("Retry-After", "5");
        });
        assertThat(calls.get()).isEqualTo(3);
        calls.set(0);
        assertThatThrownBy(() -> store.update("INSERT DATA {}"))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(503));
        assertThat(calls.get()).isEqualTo(1);
        assertThat(store.ping()).isFalse();
    }

    @Test
    void openBreakerGives503WithoutCallingTheStore() {
        CircuitBreaker breaker =
                CircuitBreaker.of("open", CircuitBreakerConfig.custom().build());
        breaker.transitionToOpenState();
        SparqlStore store = store("http://127.0.0.1:9/ds", breaker);
        long start = System.nanoTime();
        assertThatThrownBy(() -> store.select("SELECT * {}"))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(503));
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(150);
    }
}
