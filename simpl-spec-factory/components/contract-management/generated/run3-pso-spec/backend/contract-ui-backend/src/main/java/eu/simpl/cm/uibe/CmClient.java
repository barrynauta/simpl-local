package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.ApiException;
import eu.simpl.cm.common.CorrelationContext;
import eu.simpl.cm.common.ProblemTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/** UIBE to its own CM (D1, D7). CM errors are passed on with the same status code (D23). */
@Component
public class CmClient {

    private static final Logger LOG = LoggerFactory.getLogger(CmClient.class);

    private final UibeProperties props;
    private final RestClient client;
    private final ObjectMapper mapper;

    public CmClient(UibeProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(3000)).version(HttpClient.Version.HTTP_1_1).build());
        f.setReadTimeout(Duration.ofMillis(15000));
        this.client = RestClient.builder().requestFactory(f).build();
    }

    public record Result(int status, JsonNode body) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    public UriComponentsBuilder uri(String path) {
        return UriComponentsBuilder.fromUriString(props.cmBaseUrl() + path);
    }

    public Result call(HttpMethod method, URI uri, Object body, String actor) {
        try {
            RestClient.RequestBodySpec spec = client.method(method).uri(uri)
                    .header("X-Api-Key", props.cmApiKey())
                    .accept(MediaType.APPLICATION_JSON, MediaType.valueOf("application/problem+json"));
            String cid = CorrelationContext.get();
            if (cid != null) {
                spec.header("X-Correlation-Id", cid);
            }
            if (actor != null) {
                spec.header("X-Actor", actor);
            }
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.exchange((req, res) -> {
                byte[] bytes = res.getBody().readAllBytes();
                JsonNode json = null;
                if (bytes.length > 0) {
                    try {
                        json = mapper.readTree(bytes);
                    } catch (Exception e) {
                        json = null;
                    }
                }
                return new Result(res.getStatusCode().value(), json);
            });
        } catch (Exception e) {
            LOG.warn("CM call {} {} failed: {}", method, uri, e.toString());
            return new Result(-1, null);
        }
    }

    /** Calls CM and returns the body, or throws the CM error with the same status. */
    public JsonNode require(HttpMethod method, URI uri, Object body, String actor) {
        Result r = call(method, uri, body, actor);
        if (r.ok()) {
            return r.body();
        }
        throw relay(r);
    }

    public static ApiException relay(Result r) {
        if (r.status() < 0) {
            return new ApiException(502, ProblemTypes.INTERNAL_SERVER_ERROR, "Bad Gateway",
                    "Contract manager not reachable");
        }
        JsonNode b = r.body();
        String type = b != null && b.path("type").isTextual() ? b.get("type").asText()
                : ProblemTypes.INTERNAL_SERVER_ERROR;
        String title = b != null && b.path("title").isTextual() ? b.get("title").asText() : "Error";
        String detail = b != null && b.path("detail").isTextual() ? b.get("detail").asText()
                : "Contract manager answered " + r.status();
        return new ApiException(r.status(), type, title, detail);
    }

    public static Map<String, Object> asMap(ObjectMapper mapper, JsonNode n) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = mapper.convertValue(n, java.util.LinkedHashMap.class);
        return m;
    }
}
