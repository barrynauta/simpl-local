package eu.simpl.vocabularymanager.support;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Small HTTP client for the API tests: real requests through Tomcat and every filter. */
public final class Api {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final String base;

    /**
     * @param port the port of the server
     */
    public Api(int port) {
        this.base = "http://127.0.0.1:" + port;
    }

    /** A response with its body. */
    public record Result(int status, String contentType, byte[] body, HttpResponse<byte[]> raw) {

        /**
         * @return the body as JSON
         */
        public JsonNode json() {
            return JSON.readTree(body);
        }

        /**
         * @return the body as text
         */
        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        /**
         * @param name header name
         * @return the first value or null
         */
        public String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }

        /**
         * @return the issue names of a problem body
         */
        public List<String> issueNames() {
            List<String> names = new ArrayList<>();
            json().path("issues").forEach(issue -> names.add(issue.path("name").asString()));
            return names;
        }

        /**
         * @return the issue details of a problem body
         */
        public List<String> issueDetails() {
            List<String> details = new ArrayList<>();
            json().path("issues").forEach(issue -> details.add(issue.path("detail").asString()));
            return details;
        }
    }

    /** One multipart part. */
    public record Part(String name, String filename, String contentType, byte[] content) {

        /**
         * @param name field name
         * @param value field value
         * @return a text field
         */
        public static Part field(String name, String value) {
            return new Part(name, null, null, value.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * @param content Turtle bytes
         * @return the vocabularyFile part
         */
        public static Part file(byte[] content) {
            return new Part("vocabularyFile", "vocabulary.ttl", "text/turtle", content);
        }

        /**
         * @param content Turtle text
         * @return the vocabularyFile part
         */
        public static Part file(String content) {
            return file(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * @param method HTTP method
     * @param path path below the server root, for example /v1/vocabularies
     * @param token bearer token or null
     * @param contentType request content type or null
     * @param body request body or null
     * @param headers extra headers as name, value pairs
     * @return the result
     */
    public Result send(String method, String path, String token, String contentType, byte[] body, String... headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(60));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        for (int i = 0; i + 1 < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        builder.method(
                method,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
        try {
            HttpResponse<byte[]> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Result(
                    response.statusCode(),
                    response.headers().firstValue("Content-Type").orElse(null),
                    response.body(),
                    response);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param path path
     * @param token token or null
     * @param headers extra headers
     * @return the result
     */
    public Result get(String path, String token, String... headers) {
        return send("GET", path, token, null, null, headers);
    }

    /**
     * @param path path
     * @param token token or null
     * @return the result
     */
    public Result delete(String path, String token) {
        return send("DELETE", path, token, null, null);
    }

    /**
     * @param method POST or PATCH
     * @param path path
     * @param token token or null
     * @param json JSON text
     * @return the result
     */
    public Result json(String method, String path, String token, String json) {
        return send(method, path, token, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param path path
     * @param token token or null
     * @param parts the parts
     * @return the result
     */
    public Result multipart(String path, String token, Part... parts) {
        String boundary = "b" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Part part : parts) {
            StringBuilder head = new StringBuilder("--").append(boundary).append("\r\n");
            head.append("Content-Disposition: form-data; name=\"").append(part.name()).append('"');
            if (part.filename() != null) {
                head.append("; filename=\"").append(part.filename()).append('"');
            }
            head.append("\r\n");
            if (part.contentType() != null) {
                head.append("Content-Type: ").append(part.contentType()).append("\r\n");
            }
            head.append("\r\n");
            out.writeBytes(head.toString().getBytes(StandardCharsets.UTF_8));
            out.writeBytes(part.content());
            out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send("POST", path, token, "multipart/form-data; boundary=" + boundary, out.toByteArray());
    }

    /**
     * @return a unique suffix for test data
     */
    public static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
