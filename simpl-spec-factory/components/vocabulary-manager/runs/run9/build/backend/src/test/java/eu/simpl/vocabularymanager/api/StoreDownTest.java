package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.Http;
import eu.simpl.vocabularymanager.support.TestIdentityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** D44: with the store URL on a port where nothing listens, reads and writes give 503 and health is DOWN. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreDownTest {

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vm.store.query-url", () -> "http://127.0.0.1:1/vocabulary/query");
        registry.add("vm.store.update-url", () -> "http://127.0.0.1:1/vocabulary/update");
        registry.add("vm.store.read-password", () -> "x");
        registry.add("vm.store.update-password", () -> "x");
        registry.add("vm.identity.jwk-set-uri", () -> TestIdentityProvider.get().jwkSetUri());
        registry.add("vm.identity.issuer-uri", () -> TestIdentityProvider.ISSUER);
        registry.add("management.server.port", () -> "0");
        registry.add("logging.structured.format.console", () -> "");
    }

    @Test
    void storeDownGives503() {
        Http http = new Http("http://localhost:" + port);
        String token = TestIdentityProvider.get().token("ga-admin", "GA_VOCABULARY_ADMIN");
        Http.Response list = http.get("/v1/vocabularies", token);
        assertThat(list.status()).isEqualTo(503);
        assertThat(list.header("Retry-After")).isEqualTo("5");
        assertThat(list.json().get("type").asString()).isEqualTo("urn:problem-type:belgif:serviceUnavailable");
        assertThat(list.json().get("detail").asString())
                .isEqualTo("The vocabulary store is not available. Retry later.");
        Http.Response create = http.multipart(
                "/v1/vocabularies",
                token,
                new Http.Multipart()
                        .file("@prefix e: <http://e.org/> . e:a e:b e:c .")
                        .field("name", "StoreDown")
                        .field("description", "d")
                        .field("changelog", "c"));
        assertThat(create.status()).isEqualTo(503);
        assertThat(http.get("/v1/health", null).status()).isEqualTo(503);
        assertThat(http.get("/v1/health", null).json().get("status").asString()).isEqualTo("DOWN");
    }
}
