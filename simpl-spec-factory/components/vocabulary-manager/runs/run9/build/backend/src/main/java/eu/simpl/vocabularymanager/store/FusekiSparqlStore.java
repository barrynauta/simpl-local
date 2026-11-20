package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SPARQL over HTTP to Apache Jena Fuseki with the timeouts and retries of D44 and D71. Queries use the read user,
 * updates the update user (D83). Each update request is one store transaction.
 */
@Component
public class FusekiSparqlStore implements SparqlStore {

    private static final Logger LOG = LoggerFactory.getLogger(FusekiSparqlStore.class);
    private static final long[] READ_RETRY_DELAYS_MS = {200, 400};
    private static final String RESULTS_JSON = "application/sparql-results+json";

    private final HttpClient client;
    private final VocabularyManagerProperties.Store config;
    private final OutboundHeaders outboundHeaders;
    private final String readAuthorization;
    private final String updateAuthorization;

    /**
     * @param properties component configuration
     * @param outboundHeaders trace headers to forward
     */
    public FusekiSparqlStore(VocabularyManagerProperties properties, OutboundHeaders outboundHeaders) {
        this.config = properties.store();
        this.outboundHeaders = outboundHeaders;
        this.client = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.readAuthorization = basic(config.readUser(), config.readPassword());
        this.updateAuthorization = basic(config.updateUser(), config.updatePassword());
    }

    private static String basic(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<Row> select(String query) {
        byte[] body = queryWithRetry(query, config.readTimeout());
        ResultSet results = ResultSetMgr.read(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
        List<String> variables = results.getResultVars();
        List<Row> rows = new ArrayList<>();
        while (results.hasNext()) {
            QuerySolution solution = results.next();
            Map<String, org.apache.jena.graph.Node> values = new HashMap<>();
            for (String variable : variables) {
                RDFNode node = solution.get(variable);
                if (node != null) {
                    values.put(variable, node.asNode());
                }
            }
            rows.add(new Row(values));
        }
        return rows;
    }

    @Override
    public boolean ask(String query) {
        byte[] body = queryWithRetry(query, config.readTimeout());
        return ResultSetMgr.readBoolean(new ByteArrayInputStream(body), ResultSetLang.RS_JSON);
    }

    @Override
    public void update(String update) {
        HttpRequest request = request(config.updateUrl(), updateAuthorization, config.updateTimeout())
                .header("Content-Type", "application/sparql-update; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(update, StandardCharsets.UTF_8))
                .build();
        send(request, "update");
    }

    @Override
    public boolean ping(Duration timeout) {
        HttpRequest request = request(config.queryUrl(), readAuthorization, timeout)
                .header("Content-Type", "application/sparql-query; charset=utf-8")
                .header("Accept", RESULTS_JSON)
                .POST(HttpRequest.BodyPublishers.ofString("ASK {}", StandardCharsets.UTF_8))
                .build();
        try {
            send(request, "health");
            return true;
        } catch (StoreUnavailableException | StoreFailureException ex) {
            LOG.warn("Store health probe failed: {}", ex.getMessage());
            return false;
        }
    }

    private byte[] queryWithRetry(String query, Duration timeout) {
        StoreUnavailableException last = null;
        for (int attempt = 0; attempt <= READ_RETRY_DELAYS_MS.length; attempt++) {
            if (attempt > 0) {
                pause(READ_RETRY_DELAYS_MS[attempt - 1]);
            }
            HttpRequest request = request(config.queryUrl(), readAuthorization, timeout)
                    .header("Content-Type", "application/sparql-query; charset=utf-8")
                    .header("Accept", RESULTS_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8))
                    .build();
            try {
                return send(request, "query");
            } catch (StoreUnavailableException ex) {
                last = ex;
                LOG.warn("Store query attempt {} failed: {}", attempt + 1, ex.getMessage());
            }
        }
        throw last;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted while waiting to retry the store", ex);
        }
    }

    private HttpRequest.Builder request(String url, String authorization, Duration timeout) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(url)).timeout(timeout).header("Authorization", authorization);
        outboundHeaders.current().forEach(builder::header);
        return builder;
    }

    private byte[] send(HttpRequest request, String kind) {
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpConnectTimeoutException | ConnectException ex) {
            throw new StoreUnavailableException("Store connection failed on " + kind, ex);
        } catch (HttpTimeoutException ex) {
            throw new StoreUnavailableException("Store timed out on " + kind, ex);
        } catch (IOException ex) {
            throw new StoreUnavailableException(
                    "Store I/O failure on " + kind + ": " + ex.getClass().getSimpleName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted during store " + kind, ex);
        }
        int status = response.statusCode();
        if (status == 503 || status == 502 || status == 504) {
            throw new StoreUnavailableException("Store answered " + status + " on " + kind, null);
        }
        if (status < 200 || status >= 300) {
            throw new StoreFailureException("Store answered " + status + " on " + kind);
        }
        return response.body();
    }
}
