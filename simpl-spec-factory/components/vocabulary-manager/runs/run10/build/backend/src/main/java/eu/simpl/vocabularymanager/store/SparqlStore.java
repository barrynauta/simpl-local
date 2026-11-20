package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ProblemException;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.web.OutboundHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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

/**
 * SPARQL access to the store (E3) over the SPARQL 1.1 protocol. Queries use the read user, updates the update user
 * (D83). Timeouts, read retries and the circuit breaker follow D44, D71 and D103. Results are parsed with Jena.
 */
public class SparqlStore {

    private static final Logger LOG = LoggerFactory.getLogger(SparqlStore.class);
    private static final long[] READ_BACKOFF_MS = {200, 400};

    private final AppProperties.Store settings;
    private final HttpClient httpClient;
    private final CircuitBreaker breaker;
    private final OutboundHeaders outboundHeaders;
    private final String readAuthorization;
    private final String updateAuthorization;

    public SparqlStore(
            AppProperties.Store settings,
            HttpClient httpClient,
            CircuitBreaker breaker,
            OutboundHeaders outboundHeaders) {
        this.settings = settings;
        this.httpClient = httpClient;
        this.breaker = breaker;
        this.outboundHeaders = outboundHeaders;
        this.readAuthorization = basic(settings.readUser(), settings.readPassword());
        this.updateAuthorization = basic(settings.updateUser(), settings.updatePassword());
    }

    /** Creates the HTTP client with the decided connect timeout (D44). */
    public static HttpClient httpClient(Duration connectTimeout) {
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Runs a SELECT query, retrying at most twice on a connection failure or timeout.
     *
     * @throws ProblemException 503 when the store stays unavailable, 500 on another store error
     */
    public List<QuerySolution> select(String query) {
        for (int attempt = 0; ; attempt++) {
            try {
                return guarded(() -> selectOnce(query));
            } catch (StoreUnavailableException e) {
                if (attempt >= READ_BACKOFF_MS.length || e.breakerOpen()) {
                    throw e;
                }
                sleep(READ_BACKOFF_MS[attempt]);
            }
        }
    }

    /**
     * Runs an update request as one store transaction. Updates are never retried here (D44).
     *
     * @throws ProblemException 503 when the store is unavailable or times out, 500 on another store error
     */
    public void update(String update) {
        guarded(() -> {
            send(
                    settings.url() + "/update",
                    "application/sparql-update",
                    update,
                    updateAuthorization,
                    "*/*",
                    settings.updateTimeout());
            return null;
        });
    }

    /** True when the store answers an ASK within the health timeout; bypasses breaker and retries. */
    public boolean ping() {
        try {
            HttpResponse<InputStream> response = send(
                    settings.url() + "/query",
                    "application/sparql-query",
                    "ASK {}",
                    readAuthorization,
                    "application/sparql-results+json",
                    settings.healthTimeout());
            response.body().close();
            return true;
        } catch (ProblemException | IOException e) {
            return false;
        }
    }

    private List<QuerySolution> selectOnce(String query) {
        HttpResponse<InputStream> response = send(
                settings.url() + "/query",
                "application/sparql-query",
                query,
                readAuthorization,
                "application/sparql-results+json",
                settings.readTimeout());
        try (InputStream body = response.body()) {
            ResultSet results = ResultSetMgr.read(body, ResultSetLang.RS_JSON);
            List<QuerySolution> rows = new ArrayList<>();
            results.forEachRemaining(rows::add);
            return rows;
        } catch (IOException e) {
            throw new StoreUnavailableException("store response could not be read", false);
        } catch (RuntimeException e) {
            LOG.error("Store returned an unreadable result", e);
            throw new StoreFailureException();
        }
    }

    private <T> T guarded(Supplier<T> call) {
        try {
            return CircuitBreaker.decorateSupplier(breaker, call).get();
        } catch (CallNotPermittedException e) {
            throw new StoreUnavailableException("circuit breaker open", true);
        }
    }

    private HttpResponse<InputStream> send(
            String url, String contentType, String body, String authorization, String accept, Duration timeout) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", contentType + "; charset=utf-8")
                .header("Accept", accept)
                .header("Authorization", authorization)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        outboundHeaders.current().forEach(request::header);
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new StoreUnavailableException(
                    "connection failed: " + e.getClass().getSimpleName(), false);
        } catch (HttpTimeoutException e) {
            throw new StoreUnavailableException("timeout", false);
        } catch (IOException e) {
            throw new StoreUnavailableException("I/O error: " + e.getClass().getSimpleName(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("interrupted", false);
        }
        if (response.statusCode() / 100 != 2) {
            closeQuietly(response);
            LOG.error("Store answered with status {}", response.statusCode());
            throw new StoreFailureException();
        }
        return response;
    }

    private static void closeQuietly(HttpResponse<InputStream> response) {
        try {
            response.body().close();
        } catch (IOException e) {
            LOG.debug("Closing a store response failed", e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("interrupted", true);
        }
    }

    private static String basic(String user, String password) {
        String pair = (user == null ? "" : user) + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    /** The store did not answer (connection failure, timeout, open breaker): 503 (D44). */
    public static final class StoreUnavailableException extends ProblemException {
        private final boolean breakerOpen;

        StoreUnavailableException(String reason, boolean breakerOpen) {
            super(
                    503,
                    Problems.serviceUnavailable(Problems.STORE_UNAVAILABLE).type(),
                    Problems.serviceUnavailable(Problems.STORE_UNAVAILABLE).href(),
                    "Service Unavailable",
                    Problems.STORE_UNAVAILABLE,
                    null,
                    java.util.Map.of("Retry-After", "5"));
            this.breakerOpen = breakerOpen;
            LOG.warn("Store unavailable: {}", reason);
        }

        boolean breakerOpen() {
            return breakerOpen;
        }
    }

    /** The store answered with an error that is not a timeout or connection failure: 500 (D44). */
    public static final class StoreFailureException extends ProblemException {
        StoreFailureException() {
            super(
                    500,
                    Problems.internalError().type(),
                    Problems.internalError().href(),
                    "Internal Server Error",
                    "Internal Server Error",
                    null,
                    null);
        }
    }
}
