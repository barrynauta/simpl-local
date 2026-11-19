package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.web.CorrelationIdFilter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.resultset.ResultSetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * SPARQL 1.1 protocol client for the Fuseki dataset. Applies the timeouts and the retry policy of decision D44:
 * connect timeout and response timeout per call; read-only queries are retried at most twice on a connection
 * failure or timeout (after 200 ms, then 400 ms); updates are never retried.
 */
@Component
public class SparqlClient {

    private static final Logger LOG = LoggerFactory.getLogger(SparqlClient.class);
    private static final long[] RETRY_DELAYS_MS = {200, 400};

    private final HttpClient httpClient;
    private final URI queryUri;
    private final URI updateUri;
    private final Duration responseTimeout;
    private final Duration healthTimeout;
    private final Sleeper sleeper;

    /** Pause between retries; replaced in unit tests. */
    @FunctionalInterface
    public interface Sleeper {
        /**
         * Sleeps.
         *
         * @param millis how long
         * @throws InterruptedException when interrupted
         */
        void sleep(long millis) throws InterruptedException;
    }

    /**
     * Creates the client from the configuration.
     *
     * @param properties the configuration
     */
    @org.springframework.beans.factory.annotation.Autowired
    public SparqlClient(AppProperties properties) {
        this(properties.store(), Thread::sleep);
    }

    /**
     * Creates the client.
     *
     * @param store the store settings
     * @param sleeper the pause between retries
     */
    public SparqlClient(AppProperties.Store store, Sleeper sleeper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(store.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String base = store.datasetUrl().endsWith("/")
                ? store.datasetUrl().substring(0, store.datasetUrl().length() - 1)
                : store.datasetUrl();
        this.queryUri = URI.create(base + "/query");
        this.updateUri = URI.create(base + "/update");
        this.responseTimeout = store.responseTimeout();
        this.healthTimeout = store.healthTimeout();
        this.sleeper = sleeper;
    }

    /**
     * Runs a SELECT query, with the read retry policy.
     *
     * @param sparql the query text, built with parameterised strings
     * @return the solutions
     * @throws StoreUnavailableException when the store stays unreachable or times out
     * @throws StoreFailureException when the store answers with an error
     */
    public List<QuerySolution> select(String sparql) {
        StoreUnavailableException last = null;
        for (int attempt = 0; attempt <= RETRY_DELAYS_MS.length; attempt++) {
            if (attempt > 0) {
                pause(RETRY_DELAYS_MS[attempt - 1]);
            }
            try {
                return parse(send(queryUri, "application/sparql-query", sparql, responseTimeout));
            } catch (StoreUnavailableException e) {
                last = e;
                LOG.warn("Store query attempt {} failed: {}", attempt + 1, e.getMessage());
            }
        }
        throw last;
    }

    /**
     * Runs an update in one store transaction. Never retried (decision D44).
     *
     * @param sparqlUpdate the update request text
     * @throws StoreUnavailableException when the store is unreachable or times out
     * @throws StoreFailureException when the store answers with an error
     */
    public void update(String sparqlUpdate) {
        send(updateUri, "application/sparql-update", sparqlUpdate, responseTimeout);
    }

    /**
     * Checks that the store answers a trivial query within the health timeout, without retries.
     *
     * @return true when the store answers
     */
    public boolean ping() {
        try {
            send(queryUri, "application/sparql-query", "ASK {}", healthTimeout);
            return true;
        } catch (StoreUnavailableException | StoreFailureException e) {
            LOG.warn("Store health probe failed: {}", e.getMessage());
            return false;
        }
    }

    private byte[] send(URI uri, String contentType, String body, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Content-Type", contentType + "; charset=utf-8")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            builder.header(CorrelationIdFilter.HEADER, correlationId);
        }
        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new StoreUnavailableException("Store call to " + uri.getPath() + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Store call to " + uri.getPath() + " was interrupted", e);
        }
        if (response.statusCode() / 100 != 2) {
            throw new StoreFailureException(
                    "Store call to " + uri.getPath() + " answered HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static List<QuerySolution> parse(byte[] body) {
        try {
            ResultSet resultSet = ResultSetMgr.read(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
            List<QuerySolution> solutions = new ArrayList<>();
            while (resultSet.hasNext()) {
                solutions.add(resultSet.next());
            }
            return solutions;
        } catch (ResultSetException | org.apache.jena.riot.RiotException e) {
            throw new StoreFailureException("Store answer could not be read", e);
        }
    }

    private void pause(long millis) {
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted while waiting to retry", e);
        }
    }
}
