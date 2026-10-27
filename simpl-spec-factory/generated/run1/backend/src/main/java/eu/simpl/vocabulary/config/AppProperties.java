package eu.simpl.vocabulary.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Application settings, bound from the {@code app.*} keys. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String publicBaseUrl,
        String referenceBase,
        long maxUploadBytes,
        int maxPageSize,
        Fuseki fuseki,
        Security security) {

    public record Fuseki(String url) {}

    public record Security(String jwkSetUri, String issuers, String rolesClaim) {
        public List<String> issuerList() {
            return Arrays.stream(issuers.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
    }

    /** Reference base without a trailing slash. */
    public String normalizedReferenceBase() {
        return referenceBase.endsWith("/") ? referenceBase.substring(0, referenceBase.length() - 1) : referenceBase;
    }

    public String normalizedPublicBaseUrl() {
        return publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }
}
