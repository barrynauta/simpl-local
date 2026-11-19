package eu.simpl.vocabulary.api;

import eu.simpl.vocabulary.support.EmbeddedFuseki;
import eu.simpl.vocabulary.support.Http;
import eu.simpl.vocabulary.support.TestKeyConfig;
import eu.simpl.vocabulary.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Runs the real server on a random port against an embedded Fuseki, with test realm keys. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "spring.main.allow-bean-definition-overriding=true",
            "logging.structured.format.console=",
            "vocabulary.security.issuer-uri=" + TestTokens.ISSUER,
            "vocabulary.security.jwks-uri=http://127.0.0.1:1/certs"
        })
@Import(TestKeyConfig.class)
abstract class AbstractApiIT {

    @LocalServerPort
    int port;

    Http http;

    @DynamicPropertySource
    static void store(DynamicPropertyRegistry registry) {
        registry.add("vocabulary.store.url", EmbeddedFuseki::url);
    }

    @BeforeEach
    void client() {
        http = new Http(port);
        TestKeyConfig.IDP_DOWN.set(false);
    }

    static String unique(String prefix) {
        return prefix + Long.toString(System.nanoTime(), 36).toUpperCase().replaceAll("[^A-Z0-9]", "") + "x";
    }
}
