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
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A small HTTP client for the API tests over a real port. */
public final class Http {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final HttpClient CLIENT =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final String base;

    /** @param base base URL of the server, without /v1 */
    public Http(String base) {
        this.base = base;
    }

    /** A response with helpers. */
    public record Response(int status, Map<String, List<String>> headers, byte[] body) {

        /** @return the body as JSON */
        public JsonNode json() {
            return MAPPER.readTree(body);
        }

        /** @return the body as text */
        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        /** @return the first value of a header, or null */
        public String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }
    }

    /** Sends a request. */
    public Response send(String method, String path, String token, String contentType, byte[] body, String... headers) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(60));
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
            return new Response(response.statusCode(), response.headers().map(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** GET. */
    public Response get(String path, String token, String... headers) {
        return send("GET", path, token, null, null, headers);
    }

    /** JSON request. */
    public Response json(String method, String path, String token, String json, String... headers) {
        return send(method, path, token, "application/json", json.getBytes(StandardCharsets.UTF_8), headers);
    }

    /** Multipart request. */
    public Response multipart(String path, String token, Multipart form, String... headers) {
        return send("POST", path, token, form.contentType(), form.body(), headers);
    }

    /** A multipart/form-data body builder. */
    public static final class Multipart {
        private final String boundary = "----vmtest" + System.nanoTime();
        private final List<byte[]> chunks = new ArrayList<>();

        /** Adds a text field. */
        public Multipart field(String name, String value) {
            chunks.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value
                            + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            return this;
        }

        /** Adds a file part. */
        public Multipart file(String name, String filename, byte[] content) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\""
                            + filename + "\"\r\nContent-Type: text/turtle\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.writeBytes(content);
            out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
            chunks.add(out.toByteArray());
            return this;
        }

        /** Adds a file part from text. */
        public Multipart file(String content) {
            return file("vocabularyFile", "vocabulary.ttl", content.getBytes(StandardCharsets.UTF_8));
        }

        String contentType() {
            return "multipart/form-data; boundary=" + boundary;
        }

        byte[] body() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            chunks.forEach(out::writeBytes);
            out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }
    }
}
