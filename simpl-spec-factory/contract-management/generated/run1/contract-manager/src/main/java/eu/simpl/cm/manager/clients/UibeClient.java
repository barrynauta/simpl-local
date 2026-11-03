package eu.simpl.cm.manager.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.CorrelationIdFilter;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.manager.CmSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** CM to UIBE: POST /contractOffers/{nid} (D1, D20, D33). */
@Component
public class UibeClient {
    private final CmSettings settings;
    private final HttpJson http;

    public UibeClient(CmSettings settings, ObjectMapper mapper) {
        this.settings = settings;
        this.http = new HttpJson(mapper, Duration.ofSeconds(10));
    }

    public HttpJson.Response postOffer(String nid, String humanReadable, String hash) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("x-api-key", settings.uibeApiKey);
        h.put(CorrelationIdFilter.HEADER, CorrelationIdFilter.current());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("humanReadable", humanReadable);
        body.put("hash", hash);
        return http.send("POST", settings.uibeBaseUrl + "/contractOffers/" + nid, h, body);
    }
}
