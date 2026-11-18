package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.HttpApi;
import eu.simpl.vocabularymanager.support.HttpApi.Result;
import eu.simpl.vocabularymanager.support.TestServers;
import eu.simpl.vocabularymanager.support.TestTokens;
import eu.simpl.vocabularymanager.support.Turtle;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** D44: the store URL points at a port where nothing listens. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreDownIT {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vocabulary.store.url", () -> "http://127.0.0.1:1/vocabulary");
        registry.add("vocabulary.identity.jwks-url", TestServers::jwksUrl);
        registry.add("vocabulary.identity.issuer", () -> TestTokens.ISSUER);
        registry.add("logging.structured.format.console", () -> "");
    }

    @LocalServerPort
    private int port;

    @Test
    void readsWritesAndHealthReportTheStoreAsUnavailable() {
        HttpApi api = new HttpApi("http://127.0.0.1:" + port + "/v1");
        Result list = api.get("/vocabularies", TestServers.TOKENS.viewer());
        assertThat(list.status()).isEqualTo(503);
        assertThat(list.header("Retry-After")).isEqualTo("5");
        assertThat(list.json().get("type").asString()).isEqualTo("urn:problem-type:belgif:serviceUnavailable");
        assertThat(list.json().get("detail").asString())
                .isEqualTo("The vocabulary store is not available. Retry later.");
        Result create = api.post(
                "/vocabularies",
                TestServers.TOKENS.admin(),
                new HttpApi.Form()
                        .field("name", "Down")
                        .field("description", "d")
                        .field("changelog", "c")
                        .file("vocabularyFile", "d.ttl", Turtle.valid("Down")));
        assertThat(create.status()).isEqualTo(503);
        Result health = api.get("/health", null);
        assertThat(health.status()).isEqualTo(503);
        assertThat(health.json().get("status").asString()).isEqualTo("DOWN");
    }
}
