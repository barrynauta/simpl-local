package eu.simpl.schemamanager.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service settings. Each value is read from a D60 configuration name, see application.yml.
 */
@ConfigurationProperties(prefix = "sm")
public record Settings(
        String publicBaseUrl,
        String keycloakUrl,
        String keycloakRealm,
        String keycloakJwksUrl,
        String vocabularyManagerUrl,
        String vocabularyNamespaceBase,
        String defaultProperties,
        String notificationFrom,
        String providerNotificationEmails,
        String providerInstructionsUrl,
        String corsAllowedOrigins) {

    public String publicBase() {
        return stripSlash(publicBaseUrl);
    }

    public String issuer() {
        return stripSlash(keycloakUrl) + "/realms/" + keycloakRealm;
    }

    public String jwksUrl() {
        if (keycloakJwksUrl != null && !keycloakJwksUrl.isBlank()) {
            return keycloakJwksUrl.trim();
        }
        return issuer() + "/protocol/openid-connect/certs";
    }

    public List<String> defaultPropertyIris() {
        return split(defaultProperties);
    }

    public List<String> providerEmails() {
        return split(providerNotificationEmails);
    }

    public List<String> corsOrigins() {
        return split(corsAllowedOrigins);
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String stripSlash(String s) {
        if (s == null) {
            return "";
        }
        String r = s.trim();
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }
}
