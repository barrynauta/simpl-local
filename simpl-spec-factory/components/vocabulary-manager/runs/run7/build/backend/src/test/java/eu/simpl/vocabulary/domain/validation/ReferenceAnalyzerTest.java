package eu.simpl.vocabulary.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.External;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.Internal;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.InternalFacts;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.ReferenceFacts;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Vocabulary references (D11, D12, D47, D69). */
class ReferenceAnalyzerTest {

    static final String BASE = "https://authority.example/vocabulary-manager";
    final ReferenceAnalyzer analyzer = new ReferenceAnalyzer(BASE);

    static PrefixDeclaration p(String prefix, String namespace) {
        return new PrefixDeclaration(prefix, namespace);
    }

    @Test
    void classifiesReferences() {
        List<ReferenceAnalyzer.Reference> references = analyzer.classify(
                List.of(
                        p("a", BASE + "/content/Health"),
                        p("b", BASE + "/content/Health/versions/2#"),
                        p("c", BASE + "/content/Health/versions/x/"),
                        p("own", BASE + "/content/Self/versions/1#"),
                        p("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
                        p("skos", "http://www.w3.org/2004/02/skos/core#"),
                        p("mine", "http://mine.org/"),
                        p("lower", BASE + "/content/lower#"),
                        p("empty", BASE + "/content/"),
                        p("deep", BASE + "/content/Health/other#"),
                        p("dc", "http://purl.org/dc/terms/")),
                "Self",
                Set.of("http://mine.org/thing"));
        assertThat(references).hasSize(7);
        assertThat(references.get(0)).isEqualTo(new Internal(p("a", BASE + "/content/Health"), "Health", null));
        assertThat(((Internal) references.get(1)).rawVersion()).isEqualTo("2");
        assertThat(((Internal) references.get(2)).rawVersion()).isEqualTo("x");
        assertThat(references.subList(3, 7)).allMatch(External.class::isInstance);
        assertThat(((External) references.get(6)).key()).isEqualTo("http://purl.org/dc/terms");
        assertThat(ReferenceAnalyzer.internalNames(references)).containsExactly("Health");
    }

    static InternalFacts facts(String name, VocabularyStatus status, VocabularyStatus... versions) {
        TreeMap<Long, VocabularyStatus> map = new TreeMap<>();
        for (int i = 0; i < versions.length; i++) {
            map.put((long) i + 1, versions[i]);
        }
        return new InternalFacts(name, status, map);
    }

    @Test
    void deprecatedReferenceRecommendsLatestActiveVersion() {
        ReferenceFacts store = new ReferenceFacts(
                Map.of(
                        "Health",
                        facts("Health", VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED, VocabularyStatus.ACTIVE)),
                Set.of());
        ReferenceAnalyzer.Outcome outcome = analyzer.check(
                analyzer.classify(List.of(p("h", BASE + "/content/Health/versions/1#")), "X", Set.of()), store);
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.warnings())
                .containsExactly(new ValidationWarning(
                        ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE,
                        "Referenced internal vocabulary 'Health' version 1 is deprecated. Use version 2, the latest ACTIVE version.",
                        "prefix 'h' -> '" + BASE + "/content/Health/versions/1#'"));
    }

    @Test
    void deprecatedVocabularyRecommendsNothing() {
        ReferenceFacts store = new ReferenceFacts(
                Map.of("Health", facts("Health", VocabularyStatus.DEPRECATED, VocabularyStatus.ACTIVE)), Set.of());
        ReferenceAnalyzer.Outcome outcome =
                analyzer.check(analyzer.classify(List.of(p("h", BASE + "/content/Health")), "X", Set.of()), store);
        assertThat(outcome.warnings().get(0).description())
                .isEqualTo(
                        "Referenced internal vocabulary 'Health' version 1 is deprecated. The vocabulary 'Health' is deprecated.");
    }

    @Test
    void deprecatedWithoutOtherActiveVersionHasNoRecommendation() {
        ReferenceFacts store = new ReferenceFacts(
                Map.of("Health", facts("Health", VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED)), Set.of());
        ReferenceAnalyzer.Outcome outcome = analyzer.check(
                analyzer.classify(List.of(p("h", BASE + "/content/Health/versions/1")), "X", Set.of()), store);
        assertThat(outcome.warnings().get(0).description())
                .isEqualTo("Referenced internal vocabulary 'Health' version 1 is deprecated.");
    }

    @Test
    void missingReferencesAreErrors() {
        ReferenceFacts store = new ReferenceFacts(
                Map.of("Health", facts("Health", VocabularyStatus.ACTIVE, VocabularyStatus.ACTIVE)), Set.of());
        ReferenceAnalyzer.Outcome outcome = analyzer.check(
                analyzer.classify(
                        List.of(
                                p("a", BASE + "/content/Health/versions/5#"),
                                p("b", BASE + "/content/Other#"),
                                p("c", BASE + "/content/Other/versions/1#"),
                                p("d", BASE + "/content/Health/versions/0#"),
                                p("e", BASE + "/content/Health#")),
                        "X",
                        Set.of()),
                store);
        assertThat(outcome.errors())
                .extracting(ValidationError::detail)
                .containsExactly(
                        "Referenced internal vocabulary 'Health' version 5 does not exist.",
                        "Referenced internal vocabulary 'Other' does not exist.",
                        "Referenced internal vocabulary 'Other' version 1 does not exist.",
                        "Referenced internal vocabulary 'Health' version 0 does not exist.");
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void registeredExternalNamespacesGiveNoWarning() {
        ReferenceAnalyzer.Outcome outcome = analyzer.check(
                analyzer.classify(
                        List.of(p("dc", "http://purl.org/dc/terms/"), p("x", "http://unknown.org/")), "X", Set.of()),
                new ReferenceFacts(Map.of(), Set.of("http://purl.org/dc/terms")));
        assertThat(outcome.warnings())
                .extracting(ValidationWarning::code)
                .containsExactly(ValidationWarning.EXTERNAL_REFERENCE_SKIPPED);
    }

    @Test
    void latestActiveIsTheHighestActiveVersion() {
        TreeMap<Long, VocabularyStatus> versions = new TreeMap<>(
                Map.of(1L, VocabularyStatus.ACTIVE, 2L, VocabularyStatus.ACTIVE, 3L, VocabularyStatus.DEPRECATED));
        assertThat(ReferenceAnalyzer.latestActive(versions)).contains(2L);
        assertThat(ReferenceAnalyzer.latestActive(new TreeMap<>())).isEmpty();
    }
}
