package eu.simpl.cm.common.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Small JSON HTTP client used for outbound calls. Never throws: a transport error gives status -1. */
public class HttpJson {
    private static final Logger log = LoggerFactory.getLogger(HttpJson.class);

    public record Response(int status, String body, String contentType) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public JsonNode json(ObjectMapper mapper) {
            if (body == null || body.isBlank()) {
                return null;
            }
            try {
                return mapper.readTree(body);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Duration timeout;

    public HttpJson(ObjectMapper mapper, Duration timeout) {
        this.mapper = mapper;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public Response send(String method, String url, Map<String, String> headers, Object body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(timeout);
            headers.forEach((k, v) -> {
                if (v != null) {
                    b.header(k, v);
                }
            });
            b.header("Accept", "application/json, application/problem+json, */*");
            if (body != null) {
                String text = body instanceof String s ? s : mapper.writeValueAsString(body);
                b.header("Content-Type", "application/json");
                b.method(method, HttpRequest.BodyPublishers.ofString(text));
            } else {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            log.debug("{} {} -> {}", method, url, r.statusCode());
            return new Response(r.statusCode(), r.body(), r.headers().firstValue("Content-Type").orElse(null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Response(-1, null, null);
        } catch (Exception e) {
            log.warn("{} {} failed: {}", method, url, e.toString());
            return new Response(-1, null, null);
        }
    }
}
