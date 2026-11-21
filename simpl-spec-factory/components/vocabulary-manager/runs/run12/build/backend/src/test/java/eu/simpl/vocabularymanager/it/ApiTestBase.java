package eu.simpl.vocabularymanager.it;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration test base: the real service on a random port, an in-process Fuseki store (Jena Fuseki
 * main, in-memory transactional dataset) and a stand-in identity provider.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0"})
public abstract class ApiTestBase {

    static final Dataset DATASET = DatasetFactory.createTxnMem();
    static final FusekiServer FUSEKI;
    static final TestIdp IDP;
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final AtomicInteger COUNTER = new AtomicInteger();

    static {
        FUSEKI = FusekiServer.create()
                .port(0)
                .add("/vocabulary", DATASET)
                .build()
                .start();
        try {
            IDP = new TestIdp();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @LocalServerPort
    int port;

    static String admin;
    static String viewer;
    static String norole;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        String base = "http://localhost:" + FUSEKI.getPort() + "/vocabulary";
        r.add("vm.store.query-url", () -> base + "/query");
        r.add("vm.store.update-url", () -> base + "/update");
        r.add("vm.auth.issuer", () -> TestIdp.ISSUER);
        r.add("vm.auth.jwks-uri", IDP::jwksUri);
    }

    @BeforeAll
    static void tokens() throws Exception {
        admin = IDP.token("ga-admin", "GA_VOCABULARY_ADMIN");
        viewer = IDP.token("ga-viewer", "GA_VOCABULARY_VIEWER");
        norole = IDP.token("ga-norole");
    }

    /** A unique vocabulary name for one test (the store is never reset). */
    static String unique(String prefix) {
        return prefix
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase()
                + COUNTER.incrementAndGet();
    }

    /** Sends a thread's requests to another instance (TwoInstancesTest). */
    static final ThreadLocal<Integer> PORT_OVERRIDE = new ThreadLocal<>();

    String url(String path) {
        Integer p = PORT_OVERRIDE.get();
        return "http://localhost:" + (p != null ? p : port) + path;
    }

    HttpResponse<String> send(
            String method, String path, String token, Map<String, String> headers, HttpRequest.BodyPublisher body)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url(path)));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        headers.forEach(b::header);
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : body);
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> get(String path, String token) throws Exception {
        return send("GET", path, token, Map.of(), null);
    }

    HttpResponse<String> json(String method, String path, String token, String body) throws Exception {
        return send(
                method,
                path,
                token,
                Map.of("Content-Type", "application/json"),
                HttpRequest.BodyPublishers.ofString(body));
    }

    /** Multipart form: text fields, then the file part (null for none). */
    HttpResponse<String> multipart(String path, String token, List<String[]> fields, byte[] file) throws Exception {
        String boundary = "b" + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String[] f : fields) {
            out.writeBytes(("--"
                            + boundary
                            + "\r\nContent-Disposition: form-data; name=\""
                            + f[0]
                            + "\"\r\n\r\n"
                            + f[1]
                            + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        if (file != null) {
            out.writeBytes(("--"
                            + boundary
                            + "\r\nContent-Disposition: form-data; name=\"vocabularyFile\";"
                            + " filename=\"v.ttl\"\r\nContent-Type: text/turtle\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.writeBytes(file);
            out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(
                "POST",
                path,
                token,
                Map.of("Content-Type", "multipart/form-data; boundary=" + boundary),
                HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
    }

    static List<String[]> fields(String... kv) {
        List<String[]> l = new ArrayList<>();
        for (int i = 0; i < kv.length; i += 2) {
            l.add(new String[] {kv[i], kv[i + 1]});
        }
        return l;
    }

    static byte[] ttl(String body) {
        return ("@prefix ex: <https://example.org/test#> .\n"
                        + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                        + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                        + body)
                .getBytes(StandardCharsets.UTF_8);
    }

    HttpResponse<String> createVocabulary(String name, byte[] file) throws Exception {
        return multipart(
                "/v1/vocabularies",
                admin,
                fields("name", name, "description", "Description of " + name, "changelog", "Initial version."),
                file);
    }

    HttpResponse<String> createVersion(String name, String description, byte[] file) throws Exception {
        return multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                fields("description", description, "changelog", "Changed."),
                file);
    }

    static JsonNode body(HttpResponse<String> r) {
        return JSON.readTree(r.body());
    }
}
