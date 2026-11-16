package eu.simpl.cm.manager;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** CM settings of D77. */
@ConfigurationProperties(prefix = "cm")
public record CmProperties(
        String participantId,
        String apiKey,
        String stubBaseUrl,
        String stubEdcApiKey,
        String uibeBaseUrl,
        String uibeApiKey,
        String uiPublicUrl,
        Duration contractVcValidity,
        long clauseMaxBytes,
        boolean kafkaEnabled) {
}
