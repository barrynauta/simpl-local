package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import eu.simpl.vocabularymanager.config.AppProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Source of the realm signing keys for the Nimbus JWT processor, with the behaviour of decision D44: keys are
 * fetched at start and again when a token names an unknown key id (timeout 2 s, no retry inside a request);
 * cached keys keep working while Keycloak is down; an unknown key id that cannot be fetched fails closed with
 * {@link IdentityProviderUnavailableException}. Signature checking itself stays in Nimbus (REUSE-01).
 */
public class RealmKeySource implements JWKSource<SecurityContext> {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeySource.class);

    private final HttpClient httpClient;
    private final URI jwkSetUri;
    private final Duration fetchTimeout;
    private final Duration cooldown;
    private final Clock clock;

    private volatile JWKSet keys = new JWKSet();
    private volatile Instant lastAttempt = Instant.EPOCH;
    private volatile boolean lastAttemptFailed;

    /** The keys are needed and the identity provider does not answer. */
    public static final class IdentityProviderUnavailableException extends KeySourceException {

        private static final long serialVersionUID = 1L;

        /**
         * Creates the exception.
         *
         * @param message what failed
         */
        public IdentityProviderUnavailableException(String message) {
            super(message);
        }
    }

    /**
     * Creates the key source.
     *
     * @param identity the identity provider settings
     * @param clock the clock
     */
    public RealmKeySource(AppProperties.Identity identity, Clock clock) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(identity.fetchTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.jwkSetUri = URI.create(identity.jwkSetUri());
        this.fetchTimeout = identity.fetchTimeout();
        this.cooldown = identity.refreshCooldown();
        this.clock = clock;
    }

    @Override
    public List<JWK> get(JWKSelector selector, SecurityContext context) throws KeySourceException {
        List<JWK> matches = selector.select(keys);
        if (!matches.isEmpty()) {
            return matches;
        }
        boolean inCooldown = clock.instant().isBefore(lastAttempt.plus(cooldown));
        if (!inCooldown) {
            refresh();
        }
        if (lastAttemptFailed) {
            throw new IdentityProviderUnavailableException("Realm keys could not be fetched");
        }
        return selector.select(keys);
    }

    /**
     * Fetches the realm keys now. Failures are logged and remembered, never thrown.
     *
     * @return true when the keys were fetched
     */
    public synchronized boolean refresh() {
        lastAttempt = clock.instant();
        try {
            HttpRequest request = HttpRequest.newBuilder(jwkSetUri)
                    .timeout(fetchTimeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warn("Realm key fetch answered HTTP {}", response.statusCode());
                lastAttemptFailed = true;
                return false;
            }
            keys = JWKSet.parse(response.body());
            lastAttemptFailed = false;
            LOG.info("Realm keys fetched: {} key(s)", keys.getKeys().size());
            return true;
        } catch (IOException | ParseException e) {
            LOG.warn("Realm key fetch failed: {}", e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Realm key fetch interrupted");
        }
        lastAttemptFailed = true;
        return false;
    }

    /**
     * Tells whether realm keys are cached.
     *
     * @return true when at least one key is loaded
     */
    public boolean hasKeys() {
        return !keys.getKeys().isEmpty();
    }
}
