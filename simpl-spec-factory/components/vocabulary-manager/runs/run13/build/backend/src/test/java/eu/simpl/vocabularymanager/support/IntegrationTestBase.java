package eu.simpl.vocabularymanager.support;

import eu.simpl.vocabularymanager.VocabularyManagerApplication;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the application on a random port against an embedded Fuseki and a local JWKS endpoint. Both
 * are shared by all integration tests; each test uses unique names, as the suite does (E4).
 */
@SpringBootTest(
        classes = VocabularyManagerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    protected static final TestStore STORE = new TestStore();
    protected static final TestKeys KEYS = new TestKeys("test-key-1");
    protected static final String CONTENT_BASE = "https://authority.example/vocabulary-manager";

    @LocalServerPort
    protected int port;

    protected ApiClient api;
    protected String admin;
    protected String viewer;
    protected String norole;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vm.store.query-url", STORE::queryUrl);
        registry.add("vm.store.update-url", STORE::updateUrl);
        registry.add("vm.identity.issuer", () -> TestKeys.ISSUER);
        registry.add("vm.identity.jwks-uri", KEYS::jwksUri);
        registry.add("vm.content-base-url", () -> CONTENT_BASE);
        registry.add("management.server.port", () -> "0");
    }

    @BeforeEach
    void setUpClient() {
        api = new ApiClient(port);
        admin = KEYS.token("ga-admin", List.of("GA_VOCABULARY_ADMIN"));
        viewer = KEYS.token("ga-viewer", List.of("GA_VOCABULARY_VIEWER"));
        norole = KEYS.token("ga-norole", List.of());
    }

    /** A unique vocabulary name for one test. */
    protected static String uniqueName(String prefix) {
        return prefix
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    protected static String simpleTurtle(String marker) {
        return "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix ex: <https://example.eu/" + marker + "#> .\n"
                + "ex:Scheme a skos:ConceptScheme ;\n  skos:prefLabel \"Scheme " + marker + "\"@en .\n";
    }
}
