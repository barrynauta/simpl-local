package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * SPARQL 1.1 protocol client for the Fuseki dataset (E3). Applies the timeouts and retries of D44: connect 2 s,
 * response 10 s, read-only queries retried twice after 200 ms and 400 ms on a connection failure or timeout,
 * updates never retried.
 */
@Component
public class SparqlClient {

    private static final Logger LOG = LoggerFactory.getLogger(SparqlClient.class);
    private static final long[] RETRY_DELAYS_MS = {200, 400};

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final URI queryUri;
    private final URI updateUri;
    private final Duration responseTimeout;

    public SparqlClient(VocabularyProperties properties, ObjectMapper mapper) {
        VocabularyProperties.Store store = properties.store();
        this.http = HttpClient.newBuilder()
                .connectTimeout(store.connectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.mapper = mapper;
        String base = store.url().endsWith("/") ? store.url() : store.url() + "/";
        this.queryUri = URI.create(base + "query");
        this.updateUri = URI.create(base + "update");
        this.responseTimeout = store.responseTimeout();
    }

    /** Runs a SELECT query and returns the bindings as maps of variable to lexical value. */
    public List<Map<String, String>> select(String query) {
        JsonNode json = runQuery(query, responseTimeout, true);
        List<Map<String, String>> rows = new ArrayList<>();
        for (JsonNode binding : json.path("results").path("bindings")) {
            Map<String, String> row = new HashMap<>();
            binding.forEachEntry(
                    (name, value) -> row.put(name, value.path("value").asString()));
            rows.add(row);
        }
        return rows;
    }

    /** Runs an ASK query with the given timeout and no retry; used by the health check. */
    public boolean ask(String query, Duration timeout) {
        return runQuery(query, timeout, false).path("boolean").asBoolean(false);
    }

    /** Runs one SPARQL update request; Fuseki runs it in one transaction. Never retried (D44). */
    public void update(String update) {
        HttpRequest request = HttpRequest.newBuilder(updateUri)
                .timeout(responseTimeout)
                .header("Content-Type", "application/sparql-update; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(update, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() / 100 != 2) {
            throw new StoreException("Store update failed with status " + response.statusCode());
        }
    }

    private JsonNode runQuery(String query, Duration timeout, boolean retry) {
        HttpRequest request = HttpRequest.newBuilder(queryUri)
                .timeout(timeout)
                .header("Content-Type", "application/sparql-query; charset=utf-8")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofString(query, StandardCharsets.UTF_8))
                .build();
        int attempt = 0;
        while (true) {
            try {
                HttpResponse<String> response = send(request);
                if (response.statusCode() / 100 != 2) {
                    throw new StoreException("Store query failed with status " + response.statusCode());
                }
                return mapper.readTree(response.body());
            } catch (StoreUnavailableException e) {
                if (!retry || attempt >= RETRY_DELAYS_MS.length) {
                    throw e;
                }
                LOG.warn("Store query failed, retry {} after {} ms", attempt + 1, RETRY_DELAYS_MS[attempt]);
                sleep(RETRY_DELAYS_MS[attempt]);
                attempt++;
            } catch (tools.jackson.core.JacksonException e) {
                throw new StoreException("Store answer could not be read", e);
            }
        }
    }

    private HttpResponse<String> send(HttpRequest original) {
        String correlationId = org.slf4j.MDC.get("correlationId");
        HttpRequest request = correlationId == null
                ? original
                : HttpRequest.newBuilder(original, (name, value) -> true)
                        .header("X-Correlation-Id", correlationId)
                        .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new StoreUnavailableException("Store connection failed", e);
        } catch (HttpTimeoutException e) {
            throw new StoreUnavailableException("Store did not answer in time", e);
        } catch (IOException e) {
            throw new StoreUnavailableException("Store connection failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted while calling the store", e);
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
