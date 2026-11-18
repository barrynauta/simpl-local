package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.domain.WarningCode;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import eu.simpl.vocabularymanager.service.VocabularyCommands.CreateVersion;
import eu.simpl.vocabularymanager.service.VocabularyCommands.CreateVocabulary;
import eu.simpl.vocabularymanager.service.VocabularyCommands.Created;
import eu.simpl.vocabularymanager.service.VocabularyCommands.SearchReference;
import eu.simpl.vocabularymanager.service.VocabularyCommands.SearchResult;
import eu.simpl.vocabularymanager.store.FusekiExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.FusekiVocabularyRepository;
import eu.simpl.vocabularymanager.support.InMemorySparqlClient;
import eu.simpl.vocabularymanager.support.Turtle;
import eu.simpl.vocabularymanager.validation.ReferenceRules;
import eu.simpl.vocabularymanager.validation.SemanticRules;
import eu.simpl.vocabularymanager.validation.TurtleSyntaxValidator;
import eu.simpl.vocabularymanager.validation.VocabularyValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VocabularyServiceTest {

    private final InMemorySparqlClient store = new InMemorySparqlClient();
    private final FusekiVocabularyRepository repository = new FusekiVocabularyRepository(store);
    private final FusekiExternalVocabularyRepository externalRepository = new FusekiExternalVocabularyRepository(store);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-17T10:00:00.500Z"));
    private final Clock clock = new Clock() {
        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };
    private VocabularyService service;
    private ExternalVocabularyService externalService;
    private final Caller admin = new Caller("ga-admin@example.com");

    @BeforeEach
    void setUp() {
        VocabularyManagerProperties properties = new VocabularyManagerProperties(Turtle.BASE, 0, null, null);
        VocabularyValidator validator = new VocabularyValidator(
                new TurtleSyntaxValidator(), new SemanticRules(), new ReferenceRules(properties));
        VocabularyLocks locks = new VocabularyLocks();
        service = new VocabularyService(repository, externalRepository, validator, locks, clock, properties);
        externalService = new ExternalVocabularyService(externalRepository, validator, locks, clock);
    }

    private Created create(String name) {
        return service.create(
                new CreateVocabulary(name, "Description " + name, "Initial version.", Turtle.simple(name), admin));
    }

    private Created newVersion(String name, String label) {
        return service.createVersion(
                name, new CreateVersion("Description " + label, "Changed.", Turtle.simple(label), admin));
    }

    private static int status(VocabularyManagerException e) {
        return e.code().status();
    }

    @Test
    void createStoresVersionOneActiveWithCreatorAndSecondPrecisionDates() {
        Created created = create("Health");
        assertThat(created.version().version()).isEqualTo(1);
        assertThat(created.version().status()).isEqualTo(VocabularyStatus.ACTIVE);
        Vocabulary stored = service.get("Health");
        assertThat(stored.status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(stored.createdBy()).isEqualTo("ga-admin@example.com");
        assertThat(stored.creationDate()).isEqualTo(Instant.parse("2026-11-17T10:00:00Z"));
        assertThat(stored.latestVersion()).isEqualTo(1);
        assertThat(stored.description()).isEqualTo("Description Health");
        assertThat(service.latestContent("Health").bytes()).isEqualTo(Turtle.simple("Health"));
    }

    @Test
    void duplicateNameIgnoringCaseIsAConflictAndStoresNothing() {
        create("Health");
        int updates = store.updateCount();
        assertThatThrownBy(() -> create("HEALTH")).isInstanceOfSatisfying(VocabularyManagerException.class, e -> {
            assertThat(status(e)).isEqualTo(409);
            assertThat(e.detail()).isEqualTo("Vocabulary with name 'HEALTH' already exists.");
        });
        assertThat(store.updateCount()).isEqualTo(updates);
    }

    @Test
    void invalidContentStoresNothing() {
        assertThatThrownBy(() ->
                        service.create(new CreateVocabulary("Broken", "d", "c", Turtle.bytes("not turtle"), admin)))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(400));
        assertThat(store.updateCount()).isZero();
        assertThatThrownBy(() -> service.get("Broken")).isInstanceOf(VocabularyManagerException.class);
    }

    @Test
    void newVersionDeprecatesEveryOlderActiveVersion() {
        create("Health");
        now.set(Instant.parse("2026-11-17T11:00:00Z"));
        Created second = newVersion("Health", "two");
        assertThat(second.version().version()).isEqualTo(2);
        List<VocabularyVersion> versions = service.listVersions(
                        "Health", SortOrder.versions(null).orElseThrow(), new PageRequest(1, 20))
                .page()
                .items();
        assertThat(versions).extracting(VocabularyVersion::version).containsExactly(2, 1);
        assertThat(versions)
                .extracting(VocabularyVersion::status)
                .containsExactly(VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED);
        assertThat(versions.get(1).lastUpdateDate()).isEqualTo(Instant.parse("2026-11-17T11:00:00Z"));
        Vocabulary vocabulary = service.get("Health");
        assertThat(vocabulary.latestVersion()).isEqualTo(2);
        assertThat(vocabulary.description()).isEqualTo("Description two");
        assertThat(vocabulary.lastUpdateDate()).isEqualTo(Instant.parse("2026-11-17T11:00:00Z"));
        assertThat(vocabulary.creationDate()).isEqualTo(Instant.parse("2026-11-17T10:00:00Z"));
    }

    @Test
    void reactivatedVersionsAreAllDeprecatedByTheNextVersion() {
        create("Health");
        newVersion("Health", "two");
        service.updateVersionStatus("Health", 1, VocabularyStatus.ACTIVE);
        newVersion("Health", "three");
        assertThat(service.getVersion("Health", 1).version().status()).isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(service.getVersion("Health", 2).version().status()).isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(service.getVersion("Health", 3).version().status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(service.getVersion("Health", 1).latestVersion()).isEqualTo(3);
    }

    @Test
    void identicalVersionIsRefusedButDescriptionOnlyChangeIsAccepted() {
        create("Health");
        assertThatThrownBy(() -> service.createVersion(
                        "Health",
                        new CreateVersion("Description Health", "Other changelog", Turtle.simple("Health"), admin)))
                .isInstanceOfSatisfying(VocabularyManagerException.class, e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(e.detail())
                            .isEqualTo("Version 1 of vocabulary 'Health' already has this content and description.");
                });
        Created created = service.createVersion(
                "Health", new CreateVersion("New description", "Text only", Turtle.simple("Health"), admin));
        assertThat(created.version().version()).isEqualTo(2);
    }

    @Test
    void versionOfDeprecatedVocabularyKeepsVocabularyDeprecated() {
        create("Health");
        service.updateStatus("Health", VocabularyStatus.DEPRECATED);
        newVersion("Health", "two");
        assertThat(service.get("Health").status()).isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(service.getVersion("Health", 2).version().status()).isEqualTo(VocabularyStatus.ACTIVE);
    }

    @Test
    void statusChangesAndSameStatusConflicts() {
        create("Health");
        assertThatThrownBy(() -> service.updateStatus("Health", VocabularyStatus.ACTIVE))
                .isInstanceOfSatisfying(VocabularyManagerException.class, e -> {
                    assertThat(status(e)).isEqualTo(409);
                    assertThat(e.detail()).isEqualTo("Vocabulary is already in status ACTIVE");
                });
        now.set(Instant.parse("2026-11-18T09:00:00Z"));
        Vocabulary deprecated = service.updateStatus("Health", VocabularyStatus.DEPRECATED);
        assertThat(deprecated.status()).isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(deprecated.lastUpdateDate()).isEqualTo(Instant.parse("2026-11-18T09:00:00Z"));
        assertThat(service.getVersion("Health", 1).version().status()).isEqualTo(VocabularyStatus.ACTIVE);
        service.updateVersionStatus("Health", 1, VocabularyStatus.DEPRECATED);
        assertThatThrownBy(() -> service.updateVersionStatus("Health", 1, VocabularyStatus.DEPRECATED))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(e.detail()).isEqualTo("Vocabulary version is already in status DEPRECATED"));
    }

    @Test
    void unknownVocabularyOrVersionIsNotFound() {
        assertThatThrownBy(() -> service.get("Nothing"))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(404));
        create("Health");
        assertThatThrownBy(() -> service.getVersion("Health", 7))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(404));
        assertThatThrownBy(() -> service.versionContent("Health", 7)).isInstanceOf(VocabularyManagerException.class);
        assertThatThrownBy(() -> newVersion("Nothing", "x"))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(404));
    }

    @Test
    void parallelVersionCreationsAreSerialised() throws Exception {
        create("Health");
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String label = "parallel" + i;
            tasks.add(() -> newVersion("Health", label).version().version());
        }
        List<Integer> numbers = new ArrayList<>();
        for (Future<Integer> future : pool.invokeAll(tasks)) {
            numbers.add(future.get());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5, 6);
        List<VocabularyVersion> versions = service.listVersions(
                        "Health", SortOrder.versions(null).orElseThrow(), new PageRequest(1, 20))
                .page()
                .items();
        assertThat(versions).hasSize(6);
        assertThat(versions.stream().filter(v -> v.status() == VocabularyStatus.ACTIVE))
                .singleElement()
                .satisfies(v -> assertThat(v.version()).isEqualTo(6));
    }

    @Test
    void lockTimeoutGivesServiceUnavailable() throws Exception {
        VocabularyLocks locks = new VocabularyLocks(Duration.ofMillis(50));
        Thread holder = new Thread(() -> locks.withLock("Busy", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        holder.start();
        Thread.sleep(100);
        assertThatThrownBy(() -> locks.withLock("busy", () -> 1))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(503));
        holder.join();
    }

    @Test
    void searchKeepsRequestOrderDropsMissingAndRepeats() {
        create("Alpha");
        create("Bravo");
        newVersion("Bravo", "two");
        service.updateStatus("Bravo", VocabularyStatus.DEPRECATED);
        List<SearchResult> results = service.search(List.of(
                new SearchReference("Bravo", null),
                new SearchReference("Missing", null),
                new SearchReference("Bravo", 1),
                new SearchReference("Alpha", 1),
                new SearchReference("Bravo", null),
                new SearchReference("Bravo", 2),
                new SearchReference("Alpha", 9)));
        assertThat(results)
                .containsExactly(
                        new SearchResult("Bravo", 2, VocabularyStatus.DEPRECATED, true),
                        new SearchResult("Bravo", 1, VocabularyStatus.DEPRECATED, false),
                        new SearchResult("Alpha", 1, VocabularyStatus.ACTIVE, false),
                        new SearchResult("Bravo", 2, VocabularyStatus.ACTIVE, false));
    }

    @Test
    void listFiltersSortsAndPages() {
        create("Alpha");
        create("Bravo");
        create("Charlie");
        service.updateStatus("Bravo", VocabularyStatus.DEPRECATED);
        Page<Vocabulary> deprecated = service.list(
                new VocabularyFilter(null, VocabularyStatus.DEPRECATED, null, null),
                SortOrder.vocabularies(null).orElseThrow(),
                new PageRequest(1, 20));
        assertThat(deprecated.items()).extracting(Vocabulary::name).containsExactly("Bravo");
        Page<Vocabulary> page = service.list(
                VocabularyFilter.NONE, SortOrder.vocabularies("-name").orElseThrow(), new PageRequest(2, 2));
        assertThat(page.items()).extracting(Vocabulary::name).containsExactly("Alpha");
        assertThat(page.total()).isEqualTo(3);
    }

    @Test
    void deprecatedReferenceWarnsAndRegisteredExternalDoesNot() {
        create("Legacy");
        newVersion("Legacy", "two");
        externalService.register(new ExternalVocabularyService.Register(
                "DublinCore",
                "http://purl.org/dc/terms/",
                List.of("https://purl.org/dc/terms/"),
                null,
                null,
                Turtle.bytes("@prefix dcterms: <http://purl.org/dc/terms/> . dcterms:title a dcterms:Term ."),
                admin));
        String text = "@prefix legacy: <" + Turtle.BASE + "/content/Legacy/versions/1#> .\n"
                + "@prefix dcterms: <https://purl.org/dc/terms/> .\n"
                + "@prefix ex: <https://example.eu/user#> .\nex:a dcterms:title \"x\" .";
        Created created = service.create(new CreateVocabulary("User", "d", "c", Turtle.bytes(text), admin));
        assertThat(created.warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo(WarningCode.DEPRECATED_VOCABULARY_REFERENCE);
            assertThat(w.description()).endsWith("Use version 2, the latest ACTIVE version.");
        });
    }

    @Test
    void externalRegistrationConflictsSearchContentAndDelete() {
        externalService.register(new ExternalVocabularyService.Register(
                "DublinCore",
                "http://purl.org/dc/terms",
                List.of("https://purl.org/dc/terms"),
                "https://www.dublincore.org/terms.ttl",
                "Cached copy",
                Turtle.simple("dc"),
                admin));
        assertThatThrownBy(() -> externalService.register(new ExternalVocabularyService.Register(
                        "dublincore", "http://other.example/", List.of(), null, null, Turtle.simple("x"), admin)))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(409));
        assertThatThrownBy(() -> externalService.register(new ExternalVocabularyService.Register(
                        "Other", "https://purl.org/dc/terms/", List.of(), null, null, Turtle.simple("x"), admin)))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(409));
        assertThatThrownBy(() -> externalService.register(new ExternalVocabularyService.Register(
                        "Broken", "http://broken.example/", List.of(), null, null, Turtle.bytes("broken"), admin)))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(400));
        ExternalVocabulary found = externalService.get("DublinCore");
        assertThat(found.aliasUris()).containsExactly("https://purl.org/dc/terms");
        assertThat(found.sourceUri()).isEqualTo("https://www.dublincore.org/terms.ttl");
        assertThat(externalService.search(List.of("http://purl.org/dc/terms/", "https://purl.org/dc/terms#")))
                .extracting(ExternalVocabulary::name)
                .containsExactly("DublinCore");
        assertThat(externalService
                        .contentByNamespace("https://purl.org/dc/terms/")
                        .bytes())
                .isEqualTo(Turtle.simple("dc"));
        assertThat(externalService.list(new PageRequest(1, 20)).items()).hasSize(1);
        externalService.delete("DublinCore");
        assertThatThrownBy(() -> externalService.get("DublinCore"))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(404));
        assertThatThrownBy(() -> externalService.delete("DublinCore")).isInstanceOf(VocabularyManagerException.class);
        assertThatThrownBy(() -> externalService.contentByNamespace("http://purl.org/dc/terms"))
                .isInstanceOf(VocabularyManagerException.class);
    }

    @Test
    void storeDownGivesServiceUnavailable() {
        store.setDown(true);
        assertThatThrownBy(() -> service.list(
                        VocabularyFilter.NONE, SortOrder.vocabularies(null).orElseThrow(), new PageRequest(1, 20)))
                .isInstanceOfSatisfying(
                        VocabularyManagerException.class,
                        e -> assertThat(status(e)).isEqualTo(503));
    }
}
