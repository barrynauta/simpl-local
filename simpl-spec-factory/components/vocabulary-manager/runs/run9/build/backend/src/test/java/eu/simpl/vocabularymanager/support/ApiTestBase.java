package eu.simpl.vocabularymanager.support;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base of the API tests: the real application on a random port, embedded Fuseki, test realm keys. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiTestBase {

    /** Admin role. */
    protected static final String ADMIN = "GA_VOCABULARY_ADMIN";

    /** Viewer role. */
    protected static final String VIEWER = "GA_VOCABULARY_VIEWER";

    /** Content base of the tests. */
    protected static final String CONTENT_BASE = "https://authority.example/vocabulary-manager";

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final String RUN = Long.toString(System.currentTimeMillis() % 100000);

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vm.store.query-url", TestFuseki::queryUrl);
        registry.add("vm.store.update-url", TestFuseki::updateUrl);
        registry.add("vm.store.read-password", () -> "test");
        registry.add("vm.store.update-password", () -> "test");
        registry.add("vm.identity.jwk-set-uri", () -> TestIdentityProvider.get().jwkSetUri());
        registry.add("vm.identity.issuer-uri", () -> TestIdentityProvider.ISSUER);
        registry.add("management.server.port", () -> "0");
        registry.add("logging.structured.format.console", () -> "");
    }

    /** @return a client of the API */
    protected Http http() {
        return new Http("http://localhost:" + port);
    }

    /** @return an admin token */
    protected static String admin() {
        return TestIdentityProvider.get().token("ga-admin", ADMIN);
    }

    /** @return a viewer token */
    protected static String viewer() {
        return TestIdentityProvider.get().token("ga-viewer", VIEWER);
    }

    /** @return a token without roles */
    protected static String norole() {
        return TestIdentityProvider.get().token("ga-norole");
    }

    /** A unique vocabulary name. */
    protected static String uniqueName(String prefix) {
        return prefix + "R" + RUN + "N" + COUNTER.incrementAndGet();
    }

    /** Minimal valid content with its own namespace. */
    protected static String content(String name, String extra) {
        return "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix ex: <https://example.eu/" + name
                + "#> .\nex:Scheme a skos:ConceptScheme ; skos:prefLabel \"" + name + "\"@en .\n" + extra;
    }

    /** Creates a vocabulary and returns the response. */
    protected Http.Response create(String name, String turtle) {
        return http().multipart(
                        "/v1/vocabularies",
                        admin(),
                        new Http.Multipart()
                                .file(turtle)
                                .field("name", name)
                                .field("description", "Description of " + name)
                                .field("changelog", "Initial version."));
    }

    /** Creates a new version and returns the response. */
    protected Http.Response newVersion(String name, String turtle, String description) {
        return http().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        admin(),
                        new Http.Multipart()
                                .file(turtle)
                                .field("description", description)
                                .field("changelog", "Changed."));
    }
}
