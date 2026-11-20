package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.domain.ContentValidationException;
import eu.simpl.vocabularymanager.domain.ValidationIssue;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.validation.ReferenceData.ReferencedVocabulary;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Syntax validation, rules S1 to S7 and the reference warnings (D9 to D12, D47, D53, D65, D69). */
class ContentRulesTest {

    private static final String BASE = "https://authority.example/vocabulary-manager";
    private final TurtleParser parser = new TurtleParser();
    private final SemanticRules rules = new SemanticRules();

    private ParsedContent parse(String turtle) {
        return parser.parse(turtle.getBytes(StandardCharsets.UTF_8), BASE + "/content/Own/versions/1");
    }

    private List<ValidationIssue> semantic(String turtle) {
        return rules.check(parse(turtle).graph());
    }

    @Test
    void syntaxErrorHasLineAndColumn() {
        assertThatThrownBy(() -> parse("@prefix ex: <http://e.org/> .\nex:a ex:b ex:c ;\n ex:d .\n"))
                .isInstanceOfSatisfying(ContentValidationException.class, ex -> {
                    assertThat(ex.issues()).hasSize(1);
                    assertThat(ex.issues().get(0).detail()).startsWith("Encountered invalid Turtle syntax");
                    assertThat(ex.issues().get(0).location()).matches("line 3(, column \\d+)?");
                });
        assertThatThrownBy(() -> parse("\u0000\u0001binary")).isInstanceOf(ContentValidationException.class);
        assertThatThrownBy(() -> parse("ex:a ex:b ex:c .")).isInstanceOf(ContentValidationException.class);
    }

    @Test
    void parsesPrefixesSubjectsAndRelativeIris() {
        ParsedContent content = parse("@prefix ex: <http://e.org/ns#> .\nPREFIX s: <http://s.org/>\n<a> ex:p s:o .\n");
        assertThat(content.prefixes()).extracting(PrefixDeclaration::prefix).containsExactly("ex", "s");
        assertThat(content.subjectIris()).containsExactly(BASE + "/content/Own/versions/a");
        assertThat(content.prefixes().get(0).location()).isEqualTo("prefix 'ex' -> 'http://e.org/ns#'");
    }

    @Test
    void s1ToS6() {
        String prefixes = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix ex: <http://e.org/> .\n";
        assertThat(semantic(prefixes + "ex:p a owl:ObjectProperty . ex:s ex:p \"lit\" ."))
                .containsExactly(
                        new ValidationIssue(SemanticRules.S1, "statement 'http://e.org/s http://e.org/p lit'"));
        assertThat(semantic(prefixes + "ex:p a owl:DatatypeProperty . ex:s ex:p ex:o ."))
                .containsExactly(new ValidationIssue(
                        SemanticRules.S2, "statement 'http://e.org/s http://e.org/p http://e.org/o'"));
        assertThat(semantic(prefixes + "ex:p a owl:DatatypeProperty, owl:ObjectProperty ."))
                .containsExactly(new ValidationIssue(SemanticRules.S3, "resource 'http://e.org/p'"));
        assertThat(semantic(prefixes + "ex:s skos:prefLabel \"a\"@en, \"b\"@EN, \"c\", \"d\", \"e\"@fr ."))
                .containsExactly(
                        new ValidationIssue(SemanticRules.S4, "resource 'http://e.org/s', language 'en'"),
                        new ValidationIssue(SemanticRules.S4, "resource 'http://e.org/s', language 'none'"));
        assertThat(semantic(prefixes + "ex:s skos:prefLabel \"a\", \"b\"@en .")).isEmpty();
        assertThat(semantic(prefixes + "ex:A owl:disjointWith ex:B . ex:i a ex:A, ex:B . ex:j a ex:A ."))
                .containsExactly(new ValidationIssue(SemanticRules.S5, "resource 'http://e.org/i'"));
        assertThat(semantic(prefixes)).containsExactly(new ValidationIssue(SemanticRules.S6, "document"));
        assertThat(semantic(prefixes + "ex:d a owl:DatatypeProperty . ex:s ex:d [ ex:q 1 ] ."))
                .hasSize(1)
                .first()
                .satisfies(issue -> assertThat(issue.location()).contains("_:"));
    }

    @Test
    void classifiesReferences() {
        ReferenceAnalyzer analyzer = new ReferenceAnalyzer(BASE);
        List<PrefixDeclaration> prefixes = List.of(
                new PrefixDeclaration("a", BASE + "/content/Alpha#"),
                new PrefixDeclaration("b", BASE + "/content/Beta/versions/2/"),
                new PrefixDeclaration("c", BASE + "/content/Gamma/versions/x2#"),
                new PrefixDeclaration("own", BASE + "/content/Own/versions/1#"),
                new PrefixDeclaration("bad", BASE + "/content/lower#"),
                new PrefixDeclaration("none", BASE + "/content/"),
                new PrefixDeclaration("skos", "http://www.w3.org/2004/02/skos/core#"),
                new PrefixDeclaration("mine", "http://mine.org/ns#"),
                new PrefixDeclaration("dc", "http://purl.org/dc/terms/"));
        ReferenceAnalysis analysis = analyzer.analyze(prefixes, Set.of("http://mine.org/ns#x"), "Own");
        assertThat(analysis.internal())
                .extracting(ReferenceAnalysis.InternalReference::name)
                .containsExactly("Alpha", "Beta", "Gamma");
        assertThat(analysis.internal().get(1).versionNumber()).isEqualTo(2);
        assertThat(analysis.internal().get(2).versionNumber()).isNull();
        assertThat(analysis.externalKeys())
                .containsExactlyInAnyOrder(BASE + "/content/lower", BASE + "/content", "http://purl.org/dc/terms");
        assertThat(analysis.internalNames()).containsExactly("Alpha", "Beta", "Gamma");
        assertThat(analysis.isEmpty()).isFalse();
    }

    @Test
    void evaluatesReferencesAsDecided() {
        ReferenceAnalyzer analyzer = new ReferenceAnalyzer(BASE);
        List<PrefixDeclaration> prefixes = List.of(
                new PrefixDeclaration("a", BASE + "/content/Alpha/versions/1#"),
                new PrefixDeclaration("b", BASE + "/content/Beta#"),
                new PrefixDeclaration("c", BASE + "/content/Gamma/versions/3#"),
                new PrefixDeclaration("d", BASE + "/content/Delta#"),
                new PrefixDeclaration("e", BASE + "/content/Gamma/versions/x2#"),
                new PrefixDeclaration("f", BASE + "/content/Alpha/versions/2#"),
                new PrefixDeclaration("g", BASE + "/content/Epsilon/versions/1#"),
                new PrefixDeclaration("dc", "http://purl.org/dc/terms/"),
                new PrefixDeclaration("reg", "http://registered.org/ns#"));
        ReferenceAnalysis analysis = analyzer.analyze(prefixes, Set.of(), "Own");
        ReferenceData data = new ReferenceData(
                Map.of(
                        "Alpha",
                        new ReferencedVocabulary(
                                VocabularyStatus.ACTIVE,
                                2,
                                Map.of(1, VocabularyStatus.DEPRECATED, 2, VocabularyStatus.ACTIVE)),
                        "Beta",
                        new ReferencedVocabulary(VocabularyStatus.DEPRECATED, 1, Map.of(1, VocabularyStatus.ACTIVE)),
                        "Gamma",
                        new ReferencedVocabulary(VocabularyStatus.ACTIVE, 1, Map.of(1, VocabularyStatus.ACTIVE)),
                        "Epsilon",
                        new ReferencedVocabulary(VocabularyStatus.ACTIVE, 1, Map.of(1, VocabularyStatus.DEPRECATED))),
                Set.of("http://registered.org/ns"));
        ReferenceEvaluator.Result result = ReferenceEvaluator.evaluate(analysis, data);
        assertThat(result.issues())
                .extracting(ValidationIssue::detail)
                .containsExactly(
                        "Referenced internal vocabulary 'Gamma' version 3 does not exist.",
                        "Referenced internal vocabulary 'Delta' does not exist.",
                        "Referenced internal vocabulary 'Gamma' version x2 does not exist.");
        assertThat(result.warnings())
                .extracting(ValidationWarning::description)
                .containsExactly(
                        "Referenced internal vocabulary 'Alpha' version 1 is deprecated. Use version 2, the latest"
                                + " ACTIVE version.",
                        "Referenced internal vocabulary 'Beta' version 1 is deprecated. The vocabulary 'Beta' is"
                                + " deprecated.",
                        "Referenced internal vocabulary 'Epsilon' version 1 is deprecated.",
                        "Cannot resolve external namespace 'http://purl.org/dc/terms/' because no cached external"
                                + " vocabulary is registered.");
        assertThat(ReferenceEvaluator.evaluate(new ReferenceAnalysis(List.of(), List.of()), ReferenceData.empty())
                        .issues())
                .isEmpty();
    }

    @Test
    void uriKeys() {
        assertThat(UriKeys.key("http://a.org/x#")).isEqualTo("http://a.org/x");
        assertThat(UriKeys.key("http://a.org/x/")).isEqualTo("http://a.org/x");
        assertThat(UriKeys.key("http://a.org/x")).isEqualTo("http://a.org/x");
        assertThat(UriKeys.isAbsolute("http://a.org/x")).isTrue();
        assertThat(UriKeys.isAbsolute("urn:x:y")).isTrue();
        assertThat(UriKeys.isAbsolute("relative/path")).isFalse();
        assertThat(UriKeys.isAbsolute("http://a b")).isFalse();
        assertThat(UriKeys.isAbsolute("")).isFalse();
        assertThat(UriKeys.isAbsolute(null)).isFalse();
        assertThat(UriKeys.isAbsolute("http://[bad")).isFalse();
    }
}
