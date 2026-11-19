package eu.simpl.vocabulary.api;

import static eu.simpl.vocabulary.support.Http.field;
import static eu.simpl.vocabulary.support.Http.file;
import static eu.simpl.vocabulary.support.Http.parts;
import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabulary.support.Http;
import eu.simpl.vocabulary.support.TestKeyConfig;
import eu.simpl.vocabulary.support.TestTokens;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** The store is unreachable (D44): 503 with Retry-After, health DOWN. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "spring.main.allow-bean-definition-overriding=true",
            "logging.structured.format.console=",
            "vocabulary.security.issuer-uri=" + TestTokens.ISSUER,
            "vocabulary.store.url=http://127.0.0.1:1/vocabulary"
        })
@Import(TestKeyConfig.class)
class StoreDownIT {

    @LocalServerPort
    int port;

    @Test
    void storeDownGives503() {
        Http http = new Http(port);
        HttpResponse<String> list = http.get("/v1/vocabularies", TestTokens.viewer());
        assertThat(list.statusCode()).isEqualTo(503);
        assertThat(list.headers().firstValue("Retry-After")).contains("5");
        assertThat(list.body())
                .contains("urn:problem-type:belgif:serviceUnavailable")
                .contains("The vocabulary store is not available. Retry later.");
        HttpResponse<String> create = http.multipart(
                "/v1/vocabularies",
                TestTokens.admin(),
                parts(
                        field("name", "StoreDown"),
                        field("description", "d"),
                        field("changelog", "c"),
                        file("vocabularyFile", "<http://a/b> <http://a/c> <http://a/d> .")));
        assertThat(create.statusCode()).isEqualTo(503);
        HttpResponse<String> health = http.get("/v1/health", null);
        assertThat(health.statusCode()).isEqualTo(503);
        assertThat(health.body()).contains("DOWN");
    }
}
