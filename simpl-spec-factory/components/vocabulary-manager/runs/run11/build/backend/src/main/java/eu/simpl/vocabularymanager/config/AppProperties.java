package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime configuration, all from the environment (MACH-07, E2, E4).
 *
 * @param contentBase base URL of vocabulary content (VOCABULARY_CONTENT_BASE_URL, E2)
 * @param security token validation settings (E1, E5, D44, D88)
 * @param store RDF store settings (E3, D44, D71, D83)
 * @param limits component limits (D15, D71, D76, D92)
 */
@ConfigurationProperties(prefix = "vm")
public record AppProperties(String contentBase, Security security, Store store, Limits limits) {

    /** Token settings. */
    public record Security(
            String issuerUri, String audience, String jwksUri, Duration jwksTimeout, Duration jwksRefetchInterval) {}

    /** Store settings. */
    public record Store(
            String url,
            String readUser,
            String readPassword,
            String updateUser,
            String updatePassword,
            Duration connectTimeout,
            Duration readTimeout,
            Duration updateTimeout,
            Duration healthTimeout,
            int conditionalRetries) {}

    /** Limits. */
    public record Limits(
            long maxFileBytes,
            long maxMultipartBytes,
            long maxJsonBytes,
            int maxParallelUploads,
            Duration writeBudget,
            int maxIssues) {}

    /** The content base without a trailing slash. */
    public String contentBaseTrimmed() {
        return contentBase.endsWith("/") ? contentBase.substring(0, contentBase.length() - 1) : contentBase;
    }
}
