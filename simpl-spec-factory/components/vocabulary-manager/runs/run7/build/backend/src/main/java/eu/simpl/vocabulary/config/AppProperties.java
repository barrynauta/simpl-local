package eu.simpl.vocabulary.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the component, all from the environment (MACH-07, E2, E4).
 *
 * @param contentBaseUrl base of contentHref values and of internal references (D12)
 * @param store store settings
 * @param security token settings
 * @param limits component limits (Q2)
 */
@ConfigurationProperties(prefix = "vocabulary")
public record AppProperties(String contentBaseUrl, Store store, Security security, Limits limits) {

    public AppProperties {
        if (contentBaseUrl == null || contentBaseUrl.isBlank()) {
            contentBaseUrl = "https://authority.example/vocabulary-manager";
        }
        while (contentBaseUrl.endsWith("/")) {
            contentBaseUrl = contentBaseUrl.substring(0, contentBaseUrl.length() - 1);
        }
        store = store == null ? new Store(null, null, null, null, null, null) : store;
        security = security == null ? new Security(null, null, null, null) : security;
        limits = limits == null ? new Limits(0, 0, 0, null) : limits;
    }

    /**
     * Store (Fuseki) settings (D44, D71).
     *
     * @param url dataset URL, for example http://fuseki:3030/vocabulary
     * @param connectTimeout connect timeout
     * @param readTimeout response timeout of reads
     * @param writeTimeout response timeout of updates
     * @param healthTimeout timeout of the health probe
     * @param retryDelays delays before the read retries
     */
    public record Store(
            String url,
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout,
            Duration healthTimeout,
            java.util.List<Duration> retryDelays) {

        public Store {
            url = url == null || url.isBlank() ? "http://localhost:3030/vocabulary" : url;
            connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
            readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
            writeTimeout = writeTimeout == null ? Duration.ofSeconds(45) : writeTimeout;
            healthTimeout = healthTimeout == null ? Duration.ofSeconds(2) : healthTimeout;
            retryDelays = retryDelays == null
                    ? java.util.List.of(Duration.ofMillis(200), Duration.ofMillis(400))
                    : java.util.List.copyOf(retryDelays);
        }
    }

    /**
     * Token validation settings (E1, E5, D44).
     *
     * @param issuerUri the one accepted issuer
     * @param jwksUri where the realm keys are fetched
     * @param audience the required audience
     * @param keyFetchTimeout timeout of a key fetch
     */
    public record Security(String issuerUri, String jwksUri, String audience, Duration keyFetchTimeout) {

        public Security {
            issuerUri = issuerUri == null || issuerUri.isBlank() ? "http://localhost:8180/realms/simpl" : issuerUri;
            jwksUri = jwksUri == null || jwksUri.isBlank() ? issuerUri + "/protocol/openid-connect/certs" : jwksUri;
            audience = audience == null || audience.isBlank() ? "vocabulary-manager-be" : audience;
            keyFetchTimeout = keyFetchTimeout == null ? Duration.ofSeconds(2) : keyFetchTimeout;
        }
    }

    /**
     * Limits (D15, D43, D71, D76).
     *
     * @param maxFileBytes limit of the vocabularyFile part
     * @param maxRequestBytes limit of a whole multipart request
     * @param maxJsonBytes limit of a JSON request body
     * @param lockTimeout maximum wait for the per-vocabulary lock
     */
    public record Limits(long maxFileBytes, long maxRequestBytes, long maxJsonBytes, Duration lockTimeout) {

        public Limits {
            maxFileBytes = maxFileBytes <= 0 ? 5L * 1024 * 1024 : maxFileBytes;
            maxRequestBytes = maxRequestBytes <= 0 ? 6L * 1024 * 1024 : maxRequestBytes;
            maxJsonBytes = maxJsonBytes <= 0 ? 1024L * 1024 : maxJsonBytes;
            lockTimeout = lockTimeout == null ? Duration.ofSeconds(30) : lockTimeout;
        }
    }
}
