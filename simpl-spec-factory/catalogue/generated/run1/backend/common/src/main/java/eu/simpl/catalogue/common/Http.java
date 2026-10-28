package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Small HTTP helpers: explicit URL encoding and RestClients with timeouts (D47). */
public final class Http {
    private Http() {}

    public static RestClient client(Duration timeout) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).version(HttpClient.Version.HTTP_1_1).build();
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(http);
        f.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(f).build();
    }

    public static String seg(String s) {
        return UriUtils.encodePathSegment(s, StandardCharsets.UTF_8);
    }

    /** Query string builder that encodes every value strictly ('+' becomes %2B). */
    public static final class Query {
        private final List<String> parts = new ArrayList<>();

        public Query add(String name, Object value) {
            if (value == null) return this;
            String v = String.valueOf(value);
            parts.add(enc(name) + "=" + enc(v));
            return this;
        }

        public Query addAll(String name, List<String> values) {
            if (values != null) values.forEach(v -> add(name, v));
            return this;
        }

        public URI uri(String base) {
            return URI.create(parts.isEmpty() ? base : base + "?" + String.join("&", parts));
        }

        public String relative(String path) {
            return parts.isEmpty() ? path : path + "?" + String.join("&", parts);
        }

        private static String enc(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }

    public static Query query() { return new Query(); }

    public static boolean isTimeout(Throwable t) {
        while (t != null) {
            if (t instanceof HttpTimeoutException || t instanceof java.net.SocketTimeoutException) return true;
            t = t.getCause();
        }
        return false;
    }

    /** Problem body of a downstream error, or a generic one if the body is not a Problem. */
    public static ObjectNode problemBody(int status, String body) {
        JsonNode n = Json.parseOrNull(body);
        if (n instanceof ObjectNode o && o.has("type") && o.has("status")) return o;
        ProblemTypes t = status == 404 ? ProblemTypes.NOT_FOUND
                : status == 403 ? ProblemTypes.FORBIDDEN
                : status == 401 ? ProblemTypes.UNAUTHORIZED
                : status == 400 ? ProblemTypes.BAD_REQUEST : ProblemTypes.INTERNAL;
        return Problems.body(t, status, t.title);
    }
}
