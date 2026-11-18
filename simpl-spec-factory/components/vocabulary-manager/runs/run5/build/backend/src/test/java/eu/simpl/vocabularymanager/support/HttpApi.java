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
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Small HTTP client for the integration tests: real Tomcat, real multipart parsing. */
public final class HttpApi {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    public HttpApi(String base) {
        this.base = base;
    }

    /** Response with a parsed JSON body when there is one. */
    public record Result(int status, java.net.http.HttpHeaders headers, byte[] body) {
        public JsonNode json() {
            try {
                return MAPPER.readTree(body);
            } catch (tools.jackson.core.JacksonException e) {
                throw new IllegalStateException(new String(body, StandardCharsets.UTF_8), e);
            }
        }

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    /** Builder of multipart bodies. */
    public static final class Form {
        private final List<Object[]> parts = new ArrayList<>();

        public Form field(String name, String value) {
            parts.add(new Object[] {name, null, value.getBytes(StandardCharsets.UTF_8)});
            return this;
        }

        public Form file(String name, String fileName, byte[] content) {
            parts.add(new Object[] {name, fileName, content});
            return this;
        }

        byte[] encode(String boundary) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (Object[] part : parts) {
                String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + part[0] + "\""
                        + (part[1] == null ? "" : "; filename=\"" + part[1] + "\"\r\nContent-Type: text/turtle")
                        + "\r\n\r\n";
                out.writeBytes(head.getBytes(StandardCharsets.UTF_8));
                out.writeBytes((byte[]) part[2]);
                out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }
    }

    public Result get(String path, String token) {
        return send(builder(path, token).GET());
    }

    public Result get(String path, String token, String accept) {
        return send(builder(path, token).header("Accept", accept).GET());
    }

    public Result delete(String path, String token) {
        return send(builder(path, token).DELETE());
    }

    public Result post(String path, String token, Form form) {
        String boundary = "b" + UUID.randomUUID();
        return send(builder(path, token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(form.encode(boundary))));
    }

    public Result postJson(String path, String token, String json) {
        return send(builder(path, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    public Result patchJson(String path, String token, String json) {
        return send(builder(path, token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json)));
    }

    public Result send(String method, String path, String token, String contentType, String body) {
        HttpRequest.Builder b = builder(path, token);
        if (contentType != null) {
            b.header("Content-Type", contentType);
        }
        return send(b.method(
                method,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)));
    }

    private HttpRequest.Builder builder(String path, String token) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    private Result send(HttpRequest.Builder builder) {
        try {
            HttpResponse<byte[]> r = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Result(r.statusCode(), r.headers(), r.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
