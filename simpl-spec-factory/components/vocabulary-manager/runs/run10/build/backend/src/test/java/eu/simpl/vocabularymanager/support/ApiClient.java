package eu.simpl.vocabularymanager.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A small HTTP client for the API tests: JSON, multipart and raw requests over real HTTP. */
public final class ApiClient {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    public ApiClient(int port) {
        this.base = "http://127.0.0.1:" + port;
    }

    /** One response with its body parsed as JSON when possible. */
    public record Response(int status, Map<String, List<String>> headers, byte[] body) {
        public JsonNode json() {
            return MAPPER.readTree(body);
        }

        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        public String header(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(entry -> entry.getValue().getFirst())
                    .findFirst()
                    .orElse(null);
        }
    }

    /** One multipart part. */
    public record Part(String name, byte[] value, String fileName) {
        public static Part text(String name, String value) {
            return new Part(name, value.getBytes(StandardCharsets.UTF_8), null);
        }

        public static Part file(String name, byte[] value) {
            return new Part(name, value, name + ".ttl");
        }
    }

    public Response get(String path, String token) {
        return send(builder(path, token).GET());
    }

    public Response get(String path, String token, String accept) {
        HttpRequest.Builder builder = builder(path, token).GET();
        builder.header("Accept", accept);
        return send(builder);
    }

    public Response delete(String path, String token) {
        return send(builder(path, token).DELETE());
    }

    public Response method(String method, String path, String token) {
        return send(builder(path, token).method(method, HttpRequest.BodyPublishers.noBody()));
    }

    public Response json(String method, String path, String token, String body) {
        HttpRequest.Builder builder = builder(path, token)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        return send(builder);
    }

    public Response raw(String method, String path, String token, String contentType, byte[] body) {
        HttpRequest.Builder builder = builder(path, token).method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        return send(builder);
    }

    public Response multipart(String path, String token, List<Part> parts) {
        String boundary = "----test" + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Part part : parts) {
            out.writeBytes(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            String disposition = "Content-Disposition: form-data; name=\"" + part.name() + "\"";
            if (part.fileName() != null) {
                disposition += "; filename=\"" + part.fileName() + "\"\r\nContent-Type: text/turtle";
            }
            out.writeBytes((disposition + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.writeBytes(part.value());
            out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return raw("POST", path, token, "multipart/form-data; boundary=" + boundary, out.toByteArray());
    }

    /** Parts of a create request. */
    public static List<Part> createParts(String name, String description, String turtle) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.text("name", name));
        parts.add(Part.text("description", description));
        parts.add(Part.text("changelog", "Initial version."));
        parts.add(Part.file("vocabularyFile", turtle.getBytes(StandardCharsets.UTF_8)));
        return parts;
    }

    /** Parts of a new-version request. */
    public static List<Part> versionParts(String description, String changelog, String turtle) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.text("description", description));
        parts.add(Part.text("changelog", changelog));
        parts.add(Part.file("vocabularyFile", turtle.getBytes(StandardCharsets.UTF_8)));
        return parts;
    }

    private HttpRequest.Builder builder(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private Response send(HttpRequest.Builder builder) {
        try {
            HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Response(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
