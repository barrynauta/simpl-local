package eu.simpl.cm.manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Period;

/** D77 settings of the contract manager. */
@Component
public class CmSettings {
    public final String participantId;
    public final String apiKey;
    public final String stubBaseUrl;
    public final String stubEdcApiKey;
    public final String uibeBaseUrl;
    public final String uibeApiKey;
    public final String uiPublicUrl;
    public final Duration vcValidity;
    public final long clauseMaxBytes;

    public CmSettings(@Value("${CM_PARTICIPANT_ID:provider-01}") String participantId,
                      @Value("${CM_API_KEY:test-cm-provider-key}") String apiKey,
                      @Value("${STUB_BASE_URL:http://stubs:8080}") String stubBaseUrl,
                      @Value("${STUB_EDC_API_KEY:test-edc-key}") String stubEdcApiKey,
                      @Value("${UIBE_BASE_URL:http://uibe-provider:8080/contract-ui/v1}") String uibeBaseUrl,
                      @Value("${UIBE_API_KEY:test-uibe-provider-key}") String uibeApiKey,
                      @Value("${UI_PUBLIC_URL:http://localhost:19103}") String uiPublicUrl,
                      @Value("${CONTRACT_VC_VALIDITY:P365D}") String vcValidity,
                      @Value("${CLAUSE_MAX_BYTES:5242880}") long clauseMaxBytes) {
        this.participantId = participantId;
        this.apiKey = apiKey;
        this.stubBaseUrl = trim(stubBaseUrl);
        this.stubEdcApiKey = stubEdcApiKey;
        this.uibeBaseUrl = trim(uibeBaseUrl);
        this.uibeApiKey = uibeApiKey;
        this.uiPublicUrl = trim(uiPublicUrl);
        this.vcValidity = parseDuration(vcValidity);
        this.clauseMaxBytes = clauseMaxBytes;
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** ISO-8601 duration; also accepts the period form with days only (P365D) and mixed (P1DT2H). */
    public static Duration parseDuration(String s) {
        try {
            return Duration.parse(s);
        } catch (Exception e) {
            Period p = Period.parse(s);
            return Duration.ofDays(p.getYears() * 365L + p.getMonths() * 30L + p.getDays());
        }
    }
}
