package eu.simpl.vocabulary.store;

import eu.simpl.vocabulary.config.AppProperties;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL 1.1 protocol client for the Fuseki dataset (E3) with the timeouts and retries of D44 and D71:
 * connect 2 s; reads 10 s and at most 2 retries after 200 ms and 400 ms on a connection failure or
 * timeout; updates 45 s and never retried. Results are parsed by Jena (REUSE-02).
 */
public class SparqlClient {

    /** Pauses between retries; replaced in tests. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(SparqlClient.class);

    private final AppProperties.Store config;
    private final HttpClient client;
    private final URI queryUri;
    private final URI updateUri;
    private final Sleeper sleeper;

    public SparqlClient(AppProperties.Store config, Sleeper sleeper) {
        this.config = config;
        this.sleeper = sleeper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.queryUri = URI.create(config.url() + "/query");
        this.updateUri = URI.create(config.url() + "/update");
    }

    /**
     * Runs a read-only SELECT query with retries.
     *
     * @param query the query
     * @return the result rows
     * @throws StoreUnavailableException when the store stays unreachable
     * @throws StoreException when the store answers with an error
     */
    public List<QuerySolution> select(String query) {
        int attempt = 0;
        while (true) {
            try {
                return selectOnce(query, config.readTimeout());
            } catch (IOException e) {
                if (attempt >= config.retryDelays().size()) {
                    throw new StoreUnavailableException("Store query failed after retries", e);
                }
                Duration delay = config.retryDelays().get(attempt);
                attempt++;
                LOG.info(
                        "Store query failed ({}), retry {} after {} ms",
                        e.getClass().getSimpleName(),
                        attempt,
                        delay.toMillis());
                pause(delay);
            }
        }
    }

    /**
     * Health probe: one query with the health timeout and no retry.
     *
     * @return true when the store answered
     */
    public boolean ping() {
        try {
            selectOnce("SELECT * WHERE { } LIMIT 1", config.healthTimeout());
            return true;
        } catch (IOException | RuntimeException e) {
            LOG.debug("Store ping failed: {}", e.toString());
            return false;
        }
    }

    /**
     * Runs one SPARQL update request. Fuseki runs the whole request in one write transaction, so a change
     * is all or nothing (D8, D43).
     *
     * @param update the update request
     * @throws StoreUnavailableException when the store is unreachable or times out
     * @throws StoreException when the store answers with an error
     */
    public void update(String update) {
        HttpRequest request = HttpRequest.newBuilder(updateUri)
                .timeout(config.writeTimeout())
                .header("Content-Type", "application/sparql-update; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(update, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response;
        try {
            response = send(request);
        } catch (IOException e) {
            throw new StoreUnavailableException("Store update failed", e);
        }
        if (response.statusCode() / 100 != 2) {
            throw new StoreException(
                    "Store update answered " + response.statusCode() + ": " + abbreviate(response.body()));
        }
    }

    private List<QuerySolution> selectOnce(String query, Duration timeout) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(queryUri)
                .timeout(timeout)
                .header("Content-Type", "application/sparql-query; charset=utf-8")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response = send(request);
        if (response.statusCode() != 200) {
            throw new StoreException(
                    "Store query answered " + response.statusCode() + ": " + abbreviate(response.body()));
        }
        try {
            ResultSet results = ResultSetMgr.read(new ByteArrayInputStream(response.body()), ResultSetLang.RS_JSON);
            List<QuerySolution> rows = new ArrayList<>();
            results.forEachRemaining(rows::add);
            return rows;
        } catch (RuntimeException e) {
            throw new StoreException("Store query result could not be read", e);
        }
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling the store", e);
        }
    }

    private void pause(Duration delay) {
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted while waiting to retry", e);
        }
    }

    private static String abbreviate(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        return text.length() > 300 ? text.substring(0, 300) : text;
    }
}
