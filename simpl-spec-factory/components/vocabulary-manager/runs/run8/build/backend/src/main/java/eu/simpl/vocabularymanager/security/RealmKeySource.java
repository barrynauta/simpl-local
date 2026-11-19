package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.web.trace.TraceContext;
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
 * The realm keys of the identity provider (D44). Fetched at start and again when a token names an unknown key id, with
 * a 2 s timeout and no retry inside a request. Cached keys keep working while the identity provider is down. When a key
 * id is unknown and the keys cannot be fetched, {@link IdentityProviderUnavailableException} is thrown: the request
 * fails closed.
 */
public class RealmKeySource implements JWKSource<SecurityContext> {

    private static final Logger LOG = LoggerFactory.getLogger(RealmKeySource.class);
    private static final int MAX_JWKS_BYTES = 1_000_000;

    private final HttpClient httpClient;
    private final URI jwksUri;
    private final Duration fetchTimeout;
    private final Duration refreshCooldown;
    private final Clock clock;

    private volatile JWKSet keys = new JWKSet();
    private volatile Instant lastSuccessfulFetch;

    /**
     * @param httpClient client for the key fetch
     * @param identity identity provider settings
     * @param clock clock for the refresh cooldown
     */
    public RealmKeySource(HttpClient httpClient, VocabularyManagerProperties.Identity identity, Clock clock) {
        this.httpClient = httpClient;
        this.jwksUri = URI.create(identity.jwksUri());
        this.fetchTimeout = identity.fetchTimeout();
        this.refreshCooldown = identity.refreshCooldown();
        this.clock = clock;
    }

    /** Fetches the keys at start; a failure is logged and retried on the first token. */
    public void loadAtStart() {
        try {
            refresh();
        } catch (IdentityProviderUnavailableException e) {
            LOG.warn("Realm keys could not be loaded at start; they are fetched on the first token", e);
        }
    }

    /**
     * @return true when at least one key is cached
     */
    public boolean hasKeys() {
        return !keys.getKeys().isEmpty();
    }

    @Override
    public List<JWK> get(JWKSelector selector, SecurityContext context) throws KeySourceException {
        List<JWK> matches = selector.select(keys);
        if (!matches.isEmpty()) {
            return matches;
        }
        Instant last = lastSuccessfulFetch;
        if (last != null && last.plus(refreshCooldown).isAfter(clock.instant())) {
            return List.of();
        }
        refresh();
        return selector.select(keys);
    }

    /**
     * Fetches the key set once.
     *
     * @throws IdentityProviderUnavailableException when the identity provider does not answer in time or answers
     *     with something that is not a key set
     */
    public void refresh() throws IdentityProviderUnavailableException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(jwksUri)
                .timeout(fetchTimeout)
                .header("Accept", "application/json")
                .GET();
        TraceContext.outgoingHeaders().forEach(builder::header);
        try {
            HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length > MAX_JWKS_BYTES) {
                throw new IdentityProviderUnavailableException(
                        "Key set request answered with status " + response.statusCode(), null);
            }
            keys = JWKSet.parse(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
            lastSuccessfulFetch = clock.instant();
        } catch (IOException | ParseException e) {
            throw new IdentityProviderUnavailableException("Key set could not be fetched", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdentityProviderUnavailableException("Key set fetch interrupted", e);
        }
    }
}
