package eu.simpl.cm.uibe;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** UIBE settings of D77, plus UI_PUBLIC_URL and UIBE_PARTY_ROLE (see BUILD-LOG). */
@ConfigurationProperties(prefix = "uibe")
public record UibeProperties(
        String participantId,
        String partyRole,
        String apiKey,
        String cmBaseUrl,
        String cmApiKey,
        String oidcIssuer,
        String oidcJwksUri,
        String offerExpiryCron,
        Duration offerExpiryTimeout,
        String consentManagerUrl,
        String uiPublicUrl) {

    public boolean isConsumerParty() {
        return "CONSUMER".equalsIgnoreCase(partyRole);
    }
}
