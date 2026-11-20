package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.store.OutboundHeaders;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The realm keys of Keycloak with the fetch rules of D44 and D88. Keys are fetched at start and again when a token
 * names an unknown key id, at most once per refetch interval, with a 2 s timeout and no retry. Cached keys keep
 * working while Keycloak is down. Inside the interval an unknown key id gives no key (401). When keys cannot be
 * fetched at all the source throws (503, fail closed). Parsing and selection reuse Nimbus JOSE.
 */
public class RealmKeySource implements JWKSource<SecurityContext> {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeySource.class);

    private final HttpClient client;
    private final URI jwkSetUri;
    private final Duration timeout;
    private final long refetchMillis;
    private final Clock clock;
    private final OutboundHeaders outboundHeaders;

    private volatile JWKSet keys = new JWKSet();
    private volatile long lastAttempt = Long.MIN_VALUE / 2;
    private volatile boolean lastAttemptFailed;

    /**
     * @param identity identity provider settings
     * @param clock time source
     * @param outboundHeaders trace headers to forward
     */
    public RealmKeySource(VocabularyManagerProperties.Identity identity, Clock clock, OutboundHeaders outboundHeaders) {
        this.jwkSetUri = URI.create(identity.jwkSetUri());
        this.timeout = identity.fetchTimeout();
        this.refetchMillis = identity.refetchInterval().toMillis();
        this.clock = clock;
        this.outboundHeaders = outboundHeaders;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public List<JWK> get(JWKSelector selector, SecurityContext context) throws KeySourceException {
        List<JWK> cached = selector.select(keys);
        if (!cached.isEmpty()) {
            return cached;
        }
        synchronized (this) {
            cached = selector.select(keys);
            if (!cached.isEmpty()) {
                return cached;
            }
            long now = clock.millis();
            if (now - lastAttempt < refetchMillis) {
                if (lastAttemptFailed) {
                    throw new KeySourceException("Realm keys cannot be fetched");
                }
                return List.of();
            }
            if (!refresh()) {
                throw new KeySourceException("Realm keys cannot be fetched");
            }
            return selector.select(keys);
        }
    }

    /**
     * Fetches the keys now, ignoring the interval.
     *
     * @return whether the fetch succeeded
     */
    public synchronized boolean refresh() {
        lastAttempt = clock.millis();
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(jwkSetUri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET();
            outboundHeaders.current().forEach(request::header);
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Key endpoint answered " + response.statusCode());
            }
            keys = JWKSet.parse(response.body());
            lastAttemptFailed = false;
            LOG.info("Realm keys loaded: {} keys", keys.getKeys().size());
            return true;
        } catch (IOException | ParseException ex) {
            lastAttemptFailed = true;
            LOG.warn("Realm keys could not be fetched: {}", ex.getMessage());
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            lastAttemptFailed = true;
            return false;
        }
    }

    /** @return whether at least one realm key is cached */
    public boolean hasKeys() {
        return !keys.getKeys().isEmpty();
    }
}
