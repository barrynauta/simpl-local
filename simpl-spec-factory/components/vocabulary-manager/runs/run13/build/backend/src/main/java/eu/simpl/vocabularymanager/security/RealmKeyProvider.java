package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Realm keys of the identity provider, fetched with Nimbus and cached (D44, D88, D108, D129).
 *
 * <p>The first fetch is lazy, on the first token. A token whose key id is not cached causes one fetch when
 * the last fetch is older than the cool-down (10 s). Inside the window the outcome is 401 after a successful
 * fetch and 503 after a failed one. Cached keys keep working while the identity provider is down.
 */
public class RealmKeyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeyProvider.class);
    private static final int JWKS_SIZE_LIMIT = 512 * 1024;

    /** Key state for the health view of D44 and D134. */
    public enum KeyState {
        NOT_FETCHED,
        LOADED,
        FETCH_FAILED
    }

    /** No key for the token's key id: the token is invalid (401). */
    public static final class UnknownKeyException extends Exception {
        private static final long serialVersionUID = 1L;

        UnknownKeyException(String message) {
            super(message);
        }
    }

    /** The keys cannot be fetched: the request fails closed (503). */
    public static final class KeysUnavailableException extends Exception {
        private static final long serialVersionUID = 1L;

        KeysUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final URL jwksUrl;
    private final int timeoutMillis;
    private final long cooldownMillis;
    private final Clock clock;
    private final CircuitBreaker breaker;
    private final TraceHeaders traceHeaders;

    private volatile JWKSet cached;
    private volatile Instant lastFetch;
    private volatile boolean lastFetchFailed;

    public RealmKeyProvider(
            VocabularyManagerProperties.Identity identity,
            Clock clock,
            CircuitBreaker breaker,
            TraceHeaders traceHeaders) {
        try {
            this.jwksUrl = URI.create(identity.jwksUri()).toURL();
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("JWKS_URI is not a valid URL", e);
        }
        this.timeoutMillis = (int) identity.fetchTimeout().toMillis();
        this.cooldownMillis = identity.refetchCooldown().toMillis();
        this.clock = clock;
        this.breaker = breaker;
        this.traceHeaders = traceHeaders;
    }

    /**
     * Returns the key with this key id.
     *
     * @throws UnknownKeyException when no such key exists after the allowed fetch
     * @throws KeysUnavailableException when the keys cannot be fetched
     */
    public JWK keyFor(String keyId) throws UnknownKeyException, KeysUnavailableException {
        JWK key = cachedKey(keyId);
        if (key != null) {
            return key;
        }
        synchronized (this) {
            key = cachedKey(keyId);
            if (key != null) {
                return key;
            }
            Instant now = clock.instant();
            if (lastFetch != null && now.isBefore(lastFetch.plusMillis(cooldownMillis))) {
                if (lastFetchFailed) {
                    throw new KeysUnavailableException("Realm keys could not be fetched recently", null);
                }
                throw new UnknownKeyException("Unknown key id");
            }
            fetch(now);
            key = cachedKey(keyId);
            if (key == null) {
                throw new UnknownKeyException("Unknown key id");
            }
            return key;
        }
    }

    private JWK cachedKey(String keyId) {
        JWKSet set = cached;
        return set == null ? null : set.getKeyByKeyId(keyId);
    }

    private void fetch(Instant now) throws KeysUnavailableException {
        lastFetch = now;
        try {
            JWKSet fetched = breaker.executeCallable(this::load);
            cached = fetched;
            lastFetchFailed = false;
        } catch (Exception e) {
            lastFetchFailed = true;
            LOG.warn("Realm key fetch failed: {}", e.getClass().getSimpleName());
            throw new KeysUnavailableException("Realm keys could not be fetched", e);
        }
    }

    private JWKSet load() throws IOException, ParseException {
        DefaultResourceRetriever retriever =
                new DefaultResourceRetriever(timeoutMillis, timeoutMillis, JWKS_SIZE_LIMIT, false);
        retriever.setHeaders(traceHeaders.forOutgoingCall());
        Resource resource = retriever.retrieveResource(jwksUrl);
        return JWKSet.parse(resource.getContent());
    }

    /** The cached key state, without fetching (D134). */
    public KeyState state() {
        if (cached != null) {
            return KeyState.LOADED;
        }
        return lastFetchFailed ? KeyState.FETCH_FAILED : KeyState.NOT_FETCHED;
    }
}
