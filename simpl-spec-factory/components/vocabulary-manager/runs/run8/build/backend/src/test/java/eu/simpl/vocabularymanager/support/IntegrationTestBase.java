package eu.simpl.vocabularymanager.support;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.Api.Part;
import eu.simpl.vocabularymanager.support.Api.Result;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base of the API tests: the whole application on a random port against the embedded store and key set. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    protected static final String CONTENT_BASE = "https://authority.example/vocabulary-manager";
    protected static final TestEnvironment ENV = TestEnvironment.get();

    @LocalServerPort
    protected int port;

    protected Api api;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vocabulary-manager.store.url", ENV::storeUrl);
        registry.add("vocabulary-manager.store.read-password", () -> "unused");
        registry.add("vocabulary-manager.store.update-password", () -> "unused");
        registry.add("vocabulary-manager.identity.issuer-uri", () -> TestEnvironment.ISSUER);
        registry.add("vocabulary-manager.identity.jwks-uri", ENV::jwksUrl);
        registry.add("vocabulary-manager.identity.refresh-cooldown", () -> "0s");
        registry.add("vocabulary-manager.content-base-url", () -> CONTENT_BASE);
        registry.add("management.server.port", ENV::managementPort);
    }

    @BeforeEach
    void setUpApi() {
        api = new Api(port);
        ENV.identityProviderUp(true);
    }

    /**
     * @param suffix unique part
     * @return Turtle with one statement in its own namespace
     */
    protected static String simpleTurtle(String suffix) {
        return "@prefix ex: <https://example.org/" + suffix + "#> .\nex:a ex:b ex:c .\n";
    }

    /**
     * @param name vocabulary name
     * @param turtle content
     * @return the 201 result
     */
    protected Result create(String name, String turtle) {
        Result result = api.multipart(
                "/v1/vocabularies",
                ENV.admin(),
                Part.field("name", name),
                Part.field("description", "Description of " + name),
                Part.field("changelog", "Initial version."),
                Part.file(turtle));
        assertThat(result.status()).as(result.text()).isEqualTo(201);
        return result;
    }

    /**
     * @param name vocabulary name
     * @param description description
     * @param turtle content
     * @return the result
     */
    protected Result newVersion(String name, String description, String turtle) {
        return api.multipart(
                "/v1/vocabularies/" + name + "/versions",
                ENV.admin(),
                Part.field("description", description),
                Part.field("changelog", "Changed."),
                Part.file(turtle));
    }
}
