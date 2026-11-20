package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The store is unreachable (D44, D103): 503 with Retry-After, health DOWN, nothing stored. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
class StoreDownIT {

    @LocalServerPort
    int port;

    private ApiClient api;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vm.store.url", () -> "http://127.0.0.1:1/vocabulary");
        registry.add("vm.security.jwks-uri", TestEnvironment::jwksUrl);
        registry.add("vm.security.issuer-uri", () -> TestEnvironment.ISSUER);
        registry.add("logging.structured.format.console", () -> "");
    }

    @BeforeEach
    void client() {
        api = new ApiClient(port);
    }

    @Test
    void readsWritesAndHealthReportTheStoreAsUnavailable() {
        ApiClient.Result list = api.get("/v1/vocabularies", TestEnvironment.viewer());
        assertThat(list.status()).isEqualTo(503);
        assertThat(list.header("Retry-After")).isEqualTo("5");
        assertThat(list.type()).isEqualTo("urn:problem-type:belgif:serviceUnavailable");
        assertThat(list.json().get("detail").asString())
                .isEqualTo("The vocabulary store is not available. Retry later.");
        ApiClient.Result create = api.post(
                "/v1/vocabularies",
                TestEnvironment.admin(),
                new ApiClient.Form()
                        .field("name", "Abcd")
                        .field("description", "d")
                        .field("changelog", "c")
                        .file("vocabularyFile", "<a> <b> <c> ."));
        assertThat(create.status()).isEqualTo(503);
        ApiClient.Result health = api.get("/v1/health", null);
        assertThat(health.status()).isEqualTo(503);
        assertThat(health.json().get("status").asString()).isEqualTo("DOWN");
        for (int i = 0; i < 12; i++) {
            api.get("/v1/vocabularies", TestEnvironment.viewer());
        }
        long start = System.nanoTime();
        assertThat(api.get("/v1/vocabularies", TestEnvironment.viewer()).status())
                .isEqualTo(503);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(500);
    }
}
