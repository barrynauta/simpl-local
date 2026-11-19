package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import eu.simpl.vocabularymanager.security.SecurityConfig;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Runs the real Tomcat connector (MockMvc skips it), so the container limits are tested as a client sees them:
 * an over-long text field gets the D37 400, a file over 5 MB gets the D15 413, and a non-standard method gets 405.
 * The store is unreachable; every case here is decided before the store is called (D51).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vm.store.dataset-url=http://127.0.0.1:1/vocabulary", "management.server.port=0"})
class TomcatLimitsIntegrationTest {

    private static final String BOUNDARY = "TomcatLimitsBoundary";

    @LocalServerPort
    private int port;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void adminToken() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("admin")
                .claim("email", "admin@example.com")
                .claim("realm_access", Map.of("roles", List.of(SecurityConfig.ADMIN)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
    }

    @Test
    void longDescriptionIsAFieldErrorNotATooLargeUpload() throws Exception {
        HttpResponse<String> response = create("a".repeat(70_000), new byte[] {'#'});
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("description");
    }

    @Test
    void fileOverFiveMegabytesIsTooLarge() throws Exception {
        HttpResponse<String> response = create("d", new byte[5_242_881]);
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("urn:problem-type:simpl:payloadTooLarge");
    }

    @Test
    void nonStandardMethodIsNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/vocabularies"))
                .header("Authorization", "Bearer token")
                .method("QUERY", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Allow")).hasValue("GET, POST");
    }

    private HttpResponse<String> create(String description, byte[] file) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        field(body, "name", "TomcatLimits");
        field(body, "description", description);
        field(body, "changelog", "Initial version.");
        body.write(
                ("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"vocabularyFile\"; filename=\"v.ttl\"\r\n"
                                + "Content-Type: text/turtle\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        body.write(file);
        body.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/vocabularies"))
                .header("Authorization", "Bearer token")
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void field(ByteArrayOutputStream body, String name, String value) {
        body.writeBytes(
                ("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
    }
}
