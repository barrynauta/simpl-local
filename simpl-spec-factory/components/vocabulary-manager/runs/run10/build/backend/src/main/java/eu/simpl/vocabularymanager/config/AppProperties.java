package eu.simpl.vocabularymanager.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the component. Every value comes from the environment (E2, E4, MACH-07); application.yml maps
 * the environment variables to these properties and holds the defaults.
 */
@ConfigurationProperties(prefix = "vm")
public record AppProperties(String contentBaseUrl, Security security, Store store, Uploads uploads) {

    /** Token validation settings (E1, E5, D44, D88). */
    public record Security(
            String issuerUri,
            String jwksUri,
            String audience,
            Duration keyFetchTimeout,
            Duration keyRefetchCooldown,
            Duration clockSkew) {}

    /** Store access settings (E3, D44, D71, D83). */
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

    /** Upload limits (D15, D76, D92). */
    public record Uploads(int maxConcurrent) {}

    /** The content base without a trailing slash. */
    public String contentBase() {
        return contentBaseUrl.endsWith("/") ? contentBaseUrl.substring(0, contentBaseUrl.length() - 1) : contentBaseUrl;
    }
}
