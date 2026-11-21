package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.AppProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * SPARQL over HTTP to Fuseki with the timeouts, retries and circuit breaker of decisions D44, D71,
 * D103 and D134. Queries use the read user, updates the update user (D83). Each update request is
 * one store transaction. The W3C trace context and the correlation id are forwarded (D86).
 */
@Component
public class StoreClient {

    private static final Logger LOG = LoggerFactory.getLogger(StoreClient.class);
    private static final long[] READ_RETRY_DELAYS_MS = {200, 400};

    private final AppProperties.Store cfg;
    private final HttpClient http;
    private final CircuitBreaker breaker;
    private final String readAuth;
    private final String updateAuth;

    public StoreClient(AppProperties props, MeterRegistry meters) {
        this.cfg = props.store();
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(cfg.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        CircuitBreakerConfig cbc = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(false)
                .recordException(t -> t instanceof StoreException se && se.countsAsFailure())
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(cbc);
        this.breaker = registry.circuitBreaker("store");
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
        this.readAuth = basic(cfg.readUser(), cfg.readPassword());
        this.updateAuth = basic(cfg.updateUser(), cfg.updatePassword());
    }

    private static String basic(String user, String password) {
        String raw = (user == null ? "" : user) + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** A SELECT query; results are fully read. Retried on connection failure or timeout. */
    public List<QuerySolution> select(String sparql) {
        return withReadRetry(() -> {
            byte[] body = post(cfg.queryUrl(), sparql, true, cfg.readTimeout());
            ResultSet rs = ResultSetMgr.read(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
            List<QuerySolution> rows = new ArrayList<>();
            rs.forEachRemaining(rows::add);
            return rows;
        });
    }

    /** An ASK query. */
    public boolean ask(String sparql) {
        return withReadRetry(() -> askOnce(sparql, cfg.readTimeout()));
    }

    /** One ASK with a short timeout and no retry, for GET /health (D44, D134). */
    public boolean ping(Duration timeout) {
        try {
            return breaker.executeSupplier(() -> askOnce("ASK {}", timeout));
        } catch (CallNotPermittedException e) {
            return false;
        } catch (StoreException e) {
            return false;
        }
    }

    private boolean askOnce(String sparql, Duration timeout) {
        byte[] body = post(cfg.queryUrl(), sparql, true, timeout);
        return ResultSetMgr.readBoolean(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
    }

    /** A SPARQL update: one store transaction, never retried here (D44). */
    public void update(String sparqlUpdate) {
        guarded(() -> post(cfg.updateUrl(), sparqlUpdate, false, cfg.updateTimeout()));
    }

    private <T> T withReadRetry(Supplier<T> call) {
        int attempt = 0;
        while (true) {
            try {
                return guarded(call);
            } catch (StoreException e) {
                if (!e.unavailable() || attempt >= READ_RETRY_DELAYS_MS.length || breakerOpen()) {
                    throw e;
                }
                sleep(READ_RETRY_DELAYS_MS[attempt++]);
            }
        }
    }

    private boolean breakerOpen() {
        return breaker.getState() == CircuitBreaker.State.OPEN;
    }

    private <T> T guarded(Supplier<T> call) {
        try {
            return breaker.executeSupplier(call);
        } catch (CallNotPermittedException e) {
            throw StoreException.unavailable("store circuit breaker is open", e);
        }
    }

    private byte[] post(String url, String body, boolean query, Duration timeout) {
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Authorization", query ? readAuth : updateAuth)
                .header("Content-Type", query ? "application/sparql-query" : "application/sparql-update; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (query) {
            rb.header("Accept", "application/sparql-results+json");
        }
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            rb.header("X-Correlation-Id", correlationId);
        }
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");
        if (traceId != null && spanId != null && traceId.length() == 32 && spanId.length() == 16) {
            rb.header("traceparent", "00-" + traceId + "-" + spanId + "-01");
        }
        HttpResponse<byte[]> resp;
        try {
            resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw StoreException.unavailable("store connection failed", e);
        } catch (HttpTimeoutException e) {
            throw StoreException.unavailable("store call timed out", e);
        } catch (IOException e) {
            throw StoreException.unavailable("store I/O failure", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw StoreException.unavailable("interrupted", e);
        }
        int status = resp.statusCode();
        if (status >= 200 && status < 300) {
            return resp.body();
        }
        LOG.warn("store answered status {}", status);
        if (status == 502 || status == 503 || status == 504) {
            throw StoreException.unavailable("store status " + status, null);
        }
        if (status >= 500) {
            throw StoreException.serverError("store status " + status);
        }
        throw StoreException.clientError("store status " + status);
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw StoreException.unavailable("interrupted", e);
        }
    }

    CircuitBreaker breaker() {
        return breaker;
    }
}
