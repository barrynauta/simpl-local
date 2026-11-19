package eu.simpl.vocabularymanager.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.validation.ContentIssue;
import eu.simpl.vocabularymanager.validation.ParsedContent;
import eu.simpl.vocabularymanager.validation.ReferenceLookup;
import eu.simpl.vocabularymanager.validation.ReferenceRules;
import eu.simpl.vocabularymanager.validation.SemanticRules;
import eu.simpl.vocabularymanager.validation.TurtleParser;
import eu.simpl.vocabularymanager.validation.TurtleSyntaxException;
import eu.simpl.vocabularymanager.validation.ValidationOutcome;
import eu.simpl.vocabularymanager.validation.ValidationWarning;
import eu.simpl.vocabularymanager.validation.VocabularyContentValidator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Unit tests of the field rules (D37, D56, D73), the Turtle parser (D9, D57) and the rules S1 to S7 (D9, D12, D47). */
class DomainAndValidationTest {

    private static final String BASE = "https://authority.example/vocabulary-manager";

    private static ParsedContent parse(String turtle) throws TurtleSyntaxException {
        return TurtleParser.parse(turtle.getBytes(StandardCharsets.UTF_8), BASE + "/content/Self/versions/1");
    }

    private static List<String> details(List<ContentIssue> issues) {
        return issues.stream().map(ContentIssue::detail).toList();
    }

    @Test
    void nameRules() {
        assertThat(FieldRules.isValidName("Abc")).isTrue();
        assertThat(FieldRules.isValidName("A" + "b".repeat(63))).isTrue();
        assertThat(FieldRules.isValidName("A" + "b".repeat(64))).isFalse();
        assertThat(FieldRules.isValidName("Ab")).isFalse();
        assertThat(FieldRules.isValidName("abc")).isFalse();
        assertThat(FieldRules.isValidName("Ab-c")).isFalse();
        assertThat(FieldRules.isValidName("Abç")).isFalse();
        assertThat(FieldRules.isValidName(null)).isFalse();
        assertThat(FieldRules.nameKey("HeLLo")).isEqualTo("hello");
    }

    @Test
    void versionTextAndUriRules() {
        assertThat(FieldRules.isValidVersion("1")).isTrue();
        assertThat(FieldRules.isValidVersion("0")).isFalse();
        assertThat(FieldRules.isValidVersion("-1")).isFalse();
        assertThat(FieldRules.isValidVersion("01")).isFalse();
        assertThat(FieldRules.isValidVersion("1.0")).isFalse();
        assertThat(FieldRules.isValidVersion("99999999999999999999")).isFalse();
        assertThat(FieldRules.isValidVersion(null)).isFalse();
        assertThat(FieldRules.isValidRequiredText("  x  ")).isTrue();
        assertThat(FieldRules.isValidRequiredText("   ")).isFalse();
        assertThat(FieldRules.isValidRequiredText(null)).isFalse();
        assertThat(FieldRules.isValidRequiredText("x".repeat(1000))).isTrue();
        assertThat(FieldRules.isValidRequiredText(" " + "x".repeat(1000) + " ")).isTrue();
        assertThat(FieldRules.isValidRequiredText("x".repeat(1001))).isFalse();
        assertThat(FieldRules.isValidOptionalText(null)).isTrue();
        assertThat(FieldRules.isValidOptionalText("")).isTrue();
        assertThat(FieldRules.isValidOptionalText("x".repeat(1001))).isFalse();
        assertThat(FieldRules.isAbsoluteUri("https://a.example/x#")).isTrue();
        assertThat(FieldRules.isAbsoluteUri("urn:isbn:123")).isTrue();
        assertThat(FieldRules.isAbsoluteUri("relative/path")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("a,b")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("https://a b")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("")).isFalse();
        assertThat(FieldRules.isAbsoluteUri(null)).isFalse();
        assertThat(FieldRules.isAbsoluteUri("https://x.example/" + "a".repeat(3000))).isFalse();
        assertThat(FieldRules.normalizeNamespace("https://a.example/x#")).isEqualTo("https://a.example/x");
        assertThat(FieldRules.normalizeNamespace("https://a.example/x/")).isEqualTo("https://a.example/x");
        assertThat(FieldRules.normalizeNamespace("https://a.example/x")).isEqualTo("https://a.example/x");
    }

    @Test
    void statusValuesAreExact() {
        assertThat(VocabularyStatus.parse("ACTIVE")).contains(VocabularyStatus.ACTIVE);
        assertThat(VocabularyStatus.parse("DEPRECATED")).contains(VocabularyStatus.DEPRECATED);
        assertThat(VocabularyStatus.parse("active")).isEmpty();
        assertThat(VocabularyStatus.parse(null)).isEmpty();
    }

    @Test
    void parserReportsFirstSyntaxErrorWithLineAndColumn() {
        assertThatThrownBy(() -> parse("@prefix ex: <https://x.example/#> .\n\nex:a ex:b ."))
                .isInstanceOfSatisfying(TurtleSyntaxException.class, e -> {
                    assertThat(e.detail()).startsWith("Encountered invalid Turtle syntax");
                    assertThat(e.location()).startsWith("line 3");
                });
        assertThatThrownBy(() -> TurtleParser.parse(new byte[] {(byte) 0xff, (byte) 0xfe, 0x00}, BASE))
                .isInstanceOf(TurtleSyntaxException.class);
    }

    @Test
    void parserResolvesRelativeIrisAgainstTheVersionUrl() throws Exception {
        ParsedContent parsed = parse("<#a> <#b> <c> .");
        assertThat(parsed.triples()).hasSize(1);
        assertThat(parsed.triples().get(0).getSubject().getURI())
                .isEqualTo(BASE + "/content/Self/versions/1#a");
    }

    @Test
    void semanticRulesOneByOne() throws Exception {
        String prefixes = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix ex: <https://x.example/#> .\n";
        assertThat(SemanticRules.check(parse(prefixes + "ex:a ex:b ex:c ."))).isEmpty();
        assertThat(details(SemanticRules.check(parse(prefixes))))
                .containsExactly("Vocabulary contains no statements");
        assertThat(details(SemanticRules.check(parse(prefixes + "ex:p a owl:ObjectProperty . ex:s ex:p 5 ."))))
                .containsExactly("Literal value for object property");
        assertThat(details(SemanticRules.check(parse(prefixes + "ex:p a owl:DatatypeProperty . ex:s ex:p [] ."))))
                .containsExactly("Resource value for datatype property");
        assertThat(SemanticRules.check(parse(prefixes + "ex:p a owl:DatatypeProperty . ex:s ex:p \"x\" ."))).isEmpty();
        List<ContentIssue> both = SemanticRules.check(
                parse(prefixes + "ex:p a owl:DatatypeProperty . ex:p a owl:ObjectProperty ."));
        assertThat(details(both)).containsExactly("Property declared as both object and datatype property");
        assertThat(both.get(0).location()).isEqualTo("resource 'https://x.example/#p'");
        // D53: two untagged labels clash, one untagged and one tagged do not.
        assertThat(details(SemanticRules.check(parse(prefixes + "ex:s skos:prefLabel \"a\" , \"b\" ."))))
                .containsExactly("Duplicate preferred label for the same language");
        assertThat(SemanticRules.check(parse(prefixes + "ex:s skos:prefLabel \"a\" , \"b\"@en ."))).isEmpty();
        assertThat(SemanticRules.check(parse(prefixes + "ex:s skos:prefLabel \"a\"@en , \"b\"@nl ."))).isEmpty();
        List<ContentIssue> tagged = SemanticRules.check(
                parse(prefixes + "ex:s skos:prefLabel \"a\"@en , \"b\"@en , \"c\"@en ."));
        assertThat(tagged).hasSize(1);
        assertThat(tagged.get(0).location()).isEqualTo("resource 'https://x.example/#s', language 'en'");
        assertThat(details(SemanticRules.check(parse(prefixes
                        + "ex:A owl:disjointWith ex:B . ex:i a ex:A , ex:B . ex:j a ex:A ."))))
                .containsExactly("Resource is an instance of disjoint classes");
        assertThat(details(SemanticRules.check(parse(prefixes
                        + "ex:B owl:disjointWith ex:A . _:x a ex:A , ex:B ."))))
                .containsExactly("Resource is an instance of disjoint classes");
    }

    /** A lookup that counts its calls (Q5: at most one store query per upload). */
    private static final class CountingLookup implements ReferenceLookup {
        final AtomicInteger calls = new AtomicInteger();
        final State state;

        CountingLookup(State state) {
            this.state = state;
        }

        @Override
        public State lookup(Set<String> internalNames, Set<Long> versions, Set<String> externalKeys) {
            calls.incrementAndGet();
            return state;
        }
    }

    @Test
    void referenceRulesWarnAndErrorWithOneLookup() throws Exception {
        var target = new ReferenceLookup.InternalVocabularyState("Target", VocabularyStatus.ACTIVE, 3,
                Map.of(1L, VocabularyStatus.DEPRECATED, 2L, VocabularyStatus.ACTIVE, 3L, VocabularyStatus.DEPRECATED));
        var gone = new ReferenceLookup.InternalVocabularyState("Gone", VocabularyStatus.DEPRECATED, 1,
                Map.of(1L, VocabularyStatus.ACTIVE));
        CountingLookup lookup = new CountingLookup(new ReferenceLookup.State(
                Map.of("Target", target, "Gone", gone), Set.of("https://registered.example/ns")));
        String turtle = """
                @prefix t1: <%1$s/content/Target/versions/1#> .
                @prefix t2: <%1$s/content/Target/versions/2/> .
                @prefix tl: <%1$s/content/Target> .
                @prefix t9: <%1$s/content/Target/versions/9#> .
                @prefix g: <%1$s/content/Gone#> .
                @prefix n: <%1$s/content/Nobody> .
                @prefix self: <%1$s/content/Self/versions/1#> .
                @prefix low: <%1$s/content/lower> .
                @prefix root: <%1$s/content/> .
                @prefix reg: <https://registered.example/ns#> .
                @prefix un: <https://unregistered.example/ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix own: <https://own.example/> .
                own:x rdfs:label "x" .
                """.formatted(BASE);
        ValidationOutcome outcome = ReferenceRules.check(parse(turtle), "Self", BASE, lookup);
        assertThat(lookup.calls.get()).isEqualTo(1);
        assertThat(details(outcome.errors())).containsExactly(
                "Referenced internal vocabulary 'Target' version 9 does not exist.",
                "Referenced internal vocabulary 'Nobody' does not exist.");
        List<String> descriptions = outcome.warnings().stream().map(ValidationWarning::description).toList();
        assertThat(descriptions).contains(
                "Referenced internal vocabulary 'Target' version 1 is deprecated. Use version 2, the latest ACTIVE"
                        + " version.",
                "Referenced internal vocabulary 'Target' version 3 is deprecated. Use version 2, the latest ACTIVE"
                        + " version.",
                "Referenced internal vocabulary 'Gone' version 1 is deprecated. The vocabulary 'Gone' is"
                        + " deprecated.");
        List<String> codes = outcome.warnings().stream().map(ValidationWarning::code).toList();
        assertThat(codes.stream().filter("externalReferenceSkipped"::equals).count()).isEqualTo(3);
        assertThat(outcome.warnings().stream().map(ValidationWarning::location).toList())
                .contains("prefix 'un' -> 'https://unregistered.example/ns#'", "prefix 'low' -> '" + BASE
                        + "/content/lower'", "prefix 'root' -> '" + BASE + "/content/'");
    }

    @Test
    void noLookupWithoutReferences() throws Exception {
        CountingLookup lookup = new CountingLookup(new ReferenceLookup.State(Map.of(), Set.of()));
        ValidationOutcome outcome = ReferenceRules.check(parse(
                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n<https://x.example/a> a owl:Class ."),
                "Self", BASE, lookup);
        assertThat(outcome.valid()).isTrue();
        assertThat(lookup.calls.get()).isZero();
    }

    @Test
    void validatorCombinesStepsAndChecksExternalContent() {
        VocabularyContentValidator validator = new VocabularyContentValidator();
        CountingLookup lookup = new CountingLookup(new ReferenceLookup.State(Map.of(), Set.of()));
        ValidationOutcome syntax = validator.validate("nonsense".getBytes(StandardCharsets.UTF_8), "Self",
                BASE + "/content/Self/versions/1", BASE, lookup);
        assertThat(syntax.errors()).hasSize(1);
        assertThat(lookup.calls.get()).isZero();
        ValidationOutcome empty = validator.validate(new byte[0], "Self", BASE, BASE, lookup);
        assertThat(details(empty.errors())).containsExactly("Vocabulary contains no statements");
        assertThat(validator.validateExternal("<a:b> <a:c> <a:d> .".getBytes(StandardCharsets.UTF_8), BASE))
                .isEmpty();
        assertThat(details(validator.validateExternal(new byte[0], BASE)))
                .containsExactly("Vocabulary contains no statements");
        assertThat(validator.validateExternal("<a:b>".getBytes(StandardCharsets.UTF_8), BASE)).hasSize(1);
    }
}
