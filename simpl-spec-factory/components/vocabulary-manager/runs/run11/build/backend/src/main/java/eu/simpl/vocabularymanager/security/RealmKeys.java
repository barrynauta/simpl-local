package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.config.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The realm keys of the identity provider (E1), parsed with Nimbus JWKSet (D108). Own fetch timing
 * only (D88, D129, D132): fetched lazily on the first token, again when a token names an unknown
 * key id and the last fetch is older than the refetch interval (10 s). Timeout 2 s, no retry inside
 * a request (D44), behind a circuit breaker (D103). Cached keys keep working while the identity
 * provider is down.
 */
@Component
public class RealmKeys {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeys.class);

    /** Outcome of a key lookup. */
    public sealed interface Lookup permits Found, Unknown, Unavailable {}

    /** The key is known. */
    public record Found(JWK key) implements Lookup {}

    /** The key id is not in the realm keys (401, D88). */
    public record Unknown() implements Lookup {}

    /** The keys could not be fetched (503, D44, D129). */
    public record Unavailable() implements Lookup {}

    private final HttpClient http;
    private final CircuitBreaker breaker;
    private final URI jwksUri;
    private final Duration timeout;
    private final Duration refetchInterval;
    private final Clock clock;
    private final TraceHeaders traceHeaders;

    private volatile JWKSet keys;
    private volatile Instant lastFetch;
    private volatile boolean lastFetchFailed;

    public RealmKeys(
            AppProperties properties,
            HttpClient identityHttpClient,
            CircuitBreaker identityCircuitBreaker,
            Clock clock,
            TraceHeaders traceHeaders) {
        this.http = identityHttpClient;
        this.breaker = identityCircuitBreaker;
        this.jwksUri = URI.create(properties.security().jwksUri());
        this.timeout = properties.security().jwksTimeout();
        this.refetchInterval = properties.security().jwksRefetchInterval();
        this.clock = clock;
        this.traceHeaders = traceHeaders;
    }

    /** Finds the key for a key id, fetching the realm keys when D88 allows it. */
    public Lookup lookup(String keyId) {
        Optional<JWK> cached = cached(keyId);
        if (cached.isPresent()) {
            return new Found(cached.get());
        }
        synchronized (this) {
            cached = cached(keyId);
            if (cached.isPresent()) {
                return new Found(cached.get());
            }
            Instant now = clock.instant();
            boolean mayFetch = lastFetch == null || !now.isBefore(lastFetch.plus(refetchInterval));
            if (mayFetch) {
                fetch(now);
            }
            if (lastFetchFailed) {
                return new Unavailable();
            }
            return cached(keyId).<Lookup>map(Found::new).orElseGet(Unknown::new);
        }
    }

    /** True when realm keys are cached (for GET /health, D108, D134). */
    public boolean loaded() {
        return keys != null;
    }

    /** True when the last fetch failed (for GET /health: DEGRADED, D108). */
    public boolean lastFetchFailed() {
        return lastFetchFailed;
    }

    private Optional<JWK> cached(String keyId) {
        JWKSet current = keys;
        if (current == null || keyId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(current.getKeyByKeyId(keyId));
    }

    private void fetch(Instant now) {
        lastFetch = now;
        try {
            keys = breaker.executeCallable(this::download);
            lastFetchFailed = false;
            LOG.info("Realm keys fetched: {} keys", keys.getKeys().size());
        } catch (CallNotPermittedException ex) {
            lastFetchFailed = true;
            LOG.warn("Realm key fetch skipped: identity provider circuit open");
        } catch (Exception ex) {
            lastFetchFailed = true;
            LOG.warn("Realm key fetch failed: {}", ex.toString());
        }
    }

    private JWKSet download() throws IOException, InterruptedException, ParseException {
        HttpRequest request = traceHeaders
                .apply(HttpRequest.newBuilder(jwksUri)
                        .timeout(timeout)
                        .header("Accept", "application/json")
                        .GET())
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("realm keys answered " + response.statusCode());
        }
        return JWKSet.parse(response.body());
    }
}
