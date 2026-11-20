package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.web.OutboundHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Realm keys of the identity provider (D44, D88, D108). Keys are parsed with Nimbus JWKSet; the fetch timing is our
 * own so the decided cooldown holds: a token with an unknown key id causes at most one fetch per cooldown window.
 * Cached keys keep working while the identity provider is down.
 */
public class RealmKeyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeyProvider.class);

    private final AppProperties.Security settings;
    private final HttpClient httpClient;
    private final CircuitBreaker breaker;
    private final OutboundHeaders outboundHeaders;
    private final Clock clock;

    private volatile JWKSet keys = new JWKSet();
    private volatile Instant lastFetch;
    private volatile boolean lastFetchFailed;

    public RealmKeyProvider(
            AppProperties.Security settings,
            HttpClient httpClient,
            CircuitBreaker breaker,
            OutboundHeaders outboundHeaders,
            Clock clock) {
        this.settings = settings;
        this.httpClient = httpClient;
        this.breaker = breaker;
        this.outboundHeaders = outboundHeaders;
        this.clock = clock;
    }

    /**
     * Returns the key with the given id.
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 401 when the key id stays unknown, 503 when the keys
     *     cannot be fetched
     */
    public JWK keyFor(String keyId) {
        JWK key = keys.getKeyByKeyId(keyId);
        if (key != null) {
            return key;
        }
        synchronized (this) {
            key = keys.getKeyByKeyId(keyId);
            if (key != null) {
                return key;
            }
            Instant now = clock.instant();
            boolean inCooldown = lastFetch != null && now.isBefore(lastFetch.plus(settings.keyRefetchCooldown()));
            if (inCooldown) {
                if (lastFetchFailed) {
                    throw Problems.serviceUnavailable(Problems.IDP_UNAVAILABLE);
                }
                throw Problems.unauthorized("The access token is not valid.");
            }
            if (!refresh()) {
                throw Problems.serviceUnavailable(Problems.IDP_UNAVAILABLE);
            }
            key = keys.getKeyByKeyId(keyId);
            if (key == null) {
                throw Problems.unauthorized("The access token is not valid.");
            }
            return key;
        }
    }

    /** Fetches the realm keys once; returns false when they could not be fetched. */
    public synchronized boolean refresh() {
        lastFetch = clock.instant();
        Supplier<JWKSet> fetch = CircuitBreaker.decorateSupplier(breaker, this::fetch);
        try {
            keys = fetch.get();
            lastFetchFailed = false;
            return true;
        } catch (CallNotPermittedException e) {
            LOG.warn("Realm key fetch skipped: circuit breaker is open");
        } catch (KeyFetchException e) {
            LOG.warn("Realm key fetch failed: {}", e.getMessage());
        }
        lastFetchFailed = true;
        return false;
    }

    /** True when at least one realm key is cached. */
    public boolean hasKeys() {
        return !keys.getKeys().isEmpty();
    }

    /** True when the last fetch failed (D108: health is DEGRADED only after a failed fetch). */
    public boolean lastFetchFailed() {
        return lastFetchFailed;
    }

    private JWKSet fetch() {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(settings.jwksUri()))
                    .timeout(settings.keyFetchTimeout())
                    .header("Accept", "application/json")
                    .GET();
            outboundHeaders.current().forEach(request::header);
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new KeyFetchException("status " + response.statusCode());
            }
            return JWKSet.parse(response.body());
        } catch (IOException | java.text.ParseException e) {
            throw new KeyFetchException(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyFetchException("interrupted");
        }
    }

    /** Creates the HTTP client used for the key fetch, with the decided connect timeout. */
    public static HttpClient httpClient(Duration timeout) {
        return HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** The key fetch failed (connection, timeout, bad status or bad document). */
    static final class KeyFetchException extends RuntimeException {
        KeyFetchException(String message) {
            super(message);
        }
    }
}
