package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.CorrelationIdFilter;
import eu.simpl.cm.common.web.HttpJson;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** UIBE to its own CM, with the CM API key. */
@Component
public class CmClient {
    private final UibeSettings settings;
    private final HttpJson http;

    public CmClient(UibeSettings settings, ObjectMapper mapper) {
        this.settings = settings;
        this.http = new HttpJson(mapper, Duration.ofSeconds(15));
    }

    public HttpJson.Response call(String method, String pathAndQuery, Object body, String actor) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("X-Api-Key", settings.cmApiKey);
        h.put(CorrelationIdFilter.HEADER, CorrelationIdFilter.current());
        if (actor != null) {
            h.put("X-Actor", actor);
        }
        return http.send(method, settings.cmBaseUrl + pathAndQuery, h, body);
    }
}
