package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.WarningCode;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.support.Turtle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VocabularyValidatorTest {

    private static final String BASE = Turtle.BASE + "/content/Own/versions/1";
    private final VocabularyManagerProperties properties = new VocabularyManagerProperties(Turtle.BASE, 0, null, null);
    private final VocabularyValidator validator =
            new VocabularyValidator(new TurtleSyntaxValidator(), new SemanticRules(), new ReferenceRules(properties));
    private final Map<String, InternalVocabularyState> internal = new HashMap<>();
    private final Set<String> external = new java.util.HashSet<>();
    private final ReferenceLookup lookup = new ReferenceLookup() {
        @Override
        public Optional<InternalVocabularyState> internalVocabulary(String name) {
            return Optional.ofNullable(internal.get(name));
        }

        @Override
        public boolean isRegisteredExternal(String namespace) {
            return external.contains(eu.simpl.vocabularymanager.domain.Namespaces.normalise(namespace));
        }
    };

    private ValidationOutcome validate(String text) {
        return validator.validate(Turtle.bytes(text), BASE, "Own", lookup);
    }

    @Test
    void validContentPassesWithoutErrorsOrWarnings() {
        ValidationOutcome outcome = validator.validate(Turtle.simple("Test"), BASE, "Own", lookup);
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void syntaxErrorStopsValidationWithOneIssueAndLocation() {
        ValidationOutcome outcome =
                validate("@prefix ex: <http://example.org/> .\nex:a ex:b ex:c ;;; .\nex:x ex:y \"open");
        assertThat(outcome.errors()).hasSize(1);
        Issue issue = outcome.errors().get(0);
        assertThat(issue.detail()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issue.location()).matches("line \\d+, column \\d+");
        assertThat(issue.name()).isEqualTo("vocabularyFile");
        assertThat(issue.in()).isEqualTo("body");
        assertThat(issue.type()).isEqualTo(Issue.INVALID_INPUT);
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void syntaxErrorReportsTheLineOfTheError() {
        ValidationOutcome outcome = validate("@prefix ex: <http://example.org/> .\n\nex:a ex:b .\n");
        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).location()).startsWith("line 3,");
    }

    @Test
    void undefinedPrefixIsASyntaxError() {
        ValidationOutcome outcome = validate("nope:a nope:b nope:c .");
        assertThat(outcome.errors())
                .singleElement()
                .satisfies(i -> assertThat(i.detail()).startsWith("Encountered invalid Turtle syntax"));
    }

    @Test
    void emptyContentGivesS6() {
        ValidationOutcome outcome = validate("");
        assertThat(outcome.errors()).singleElement().satisfies(i -> {
            assertThat(i.detail()).isEqualTo(SemanticRules.S6);
            assertThat(i.location()).isEqualTo("document");
        });
    }

    @Test
    void allSemanticRulesRunAndReturnEveryError() {
        String text = """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix ex: <http://example.org/> .
                ex:objProp a owl:ObjectProperty .
                ex:dataProp a owl:DatatypeProperty .
                ex:both a owl:ObjectProperty , owl:DatatypeProperty .
                ex:A owl:disjointWith ex:B .
                ex:subject ex:objProp "invalid-literal" .
                ex:subject ex:dataProp ex:resource .
                ex:subject skos:prefLabel "One"@en , "Two"@en , "Un"@fr .
                ex:thing a ex:A , ex:B .
                """;
        ValidationOutcome outcome = validate(text);
        List<String> details = outcome.errors().stream().map(Issue::detail).toList();
        assertThat(details)
                .containsExactly(
                        SemanticRules.S1, SemanticRules.S2, SemanticRules.S3, SemanticRules.S4, SemanticRules.S5);
        assertThat(outcome.errors().get(0).location())
                .isEqualTo("statement 'http://example.org/subject http://example.org/objProp invalid-literal'");
        assertThat(outcome.errors().get(1).location())
                .isEqualTo(
                        "statement 'http://example.org/subject http://example.org/dataProp http://example.org/resource'");
        assertThat(outcome.errors().get(2).location()).isEqualTo("resource 'http://example.org/both'");
        assertThat(outcome.errors().get(3).location()).isEqualTo("resource 'http://example.org/subject'");
        assertThat(outcome.errors().get(4).location()).isEqualTo("resource 'http://example.org/thing'");
    }

    @Test
    void blankNodeObjectOfDatatypePropertyIsS2() {
        String text = """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix ex: <http://example.org/> .
                ex:p a owl:DatatypeProperty .
                ex:s ex:p [ ex:q "x" ] .
                """;
        assertThat(validate(text).errors()).singleElement().satisfies(i -> {
            assertThat(i.detail()).isEqualTo(SemanticRules.S2);
            assertThat(i.location()).contains("_:");
        });
    }

    @Test
    void labelsInDifferentLanguagesAreFine() {
        String text = """
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix ex: <http://example.org/> .
                ex:s skos:prefLabel "One"@en , "Un"@fr .
                ex:s a ex:Thing .
                """;
        assertThat(validate(text).errors()).isEmpty();
    }

    @Test
    void missingInternalVocabularyIsS7() {
        ValidationOutcome outcome = validate("@prefix m: <" + Turtle.BASE + "/content/Missing/versions/2#> .\n"
                + "@prefix ex: <http://example.org/> .\nex:a ex:b ex:c .");
        assertThat(outcome.errors()).singleElement().satisfies(i -> {
            assertThat(i.detail()).isEqualTo("Referenced internal vocabulary 'Missing' version 2 does not exist.");
            assertThat(i.location()).isEqualTo("prefix 'm' -> '" + Turtle.BASE + "/content/Missing/versions/2#'");
        });
    }

    @Test
    void missingInternalVersionIsS7AndNameOnlyReferenceToMissingNameToo() {
        internal.put(
                "Known",
                new InternalVocabularyState("Known", VocabularyStatus.ACTIVE, Map.of(1, VocabularyStatus.ACTIVE)));
        ValidationOutcome outcome = validate("@prefix k: <" + Turtle.BASE + "/content/Known/versions/5/> .\n"
                + "@prefix u: <" + Turtle.BASE + "/content/Unknown> .\n"
                + "@prefix ex: <http://example.org/> .\nex:a ex:b ex:c .");
        assertThat(outcome.errors())
                .extracting(Issue::detail)
                .containsExactly(
                        "Referenced internal vocabulary 'Known' version 5 does not exist.",
                        "Referenced internal vocabulary 'Unknown' does not exist.");
    }

    @Test
    void referenceToDeprecatedVersionWarnsWithLatestActiveVersion() {
        internal.put(
                "Legacy",
                new InternalVocabularyState(
                        "Legacy",
                        VocabularyStatus.ACTIVE,
                        Map.of(1, VocabularyStatus.DEPRECATED, 2, VocabularyStatus.ACTIVE)));
        ValidationOutcome outcome = validate("@prefix legacy: <" + Turtle.BASE + "/content/Legacy/versions/1#> .\n"
                + "@prefix ex: <http://example.org/> .\nex:a ex:b ex:c .");
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo(WarningCode.DEPRECATED_VOCABULARY_REFERENCE);
            assertThat(w.description())
                    .isEqualTo("Referenced internal vocabulary 'Legacy' version 1 is deprecated."
                            + " Use version 2, the latest ACTIVE version.");
            assertThat(w.location()).isEqualTo("prefix 'legacy' -> '" + Turtle.BASE + "/content/Legacy/versions/1#'");
        });
    }

    @Test
    void referenceWithoutVersionToDeprecatedVocabularyWarnsWithoutAdviceWhenNoActiveVersion() {
        internal.put(
                "Old",
                new InternalVocabularyState(
                        "Old", VocabularyStatus.DEPRECATED, Map.of(1, VocabularyStatus.DEPRECATED)));
        ValidationOutcome outcome = validate("@prefix old: <" + Turtle.BASE
                + "/content/Old#> .\n@prefix ex: <http://example.org/> .\nex:a ex:b ex:c .");
        assertThat(outcome.warnings())
                .singleElement()
                .satisfies(w -> assertThat(w.description())
                        .isEqualTo("Referenced internal vocabulary 'Old' version 1 is deprecated."));
    }

    @Test
    void referenceToActiveVersionGivesNoWarning() {
        internal.put(
                "Fresh",
                new InternalVocabularyState("Fresh", VocabularyStatus.ACTIVE, Map.of(1, VocabularyStatus.ACTIVE)));
        ValidationOutcome outcome = validate("@prefix f: <" + Turtle.BASE
                + "/content/Fresh/> .\n@prefix ex: <http://example.org/> .\nex:a ex:b ex:c .");
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void referenceToOwnVocabularyIsIgnored() {
        ValidationOutcome outcome =
                validate("@prefix own: <" + Turtle.BASE + "/content/Own/versions/9#> .\n" + "own:a own:b own:c .");
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void externalNamespacesWarnUnlessRegisteredW3cOrOwn() {
        external.add("http://purl.org/dc/terms");
        String text = """
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix dcterms: <http://purl.org/dc/terms/> .
                @prefix own: <https://own.example/vocab#> .
                @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                own:a own:b own:c .
                """;
        ValidationOutcome outcome = validate(text);
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo(WarningCode.EXTERNAL_REFERENCE_SKIPPED);
            assertThat(w.description())
                    .isEqualTo("Cannot resolve external namespace 'http://xmlns.com/foaf/0.1/' because no cached"
                            + " external vocabulary is registered.");
            assertThat(w.location()).isEqualTo("prefix 'foaf' -> 'http://xmlns.com/foaf/0.1/'");
        });
    }

    @Test
    void syntaxOnlyValidationIgnoresSemantics() {
        assertThat(validator.validateSyntax(Turtle.bytes(""), BASE).hasErrors()).isFalse();
        assertThat(validator.validateSyntax(Turtle.bytes("broken"), BASE).hasErrors())
                .isTrue();
    }

    @Test
    void deprecatedDescriptionWithoutActiveVersion() {
        assertThat(ReferenceRules.deprecatedDescription("X", 3, java.util.OptionalInt.empty()))
                .isEqualTo("Referenced internal vocabulary 'X' version 3 is deprecated.");
    }

    @Test
    void internalStateKnowsLatestAndLatestActive() {
        InternalVocabularyState state = new InternalVocabularyState(
                "V",
                VocabularyStatus.ACTIVE,
                Map.of(1, VocabularyStatus.ACTIVE, 2, VocabularyStatus.ACTIVE, 3, VocabularyStatus.DEPRECATED));
        assertThat(state.latestVersion()).isEqualTo(3);
        assertThat(state.latestActiveVersion()).hasValue(2);
        assertThat(state.isEffectivelyDeprecated(3)).isTrue();
        assertThat(state.isEffectivelyDeprecated(2)).isFalse();
    }
}
