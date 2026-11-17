package eu.simpl.cm.uibe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Period;

/** D77 settings of the contract UI backend. */
@Component
public class UibeSettings {
    public final String participantId;
    public final String apiKey;
    public final String cmBaseUrl;
    public final String cmApiKey;
    public final String oidcIssuer;
    public final String oidcJwksUri;
    public final Duration offerExpiryTimeout;
    public final String consentManagerUrl;
    public final String uiPublicUrl;

    public UibeSettings(@Value("${UIBE_PARTICIPANT_ID:provider-01}") String participantId,
                        @Value("${UIBE_API_KEY:test-uibe-provider-key}") String apiKey,
                        @Value("${CM_BASE_URL:http://cm-provider:8080/contract/v1}") String cmBaseUrl,
                        @Value("${CM_API_KEY:test-cm-provider-key}") String cmApiKey,
                        @Value("${OIDC_ISSUER:http://localhost:19080/realms/provider}") String oidcIssuer,
                        @Value("${OIDC_JWKS_URI:http://keycloak:8080/realms/provider/protocol/openid-connect/certs}") String oidcJwksUri,
                        @Value("${OFFER_EXPIRY_TIMEOUT:P7D}") String offerExpiryTimeout,
                        @Value("${CONSENT_MANAGER_URL:http://localhost:19090/consent}") String consentManagerUrl,
                        @Value("${UI_PUBLIC_URL:http://localhost:19103}") String uiPublicUrl) {
        this.participantId = participantId;
        this.apiKey = apiKey;
        this.cmBaseUrl = cmBaseUrl.endsWith("/") ? cmBaseUrl.substring(0, cmBaseUrl.length() - 1) : cmBaseUrl;
        this.cmApiKey = cmApiKey;
        this.oidcIssuer = oidcIssuer;
        this.oidcJwksUri = oidcJwksUri;
        this.offerExpiryTimeout = parseDuration(offerExpiryTimeout);
        this.consentManagerUrl = consentManagerUrl;
        this.uiPublicUrl = uiPublicUrl.endsWith("/") ? uiPublicUrl.substring(0, uiPublicUrl.length() - 1) : uiPublicUrl;
    }

    public static Duration parseDuration(String s) {
        try {
            return Duration.parse(s);
        } catch (Exception e) {
            Period p = Period.parse(s);
            return Duration.ofDays(p.getYears() * 365L + p.getMonths() * 30L + p.getDays());
        }
    }
}
