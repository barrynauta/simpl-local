package eu.simpl.vocabularymanager.support;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import java.time.Duration;

/** Properties for unit tests. */
public final class TestProperties {

    public static final String CONTENT_BASE = "https://authority.example/vocabulary-manager";

    private TestProperties() {}

    public static VocabularyProperties create(String storeUrl, String jwksUrl) {
        return new VocabularyProperties(
                CONTENT_BASE,
                5_242_880L,
                Duration.ofSeconds(2),
                new VocabularyProperties.Store(
                        storeUrl, Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(2)),
                new VocabularyProperties.Identity(
                        "http://issuer.test/realms/simpl",
                        jwksUrl,
                        "vocabulary-manager-be",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(30)));
    }

    public static VocabularyProperties create() {
        return create("http://127.0.0.1:1/vocabulary", "http://127.0.0.1:1/certs");
    }
}
