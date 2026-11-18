package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime configuration, all from the environment (MACH-07).
 *
 * @param contentBaseUrl base of contentHref values and of internal vocabulary references (decision E2, D12)
 * @param maxUploadBytes upload limit of a vocabulary file (decision D15)
 * @param store the vocabulary store (decision E3, D44)
 * @param security the identity provider (decision E1, D44)
 */
@ConfigurationProperties(prefix = "vocabulary")
public record VocabularyManagerProperties(String contentBaseUrl, long maxUploadBytes, Store store, Security security) {

    public VocabularyManagerProperties {
        if (contentBaseUrl == null || contentBaseUrl.isBlank()) {
            contentBaseUrl = "https://authority.example/vocabulary-manager";
        }
        while (contentBaseUrl.endsWith("/")) {
            contentBaseUrl = contentBaseUrl.substring(0, contentBaseUrl.length() - 1);
        }
        if (maxUploadBytes <= 0) {
            maxUploadBytes = 5L * 1024 * 1024;
        }
        if (store == null) {
            store = new Store(null, null, null, 2, null);
        }
        if (security == null) {
            security = new Security(null, null, null, null);
        }
    }

    /**
     * Store settings.
     *
     * @param url dataset URL, for example http://fuseki:3030/vocabulary
     * @param connectTimeout connect timeout per store call
     * @param responseTimeout response timeout per store call
     * @param readRetries retries of read-only queries on connection failure or timeout
     * @param healthTimeout time the store has to answer the health check
     */
    public record Store(
            String url, Duration connectTimeout, Duration responseTimeout, int readRetries, Duration healthTimeout) {
        public Store {
            if (url == null || url.isBlank()) {
                url = "http://localhost:3030/vocabulary";
            }
            connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
            responseTimeout = responseTimeout == null ? Duration.ofSeconds(10) : responseTimeout;
            healthTimeout = healthTimeout == null ? Duration.ofSeconds(2) : healthTimeout;
            if (readRetries < 0) {
                readRetries = 2;
            }
        }
    }

    /**
     * Identity provider settings.
     *
     * @param jwksUrl where the realm keys are fetched
     * @param issuer expected iss claim
     * @param audience expected aud value, empty to skip the check
     * @param keyFetchTimeout timeout of one key fetch
     */
    public record Security(String jwksUrl, String issuer, String audience, Duration keyFetchTimeout) {
        public Security {
            if (jwksUrl == null || jwksUrl.isBlank()) {
                jwksUrl = "http://localhost:8180/realms/simpl/protocol/openid-connect/certs";
            }
            if (issuer == null || issuer.isBlank()) {
                issuer = "http://localhost:8180/realms/simpl";
            }
            if (audience == null) {
                audience = "vocabulary-manager-be";
            }
            keyFetchTimeout = keyFetchTimeout == null ? Duration.ofSeconds(2) : keyFetchTimeout;
        }
    }
}
