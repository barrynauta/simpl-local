package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the component, read from environment variables (MACH-07, NFR-11, decision E4).
 *
 * @param contentBaseUrl base of contentHref values and of internal references (decision E2)
 * @param store the store settings
 * @param identity the identity provider settings
 * @param limits the request limits
 */
@ConfigurationProperties(prefix = "vm")
public record AppProperties(String contentBaseUrl, Store store, Identity identity, Limits limits) {

    /**
     * Store settings (decision D44).
     *
     * @param datasetUrl URL of the Fuseki dataset, for example http://fuseki:3030/vocabulary
     * @param connectTimeout connect timeout per store call
     * @param responseTimeout response timeout per store call
     * @param healthTimeout timeout of the health probe
     * @param lockTimeout longest wait for the per-vocabulary lock (decision D43)
     */
    public record Store(
            String datasetUrl,
            Duration connectTimeout,
            Duration responseTimeout,
            Duration healthTimeout,
            Duration lockTimeout) {}

    /**
     * Identity provider settings (decisions E1, D17, D44).
     *
     * @param issuer expected token issuer
     * @param audience expected token audience
     * @param jwkSetUri where the realm keys are fetched
     * @param fetchTimeout timeout of a key fetch
     * @param refreshCooldown shortest time between two key fetches
     */
    public record Identity(
            String issuer, String audience, String jwkSetUri, Duration fetchTimeout, Duration refreshCooldown) {}

    /**
     * Limits (NFR-04, decisions D15, D22, D38, D56).
     *
     * @param maxUploadBytes largest uploaded file
     * @param maxJsonBodyBytes largest JSON request body
     * @param maxValidationIssues most validation issues returned in one report
     */
    public record Limits(long maxUploadBytes, int maxJsonBodyBytes, int maxValidationIssues) {}
}
