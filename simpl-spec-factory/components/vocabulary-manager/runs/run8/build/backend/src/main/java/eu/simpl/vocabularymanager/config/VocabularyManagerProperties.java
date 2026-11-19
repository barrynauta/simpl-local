package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the component, read from the environment (MACH-07, E4). Every value has a default for local runs;
 * the deployment sets the real ones.
 *
 * @param contentBaseUrl contentBase of E2, used for contentHref and for recognising internal references (D12)
 * @param store the RDF store (E3, D44, D83)
 * @param identity the identity provider (E1, E5, D44)
 * @param writes the write budget (D71, D82)
 */
@ConfigurationProperties(prefix = "vocabulary-manager")
public record VocabularyManagerProperties(String contentBaseUrl, Store store, Identity identity, Writes writes) {

    /**
     * @param url base URL of the dataset, for example http://fuseki:3030/vocabulary
     * @param readUser user for queries
     * @param readPassword password of the read user
     * @param updateUser user for updates
     * @param updatePassword password of the update user
     * @param connectTimeout connect timeout per call
     * @param readTimeout response timeout of a query
     * @param updateTimeout response timeout of an update
     * @param healthTimeout timeout of the health probe
     */
    public record Store(
            String url,
            String readUser,
            String readPassword,
            String updateUser,
            String updatePassword,
            Duration connectTimeout,
            Duration readTimeout,
            Duration updateTimeout,
            Duration healthTimeout) {}

    /**
     * @param issuerUri the one accepted issuer (E5)
     * @param jwksUri where the realm keys are fetched
     * @param audience the audience tokens must carry (E5)
     * @param fetchTimeout timeout of the key fetch (D44)
     * @param refreshCooldown minimum time between two key fetches after a success
     */
    public record Identity(
            String issuerUri, String jwksUri, String audience, Duration fetchTimeout, Duration refreshCooldown) {}

    /**
     * @param budget total time a write may spend on conditional retries (D71)
     * @param maxRetries number of retries after a failed store condition (D82)
     */
    public record Writes(Duration budget, int maxRetries) {}

    /**
     * @return contentBase without a trailing slash
     */
    public String contentBase() {
        return contentBaseUrl.endsWith("/") ? contentBaseUrl.substring(0, contentBaseUrl.length() - 1) : contentBaseUrl;
    }
}
