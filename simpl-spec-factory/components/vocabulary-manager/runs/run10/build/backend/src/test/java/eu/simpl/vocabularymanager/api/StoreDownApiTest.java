package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import eu.simpl.vocabularymanager.support.TestIdentityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** D44: with the store URL on a port where nothing listens, reads and writes give 503 and health is DOWN. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreDownApiTest {

    private static final TestIdentityProvider IDP = new TestIdentityProvider();

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vm.store.url", () -> "http://127.0.0.1:9/vocabulary");
        registry.add("vm.security.issuer-uri", () -> TestIdentityProvider.ISSUER);
        registry.add("vm.security.jwks-uri", IDP::jwksUri);
        registry.add("management.server.port", () -> "0");
        registry.add("logging.structured.format.console", () -> "");
    }

    @Test
    void storeDownGives503WithRetryAfter() {
        ApiClient api = new ApiClient(port);
        ApiClient.Response list = api.get("/v1/vocabularies", IDP.viewer());
        assertThat(list.status()).isEqualTo(503);
        assertThat(list.header("Retry-After")).isEqualTo("5");
        assertThat(list.header("Content-Type")).isEqualTo("application/problem+json");
        assertThat(list.json().get("type").asString()).isEqualTo("urn:problem-type:belgif:serviceUnavailable");
        assertThat(list.json().get("title").asString()).isEqualTo("Service Unavailable");
        assertThat(list.json().get("detail").asString())
                .isEqualTo("The vocabulary store is not available. Retry later.");

        ApiClient.Response create = api.multipart(
                "/v1/vocabularies",
                IDP.admin(),
                ApiClient.createParts("Down" + System.nanoTime(), "D", "@prefix a: <urn:a#> . a:b a:c a:d ."));
        assertThat(create.status()).isEqualTo(503);

        ApiClient.Response health = api.get("/v1/health", null);
        assertThat(health.status()).isEqualTo(503);
        assertThat(health.json().get("status").asString()).isEqualTo("DOWN");

        for (int i = 0; i < 6; i++) {
            api.get("/v1/vocabularies", IDP.viewer());
        }
        long start = System.nanoTime();
        assertThat(api.get("/v1/vocabularies/Abcd", IDP.viewer()).status()).isEqualTo(503);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(500);
        assertThat(api.get("/v1/vocabularies", null).status()).isEqualTo(401);
        assertThat(IntegrationTestBase.class).isNotNull();
    }
}
