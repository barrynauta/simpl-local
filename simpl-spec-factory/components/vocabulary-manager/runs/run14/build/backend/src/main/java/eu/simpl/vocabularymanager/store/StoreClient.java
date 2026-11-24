package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HTTP access to the Fuseki dataset (E3, D44, D83, D103, D134, D201). Queries use the read user, updates the
 * update user. Read queries are retried twice on a connection failure or timeout; updates never. A circuit
 * breaker counts timeouts, connection failures and store 5xx; while it is open the caller gets 503 at once.
 */
@Component
public class StoreClient {

    public static final String STORE_DOWN = "The vocabulary store is not available. Retry later.";

    private static final Logger LOG = LoggerFactory.getLogger(StoreClient.class);
    private static final List<Long> READ_RETRY_DELAYS_MS = List.of(200L, 400L);

    private final HttpClient httpClient;
    private final URI queryUri;
    private final URI updateUri;
    private final String readAuthorization;
    private final String updateAuthorization;
    private final String readUser;
    private final String updateUser;
    private final Duration readTimeout;
    private final Duration updateTimeout;
    private final CircuitBreaker breaker;

    public StoreClient(AppProperties properties, CircuitBreakerRegistry breakers) {
        AppProperties.Store store = properties.store();
        String base = store.url().endsWith("/") ? store.url() : store.url() + "/";
        this.queryUri = URI.create(base + store.dataset() + "/query");
        this.updateUri = URI.create(base + store.dataset() + "/update");
        this.readUser = store.readUser();
        this.updateUser = store.updateUser();
        this.readAuthorization = basic(store.readUser(), store.readPassword());
        this.updateAuthorization = basic(store.updateUser(), store.updatePassword());
        this.readTimeout = Duration.ofMillis(store.readTimeoutMs());
        this.updateTimeout = Duration.ofMillis(store.updateTimeoutMs());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(store.connectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.breaker = breakers.circuitBreaker("store");
    }

    /**
     * Runs a SELECT query with the read retries of D44.
     *
     * @param sparql the query, built with ParameterizedSparqlString
     * @return the rewindable result
     * @throws ApiException 503 when the store is unreachable, 500 on other store errors
     */
    public ResultSetRewindable select(String sparql) {
        return withReadRetries(() -> guarded(() -> runSelect(sparql, readTimeout)));
    }

    /**
     * Runs an ASK query with the read retries of D44.
     *
     * @param sparql the query
     * @return the answer
     * @throws ApiException 503 when the store is unreachable, 500 on other store errors
     */
    public boolean ask(String sparql) {
        return withReadRetries(() -> guarded(() -> runAsk(sparql, readTimeout)));
    }

    /**
     * Probes the store for GET /health: one call, no retry, no breaker (D134).
     *
     * @param timeout the probe timeout
     * @return true when the store answered
     */
    public boolean probe(Duration timeout) {
        try {
            runAsk("ASK {}", timeout);
            return true;
        } catch (StoreUnavailableException | StoreFailureException e) {
            LOG.debug("Store probe failed: {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Runs one SPARQL update request, which the store runs as one transaction. Never retried (D44).
     *
     * @param sparql the update
     * @throws ApiException 503 when the store is unreachable or times out, 500 on other store errors
     */
    public void update(String sparql) {
        try {
            guarded(() -> {
                HttpRequest request = request(updateUri, updateAuthorization, updateTimeout)
                        .header("Content-Type", "application/sparql-update; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(sparql, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<InputStream> response = send(request);
                try (InputStream body = response.body()) {
                    checkStatus(response.statusCode(), updateUser);
                    body.transferTo(OutputStreamSink.INSTANCE);
                } catch (IOException e) {
                    throw new StoreUnavailableException("Reading the store answer failed", e);
                }
                return Boolean.TRUE;
            });
        } catch (StoreUnavailableException e) {
            throw ApiException.unavailable(STORE_DOWN, e);
        } catch (StoreFailureException e) {
            throw ApiException.internal(e);
        }
    }

    private <T> T withReadRetries(Supplier<T> call) {
        StoreUnavailableException last = null;
        for (int attempt = 0; attempt <= READ_RETRY_DELAYS_MS.size(); attempt++) {
            try {
                return call.get();
            } catch (BreakerOpenException e) {
                throw ApiException.unavailable(STORE_DOWN, e);
            } catch (StoreUnavailableException e) {
                last = e;
                if (attempt < READ_RETRY_DELAYS_MS.size()) {
                    sleep(READ_RETRY_DELAYS_MS.get(attempt));
                }
            } catch (StoreFailureException e) {
                throw ApiException.internal(e);
            }
        }
        throw ApiException.unavailable(STORE_DOWN, last);
    }

    private <T> T guarded(StoreCall<T> call) {
        try {
            return breaker.executeCheckedSupplier(call::run);
        } catch (CallNotPermittedException e) {
            throw new BreakerOpenException(e);
        } catch (StoreUnavailableException | StoreFailureException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted", e);
        } catch (Throwable e) {
            throw new StoreFailureException("Unexpected store error", e, false);
        }
    }

    private ResultSetRewindable runSelect(String sparql, Duration timeout) throws InterruptedException {
        HttpResponse<InputStream> response = send(queryRequest(sparql, timeout));
        try (InputStream body = response.body()) {
            checkStatus(response.statusCode(), readUser);
            ResultSet results = ResultSetMgr.read(body, ResultSetLang.RS_JSON);
            return ResultSetFactory.copyResults(results);
        } catch (IOException e) {
            throw new StoreUnavailableException("Reading the store answer failed", e);
        }
    }

    private boolean runAsk(String sparql, Duration timeout) throws InterruptedException {
        HttpResponse<InputStream> response = send(queryRequest(sparql, timeout));
        try (InputStream body = response.body()) {
            checkStatus(response.statusCode(), readUser);
            return ResultSetMgr.readBoolean(body, ResultSetLang.RS_JSON);
        } catch (IOException e) {
            throw new StoreUnavailableException("Reading the store answer failed", e);
        }
    }

    private HttpRequest queryRequest(String sparql, Duration timeout) {
        return request(queryUri, readAuthorization, timeout)
                .header("Content-Type", "application/sparql-query; charset=utf-8")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofString(sparql, StandardCharsets.UTF_8))
                .build();
    }

    private HttpRequest.Builder request(URI uri, String authorization, Duration timeout) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(uri).timeout(timeout).header("Authorization", authorization);
        TraceHeaders.current().forEach(builder::header);
        return builder;
    }

    private HttpResponse<InputStream> send(HttpRequest request) throws InterruptedException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new StoreUnavailableException("Store connection failed", e);
        } catch (HttpTimeoutException e) {
            throw new StoreUnavailableException("Store timed out", e);
        } catch (IOException e) {
            throw new StoreUnavailableException("Store connection failed", e);
        }
    }

    private static void checkStatus(int status, String user) {
        if (status >= 200 && status < 300) {
            return;
        }
        if (status == 401 || status == 403) {
            LOG.error("The store refused user '{}' with status {}", user, status);
            throw new StoreFailureException("Store refused the user", null, false);
        }
        throw new StoreFailureException("Store answered " + status, null, status >= 500);
    }

    private static String basic(String user, String password) {
        String pair = user + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface StoreCall<T> {
        T run() throws InterruptedException;
    }

    /** Discards bytes. */
    private static final class OutputStreamSink extends java.io.OutputStream {
        static final OutputStreamSink INSTANCE = new OutputStreamSink();

        @Override
        public void write(int b) {
            // Discard the update answer; only the status matters.
        }

        @Override
        public void write(byte[] b, int off, int len) {
            // Discard the update answer; only the status matters.
        }
    }

    /** The breaker is open. */
    static final class BreakerOpenException extends StoreUnavailableException {
        private static final long serialVersionUID = 1L;

        BreakerOpenException(Throwable cause) {
            super("Store circuit breaker is open", cause);
        }
    }
}
