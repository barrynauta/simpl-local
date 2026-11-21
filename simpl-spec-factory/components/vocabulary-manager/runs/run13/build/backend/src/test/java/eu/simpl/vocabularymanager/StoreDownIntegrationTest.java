package eu.simpl.vocabularymanager;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.ApiClient.Response;
import eu.simpl.vocabularymanager.support.TestKeys;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** D44, D103: the store is unreachable; operations answer 503 with Retry-After and health is DOWN. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreDownIntegrationTest {

    private static final TestKeys KEYS = new TestKeys("down-key");

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vm.store.query-url", () -> "http://127.0.0.1:1/vocabulary/query");
        registry.add("vm.store.update-url", () -> "http://127.0.0.1:1/vocabulary/update");
        registry.add("vm.identity.issuer", () -> TestKeys.ISSUER);
        registry.add("vm.identity.jwks-uri", KEYS::jwksUri);
        registry.add("management.server.port", () -> "0");
    }

    @AfterAll
    static void stop() {
        KEYS.stop();
    }

    @Test
    void readsWritesAndHealthReportTheStoreOutage() {
        ApiClient api = new ApiClient(port);
        String admin = KEYS.token("ga-admin", List.of("GA_VOCABULARY_ADMIN"));
        Response list = api.get("/v1/vocabularies", admin);
        assertThat(list.status()).isEqualTo(503);
        assertThat(list.header("Retry-After")).isEqualTo("5");
        assertThat(list.json().get("type").asString()).isEqualTo("urn:problem-type:belgif:serviceUnavailable");
        assertThat(list.json().get("detail").asString())
                .isEqualTo("The vocabulary store is not available. Retry later.");
        Response create = api.multipart(
                "/v1/vocabularies",
                admin,
                ApiClient.createParts("DownStore", "<http://a> <http://b> <http://c> .", "D"));
        assertThat(create.status()).isEqualTo(503);
        Response health = api.get("/v1/health", null);
        assertThat(health.status()).isEqualTo(503);
        assertThat(health.json().get("status").asString()).isEqualTo("DOWN");
        for (int i = 0; i < 12; i++) {
            api.get("/v1/vocabularies/Abc", admin);
        }
        Response open = api.get("/v1/vocabularies/Abc", admin);
        assertThat(open.status()).as("breaker open, still 503").isEqualTo(503);
        assertThat(api.get("/v1/vocabularies", null).status())
                .as("401 before the store")
                .isEqualTo(401);
    }
}
