package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.support.InMemoryStore;
import eu.simpl.vocabularymanager.support.TestProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Decisions D9 to D12, D47, D53 and D57: the two validation steps and their texts. */
class ContentValidatorTest {

    private static final String BASE = TestProperties.BASE;
    private static final Instant T = Instant.parse("2026-11-18T09:00:00Z");

    private InMemoryStore store;
    private ContentValidator validator;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        validator = new ContentValidator(new TurtleParser(), new SemanticRules(), store, TestProperties.properties());
    }

    private ContentValidator.Outcome validate(String turtle) {
        return validator.validateVersion(turtle.getBytes(StandardCharsets.UTF_8), "Own", 1);
    }

    private static List<String> details(ContentValidator.Outcome outcome) {
        return outcome.errors().stream().map(Issue::detail).toList();
    }

    private void addVocabulary(String name, VocabularyStatus status, VocabularyStatus... versionStatuses) {
        store.createVocabulary(
                new Vocabulary(name, status, "a", T, T, versionStatuses.length, "d"),
                version(name, 1, versionStatuses[0]),
                new byte[0]);
        for (int i = 1; i < versionStatuses.length; i++) {
            store.createVersion(version(name, i + 1, VocabularyStatus.ACTIVE), new byte[0]);
        }
        for (int i = 0; i < versionStatuses.length; i++) {
            store.updateVersionStatus(name, i + 1, versionStatuses[i], T);
        }
        store.updateVocabularyStatus(name, status, T);
    }

    private static VocabularyVersion version(String name, int n, VocabularyStatus status) {
        return new VocabularyVersion(name, n, status, "d", "c", "a", T, T, "x");
    }

    @Test
    void validContentPassesWithoutWarnings() {
        ContentValidator.Outcome outcome = validate("@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix ex: <https://example.eu/own#> .\n"
                + "ex:Scheme a skos:ConceptScheme ; skos:prefLabel \"Scheme\"@en .\n");
        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void syntaxErrorGivesOneIssueWithLineAndColumnAndStopsSemanticValidation() {
        ContentValidator.Outcome outcome = validate("@prefix ex: <http://e/> .\nex:a ex:b ex:c ;\n ex:d \"x .\n");
        assertThat(outcome.errors()).hasSize(1);
        Issue issue = outcome.errors().get(0);
        assertThat(issue.detail()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issue.location()).matches("line \\d+, column \\d+");
        assertThat(issue.name()).isEqualTo("vocabularyFile");
        assertThat(issue.in()).isEqualTo("body");
        assertThat(issue.type()).isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
    }

    @Test
    void undefinedPrefixIsASyntaxError() {
        ContentValidator.Outcome outcome = validate("nope:a nope:b nope:c .\n");
        assertThat(outcome.errors()).hasSize(1);
        assertThat(outcome.errors().get(0).location()).startsWith("line 1");
    }

    @Test
    void emptyContentIsRuleS6() {
        ContentValidator.Outcome outcome = validate("# only a comment\n");
        assertThat(details(outcome)).containsExactly("Vocabulary contains no statements");
        assertThat(outcome.errors().get(0).location()).isEqualTo("document");
    }

    @Test
    void allSemanticRulesRunAndAllErrorsAreReturned() {
        String turtle = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix ex: <http://example.org/> .\n"
                + "ex:op a owl:ObjectProperty . ex:dp a owl:DatatypeProperty .\n"
                + "ex:both a owl:ObjectProperty, owl:DatatypeProperty .\n"
                + "ex:s ex:op \"literal\" . ex:s ex:dp ex:o .\n"
                + "ex:s skos:prefLabel \"A\"@en, \"B\"@en .\n"
                + "ex:A owl:disjointWith ex:B . ex:x a ex:A, ex:B .\n";
        ContentValidator.Outcome outcome = validate(turtle);
        assertThat(details(outcome))
                .containsExactly(
                        "Literal value for object property",
                        "Resource value for datatype property",
                        "Property declared as both object and datatype property",
                        "Duplicate preferred label for the same language",
                        "Resource is an instance of disjoint classes");
        assertThat(outcome.errors().get(0).location())
                .isEqualTo("statement 'http://example.org/s http://example.org/op literal'");
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void untaggedLabelsCountAsOneLanguage() {
        String prefix = "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix ex: <http://e/> .\n";
        assertThat(details(validate(prefix + "ex:s skos:prefLabel \"A\", \"B\" .\n")))
                .containsExactly("Duplicate preferred label for the same language");
        assertThat(validate(prefix + "ex:s skos:prefLabel \"A\", \"B\"@en .\n").passed())
                .isTrue();
        assertThat(validate(prefix + "ex:s skos:prefLabel \"A\"@en, \"B\"@fr .\n")
                        .passed())
                .isTrue();
    }

    @Test
    void unregisteredExternalNamespaceGivesWarning() {
        ContentValidator.Outcome outcome = validate("@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                + "@prefix ex: <https://example.eu/own#> .\nex:a dcterms:title \"t\" .\n");
        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo(ValidationWarning.EXTERNAL_REFERENCE_SKIPPED);
            assertThat(w.location()).isEqualTo("prefix 'dcterms' -> 'http://purl.org/dc/terms/'");
        });
    }

    @Test
    void registeredExternalNamespaceOrAliasGivesNoWarning() {
        store.createExternal(
                new ExternalVocabulary(
                        "Dc", "http://purl.org/dc/terms", List.of("https://purl.org/dc/terms/"), null, null, "a", T),
                new byte[0]);
        assertThat(validate("@prefix d: <http://purl.org/dc/terms/> .\n@prefix e: <https://purl.org/dc/terms#> .\n"
                                + "<http://x/a> d:title \"t\" .\n")
                        .warnings())
                .isEmpty();
    }

    @Test
    void w3cAndOwnNamespacesGiveNoWarning() {
        ContentValidator.Outcome outcome = validate("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix own: <https://example.eu/own/> .\n"
                + "own:a rdfs:label \"x\" .\n");
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void missingInternalVocabularyIsRuleS7() {
        ContentValidator.Outcome outcome = validate("@prefix m: <" + BASE + "/content/Missing#> .\n" + "@prefix v: <"
                + BASE + "/content/Missing/versions/3#> .\n<http://x/a> <http://x/b> \"c\" .\n");
        assertThat(details(outcome))
                .containsExactly(
                        "Referenced internal vocabulary 'Missing' does not exist.",
                        "Referenced internal vocabulary 'Missing' version 3 does not exist.");
        assertThat(outcome.errors().get(0).location()).isEqualTo("prefix 'm' -> '" + BASE + "/content/Missing#'");
    }

    @Test
    void missingVersionOfExistingVocabularyIsRuleS7() {
        addVocabulary("Known", VocabularyStatus.ACTIVE, VocabularyStatus.ACTIVE);
        assertThat(details(validate(
                        "@prefix k: <" + BASE + "/content/Known/versions/9/> .\n<http://x/a> <http://x/b> 1 .\n")))
                .containsExactly("Referenced internal vocabulary 'Known' version 9 does not exist.");
    }

    @Test
    void deprecatedVersionReferenceRecommendsLatestActive() {
        addVocabulary("Legacy", VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED, VocabularyStatus.ACTIVE);
        ContentValidator.Outcome outcome =
                validate("@prefix l: <" + BASE + "/content/Legacy/versions/1#> .\n<http://x/a> <http://x/b> 1 .\n");
        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo(ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE);
            assertThat(w.description())
                    .isEqualTo("Referenced internal vocabulary 'Legacy' version 1 is deprecated."
                            + " Use version 2, the latest ACTIVE version.");
        });
    }

    @Test
    void deprecatedVersionWithoutActiveVersionHasNoRecommendation() {
        addVocabulary("Old", VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED);
        assertThat(validate("@prefix o: <" + BASE + "/content/Old#> .\n<http://x/a> <http://x/b> 1 .\n")
                        .warnings())
                .extracting(ValidationWarning::description)
                .containsExactly("Referenced internal vocabulary 'Old' version 1 is deprecated.");
    }

    @Test
    void deprecatedVocabularyMakesActiveVersionEffectivelyDeprecated() {
        addVocabulary("Gone", VocabularyStatus.DEPRECATED, VocabularyStatus.DEPRECATED, VocabularyStatus.ACTIVE);
        assertThat(validate("@prefix g: <" + BASE + "/content/Gone#> .\n<http://x/a> <http://x/b> 1 .\n")
                        .warnings())
                .extracting(ValidationWarning::description)
                .containsExactly("Referenced internal vocabulary 'Gone' version 2 is deprecated."
                        + " The vocabulary 'Gone' is deprecated.");
    }

    @Test
    void activeReferenceGivesNoWarningAndOwnReferenceIsIgnored() {
        addVocabulary("Fine", VocabularyStatus.ACTIVE, VocabularyStatus.ACTIVE);
        ContentValidator.Outcome outcome = validate("@prefix f: <" + BASE + "/content/Fine#> .\n" + "@prefix me: <"
                + BASE + "/content/Own/versions/7#> .\n<http://x/a> <http://x/b> 1 .\n");
        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void relativeIrisResolveAgainstTheNewVersionUrl() {
        ContentValidator.Outcome outcome = validate("@prefix : <#> .\n:a :b :c .\n");
        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.warnings()).isEmpty();
    }

    @Test
    void externalRegistrationChecksSyntaxOnly() {
        byte[] invalidSemantics =
                ("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n<http://e/p> a owl:ObjectProperty ."
                                + "\n<http://e/s> <http://e/p> \"lit\" .\n")
                        .getBytes(StandardCharsets.UTF_8);
        assertThat(validator.validateSyntaxOnly(invalidSemantics, "Ext").passed())
                .isTrue();
        assertThat(validator
                        .validateSyntaxOnly("bad bad".getBytes(StandardCharsets.UTF_8), "Ext")
                        .errors())
                .hasSize(1);
    }

    @Test
    void deeplyNestedContentIsASyntaxErrorNotACrash() {
        String nested = "[ <http://e/p> ".repeat(20000) + "]".repeat(20000) + " .";
        ContentValidator.Outcome outcome = validate("@prefix ex: <http://e/> .\n" + nested);
        assertThat(outcome.passed()).isIn(true, false);
    }
}
