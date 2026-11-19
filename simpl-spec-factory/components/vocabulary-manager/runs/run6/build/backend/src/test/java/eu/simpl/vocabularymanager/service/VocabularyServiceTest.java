package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.problem.ProblemException;
import eu.simpl.vocabularymanager.store.SortOrder;
import eu.simpl.vocabularymanager.support.InMemoryStore;
import eu.simpl.vocabularymanager.support.TestProperties;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.SemanticRules;
import eu.simpl.vocabularymanager.validation.TurtleParser;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Lifecycle and state machine of decisions D3 to D8, D43 and D46. */
class VocabularyServiceTest {

    private static final byte[] V1 = turtle("one");
    private static final byte[] V2 = turtle("two");

    private InMemoryStore store;
    private VocabularyService service;

    private static byte[] turtle(String label) {
        return ("@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix ex: <https://example.eu/x#> .\n"
                        + "ex:s skos:prefLabel \"" + label + "\"@en .\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        ContentValidator validator =
                new ContentValidator(new TurtleParser(), new SemanticRules(), store, TestProperties.properties());
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:27:00.789Z"), ZoneOffset.UTC);
        service = new VocabularyService(store, validator, new VocabularyLocks(TestProperties.properties()), clock);
    }

    private static int status(Runnable action) {
        try {
            action.run();
            return 0;
        } catch (ProblemException e) {
            return e.status();
        }
    }

    @Test
    void createGivesVersionOneActiveWithTimesToTheSecond() {
        VocabularyService.Created created = service.createVocabulary("Health", "D", "Initial version.", V1, "ga@x");
        assertThat(created.version().version()).isEqualTo(1);
        assertThat(created.version().status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(created.version().creationDate()).isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(service.getVocabulary("Health").status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(service.getVocabulary("Health").createdBy()).isEqualTo("ga@x");
    }

    @Test
    void duplicateNameIsCaseInsensitiveConflictEvenWithInvalidContent() {
        service.createVocabulary("Health", "D", "C", V1, "a");
        assertThatThrownBy(() -> service.createVocabulary("HEALTH", "D", "C", "bad".getBytes(), "a"))
                .isInstanceOfSatisfying(ProblemException.class, e -> {
                    assertThat(e.status()).isEqualTo(409);
                    assertThat(e.problem().detail()).isEqualTo("Vocabulary with name 'HEALTH' already exists.");
                });
    }

    @Test
    void invalidContentStoresNothing() {
        assertThat(status(() -> service.createVocabulary("Bad", "D", "C", "x y".getBytes(), "a")))
                .isEqualTo(400);
        assertThat(store.findVocabulary("Bad")).isEmpty();
    }

    @Test
    void newVersionDeprecatesEveryOlderActiveVersionAndKeepsVocabularyStatus() {
        service.createVocabulary("Health", "D", "C", V1, "a");
        service.changeVocabularyStatus("Health", VocabularyStatus.DEPRECATED, "a");
        VocabularyService.Created v2 = service.createVersion("Health", "D2", "C2", V2, "b");
        assertThat(v2.version().version()).isEqualTo(2);
        assertThat(store.findVersion("Health", 1).orElseThrow().status()).isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(service.getVocabulary("Health").status()).isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(service.getVocabulary("Health").latestDescription()).isEqualTo("D2");
    }

    @Test
    void identicalContentAndDescriptionIsAConflictButChangelogIsIgnored() {
        service.createVocabulary("Health", "D", "C", V1, "a");
        assertThatThrownBy(() -> service.createVersion("Health", "D", "Other changelog", V1, "a"))
                .isInstanceOfSatisfying(
                        ProblemException.class,
                        e -> assertThat(e.problem().detail())
                                .isEqualTo(
                                        "Version 1 of vocabulary 'Health' already has this content and description."));
        assertThat(service.createVersion("Health", "New description", "C", V1, "a")
                        .version()
                        .version())
                .isEqualTo(2);
    }

    @Test
    void versionOfUnknownVocabularyIsNotFound() {
        assertThat(status(() -> service.createVersion("Nope", "D", "C", V1, "a")))
                .isEqualTo(404);
    }

    @Test
    void statusChangesFollowTheStateTable() {
        service.createVocabulary("Health", "D", "C", V1, "a");
        assertThat(status(() -> service.changeVocabularyStatus("Health", VocabularyStatus.ACTIVE, "a")))
                .isEqualTo(409);
        assertThat(service.changeVocabularyStatus("Health", VocabularyStatus.DEPRECATED, "a")
                        .status())
                .isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(store.findVersion("Health", 1).orElseThrow().status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(service.changeVocabularyStatus("Health", VocabularyStatus.ACTIVE, "a")
                        .status())
                .isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(status(() -> service.changeVocabularyStatus("Nope", VocabularyStatus.ACTIVE, "a")))
                .isEqualTo(404);
    }

    @Test
    void versionStatusChangeAllowsTwoActiveVersions() {
        service.createVocabulary("Health", "D", "C", V1, "a");
        service.createVersion("Health", "D2", "C", V2, "a");
        VocabularyVersion v1 = service.changeVersionStatus("Health", "1", VocabularyStatus.ACTIVE, "a");
        assertThat(v1.status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(store.findVersion("Health", 2).orElseThrow().status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThatThrownBy(() -> service.changeVersionStatus("Health", "2", VocabularyStatus.ACTIVE, "a"))
                .isInstanceOfSatisfying(
                        ProblemException.class,
                        e -> assertThat(e.problem().detail())
                                .isEqualTo("Vocabulary version is already in status ACTIVE"));
        assertThat(status(() -> service.changeVersionStatus("Health", "9", VocabularyStatus.ACTIVE, "a")))
                .isEqualTo(404);
        assertThat(status(() ->
                        service.changeVersionStatus("Health", "99999999999999999999", VocabularyStatus.ACTIVE, "a")))
                .isEqualTo(404);
    }

    @Test
    void readsAndSearch() {
        service.createVocabulary("Health", "D", "C", V1, "a");
        service.createVersion("Health", "D2", "C", V2, "a");
        assertThat(service.latestContent("Health")).isEqualTo(V2);
        assertThat(service.versionContent("Health", "1")).isEqualTo(V1);
        assertThat(service.listVersions("Health", new SortOrder(SortOrder.Field.VERSION, true), 0, 100)
                        .total())
                .isEqualTo(2);
        assertThat(status(() -> service.getVersion("Health", "3"))).isEqualTo(404);
        assertThat(status(() -> service.listVersions("Nope", new SortOrder(SortOrder.Field.VERSION, true), 0, 1)))
                .isEqualTo(404);
        List<VocabularyService.SearchResult> results = service.search(List.of(
                new VocabularyService.SearchReference("Health", null),
                new VocabularyService.SearchReference("Health", 1L),
                new VocabularyService.SearchReference("Nope", null),
                new VocabularyService.SearchReference("Health", 7L)));
        assertThat(results)
                .containsExactly(
                        new VocabularyService.SearchResult("Health", 2, VocabularyStatus.ACTIVE, true),
                        new VocabularyService.SearchResult("Health", 1, VocabularyStatus.DEPRECATED, false));
    }

    @Test
    void parallelVersionCreationsAreSerialised() throws Exception {
        service.createVocabulary("Busy", "D", "C", V1, "a");
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            byte[] content = turtle("parallel " + i);
            tasks.add(() -> service.createVersion("Busy", "D", "C", content, "a")
                    .version()
                    .version());
        }
        List<Integer> numbers = new ArrayList<>();
        for (Future<Integer> f : pool.invokeAll(tasks)) {
            numbers.add(f.get());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5, 6);
        assertThat(store.findVersion("Busy", 6).orElseThrow().status()).isEqualTo(VocabularyStatus.ACTIVE);
        for (int n = 1; n <= 5; n++) {
            assertThat(store.findVersion("Busy", n).orElseThrow().status()).isEqualTo(VocabularyStatus.DEPRECATED);
        }
    }

    @Test
    void sha256IsHex() {
        assertThat(VocabularyService.sha256(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
