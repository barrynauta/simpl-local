package eu.simpl.vocabularymanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the component, all from the environment (MACH-07, E4).
 *
 * @param contentBaseUrl contentBase of E2: base of contentHref and of internal references (D12)
 * @param uploadSlots number of create or new-version requests one instance handles at once (D92)
 * @param store the RDF store
 * @param identity the identity provider
 */
@ConfigurationProperties(prefix = "vocabulary")
public record AppProperties(String contentBaseUrl, int uploadSlots, Store store, Identity identity) {

    /**
     * The RDF store (E3, D83).
     *
     * @param url base URL of Fuseki
     * @param dataset dataset name
     * @param readUser user for queries
     * @param readPassword its password
     * @param updateUser user for updates
     * @param updatePassword its password
     * @param connectTimeoutMs connect timeout (D44)
     * @param readTimeoutMs response timeout of queries (D44)
     * @param updateTimeoutMs response timeout of updates (D71)
     */
    public record Store(
            String url,
            String dataset,
            String readUser,
            String readPassword,
            String updateUser,
            String updatePassword,
            long connectTimeoutMs,
            long readTimeoutMs,
            long updateTimeoutMs) {}

    /**
     * The identity provider (E1, E5).
     *
     * @param issuer the one accepted issuer
     * @param audience the required audience
     * @param jwksUrl URL of the realm keys
     * @param keyFetchTimeoutMs timeout of one key fetch (D44)
     */
    public record Identity(String issuer, String audience, String jwksUrl, long keyFetchTimeoutMs) {}
}
