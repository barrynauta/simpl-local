package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.web.trace.TraceContext;
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
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPARQL 1.1 protocol client for Apache Jena Fuseki (E3), with the timeouts and retries of D44 and D71, the two store
 * users of D83, and trace forwarding (D86). Results are parsed with Jena's SPARQL JSON results reader (REUSE-02).
 */
public class HttpSparqlStore implements SparqlStore {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSparqlStore.class);
    private static final long[] READ_RETRY_DELAYS_MS = {200, 400};
    private static final String RESULTS_JSON = "application/sparql-results+json";

    private final HttpClient httpClient;
    private final URI queryUri;
    private final URI updateUri;
    private final String readAuthorization;
    private final String updateAuthorization;
    private final Duration readTimeout;
    private final Duration updateTimeout;

    /**
     * @param httpClient client with the connect timeout of D44
     * @param store store settings
     */
    public HttpSparqlStore(HttpClient httpClient, VocabularyManagerProperties.Store store) {
        this.httpClient = httpClient;
        String base = store.url().endsWith("/") ? store.url().substring(0, store.url().length() - 1) : store.url();
        this.queryUri = URI.create(base + "/query");
        this.updateUri = URI.create(base + "/update");
        this.readAuthorization = basic(store.readUser(), store.readPassword());
        this.updateAuthorization = basic(store.updateUser(), store.updatePassword());
        this.readTimeout = store.readTimeout();
        this.updateTimeout = store.updateTimeout();
    }

    private static String basic(String user, String password) {
        String pair = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<QuerySolution> select(String query) {
        byte[] body = readWithRetry(query, readTimeout);
        ResultSet results = ResultSetMgr.read(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
        List<QuerySolution> solutions = new ArrayList<>();
        results.forEachRemaining(solutions::add);
        return solutions;
    }

    @Override
    public boolean ask(String query) {
        byte[] body = readWithRetry(query, readTimeout);
        return ResultSetMgr.readBoolean(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
    }

    @Override
    public void update(String update) {
        HttpRequest request = request(updateUri, updateAuthorization, updateTimeout)
                .header("Content-Type", "application/sparql-update; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(update, StandardCharsets.UTF_8))
                .build();
        send(request, "update");
    }

    @Override
    public boolean ping(Duration timeout) {
        HttpRequest request = request(queryUri, readAuthorization, timeout)
                .header("Content-Type", "application/sparql-query; charset=utf-8")
                .header("Accept", RESULTS_JSON)
                .POST(HttpRequest.BodyPublishers.ofString("ASK {}", StandardCharsets.UTF_8))
                .build();
        try {
            send(request, "ping");
            return true;
        } catch (StoreUnavailableException | StoreException e) {
            LOG.warn("Store health probe failed: {}", e.getMessage());
            return false;
        }
    }

    private byte[] readWithRetry(String query, Duration timeout) {
        int attempt = 0;
        while (true) {
            HttpRequest request = request(queryUri, readAuthorization, timeout)
                    .header("Content-Type", "application/sparql-query; charset=utf-8")
                    .header("Accept", RESULTS_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8))
                    .build();
            try {
                return send(request, "query");
            } catch (StoreUnavailableException e) {
                if (attempt >= READ_RETRY_DELAYS_MS.length) {
                    throw e;
                }
                LOG.warn("Store query failed ({}), retry {} after {} ms", e.getMessage(), attempt + 1,
                        READ_RETRY_DELAYS_MS[attempt]);
                sleep(READ_RETRY_DELAYS_MS[attempt]);
                attempt++;
            }
        }
    }

    private HttpRequest.Builder request(URI uri, String authorization, Duration timeout) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(uri).timeout(timeout).header("Authorization", authorization);
        TraceContext.outgoingHeaders().forEach(builder::header);
        return builder;
    }

    private byte[] send(HttpRequest request, String kind) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            }
            if (status == 503 || status == 502 || status == 504) {
                throw new StoreUnavailableException("Store " + kind + " answered " + status, null);
            }
            throw new StoreException("Store " + kind + " answered " + status, null);
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new StoreUnavailableException("Store " + kind + " connection failed", e);
        } catch (HttpTimeoutException e) {
            throw new StoreUnavailableException("Store " + kind + " timed out", e);
        } catch (IOException e) {
            throw new StoreUnavailableException("Store " + kind + " connection error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Store " + kind + " interrupted", e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted while waiting to retry", e);
        }
    }
}
