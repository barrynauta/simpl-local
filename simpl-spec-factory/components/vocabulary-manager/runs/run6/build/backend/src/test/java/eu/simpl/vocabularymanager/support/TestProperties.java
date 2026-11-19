package eu.simpl.vocabularymanager.support;

import eu.simpl.vocabularymanager.config.AppProperties;
import java.time.Duration;

/** Configuration values for unit tests. */
public final class TestProperties {

    public static final String BASE = "https://authority.example/vocabulary-manager";

    private TestProperties() {}

    public static AppProperties properties() {
        return properties("http://127.0.0.1:1/vocabulary");
    }

    public static AppProperties properties(String datasetUrl) {
        return new AppProperties(
                BASE,
                new AppProperties.Store(
                        datasetUrl,
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(2)),
                new AppProperties.Identity(
                        "http://issuer",
                        "vocabulary-manager-be",
                        "http://127.0.0.1:1/certs",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)),
                new AppProperties.Limits(5_242_880L, 262_144, 1000));
    }
}
