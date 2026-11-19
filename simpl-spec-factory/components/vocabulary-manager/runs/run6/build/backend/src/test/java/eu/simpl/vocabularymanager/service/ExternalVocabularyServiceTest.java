package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.problem.ProblemException;
import eu.simpl.vocabularymanager.support.InMemoryStore;
import eu.simpl.vocabularymanager.support.TestProperties;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.SemanticRules;
import eu.simpl.vocabularymanager.validation.TurtleParser;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Decisions D30, D38, D41, D42 and D56. */
class ExternalVocabularyServiceTest {

    private static final byte[] DC =
            "@prefix d: <http://purl.org/dc/terms/> .\nd:title a d:Term .\n".getBytes(StandardCharsets.UTF_8);

    private ExternalVocabularyService service;

    @BeforeEach
    void setUp() {
        InMemoryStore store = new InMemoryStore();
        ContentValidator validator =
                new ContentValidator(new TurtleParser(), new SemanticRules(), store, TestProperties.properties());
        service = new ExternalVocabularyService(
                store, validator, new VocabularyLocks(TestProperties.properties()), Clock.systemUTC());
    }

    private ExternalVocabulary register(String name, String namespace, List<String> aliases) {
        return service.register(name, namespace, aliases, null, null, DC, "a");
    }

    private static int status(Runnable r) {
        try {
            r.run();
            return 0;
        } catch (ProblemException e) {
            return e.status();
        }
    }

    @Test
    void registerLookupAndRemove() {
        register("DublinCore", "http://purl.org/dc/terms/", List.of("https://purl.org/dc/terms"));
        assertThat(service.get("DublinCore").namespaceUri()).isEqualTo("http://purl.org/dc/terms/");
        assertThat(service.byNamespace("https://purl.org/dc/terms/").name()).isEqualTo("DublinCore");
        assertThat(service.contentByName("DublinCore")).isEqualTo(DC);
        assertThat(service.list(0, 20).total()).isEqualTo(1);
        service.remove("DublinCore", "a");
        assertThat(status(() -> service.get("DublinCore"))).isEqualTo(404);
        assertThat(status(() -> service.remove("DublinCore", "a"))).isEqualTo(404);
        assertThat(status(() -> service.byNamespace("http://purl.org/dc/terms")))
                .isEqualTo(404);
        assertThat(status(() -> service.contentByName("DublinCore"))).isEqualTo(404);
    }

    @Test
    void conflictsOnNameAndUris() {
        register("DublinCore", "http://purl.org/dc/terms/", List.of("https://purl.org/dc/terms"));
        assertThat(status(() -> register("DUBLINCORE", "http://other/", List.of())))
                .isEqualTo(409);
        assertThatThrownBy(() -> register("Other", "https://purl.org/dc/terms/", List.of()))
                .isInstanceOfSatisfying(
                        ProblemException.class,
                        e -> assertThat(e.problem().detail())
                                .isEqualTo("An external vocabulary with namespace or alias 'https://purl.org/dc/terms/'"
                                        + " is already registered."));
    }

    @Test
    void invalidTurtleIsRejected() {
        assertThat(status(() -> service.register("Broken", "http://b/", List.of(), null, null, "x y".getBytes(), "a")))
                .isEqualTo(400);
    }

    @Test
    void searchKeepsFirstMatchOrderAndDeduplicates() {
        register("Alpha", "http://a/", List.of("http://a2/"));
        register("Beta", "http://b#", List.of());
        List<ExternalVocabulary> found = service.search(List.of("http://b", "http://a2#", "http://a/", "http://none"));
        assertThat(found).extracting(ExternalVocabulary::name).containsExactly("Beta", "Alpha");
    }
}
