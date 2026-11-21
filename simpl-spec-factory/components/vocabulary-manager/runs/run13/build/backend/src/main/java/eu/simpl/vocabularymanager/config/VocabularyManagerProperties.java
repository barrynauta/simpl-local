package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime configuration, all from environment variables (MACH-07, E2, E4). Defaults are the local values
 * of decision E2; the build profile overrides them.
 */
@ConfigurationProperties(prefix = "vm")
public record VocabularyManagerProperties(String contentBaseUrl, Store store, Identity identity, Limits limits) {

    /** RDF store access (E3, D44, D71, D83). */
    public record Store(
            String queryUrl,
            String updateUrl,
            String readUser,
            String readPassword,
            String updateUser,
            String updatePassword,
            Duration connectTimeout,
            Duration readTimeout,
            Duration updateTimeout,
            Duration healthTimeout) {}

    /** Token checks (E1, E5, D44, D88, D108). */
    public record Identity(
            String issuer, String audience, String jwksUri, Duration fetchTimeout, Duration refetchCooldown) {}

    /** Limits of Q2 that are configuration rather than contract. */
    public record Limits(int maxParallelUploads, int maxWriteAttempts, Duration writeBudget) {}

    /** The content base URL without a trailing slash. */
    public String contentBase() {
        return contentBaseUrl.endsWith("/") ? contentBaseUrl.substring(0, contentBaseUrl.length() - 1) : contentBaseUrl;
    }
}
