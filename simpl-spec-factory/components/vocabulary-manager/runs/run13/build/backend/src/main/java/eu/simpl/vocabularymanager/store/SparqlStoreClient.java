package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
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
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL 1.1 Protocol client for the Fuseki store (E3, D44, D83, D103).
 *
 * <p>Queries use the read user, updates the update user. Timeouts: connect 2 s, query 10 s, update 45 s.
 * Queries are retried at most twice (200 ms, 400 ms) on a connection failure or timeout; updates never.
 * A circuit breaker protects the store; while it is open, calls fail at once with 503. A timeout, a
 * connection failure and a store 5xx count as breaker failures; a store 4xx does not (D134).
 */
public class SparqlStoreClient {

    private static final Logger LOG = LoggerFactory.getLogger(SparqlStoreClient.class);
    private static final String RESULTS_JSON = "application/sparql-results+json";

    /** The store could not be reached in time. Maps to 503. */
    public static final class StoreUnavailableException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StoreUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** The store answered with an error. Maps to 500, counts for the breaker when it is a 5xx. */
    public static final class StoreErrorException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int status;

        public StoreErrorException(int status) {
            super("Store answered " + status);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    private final HttpClient httpClient;
    private final VocabularyManagerProperties.Store config;
    private final CircuitBreaker breaker;
    private final Retry readRetry;
    private final TraceHeaders traceHeaders;
    private final String readAuthorization;
    private final String updateAuthorization;

    public SparqlStoreClient(
            VocabularyManagerProperties.Store config,
            CircuitBreaker breaker,
            Retry readRetry,
            TraceHeaders traceHeaders) {
        this.config = config;
        this.breaker = breaker;
        this.readRetry = readRetry;
        this.traceHeaders = traceHeaders;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.readAuthorization = basic(config.readUser(), config.readPassword());
        this.updateAuthorization = basic(config.updateUser(), config.updatePassword());
    }

    private static String basic(String user, String password) {
        String raw = user + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Runs a SELECT query and returns all rows. Every query carries a LIMIT (D85). */
    public List<QuerySolution> select(Query query) {
        return select(query, config.readTimeout(), true);
    }

    /** Runs a SELECT with a custom timeout and without retries (health check, D44). */
    public List<QuerySolution> selectOnce(Query query, Duration timeout) {
        return select(query, timeout, false);
    }

    private List<QuerySolution> select(Query query, Duration timeout, boolean retry) {
        String body = query.serialize();
        try {
            if (retry) {
                return readRetry.executeCallable(() -> protectedCall(() -> doSelect(body, timeout)));
            }
            return protectedCall(() -> doSelect(body, timeout));
        } catch (StoreUnavailableException | StoreErrorException | ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new StoreUnavailableException("Store query failed", e);
        }
    }

    private List<QuerySolution> doSelect(String query, Duration timeout) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.queryUrl()))
                .timeout(timeout)
                .header("Content-Type", "application/sparql-query; charset=utf-8")
                .header("Accept", RESULTS_JSON)
                .header("Authorization", readAuthorization)
                .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8));
        addTraceHeaders(builder);
        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new StoreErrorException(response.statusCode());
        }
        ResultSet results = ResultSetMgr.read(new ByteArrayInputStream(response.body()), ResultSetLang.RS_JSON);
        List<QuerySolution> rows = new ArrayList<>();
        results.forEachRemaining(rows::add);
        return rows;
    }

    /** Runs an update request in one store transaction. Never retried (D44). */
    public void update(UpdateRequest request) {
        String body = request.toString();
        try {
            protectedCall(() -> {
                doUpdate(body);
                return null;
            });
        } catch (StoreUnavailableException | StoreErrorException | ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new StoreUnavailableException("Store update failed", e);
        }
    }

    private void doUpdate(String update) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.updateUrl()))
                .timeout(config.updateTimeout())
                .header("Content-Type", "application/sparql-update; charset=utf-8")
                .header("Authorization", updateAuthorization)
                .POST(HttpRequest.BodyPublishers.ofString(update, StandardCharsets.UTF_8));
        addTraceHeaders(builder);
        HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new StoreErrorException(status);
        }
    }

    private void addTraceHeaders(HttpRequest.Builder builder) {
        traceHeaders.forOutgoingCall().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
    }

    /** A store call that may fail with an I/O error. */
    @FunctionalInterface
    interface StoreCall<T> {
        T call() throws IOException, InterruptedException;
    }

    private <T> T protectedCall(StoreCall<T> call) throws Exception {
        try {
            return breaker.executeCallable(() -> translate(call));
        } catch (CallNotPermittedException e) {
            throw new StoreUnavailableException("Store circuit breaker is open", e);
        }
    }

    private static <T> T translate(StoreCall<T> call) {
        try {
            return call.call();
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new StoreUnavailableException("Store connection failed", e);
        } catch (HttpTimeoutException e) {
            throw new StoreUnavailableException("Store timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Store call interrupted", e);
        } catch (IOException e) {
            LOG.warn("Store I/O failure: {}", e.getClass().getSimpleName());
            throw new StoreUnavailableException("Store I/O failure", e);
        }
    }

    /** Maps store exceptions to the problems of D44. */
    public static ApiException toProblem(RuntimeException e) {
        if (e instanceof StoreUnavailableException) {
            return Problems.storeUnavailable();
        }
        return Problems.internal();
    }
}
