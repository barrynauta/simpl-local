package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.Problems;
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
import org.springframework.stereotype.Component;

/** SPARQL protocol client for Apache Jena Fuseki, with the timeouts and retries of decision D44. */
@Component
public class HttpSparqlClient implements SparqlClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSparqlClient.class);
    private static final long[] BACKOFF_MILLIS = {200, 400, 800, 1600};

    private final HttpClient httpClient;
    private final URI queryUri;
    private final URI updateUri;
    private final Duration responseTimeout;
    private final int readRetries;

    public HttpSparqlClient(VocabularyManagerProperties properties) {
        VocabularyManagerProperties.Store store = properties.store();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(store.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String base = store.url().endsWith("/") ? store.url() : store.url() + "/";
        this.queryUri = URI.create(base + "query");
        this.updateUri = URI.create(base + "update");
        this.responseTimeout = store.responseTimeout();
        this.readRetries = store.readRetries();
    }

    @Override
    public List<QuerySolution> select(String query) {
        StoreCallFailure lastFailure = null;
        for (int attempt = 0; attempt <= readRetries; attempt++) {
            if (attempt > 0) {
                pause(BACKOFF_MILLIS[Math.min(attempt - 1, BACKOFF_MILLIS.length - 1)]);
            }
            try {
                return parse(send(queryUri, "application/sparql-query", query, responseTimeout));
            } catch (StoreCallFailure failure) {
                lastFailure = failure;
                LOG.warn("Store query failed (attempt {} of {})", attempt + 1, readRetries + 1);
            }
        }
        throw Problems.storeUnavailable(lastFailure);
    }

    @Override
    public void update(String update) {
        try {
            send(updateUri, "application/sparql-update", update, responseTimeout);
        } catch (StoreCallFailure failure) {
            throw Problems.storeUnavailable(failure);
        }
    }

    @Override
    public boolean ping(Duration timeout) {
        try {
            send(queryUri, "application/sparql-query", "ASK {}", timeout);
            return true;
        } catch (RuntimeException e) {
            LOG.debug("Store ping failed: {}", e.getMessage());
            return false;
        }
    }

    private byte[] send(URI uri, String contentType, String body, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Content-Type", contentType + "; charset=UTF-8")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new StoreCallFailure(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreCallFailure(e);
        }
        int status = response.statusCode();
        if (status == 503 || status == 502 || status == 504) {
            throw new StoreCallFailure(new IOException("Store answered " + status));
        }
        if (status >= 300) {
            throw Problems.storeError("Internal Server Error", new IOException("Store answered " + status));
        }
        return response.body();
    }

    private static List<QuerySolution> parse(byte[] body) {
        ResultSet resultSet = ResultSetMgr.read(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
        List<QuerySolution> rows = new ArrayList<>();
        resultSet.forEachRemaining(rows::add);
        return rows;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** A connection failure or timeout: the only store failures that are retried and give 503. */
    static final class StoreCallFailure extends RuntimeException {
        StoreCallFailure(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
