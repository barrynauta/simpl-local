package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cache of the realm signing keys with the fetch timing of D44, D88, D108, D129 and D174. Nimbus parses the key
 * set; this class only decides when to fetch: lazily on the first token, again when a token names an unknown key
 * id but at most once per 10 seconds, one fetch at a time with waiting requests following its outcome.
 */
@Component
public class RealmKeyCache {

    /** Outcome of a key lookup. */
    public enum Lookup {
        FOUND,
        NOT_FOUND,
        UNAVAILABLE
    }

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeyCache.class);
    private static final Duration REFETCH_INTERVAL = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final URI jwksUri;
    private final Duration timeout;
    private final CircuitBreaker breaker;
    private final Clock clock;
    private final ReentrantLock fetchLock = new ReentrantLock();

    private volatile JWKSet keys = new JWKSet();
    private volatile Instant lastFetch;
    private volatile boolean lastFetchFailed;

    public RealmKeyCache(AppProperties properties, CircuitBreakerRegistry breakers, Clock clock) {
        this.timeout = Duration.ofMillis(properties.identity().keyFetchTimeoutMs());
        this.jwksUri = URI.create(properties.identity().jwksUrl());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.breaker = breakers.circuitBreaker("identity-provider");
        this.clock = clock;
    }

    /** The keys currently cached. */
    public JWKSet keys() {
        return keys;
    }

    /** True when at least one key is cached. */
    public boolean hasKeys() {
        return !keys.getKeys().isEmpty();
    }

    /** True when the last fetch failed (D108: health is DEGRADED only after a failed fetch). */
    public boolean lastFetchFailed() {
        return lastFetchFailed;
    }

    /**
     * Makes sure the key with the id is cached, fetching the key set when the rules allow it.
     *
     * @param keyId the key id of the token, or null when the token names none
     * @return the outcome
     */
    public Lookup lookup(String keyId) {
        if (isCached(keyId)) {
            return Lookup.FOUND;
        }
        boolean locked = false;
        try {
            locked = fetchLock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!locked) {
                return Lookup.UNAVAILABLE;
            }
            if (isCached(keyId)) {
                return Lookup.FOUND;
            }
            Instant now = clock.instant();
            if (lastFetch != null && now.isBefore(lastFetch.plus(REFETCH_INTERVAL))) {
                return lastFetchFailed ? Lookup.UNAVAILABLE : Lookup.NOT_FOUND;
            }
            return fetch(keyId, now);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Lookup.UNAVAILABLE;
        } finally {
            if (locked) {
                fetchLock.unlock();
            }
        }
    }

    private Lookup fetch(String keyId, Instant now) {
        lastFetch = now;
        try {
            JWKSet fetched = breaker.executeCheckedSupplier(this::download);
            keys = fetched;
            lastFetchFailed = false;
            LOG.info("Realm keys fetched, {} key(s) cached", fetched.getKeys().size());
            return isCached(keyId) ? Lookup.FOUND : Lookup.NOT_FOUND;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            lastFetchFailed = true;
            return Lookup.UNAVAILABLE;
        } catch (Throwable e) {
            lastFetchFailed = true;
            LOG.warn("Realm keys could not be fetched: {}", e.getClass().getSimpleName());
            return Lookup.UNAVAILABLE;
        }
    }

    private JWKSet download() throws IOException, InterruptedException, ParseException {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(jwksUri).timeout(timeout).GET();
        TraceHeaders.current().forEach(request::header);
        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Key endpoint answered " + response.statusCode());
        }
        return JWKSet.parse(response.body());
    }

    private boolean isCached(String keyId) {
        JWKSet current = keys;
        if (current.getKeys().isEmpty()) {
            return false;
        }
        if (keyId == null) {
            return true;
        }
        JWK key = current.getKeyByKeyId(keyId);
        return key != null;
    }
}
