package eu.simpl.vocabularymanager.it;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A small HTTP client for the integration tests, with a multipart builder. */
public final class ApiClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    public ApiClient(int port) {
        this.base = "http://localhost:" + port;
    }

    /** One response with its parsed JSON body (null for non-JSON bodies). */
    public record Result(int status, java.net.http.HttpHeaders headers, byte[] bytes) {
        public JsonNode json() {
            return bytes.length == 0 ? null : JSON.readTree(bytes);
        }

        public String text() {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        public String type() {
            return json().get("type").asString();
        }
    }

    /** Multipart form parts. */
    public static final class Form {
        private final String boundary = "b" + UUID.randomUUID();
        private final List<byte[]> chunks = new ArrayList<>();

        public Form field(String name, String value) {
            chunks.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value
                            + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public Form file(String name, byte[] content) {
            chunks.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                            + "\"; filename=\"v.ttl\"\r\nContent-Type: text/turtle\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            chunks.add(content);
            chunks.add("\r\n".getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public Form file(String name, String content) {
            return file(name, content.getBytes(StandardCharsets.UTF_8));
        }

        public byte[] body() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            chunks.forEach(out::writeBytes);
            out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }

        public String contentType() {
            return "multipart/form-data; boundary=" + boundary;
        }
    }

    public Result send(String method, String path, String token, String contentType, byte[] body, String... headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                    .method(
                            method,
                            body == null
                                    ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofByteArray(body));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            if (contentType != null) {
                builder.header("Content-Type", contentType);
            }
            for (int i = 0; i + 1 < headers.length; i += 2) {
                builder.header(headers[i], headers[i + 1]);
            }
            HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Result(response.statusCode(), response.headers(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Result get(String path, String token, String... headers) {
        return send("GET", path, token, null, null, headers);
    }

    public Result postJson(String path, String token, String json) {
        return send("POST", path, token, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    public Result patch(String path, String token, String json) {
        return send("PATCH", path, token, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    public Result post(String path, String token, Form form) {
        return send("POST", path, token, form.contentType(), form.body());
    }
}
