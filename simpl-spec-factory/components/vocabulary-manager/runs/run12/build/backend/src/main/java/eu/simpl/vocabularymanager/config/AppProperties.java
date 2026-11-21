package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the service; every value comes from the environment (decisions E2, E4, D83).
 *
 * @param contentBaseUrl base of contentHref values and of internal references (E2, D12)
 * @param store the RDF store (Fuseki) endpoints, users and timeouts (D44, D71, D83)
 * @param auth the identity provider (E1, E5, D44, D88)
 * @param uploads concurrency limit of uploads (D92)
 */
@ConfigurationProperties(prefix = "vm")
public record AppProperties(String contentBaseUrl, Store store, Auth auth, Uploads uploads) {

    public AppProperties {
        if (contentBaseUrl == null || contentBaseUrl.isBlank()) {
            contentBaseUrl = "https://authority.example/vocabulary-manager";
        }
        while (contentBaseUrl.endsWith("/")) {
            contentBaseUrl = contentBaseUrl.substring(0, contentBaseUrl.length() - 1);
        }
        if (uploads == null) {
            uploads = new Uploads(4);
        }
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
     * @param connectTimeout connect timeout (2 s)
     * @param readTimeout response timeout of a query (10 s)
     * @param updateTimeout response timeout of an update (45 s)
     * @param writeBudget total time a write may take, lock and retries included (30 s)
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
            Duration writeBudget) {}

    /**
     * Identity provider settings.
     *
     * @param issuer the one accepted issuer (E5)
     * @param audience the required audience (E5)
     * @param jwksUri where the realm keys are fetched
     * @param fetchTimeout timeout of a key fetch (2 s)
     * @param refetchInterval minimum time between two key fetches (10 s, D88)
     */
    public record Auth(
            String issuer, String audience, String jwksUri, Duration fetchTimeout, Duration refetchInterval) {}

    /**
     * Upload limits.
     *
     * @param maxConcurrent create or new-version requests handled at once per instance (D92)
     */
    public record Uploads(int maxConcurrent) {}
}
