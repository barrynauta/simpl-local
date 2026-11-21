package eu.simpl.vocabularymanager.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A small HTTP client for the integration tests. */
public final class ApiClient {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    public ApiClient(int port) {
        this.base = "http://localhost:" + port;
    }

    /** A response with its parsed JSON body (null when the body is not JSON). */
    public record Response(int status, Map<String, List<String>> headers, byte[] body) {
        public JsonNode json() {
            try {
                return MAPPER.readTree(body);
            } catch (RuntimeException e) {
                return null;
            }
        }

        public String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue().get(0);
                }
            }
            return null;
        }

        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    /** One form part. */
    public record Part(String name, String fileName, byte[] value) {
        public static Part field(String name, String value) {
            return new Part(name, null, value.getBytes(StandardCharsets.UTF_8));
        }

        public static Part file(String name, byte[] value) {
            return new Part(name, name + ".ttl", value);
        }
    }

    public Response send(String method, String path, String token, Map<String, String> headers, byte[] body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        headers.forEach(builder::header);
        builder.method(
                method,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
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

    public Response get(String path, String token) {
        return send("GET", path, token, Map.of(), null);
    }

    public Response json(String method, String path, String token, String json) {
        return send(
                method, path, token, Map.of("Content-Type", "application/json"), json.getBytes(StandardCharsets.UTF_8));
    }

    public Response multipart(String path, String token, List<Part> parts) {
        String boundary = "b" + UUID.randomUUID();
        return send(
                "POST",
                path,
                token,
                Map.of("Content-Type", "multipart/form-data; boundary=" + boundary),
                multipartBody(boundary, parts));
    }

    public static byte[] multipartBody(String boundary, List<Part> parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Part part : parts) {
            StringBuilder head = new StringBuilder("--").append(boundary).append("\r\n");
            head.append("Content-Disposition: form-data; name=\"")
                    .append(part.name())
                    .append('"');
            if (part.fileName() != null) {
                head.append("; filename=\"").append(part.fileName()).append('"');
                head.append("\r\nContent-Type: text/turtle");
            }
            head.append("\r\n\r\n");
            out.writeBytes(head.toString().getBytes(StandardCharsets.UTF_8));
            out.writeBytes(part.value());
            out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /** Form parts of a vocabulary creation. */
    public static List<Part> createParts(String name, String turtle, String description) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.file("vocabularyFile", turtle.getBytes(StandardCharsets.UTF_8)));
        parts.add(Part.field("name", name));
        parts.add(Part.field("description", description));
        parts.add(Part.field("changelog", "Initial version."));
        return parts;
    }

    public static List<Part> versionParts(String turtle, String description, String changelog) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.file("vocabularyFile", turtle.getBytes(StandardCharsets.UTF_8)));
        parts.add(Part.field("description", description));
        parts.add(Part.field("changelog", changelog));
        return parts;
    }

    public static Map<String, String> headers(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
