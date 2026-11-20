package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the component. Every value comes from the environment (MACH-07, E4).
 *
 * @param contentBaseUrl base of contentHref and of internal references (E2, D12)
 * @param store connection to the RDF store (E3, D44, D83)
 * @param identity identity provider settings (E1, E5, D44, D88)
 * @param limits component limits (Q2)
 */
@ConfigurationProperties(prefix = "vm")
public record VocabularyManagerProperties(String contentBaseUrl, Store store, Identity identity, Limits limits) {

    /** Strips a trailing slash so URLs can be joined safely. */
    public String contentBase() {
        return contentBaseUrl.endsWith("/") ? contentBaseUrl.substring(0, contentBaseUrl.length() - 1) : contentBaseUrl;
    }

    /**
     * Store settings.
     *
     * @param queryUrl SPARQL query endpoint
     * @param updateUrl SPARQL update endpoint
     * @param readUser user for queries
     * @param readPassword password of the read user
     * @param updateUser user for updates
     * @param updatePassword password of the update user
     * @param connectTimeout connect timeout (D44: 2 s)
     * @param readTimeout response timeout of reads (D44: 10 s)
     * @param updateTimeout response timeout of updates (D71: 45 s)
     * @param healthTimeout timeout of the health probe (D44: 2 s)
     * @param writeBudget total time budget of a conditional write with its retries (D71: 30 s)
     */
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
            Duration healthTimeout,
            Duration writeBudget) {}

    /**
     * Identity provider settings.
     *
     * @param issuerUri the one accepted issuer (E5)
     * @param jwkSetUri where the realm keys are fetched
     * @param audience required audience (E5)
     * @param fetchTimeout key fetch timeout (D44: 2 s)
     * @param refetchInterval minimum time between two key fetches (D88: 10 s)
     */
    public record Identity(
            String issuerUri, String jwkSetUri, String audience, Duration fetchTimeout, Duration refetchInterval) {}

    /**
     * Limits.
     *
     * @param maxFileBytes upload limit of the vocabularyFile part (D15: 5 242 880)
     * @param maxRequestBytes limit of the whole multipart request (D76: 6 MB)
     * @param maxJsonBytes limit of a JSON request body
     * @param maxConcurrentUploads parallel creates and new versions per instance (D92: 4)
     * @param uploadPermitWait how long an upload waits for a free slot before 503
     */
    public record Limits(
            long maxFileBytes,
            long maxRequestBytes,
            int maxJsonBytes,
            int maxConcurrentUploads,
            Duration uploadPermitWait) {}
}
