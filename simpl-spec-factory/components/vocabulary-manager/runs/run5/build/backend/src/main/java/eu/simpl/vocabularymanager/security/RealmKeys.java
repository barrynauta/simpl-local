package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import eu.simpl.vocabularymanager.config.VocabularyProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cache of the realm signing keys (D44). Keys are fetched at start and again when a token names an unknown key id.
 * Cached keys keep working while the identity provider is down. A refetch after a successful fetch happens at most
 * once per configured interval, so random key ids cannot flood the identity provider.
 */
@Component
public class RealmKeys {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeys.class);

    private final KeySource source;
    private final Clock clock;
    private final Duration minRefetchInterval;
    private final Map<String, JWK> keys = new ConcurrentHashMap<>();
    private volatile Instant lastSuccessfulFetch = Instant.EPOCH;

    public RealmKeys(KeySource source, Clock clock, VocabularyProperties properties) {
        this.source = source;
        this.clock = clock;
        this.minRefetchInterval = properties.identity().minRefetchInterval();
    }

    /** Loads the keys at start; a failure is logged and retried on the first token. */
    @EventListener(ApplicationReadyEvent.class)
    public void loadAtStart() {
        try {
            refresh();
        } catch (IdentityProviderUnavailableException e) {
            LOG.warn("Realm keys could not be loaded at start; they will be fetched on the first token", e);
        }
    }

    /** True when at least one key is cached. */
    public boolean hasKeys() {
        return !keys.isEmpty();
    }

    /**
     * Finds the key for a key id, fetching the key set when the id is not cached.
     *
     * @param keyId key id from the token header
     * @return the key, or empty when the identity provider does not know it
     * @throws IdentityProviderUnavailableException when the key is unknown and the keys cannot be fetched
     */
    public Optional<JWK> find(String keyId) {
        if (keyId == null) {
            return Optional.empty();
        }
        JWK cached = keys.get(keyId);
        if (cached != null) {
            return Optional.of(cached);
        }
        synchronized (this) {
            cached = keys.get(keyId);
            if (cached != null) {
                return Optional.of(cached);
            }
            boolean recentlyFetched = hasKeys()
                    && Duration.between(lastSuccessfulFetch, clock.instant()).compareTo(minRefetchInterval) < 0;
            if (!recentlyFetched) {
                refresh();
            }
            return Optional.ofNullable(keys.get(keyId));
        }
    }

    /**
     * Fetches the key set and replaces the cache.
     *
     * @throws IdentityProviderUnavailableException when the identity provider does not answer
     */
    public synchronized void refresh() {
        JWKSet set = source.fetch();
        Map<String, JWK> fresh = new ConcurrentHashMap<>();
        for (JWK key : set.getKeys()) {
            if (key.getKeyID() != null) {
                fresh.put(key.getKeyID(), key);
            }
        }
        keys.clear();
        keys.putAll(fresh);
        lastSuccessfulFetch = clock.instant();
        LOG.info("Loaded {} realm key(s)", fresh.size());
    }
}
