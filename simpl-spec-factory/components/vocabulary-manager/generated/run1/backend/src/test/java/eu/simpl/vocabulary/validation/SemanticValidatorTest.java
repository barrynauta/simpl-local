package eu.simpl.vocabulary.validation;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabulary.domain.ExternalVocabulary;
import eu.simpl.vocabulary.domain.Status;
import eu.simpl.vocabulary.domain.Vocabulary;
import eu.simpl.vocabulary.domain.VocabularyLifecycle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemanticValidatorTest {

    private static final String BASE = "https://vocabulary.simpl.local/vocabularies";
    private static final Instant T = Instant.parse("2026-01-01T00:00:00Z");

    private final TurtleSyntaxValidator parser = new TurtleSyntaxValidator();
    private final SemanticValidator validator = new SemanticValidator(BASE);
    private final FakeResolver resolver = new FakeResolver();

    @BeforeEach
    void setUp() {
        // Common: v1 DEPRECATED, v2 ACTIVE. Legacy: deprecated vocabulary.
        Vocabulary common = VocabularyLifecycle.addVersion(
                VocabularyLifecycle.create("Common", "d", "c", "u", T), "d", "c", "u", T.plusSeconds(1));
        resolver.vocabularies.put("Common", common);
        resolver.terms.put("Common:1", Set.of("Person"));
        resolver.terms.put("Common:2", Set.of("Person", "Address"));
        Vocabulary legacy = VocabularyLifecycle.changeStatus(
                VocabularyLifecycle.create("Legacy", "d", "c", "u", T), Status.DEPRECATED, T.plusSeconds(1));
        resolver.vocabularies.put("Legacy", legacy);
        resolver.terms.put("Legacy:1", Set.of("Thing"));
    }

    private SemanticValidator.Result run(String ttl) {
        var parsed = parser.parse(ttl, BASE + "/Self/1");
        assertThat(parsed.errors()).isEmpty();
        return validator.validate("Self", parsed.graph(), resolver);
    }

    @Test
    void referenceToActiveVersionWithExistingTermPasses() { // SIMPL-18184 scenario 1
        var r = run("""
                @prefix c: <%s/Common/2#> .
                <https://e.org/x> a c:Person .
                """.formatted(BASE));
        assertThat(r.errors()).isEmpty();
    }

    @Test
    void referenceToDeprecatedVersionIsAnErrorWithRecommendation() { // SIMPL-18184 scenario 2, D17 (b)
        var r = run("""
                @prefix c: <%s/Common/1#> .
                <https://e.org/x> a c:Person .
                """.formatted(BASE));
        assertThat(r.errors()).singleElement().satisfies(e -> {
            assertThat(e.detail()).contains("'Common' version 1 is DEPRECATED").contains("latest ACTIVE version 2");
            assertThat(e.location()).isEqualTo("prefix 'c' -> '" + BASE + "/Common/1#'");
        });
    }

    @Test
    void referenceToDeprecatedVocabularyWithoutActiveVersion() {
        var r = run("@prefix l: <%s/Legacy#> .\n<https://e.org/x> a l:Thing .".formatted(BASE));
        assertThat(r.errors()).singleElement()
                .satisfies(e -> assertThat(e.detail()).contains("No ACTIVE version of 'Legacy'"));
    }

    @Test
    void missingVocabularyAndMissingVersionAreErrors() { // SIMPL-18184 scenario 3, D17 (c)
        var r = run("""
                <https://e.org/x> a <%1$s/Nothing/1#A> .
                <https://e.org/y> a <%1$s/Common/7#A> .
                """.formatted(BASE));
        assertThat(r.errors()).extracting(ValidationIssue::detail)
                .anyMatch(d -> d.contains("'Nothing' does not exist"))
                .anyMatch(d -> d.contains("'Common' version 7 does not exist"));
    }

    @Test
    void unknownTermIsAnError() { // D17 (a)
        var r = run("<https://e.org/x> a <%s/Common/2#Unicorn> .".formatted(BASE));
        assertThat(r.errors()).singleElement()
                .satisfies(e -> assertThat(e.detail()).contains("Term 'Unicorn' does not exist"));
    }

    @Test
    void latestReferenceResolvesToLatestVersion() { // D18
        var r = run("<https://e.org/x> a <%s/Common#Address> .".formatted(BASE));
        assertThat(r.errors()).isEmpty();
    }

    @Test
    void ownNamespaceIsNotAReference() {
        var r = run("<%1$s/Self/1#A> a <%1$s/Self/1#B> .".formatted(BASE));
        assertThat(r.errors()).isEmpty();
    }

    @Test
    void classAndPropertyAtOnceIsAnError() { // D17 (d)
        var r = run("""
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                <https://e.org/x> a owl:Class , owl:ObjectProperty .
                """);
        assertThat(r.errors()).singleElement()
                .satisfies(e -> assertThat(e.detail()).contains("both as a class and as a property"));
    }

    @Test
    void unregisteredExternalNamespaceIsAWarning() { // D17 warnings, SIMPL-18183 note
        var r = run("""
                @prefix dcterms: <http://purl.org/dc/terms/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                <https://e.org/o> a owl:Ontology ; dcterms:title "x" .
                """);
        assertThat(r.errors()).isEmpty();
        assertThat(r.warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo("externalReferenceSkipped");
            assertThat(w.location()).isEqualTo("prefix 'dcterms' -> 'http://purl.org/dc/terms/'");
        });
    }

    @Test
    void registeredExternalAndBuiltInNamespacesGiveNoWarning() {
        resolver.externals.add(new ExternalVocabulary("DublinCore", "http://purl.org/dc/terms", List.of(), null,
                null, "u", T));
        var r = run("""
                @prefix dcterms: <http://purl.org/dc/terms/> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                <https://e.org/s> a skos:ConceptScheme ; dcterms:title "x" ; rdfs:label "y" .
                """);
        assertThat(r.warnings()).isEmpty();
    }

    static class FakeResolver implements ReferenceResolver {
        final Map<String, Vocabulary> vocabularies = new HashMap<>();
        final Map<String, Set<String>> terms = new HashMap<>();
        final List<ExternalVocabulary> externals = new ArrayList<>();

        @Override
        public Optional<Vocabulary> findVocabulary(String name) {
            return Optional.ofNullable(vocabularies.get(name));
        }

        @Override
        public Set<String> localNamesOf(String name, int version) {
            return terms.getOrDefault(name + ":" + version, Set.of());
        }

        @Override
        public List<ExternalVocabulary> externalVocabularies() {
            return externals;
        }
    }
}
