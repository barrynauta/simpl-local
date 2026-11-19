package eu.simpl.vocabulary.support;

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

/** Small HTTP client for the integration tests: real requests against the running server. */
public final class Http {

    private static final HttpClient CLIENT =
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    private final String base;

    public Http(int port) {
        this.base = "http://localhost:" + port;
    }

    /** One multipart part. */
    public record Part(String name, String filename, byte[] bytes) {}

    public static Part field(String name, String value) {
        return new Part(name, null, value.getBytes(StandardCharsets.UTF_8));
    }

    public static Part file(String name, String content) {
        return new Part(name, "vocabulary.ttl", content.getBytes(StandardCharsets.UTF_8));
    }

    public static Part file(String name, byte[] content) {
        return new Part(name, "vocabulary.ttl", content);
    }

    public HttpResponse<String> send(
            String method, String path, String token, String contentType, byte[] body, String... headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
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
            return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public HttpResponse<String> get(String path, String token, String... headers) {
        return send("GET", path, token, null, null, headers);
    }

    public HttpResponse<String> json(String method, String path, String token, String json) {
        return send(method, path, token, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> multipart(String path, String token, List<Part> parts) {
        String boundary = "b" + UUID.randomUUID();
        return send("POST", path, token, "multipart/form-data; boundary=" + boundary, multipartBody(boundary, parts));
    }

    public static byte[] multipartBody(String boundary, List<Part> parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Part part : parts) {
            StringBuilder head = new StringBuilder("--")
                    .append(boundary)
                    .append("\r\n")
                    .append("Content-Disposition: form-data; name=\"")
                    .append(part.name())
                    .append('"');
            if (part.filename() != null) {
                head.append("; filename=\"").append(part.filename()).append('"');
            }
            head.append("\r\n");
            if (part.filename() != null) {
                head.append("Content-Type: text/turtle\r\n");
            }
            head.append("\r\n");
            out.writeBytes(head.toString().getBytes(StandardCharsets.UTF_8));
            out.writeBytes(part.bytes());
            out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    public static List<Part> parts(Part... parts) {
        return new ArrayList<>(List.of(parts));
    }
}
