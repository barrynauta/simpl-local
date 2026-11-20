package eu.simpl.vocabularymanager.it;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base of the integration tests: the full application on a random port against the test store. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
public abstract class IntegrationTest {

    public static final String CONTENT_BASE = "https://authority.example/vocabulary-manager";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @LocalServerPort
    int port;

    protected ApiClient api;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vm.store.url", TestEnvironment::storeUrl);
        registry.add("vm.security.jwks-uri", TestEnvironment::jwksUrl);
        registry.add("vm.security.issuer-uri", () -> TestEnvironment.ISSUER);
        registry.add("vm.content-base", () -> CONTENT_BASE);
        registry.add("logging.structured.format.console", () -> "");
    }

    @BeforeEach
    void client() {
        api = new ApiClient(port);
    }

    /** A unique vocabulary name per test (the store is never reset, E4). */
    protected static String uniqueName(String prefix) {
        return prefix + "R" + UUID.randomUUID().toString().replace("-", "").substring(0, 10)
                + COUNTER.incrementAndGet();
    }

    protected static String ttl(String name) {
        return "@prefix ex: <https://example.org/" + name + "#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "ex:Scheme a skos:ConceptScheme ; skos:prefLabel \"" + name + "\"@en .\n";
    }

    protected ApiClient.Result create(String name, String content) {
        return api.post(
                "/v1/vocabularies",
                TestEnvironment.admin(),
                new ApiClient.Form()
                        .field("name", name)
                        .field("description", "Description of " + name)
                        .field("changelog", "Initial version.")
                        .file("vocabularyFile", content));
    }

    protected ApiClient.Result newVersion(String name, String description, String content) {
        return api.post(
                "/v1/vocabularies/" + name + "/versions",
                TestEnvironment.admin(),
                new ApiClient.Form()
                        .field("description", description)
                        .field("changelog", "Changed.")
                        .file("vocabularyFile", content));
    }
}
