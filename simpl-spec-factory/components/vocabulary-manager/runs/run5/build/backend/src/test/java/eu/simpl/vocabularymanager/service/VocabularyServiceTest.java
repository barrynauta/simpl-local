package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.support.InMemoryVocabularyStore;
import eu.simpl.vocabularymanager.support.TestProperties;
import eu.simpl.vocabularymanager.support.Turtle;
import eu.simpl.vocabularymanager.validation.ContentValidationException;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.ReferenceChecker;
import eu.simpl.vocabularymanager.validation.SemanticRules;
import eu.simpl.vocabularymanager.validation.TurtleParser;
import eu.simpl.vocabularymanager.web.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Lifecycle and state machine of D3 to D8, D43, D46 on an in-memory store. */
class VocabularyServiceTest {

    private final InMemoryVocabularyStore store = new InMemoryVocabularyStore();
    private final VocabularyProperties properties = TestProperties.create();
    private final ContentValidator validator =
            new ContentValidator(new TurtleParser(), new SemanticRules(), new ReferenceChecker(properties));
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:27:00.123Z"), ZoneOffset.UTC);
    private final VocabularyService service =
            new VocabularyService(store, validator, new NameLocks(properties), clock, properties);
    private final ExternalVocabularyService externals =
            new ExternalVocabularyService(store, validator, new NameLocks(properties), service, properties);

    private CreationResult create(String name) {
        return service.createVocabulary(name, "First", "Initial version.", Turtle.valid(name), "ga-admin@example.com");
    }

    private CreationResult newVersion(String name, String description) {
        return service.createVersion(name, description, "Changed.", Turtle.valid(name), "ga-admin@example.com");
    }

    private static int status(ApiException e) {
        return e.problem().status();
    }

    @Test
    void creationStoresVersionOneActiveWithUtcSecondTimes() {
        CreationResult result = create("Health");
        assertThat(result.version().version()).isEqualTo(1);
        assertThat(result.version().status()).isEqualTo(Status.ACTIVE);
        assertThat(result.version().creationDate()).isEqualTo("2026-11-18T09:27:00Z");
        assertThat(result.warnings()).isEmpty();
        assertThat(service.getVocabulary("Health").status()).isEqualTo(Status.ACTIVE);
        assertThat(service.getVocabulary("Health").createdBy()).isEqualTo("ga-admin@example.com");
    }

    @Test
    void duplicateNameIgnoringCaseIsConflictBeforeValidation() {
        create("Health");
        assertThatThrownBy(() -> service.createVocabulary("HEALTH", "d", "c", Turtle.bytes("bad {"), "u"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(e.problem().detail()).isEqualTo("Vocabulary with name 'HEALTH' already exists.");
                });
    }

    @Test
    void invalidContentStoresNothing() {
        assertThatThrownBy(() -> service.createVocabulary("Broken", "d", "c", Turtle.bytes("<a> <b> ."), "u"))
                .isInstanceOf(ContentValidationException.class);
        assertThat(store.writes).isZero();
        assertThat(store.findVocabulary("Broken")).isEmpty();
    }

    @Test
    void newVersionDeprecatesEveryOlderActiveVersionAndKeepsVocabularyStatus() {
        create("Health");
        newVersion("Health", "Second");
        service.updateVersionStatus("Health", 1, Status.ACTIVE, "admin");
        service.updateVocabularyStatus("Health", Status.DEPRECATED, "admin");
        CreationResult third = newVersion("Health", "Third");
        assertThat(third.version().version()).isEqualTo(3);
        assertThat(service.listVersions("Health"))
                .extracting(VocabularyVersion::version, VocabularyVersion::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, Status.DEPRECATED),
                        org.assertj.core.groups.Tuple.tuple(2, Status.DEPRECATED),
                        org.assertj.core.groups.Tuple.tuple(3, Status.ACTIVE));
        assertThat(service.getVocabulary("Health").status()).isEqualTo(Status.DEPRECATED);
        assertThat(service.getVocabulary("Health").latestDescription()).isEqualTo("Third");
    }

    @Test
    void identicalVersionIsConflictAndChangesNothing() {
        create("Health");
        int before = store.writes;
        assertThatThrownBy(() -> newVersion("Health", "First")).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(status(e)).isEqualTo(409);
            assertThat(e.problem().detail())
                    .isEqualTo("Version 1 of vocabulary 'Health' already has this content and description.");
        });
        assertThat(store.writes).isEqualTo(before);
        assertThat(newVersion("Health", "Only the description changed")
                        .version()
                        .version())
                .isEqualTo(2);
    }

    @Test
    void versionOfUnknownVocabularyIsNotFound() {
        assertThatThrownBy(() -> newVersion("Nope", "x"))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(status(e)).isEqualTo(404));
    }

    @Test
    void sameStatusIsConflict() {
        create("Health");
        assertThatThrownBy(() -> service.updateVocabularyStatus("Health", Status.ACTIVE, "a"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(e.problem().detail()).isEqualTo("Vocabulary is already in status ACTIVE");
                });
        service.updateVersionStatus("Health", 1, Status.DEPRECATED, "a");
        assertThatThrownBy(() -> service.updateVersionStatus("Health", 1, Status.DEPRECATED, "a"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.problem().detail())
                                .isEqualTo("Vocabulary version is already in status DEPRECATED"));
    }

    @Test
    void unknownVersionIsNotFoundWithFixedText() {
        create("Health");
        assertThatThrownBy(() -> service.getVersion("Health", 7)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(status(e)).isEqualTo(404);
            assertThat(e.problem().detail()).isEqualTo("Version 7 of vocabulary 'Health' was not found.");
        });
        assertThatThrownBy(() -> service.content("Health", 7)).isInstanceOf(ApiException.class);
        assertThat(service.latestContent("Health")).isEqualTo(Turtle.valid("Health"));
    }

    @Test
    void searchKeepsOrderDropsMissingAndRepeats() {
        create("Alpha");
        newVersion("Alpha", "Second");
        create("Beta");
        List<SearchResult> results = service.search(List.of(
                new SearchReference("Beta", null),
                new SearchReference("Missing", null),
                new SearchReference("Alpha", 1),
                new SearchReference("Alpha", 9),
                new SearchReference("Beta", null),
                new SearchReference("Alpha", 2),
                new SearchReference("Alpha", null)));
        assertThat(results)
                .containsExactly(
                        new SearchResult("Beta", 1, Status.ACTIVE, true),
                        new SearchResult("Alpha", 1, Status.DEPRECATED, false),
                        new SearchResult("Alpha", 2, Status.ACTIVE, false),
                        new SearchResult("Alpha", 2, Status.ACTIVE, true));
    }

    @Test
    void parallelVersionCreationsAreSerialised() throws Exception {
        create("Busy");
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            List<Future<CreationResult>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 5; i++) {
                String description = "Version " + i;
                futures.add(pool.submit(() -> newVersion("Busy", description)));
            }
            List<Integer> numbers = new java.util.ArrayList<>();
            for (Future<CreationResult> f : futures) {
                numbers.add(f.get().version().version());
            }
            assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5, 6);
        } finally {
            pool.shutdown();
        }
        assertThat(service.listVersions("Busy"))
                .filteredOn(v -> v.status() == Status.ACTIVE)
                .extracting(VocabularyVersion::version)
                .containsExactly(6);
    }

    @Test
    void externalRegistrationChecksNameNamespaceAndAliases() {
        ExternalVocabulary dc = new ExternalVocabulary(
                "DublinCore", "http://purl.org/dc/terms/", List.of("https://purl.org/dc/terms"), null, null, "u", null);
        ExternalVocabulary stored = externals.register(dc, Turtle.bytes("<http://a> <http://b> <http://c> ."));
        assertThat(stored.creationDate()).isEqualTo("2026-11-18T09:27:00Z");
        assertThatThrownBy(() -> externals.register(
                        new ExternalVocabulary("DUBLINCORE", "http://x.org/", List.of(), null, null, "u", null),
                        Turtle.bytes("")))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(status(e)).isEqualTo(409));
        assertThatThrownBy(() -> externals.register(
                        new ExternalVocabulary("Other", "https://purl.org/dc/terms#", List.of(), null, null, "u", null),
                        Turtle.bytes("")))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.problem().detail())
                                .isEqualTo(
                                        "An external vocabulary with namespace or alias 'https://purl.org/dc/terms#' is"
                                                + " already registered."));
        assertThatThrownBy(() -> externals.register(
                        new ExternalVocabulary("Broken", "http://broken.org/", List.of(), null, null, "u", null),
                        Turtle.bytes("<a> <b>")))
                .isInstanceOf(ContentValidationException.class);
        assertThat(externals.findByUri("http://purl.org/dc/terms")).isPresent();
        assertThat(externals.search(List.of("https://purl.org/dc/terms/"), List.of("http://purl.org/dc/terms")))
                .hasSize(1);
        assertThat(externals.content("DublinCore")).isNotEmpty();
        externals.delete("DublinCore", "u");
        assertThatThrownBy(() -> externals.get("DublinCore"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.problem().detail())
                                .isEqualTo("External vocabulary with name 'DublinCore' was not found."));
    }

    @Test
    void registeredExternalNamespaceSilencesTheWarning() {
        String turtle = "@prefix dc: <http://purl.org/dc/terms/> .\n<http://e/s> dc:title \"t\" .";
        assertThat(service.createVocabulary("Before", "d", "c", Turtle.bytes(turtle), "u")
                        .warnings())
                .hasSize(1);
        externals.register(
                new ExternalVocabulary("DublinCore", "http://purl.org/dc/terms", List.of(), null, null, "u", null),
                Turtle.bytes(""));
        assertThat(service.createVocabulary("After", "d", "c", Turtle.bytes(turtle), "u")
                        .warnings())
                .isEmpty();
    }
}
