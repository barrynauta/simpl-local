package eu.simpl.cm.manager.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.manager.CmProperties;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** CM to UIBE: POST /contractOffers/{nid} (D1, D20, D33). */
@Component
public class UibeClient {

    private final CmProperties props;
    private final Http http;

    public UibeClient(CmProperties props, ObjectMapper mapper) {
        this.props = props;
        this.http = new Http(mapper, 3000, 10000);
    }

    public HttpResult postOffer(String nid, String humanReadable, String hash) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("humanReadable", humanReadable);
        body.put("hash", hash);
        return http.exchange(HttpMethod.POST, props.uibeBaseUrl() + "/contractOffers/"
                + URLEncoder.encode(nid, StandardCharsets.UTF_8), Map.of("x-api-key", props.uibeApiKey()), body);
    }
}
