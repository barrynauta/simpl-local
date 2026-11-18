package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.Problems;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The realm keys of the identity provider (decision D44): fetched at start and again when a token names an unknown key
 * id, with a 2 s timeout and no retry inside a request. Cached keys keep working while Keycloak is down.
 */
@Component
public class RealmKeys {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeys.class);
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final URI jwksUri;
    private final Duration timeout;
    private final Clock clock;
    private final Map<String, JWK> keys = new ConcurrentHashMap<>();
    private volatile Instant lastSuccessfulFetch = Instant.EPOCH;

    public RealmKeys(VocabularyManagerProperties properties, Clock clock) {
        this.jwksUri = URI.create(properties.security().jwksUrl());
        this.timeout = properties.security().keyFetchTimeout();
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Loads the keys when the application is ready; a failure only makes the health DEGRADED. */
    @EventListener(ApplicationReadyEvent.class)
    public void loadAtStart() {
        if (!refreshQuietly()) {
            LOG.warn("Realm keys could not be loaded at start; they will be fetched on first use");
        }
    }

    public boolean hasKeys() {
        return !keys.isEmpty();
    }

    /** Fetches the keys, never throws. */
    public boolean refreshQuietly() {
        try {
            refresh();
            return true;
        } catch (KeyFetchFailure e) {
            LOG.debug("Realm key fetch failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the key with this id, fetching the key set once when the id is not cached.
     *
     * @return the key, or null when the identity provider does not know it
     * @throws eu.simpl.vocabularymanager.error.VocabularyManagerException 503 when the keys cannot be fetched
     */
    public JWK key(String keyId) {
        JWK cached = keys.get(keyId);
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = keys.get(keyId);
            if (cached != null) {
                return cached;
            }
            if (Duration.between(lastSuccessfulFetch, clock.instant()).compareTo(MIN_REFRESH_INTERVAL) < 0) {
                return null;
            }
            try {
                refresh();
            } catch (KeyFetchFailure e) {
                throw Problems.identityProviderUnavailable(e);
            }
            return keys.get(keyId);
        }
    }

    private void refresh() {
        HttpRequest request = HttpRequest.newBuilder(jwksUri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new KeyFetchFailure(new IOException("Key endpoint answered " + response.statusCode()));
            }
            JWKSet set = JWKSet.parse(response.body());
            Map<String, JWK> fresh = new ConcurrentHashMap<>();
            for (JWK jwk : set.getKeys()) {
                if (jwk.getKeyID() != null && !jwk.isPrivate()) {
                    fresh.put(jwk.getKeyID(), jwk);
                }
            }
            keys.clear();
            keys.putAll(fresh);
            lastSuccessfulFetch = clock.instant();
        } catch (IOException | ParseException e) {
            throw new KeyFetchFailure(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyFetchFailure(e);
        }
    }

    /** The key set could not be fetched. */
    static final class KeyFetchFailure extends RuntimeException {
        KeyFetchFailure(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
