package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Calls a party's CM with its API key and X-Correlation-Id = nid. */
@Component
public class CmClient {
    private static final Logger log = LoggerFactory.getLogger(CmClient.class);

    public record Result(int status, JsonNode body) {
        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final StubState state;
    private final ObjectMapper mapper;

    public CmClient(StubState state, ObjectMapper mapper) {
        this.state = state;
        this.mapper = mapper;
    }

    public Result call(String participantId, String method, String path, Object body, String nid) {
        StubState.CmEndpoint cm = state.cms.get(participantId);
        if (cm == null) {
            return new Result(-1, null);
        }
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(cm.baseUrl() + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Api-Key", cm.apiKey())
                    .header("X-Correlation-Id", nid)
                    .header("Accept", "application/json");
            if (body != null) {
                b.header("Content-Type", "application/json");
                b.method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            } else {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = null;
            if (r.body() != null && !r.body().isBlank()) {
                try {
                    json = mapper.readTree(r.body());
                } catch (Exception e) {
                    json = mapper.getNodeFactory().textNode(r.body());
                }
            }
            log.info("stub -> {} {} {} : {}", participantId, method, path, r.statusCode());
            return new Result(r.statusCode(), json);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("stub -> {} {} {} failed: {}", participantId, method, path, e.toString());
            return new Result(-1, null);
        }
    }
}
