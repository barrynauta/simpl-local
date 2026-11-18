package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.support.TestProperties;
import eu.simpl.vocabularymanager.support.Turtle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Syntax and semantic validation, D9 to D12, D47, D53, D57. */
class ContentValidatorTest {

    private static final String CB = TestProperties.CONTENT_BASE + "/content/";
    private static final String BASE = CB + "Self/versions/1";

    private final ContentValidator validator = new ContentValidator(
            new TurtleParser(), new SemanticRules(), new ReferenceChecker(TestProperties.create()));
    private final Map<String, InternalVocabularyState> stored = new HashMap<>();
    private Set<String> externalKeys = Set.of();

    private ReferenceContext context() {
        return new ReferenceContext("Self", name -> Optional.ofNullable(stored.get(name)), externalKeys);
    }

    private List<ValidationWarning> validate(String turtle) {
        return validator.validate(Turtle.bytes(turtle), BASE, context());
    }

    private List<ValidationError> errorsOf(String turtle) {
        try {
            validate(turtle);
        } catch (ContentValidationException e) {
            return e.errors();
        }
        throw new AssertionError("expected validation errors");
    }

    private void store(String name, Status vocabularyStatus, Status... versionStatuses) {
        List<VocabularyVersion> versions = new java.util.ArrayList<>();
        for (int i = 0; i < versionStatuses.length; i++) {
            versions.add(new VocabularyVersion(name, i + 1, versionStatuses[i], "d", "c", "u", "t", "t"));
        }
        stored.put(
                name,
                new InternalVocabularyState(
                        new Vocabulary(name, vocabularyStatus, "u", "t", "t", versionStatuses.length, "d"), versions));
    }

    @Test
    void validContentPassesWithoutWarnings() {
        assertThat(validate(new String(Turtle.valid("Self")))).isEmpty();
    }

    @Test
    void syntaxErrorGivesOneIssueWithLineAndColumnAndStopsSemanticValidation() {
        List<ValidationError> errors = errorsOf("@prefix ex: <http://e/> .\nex:a ex:b .\n");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).detail()).startsWith("Encountered invalid Turtle syntax");
        assertThat(errors.get(0).location()).matches("line 2, column \\d+");
    }

    @Test
    void undefinedPrefixIsASyntaxError() {
        List<ValidationError> errors = errorsOf("nope:a nope:b nope:c .");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).location()).startsWith("line 1");
    }

    @Test
    void emptyContentIsS6() {
        assertThat(errorsOf("")).containsExactly(new ValidationError(SemanticRules.S6, "document"));
        assertThat(errorsOf("@prefix ex: <http://e/> ."))
                .extracting(ValidationError::detail)
                .contains(SemanticRules.S6);
    }

    @Test
    void allSemanticRulesRunAndReportEveryError() {
        String turtle = """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix ex: <http://example.org/> .
                ex:op a owl:ObjectProperty . ex:dp a owl:DatatypeProperty .
                ex:both a owl:ObjectProperty , owl:DatatypeProperty .
                ex:s ex:op "invalid-literal" . ex:s ex:dp ex:o . ex:s ex:dp [ ex:x 1 ] .
                ex:s skos:prefLabel "a"@en , "b"@EN .
                ex:C1 owl:disjointWith ex:C2 . ex:i a ex:C1 , ex:C2 .
                """;
        List<ValidationError> errors = errorsOf(turtle);
        assertThat(errors)
                .extracting(ValidationError::detail)
                .containsExactly(
                        SemanticRules.S1,
                        SemanticRules.S2,
                        SemanticRules.S2,
                        SemanticRules.S3,
                        SemanticRules.S4,
                        SemanticRules.S5);
        assertThat(errors.get(0).location())
                .isEqualTo("statement 'http://example.org/s http://example.org/op invalid-literal'");
        assertThat(errors.get(3).location()).isEqualTo("resource 'http://example.org/both'");
        assertThat(errors.get(4).location()).isEqualTo("resource 'http://example.org/s'");
        assertThat(errors.get(5).location()).isEqualTo("resource 'http://example.org/i'");
    }

    @Test
    void untaggedLabelsCountAsOneLanguage() {
        String twoUntagged = Turtle.SKOS + "<http://e/s> skos:prefLabel \"a\" , \"b\" .";
        assertThat(errorsOf(twoUntagged)).extracting(ValidationError::detail).containsExactly(SemanticRules.S4);
        String mixed = Turtle.SKOS + "<http://e/s> skos:prefLabel \"a\" , \"b\"@en .";
        assertThat(validate(mixed)).isEmpty();
    }

    @Test
    void disjointnessWorksInBothDirections() {
        String turtle = """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                <http://e/B> owl:disjointWith <http://e/A> . <http://e/x> a <http://e/A> , <http://e/B> .
                """;
        assertThat(errorsOf(turtle)).extracting(ValidationError::detail).containsExactly(SemanticRules.S5);
    }

    @Test
    void missingInternalVocabularyIsS7() {
        String turtle = "@prefix a: <" + CB + "Missing#> .\n@prefix b: <" + CB + "Missing/versions/3/> .\n"
                + "<http://e/s> <http://e/p> <http://e/o> .";
        assertThat(errorsOf(turtle))
                .containsExactly(
                        new ValidationError(
                                "Referenced internal vocabulary 'Missing' does not exist.",
                                "prefix 'a' -> '" + CB + "Missing#'"),
                        new ValidationError(
                                "Referenced internal vocabulary 'Missing' version 3 does not exist.",
                                "prefix 'b' -> '" + CB + "Missing/versions/3/'"));
    }

    @Test
    void missingVersionOfExistingVocabularyIsS7() {
        store("Other", Status.ACTIVE, Status.ACTIVE);
        String turtle = "@prefix o: <" + CB + "Other/versions/2#> .\n<http://e/s> <http://e/p> <http://e/o> .";
        assertThat(errorsOf(turtle))
                .extracting(ValidationError::detail)
                .containsExactly("Referenced internal vocabulary 'Other' version 2 does not exist.");
    }

    @Test
    void deprecatedVersionReferenceWarnsAndRecommendsLatestActive() {
        store("Other", Status.ACTIVE, Status.DEPRECATED, Status.ACTIVE);
        String turtle = "@prefix o: <" + CB + "Other/versions/1#> .\n<http://e/s> <http://e/p> <http://e/o> .";
        assertThat(validate(turtle))
                .containsExactly(new ValidationWarning(
                        "deprecatedVocabularyReference",
                        "Referenced internal vocabulary 'Other' version 1 is deprecated. Use version 2, the latest ACTIVE version.",
                        "prefix 'o' -> '" + CB + "Other/versions/1#'"));
    }

    @Test
    void deprecatedVersionWithoutActiveVersionGivesNoRecommendation() {
        store("Other", Status.ACTIVE, Status.DEPRECATED);
        String turtle = "@prefix o: <" + CB + "Other#> .\n<http://e/s> <http://e/p> <http://e/o> .";
        assertThat(validate(turtle))
                .extracting(ValidationWarning::description)
                .containsExactly("Referenced internal vocabulary 'Other' version 1 is deprecated.");
    }

    @Test
    void deprecatedVocabularyMakesEveryVersionEffectivelyDeprecated() {
        store("Other", Status.DEPRECATED, Status.ACTIVE);
        String turtle = "@prefix o: <" + CB + "Other> .\n<http://e/s> <http://e/p> <http://e/o> .";
        assertThat(validate(turtle))
                .extracting(ValidationWarning::description)
                .containsExactly(
                        "Referenced internal vocabulary 'Other' version 1 is deprecated. The vocabulary 'Other' is deprecated.");
    }

    @Test
    void activeReferenceGivesNoWarning() {
        store("Other", Status.ACTIVE, Status.ACTIVE);
        String turtle = "@prefix o: <" + CB + "Other/versions/1/> .\n<http://e/s> <http://e/p> <http://e/o> .";
        assertThat(validate(turtle)).isEmpty();
    }

    @Test
    void ownReferencesAndRelativePrefixesAreIgnored() {
        String turtle = "@prefix self: <" + CB + "Self#> .\n@prefix : <#> .\n:a :b :c .";
        assertThat(validate(turtle)).isEmpty();
    }

    @Test
    void unregisteredExternalNamespaceWarns() {
        String turtle = "@prefix dc: <http://purl.org/dc/terms/> .\n<http://e/s> dc:title \"t\" .";
        assertThat(validate(turtle))
                .containsExactly(new ValidationWarning(
                        "externalReferenceSkipped",
                        "Cannot resolve external namespace 'http://purl.org/dc/terms/' because no cached external vocabulary is"
                                + " registered.",
                        "prefix 'dc' -> 'http://purl.org/dc/terms/'"));
    }

    @Test
    void registeredExternalW3cAndOwnNamespacesDoNotWarn() {
        externalKeys = Set.of("http://purl.org/dc/terms");
        String turtle = """
                @prefix dc: <http://purl.org/dc/terms/> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix own: <https://own.example/terms#> .
                own:a rdfs:label "a"^^xsd:string ; dc:title "t" .
                """;
        assertThat(validate(turtle)).isEmpty();
    }

    @Test
    void syntaxOnlyCheckAcceptsSemanticProblems() {
        validator.checkSyntax(Turtle.bytes(""), BASE);
        assertThatThrownBy(() -> validator.checkSyntax(Turtle.bytes("<a> <b>"), BASE))
                .isInstanceOf(ContentValidationException.class);
    }
}
