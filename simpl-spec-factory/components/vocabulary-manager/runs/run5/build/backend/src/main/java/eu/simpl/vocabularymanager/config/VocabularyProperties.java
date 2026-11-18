package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the Vocabulary Manager, read from the environment (decision E4, rule MACH-07).
 *
 * @param contentBaseUrl base of contentHref values and of internal vocabulary references (E2, D12)
 * @param uploadLimitBytes largest accepted vocabulary file (D15)
 * @param lockTimeout longest wait for the per-vocabulary lock (D43)
 * @param store store settings (D44)
 * @param identity identity provider settings (D17, D44)
 */
@ConfigurationProperties(prefix = "vocabulary")
public record VocabularyProperties(
        String contentBaseUrl, long uploadLimitBytes, Duration lockTimeout, Store store, Identity identity) {

    /** Fuseki store settings. */
    public record Store(String url, Duration connectTimeout, Duration responseTimeout, Duration healthTimeout) {}

    /** Keycloak settings. */
    public record Identity(
            String issuer,
            String jwksUrl,
            String audience,
            Duration fetchTimeout,
            Duration minRefetchInterval,
            Duration clockSkew) {}

    /** Content base without a trailing slash. */
    public String contentBase() {
        return contentBaseUrl.endsWith("/") ? contentBaseUrl.substring(0, contentBaseUrl.length() - 1) : contentBaseUrl;
    }
}
