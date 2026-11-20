package eu.simpl.vocabularymanager.support;

import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base of the API integration tests: the real backend on a random port, an in-process Fuseki with the deployed
 * access lists, and a test identity provider. Each test uses its own unique vocabulary names, so no reset is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    protected static final TestStore STORE = new TestStore();
    protected static final TestIdentityProvider IDP = new TestIdentityProvider();
    protected static final String CONTENT_BASE = "https://authority.example/vocabulary-manager";

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        register(registry, STORE, IDP);
    }

    public static void register(DynamicPropertyRegistry registry, TestStore store, TestIdentityProvider idp) {
        registry.add("vm.store.url", store::url);
        registry.add("vm.store.read-user", () -> TestStore.READ_USER);
        registry.add("vm.store.read-password", () -> TestStore.READ_PASSWORD);
        registry.add("vm.store.update-user", () -> TestStore.UPDATE_USER);
        registry.add("vm.store.update-password", () -> TestStore.UPDATE_PASSWORD);
        registry.add("vm.security.issuer-uri", () -> TestIdentityProvider.ISSUER);
        registry.add("vm.security.jwks-uri", idp::jwksUri);
        registry.add("vm.content-base-url", () -> CONTENT_BASE);
        registry.add("management.server.port", () -> "0");
        registry.add("logging.structured.format.console", () -> "");
    }

    protected ApiClient api() {
        return new ApiClient(port);
    }

    /** A unique valid vocabulary name. */
    protected static String uniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    protected static String simpleTurtle(String name) {
        return "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix ex: <https://example.org/" + name + "#> .\n"
                + "ex:Scheme a skos:ConceptScheme ;\n  skos:prefLabel \"" + name + "\"@en .\n";
    }
}
