package eu.simpl.vocabularymanager.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Base of the integration tests: the application on a random port, an in-process store and identity provider. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiTestBase {

    protected static final TestIdentityProvider IDP = TestIdentityProvider.shared();
    protected static final TestStore STORE = TestStore.shared();
    protected static final JsonMapper JSON = JsonMapper.builder().build();
    protected static final HttpClient HTTP = HttpClient.newHttpClient();

    protected final String admin = IDP.token("ga-admin", "GA_VOCABULARY_ADMIN");
    protected final String viewer = IDP.token("ga-viewer", "GA_VOCABULARY_VIEWER");
    protected final String norole = IDP.token("ga-norole");

    @Value("${local.server.port}")
    protected int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vocabulary.store.url", STORE::url);
        registry.add("vocabulary.store.read-user", () -> TestStore.READ_USER);
        registry.add("vocabulary.store.read-password", () -> TestStore.READ_PASSWORD);
        registry.add("vocabulary.store.update-user", () -> TestStore.UPDATE_USER);
        registry.add("vocabulary.store.update-password", () -> TestStore.UPDATE_PASSWORD);
        registry.add("vocabulary.identity.issuer", () -> TestIdentityProvider.ISSUER);
        registry.add("vocabulary.identity.jwks-url", IDP::jwksUrl);
        registry.add("management.server.port", () -> "0");
    }

    /** A unique vocabulary name. */
    protected static String uniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** A response with its parsed body. */
    public record Reply(int status, Map<String, List<String>> headers, byte[] bytes) {
        public JsonNode json() {
            return JSON.readTree(bytes);
        }

        public String text() {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue().get(0);
                }
            }
            return null;
        }

        public String type() {
            return json().path("type").asString();
        }
    }

    protected Reply send(String method, String path, String token, String contentType, byte[] body, String... headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
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
        try {
            HttpResponse<byte[]> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Reply(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    protected Reply get(String path, String token, String... headers) {
        return send("GET", path, token, null, null, headers);
    }

    protected Reply json(String method, String path, String token, String body) {
        return send(method, path, token, "application/json", body.getBytes(StandardCharsets.UTF_8));
    }

    /** Multipart builder. */
    public static final class Form {
        public Form() {}

        private final String boundary = "b" + UUID.randomUUID();
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public Form field(String name, String value) {
            write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
            write(value);
            write("\r\n");
            return this;
        }

        public Form file(String name, byte[] content) {
            write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                    + "\"; filename=\"v.ttl\"\r\nContent-Type: text/turtle\r\n\r\n");
            out.writeBytes(content);
            write("\r\n");
            return this;
        }

        public Form file(String content) {
            return file("vocabularyFile", content.getBytes(StandardCharsets.UTF_8));
        }

        public String contentType() {
            return "multipart/form-data; boundary=" + boundary;
        }

        public byte[] bytes() {
            ByteArrayOutputStream copy = new ByteArrayOutputStream();
            copy.writeBytes(out.toByteArray());
            copy.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return copy.toByteArray();
        }

        private void write(String text) {
            out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected Reply post(String path, String token, Form form) {
        return send("POST", path, token, form.contentType(), form.bytes());
    }

    protected static String turtle(String namespace, String label) {
        return "@prefix ex: <https://example.org/" + namespace + "#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "ex:Scheme a skos:ConceptScheme ; skos:prefLabel \"" + label + "\"@en .\n";
    }

    protected Reply createVocabulary(String name, String content) {
        return post(
                "/v1/vocabularies",
                admin,
                new Form()
                        .field("name", name)
                        .field("description", "Description of " + name)
                        .field("changelog", "Initial version.")
                        .file(content));
    }

    protected Reply createVersion(String name, String description, String content) {
        return post(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                new Form()
                        .field("description", description)
                        .field("changelog", "Changed.")
                        .file(content));
    }
}
