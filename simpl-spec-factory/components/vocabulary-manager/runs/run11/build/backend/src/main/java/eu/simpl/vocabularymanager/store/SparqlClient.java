package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.config.TraceHeaders;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
import java.util.concurrent.Callable;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SPARQL 1.1 protocol client for the Fuseki store (E3). Queries use the read user and updates the
 * update user (D83). Timeouts, read retries and the circuit breaker follow D44, D71, D103 and D134.
 * The W3C trace context and the correlation id are forwarded (D86).
 */
@Component
public class SparqlClient {

    public static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";

    private static final Logger LOG = LoggerFactory.getLogger(SparqlClient.class);
    private static final String RESULTS_JSON = "application/sparql-results+json";
    private static final long[] READ_RETRY_DELAYS_MS = {200, 400};

    private final AppProperties.Store config;
    private final HttpClient http;
    private final CircuitBreaker breaker;
    private final TraceHeaders traceHeaders;
    private final String readAuth;
    private final String updateAuth;

    public SparqlClient(
            AppProperties properties,
            HttpClient storeHttpClient,
            CircuitBreaker storeCircuitBreaker,
            TraceHeaders traceHeaders) {
        this.config = properties.store();
        this.http = storeHttpClient;
        this.breaker = storeCircuitBreaker;
        this.traceHeaders = traceHeaders;
        this.readAuth = basic(config.readUser(), config.readPassword());
        this.updateAuth = basic(config.updateUser(), config.updatePassword());
    }

    /**
     * Runs a SELECT query with the read retries of D44.
     *
     * @throws ApiException 503 when the store is unreachable, 500 on other store errors
     */
    public List<Binding> select(String query) {
        byte[] body = readWithRetry(query);
        ResultSet rows = ResultSetMgr.read(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
        List<Binding> result = new ArrayList<>();
        while (rows.hasNext()) {
            result.add(rows.nextBinding());
        }
        return result;
    }

    /**
     * Runs an ASK query with the read retries of D44.
     *
     * @throws ApiException 503 when the store is unreachable, 500 on other store errors
     */
    public boolean ask(String query) {
        byte[] body = readWithRetry(query);
        return ResultSetMgr.readBoolean(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
    }

    /**
     * Runs one SPARQL update request: one store transaction (D8, D44). Never retried.
     *
     * @throws ApiException 503 when the store is unreachable or times out, 500 on other errors
     */
    public void update(String update) {
        guarded(() -> send(post("update", update, "application/sparql-update", updateAuth, config.updateTimeout())));
    }

    /**
     * One bounded call for GET /health (D134): no retry, no breaker wait.
     *
     * @return true when the store answered in time
     */
    public boolean ping() {
        if (breaker.getState() == CircuitBreaker.State.OPEN) {
            return false;
        }
        try {
            send(post("query", "ASK {}", "application/sparql-query", readAuth, config.healthTimeout()));
            return true;
        } catch (StoreCallException ex) {
            LOG.warn("Store health check failed: {}", ex.kind());
            return false;
        }
    }

    public CircuitBreaker.State breakerState() {
        return breaker.getState();
    }

    private byte[] readWithRetry(String query) {
        int attempt = 0;
        while (true) {
            try {
                return guarded(
                        () -> send(post("query", query, "application/sparql-query", readAuth, config.readTimeout())));
            } catch (ApiException ex) {
                boolean retryable = ex.problemType() == ProblemType.SERVICE_UNAVAILABLE
                        && breaker.getState() != CircuitBreaker.State.OPEN
                        && attempt < READ_RETRY_DELAYS_MS.length;
                if (!retryable) {
                    throw ex;
                }
                pause(READ_RETRY_DELAYS_MS[attempt]);
                attempt++;
            }
        }
    }

    private byte[] guarded(Callable<byte[]> call) {
        try {
            return breaker.executeCallable(call);
        } catch (CallNotPermittedException ex) {
            throw ApiException.unavailable(STORE_UNAVAILABLE);
        } catch (StoreCallException ex) {
            if (ex.kind() == StoreCallException.Kind.UNAVAILABLE) {
                LOG.warn("Store unavailable: {}", ex.getMessage());
                throw ApiException.unavailable(STORE_UNAVAILABLE);
            }
            LOG.error("Store call failed: {}", ex.getMessage(), ex);
            throw new ApiException(ProblemType.INTERNAL_SERVER_ERROR, "Internal Server Error");
        } catch (Exception ex) {
            LOG.error("Unexpected store client failure", ex);
            throw new ApiException(ProblemType.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private HttpRequest post(String endpoint, String body, String contentType, String auth, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.url() + "/" + endpoint))
                .timeout(timeout)
                .header("Content-Type", contentType + "; charset=utf-8")
                .header("Accept", RESULTS_JSON)
                .header("Authorization", auth)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return traceHeaders.apply(builder).build();
    }

    private byte[] send(HttpRequest request) throws StoreCallException {
        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpConnectTimeoutException | ConnectException ex) {
            throw new StoreCallException(StoreCallException.Kind.UNAVAILABLE, "connect failed: " + ex, ex);
        } catch (HttpTimeoutException ex) {
            throw new StoreCallException(StoreCallException.Kind.UNAVAILABLE, "timeout: " + ex, ex);
        } catch (IOException ex) {
            throw new StoreCallException(StoreCallException.Kind.UNAVAILABLE, "I/O failure: " + ex, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StoreCallException(StoreCallException.Kind.UNAVAILABLE, "interrupted", ex);
        }
        int status = response.statusCode();
        if (status >= 500) {
            throw new StoreCallException(StoreCallException.Kind.SERVER_ERROR, "store answered " + status, null);
        }
        if (status >= 400) {
            throw new StoreCallException(StoreCallException.Kind.CLIENT_ERROR, "store answered " + status, null);
        }
        return response.body();
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ApiException.unavailable(STORE_UNAVAILABLE);
        }
    }

    private static String basic(String user, String password) {
        String raw = user + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
