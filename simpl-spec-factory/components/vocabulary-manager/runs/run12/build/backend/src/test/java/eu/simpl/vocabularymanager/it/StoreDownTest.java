package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** D44: a store where nothing listens gives 503 with Retry-After and GET /health 503 DOWN. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "vm.store.query-url=http://127.0.0.1:9/vocabulary/query",
            "vm.store.update-url=http://127.0.0.1:9/vocabulary/update"
        })
class StoreDownTest {

    static final TestIdp IDP = ApiTestBase.IDP;

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("vm.auth.issuer", () -> TestIdp.ISSUER);
        r.add("vm.auth.jwks-uri", IDP::jwksUri);
    }

    HttpResponse<String> call(String method, String path, String ct, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + IDP.token("ga-admin", "GA_VOCABULARY_ADMIN"));
        if (ct != null) {
            b.header("Content-Type", ct);
        }
        b.method(
                method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void storeUnavailable() throws Exception {
        HttpResponse<String> list = call("GET", "/v1/vocabularies", null, null);
        assertThat(list.statusCode()).isEqualTo(503);
        assertThat(list.headers().firstValue("Retry-After")).contains("5");
        assertThat(list.body()).contains("urn:problem-type:belgif:serviceUnavailable");
        assertThat(list.body()).contains("The vocabulary store is not available. Retry later.");
        String multipart = "--b\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\nAbcd\r\n"
                + "--b\r\nContent-Disposition: form-data; name=\"description\"\r\n\r\nd\r\n"
                + "--b\r\nContent-Disposition: form-data; name=\"changelog\"\r\n\r\nc\r\n"
                + "--b\r\nContent-Disposition: form-data; name=\"vocabularyFile\"; filename=\"a.ttl\"\r\n\r\n<a:b> <a:c> <a:d> .\r\n--b--\r\n";
        HttpResponse<String> create = call("POST", "/v1/vocabularies", "multipart/form-data; boundary=b", multipart);
        assertThat(create.statusCode()).isEqualTo(503);
        HttpResponse<String> health = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/health"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(health.statusCode()).isEqualTo(503);
        assertThat(health.body()).contains("DOWN");
        // after enough failures the breaker opens and answers at once (D103)
        for (int i = 0; i < 12; i++) {
            call("GET", "/v1/vocabularies/Abcd", null, null);
        }
        long start = System.nanoTime();
        assertThat(call("GET", "/v1/vocabularies/Abcd", null, null).statusCode())
                .isEqualTo(503);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(1000);
    }
}
