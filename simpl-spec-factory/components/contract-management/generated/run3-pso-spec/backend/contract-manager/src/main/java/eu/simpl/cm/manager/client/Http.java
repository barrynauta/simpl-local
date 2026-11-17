package eu.simpl.cm.manager.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.CorrelationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

/** Small wrapper around RestClient that never throws and always passes the correlation id (D14). */
public class Http {

    private static final Logger LOG = LoggerFactory.getLogger(Http.class);

    private final RestClient client;
    private final ObjectMapper mapper;

    public Http(ObjectMapper mapper, int connectMillis, int readMillis) {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMillis)).version(HttpClient.Version.HTTP_1_1).build());
        f.setReadTimeout(Duration.ofMillis(readMillis));
        this.client = RestClient.builder().requestFactory(f).build();
        this.mapper = mapper;
    }

    public HttpResult exchange(HttpMethod method, String url, Map<String, String> headers, Object body) {
        try {
            RestClient.RequestBodySpec spec = client.method(method).uri(URI.create(url))
                    .accept(MediaType.APPLICATION_JSON, MediaType.valueOf("application/problem+json"));
            String cid = CorrelationContext.get();
            if (cid != null) {
                spec.header("X-Correlation-Id", cid);
            }
            headers.forEach(spec::header);
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.exchange((req, res) -> {
                int status = res.getStatusCode().value();
                byte[] bytes = res.getBody().readAllBytes();
                JsonNode json = null;
                if (bytes.length > 0) {
                    try {
                        json = mapper.readTree(bytes);
                    } catch (Exception e) {
                        json = null;
                    }
                }
                return new HttpResult(status, json);
            });
        } catch (Exception e) {
            LOG.warn("Outbound call {} {} failed: {}", method, url, e.toString());
            return HttpResult.unreachable();
        }
    }
}
