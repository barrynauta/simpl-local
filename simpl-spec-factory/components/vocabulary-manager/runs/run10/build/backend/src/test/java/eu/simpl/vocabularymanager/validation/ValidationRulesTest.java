package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Syntax validation, semantic rules S1 to S6 and the reference check S7 with its warnings (D9 to D12, D47, D53, D65, D69). */
class ValidationRulesTest {

    private static final String BASE = "https://authority.example/vocabulary-manager";
    private final TurtleSyntaxValidator syntax = new TurtleSyntaxValidator();
    private final SemanticValidator semantic = new SemanticValidator();
    private final ReferenceChecker references = new ReferenceChecker(BASE);

    private ParsedContent parse(String turtle) {
        return syntax.parse(turtle.getBytes(StandardCharsets.UTF_8), BASE + "/content/Self/versions/1");
    }

    @Test
    void syntaxErrorHasLineAndColumnAndStopsAtTheFirst() {
        assertThatThrownBy(() -> parse("@prefix ex: <http://e/> .\nex:a ex:b ex:c .\nex:a ex:b .\nex:x ex:y ."))
                .isInstanceOfSatisfying(ProblemException.class, e -> {
                    assertThat(e.status()).isEqualTo(400);
                    assertThat(e.issues()).hasSize(1);
                    assertThat(e.issues().getFirst().location()).startsWith("line 3, column ");
                    assertThat(e.issues().getFirst().detail()).startsWith("Encountered invalid Turtle syntax: ");
                });
        assertThatThrownBy(() -> syntax.parse(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2}, BASE))
                .isInstanceOf(ProblemException.class);
        assertThatThrownBy(() -> parse("<http://e/a b> <http://e/p> <http://e/o> ."))
                .isInstanceOf(ProblemException.class);
    }

    @Test
    void syntaxIssueLocationVariants() {
        assertThat(TurtleSyntaxValidator.syntaxIssue("m", 4, -1).location()).isEqualTo("line 4");
        assertThat(TurtleSyntaxValidator.syntaxIssue(null, -1, -1).location()).isEqualTo("document");
        assertThat(TurtleSyntaxValidator.syntaxIssue(null, -1, -1).detail())
                .isEqualTo("Encountered invalid Turtle syntax.");
        assertThat(TurtleSyntaxValidator.syntaxIssue("x".repeat(500), 1, 1).detail())
                .hasSizeLessThan(400);
    }

    @Test
    void relativeIrisResolveAgainstTheVersionContentUrl() {
        ParsedContent content = parse("<#a> <#b> <#c> .");
        assertThat(content.graph().find().next().getSubject().getURI()).isEqualTo(BASE + "/content/Self/versions/1#a");
    }

    @Test
    void untaggedLabelsCountAsOneLanguageAndBlankNodesAreReported() {
        List<Issue> issues = semantic.validate(parse("@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                        + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix ex: <http://e/> .\n"
                        + "ex:a skos:prefLabel \"x\" , \"y\" , \"z\"@en .\n"
                        + "ex:dp a owl:DatatypeProperty . ex:a ex:dp [ ex:p 1 ] .\n")
                .graph());
        assertThat(issues).extracting(Issue::detail).containsExactly(SemanticValidator.S2, SemanticValidator.S4);
        assertThat(issues.get(1).location()).isEqualTo("resource 'http://e/a', language 'none'");
        assertThat(issues.get(0).location()).startsWith("statement 'http://e/a http://e/dp _:");
    }

    @Test
    void disjointClassesInEitherDirection() {
        List<Issue> issues = semantic.validate(parse("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                        + "@prefix ex: <http://e/> .\nex:B owl:disjointWith ex:A .\n"
                        + "ex:i a ex:A , ex:B . ex:j a ex:A .\n")
                .graph());
        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().location()).isEqualTo("resource 'http://e/i'");
        assertThat(semantic.validate(
                        parse("@prefix ex: <http://e/> . ex:a ex:b ex:c .").graph()))
                .isEmpty();
    }

    @Test
    void classifiesInternalAndExternalNamespaces() {
        assertThat(classify(BASE + "/content/Health").internalName()).isEqualTo("Health");
        assertThat(classify(BASE + "/content/Health#").versionText()).isNull();
        assertThat(classify(BASE + "/content/Health/versions/2#").versionText()).isEqualTo("2");
        assertThat(classify(BASE + "/content/Health/versions/x/").versionText()).isEqualTo("x");
        assertThat(classify(BASE + "/content/Health/terms#").internalName()).isEqualTo("Health");
        assertThat(classify(BASE + "/content/").internalName()).isNull();
        assertThat(classify(BASE + "/content/health#").internalName()).isNull();
        assertThat(classify(BASE + "/content/Ab").internalName()).isNull();
        assertThat(classify("http://purl.org/dc/terms/").internalName()).isNull();
    }

    private ReferenceChecker.Reference classify(String namespace) {
        return references.classify(new ParsedContent.PrefixDeclaration("p", namespace));
    }

    @Test
    void referenceCheckErrorsAndWarnings() {
        String turtle = "@prefix own: <https://example.org/own#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix self: <" + BASE + "/content/Self#> .\n"
                + "@prefix h1: <" + BASE + "/content/Health/versions/1#> .\n"
                + "@prefix h2: <" + BASE + "/content/Health/versions/2#> .\n"
                + "@prefix h3: <" + BASE + "/content/Health/versions/3#> .\n"
                + "@prefix big: <" + BASE + "/content/Health/versions/99999999999#> .\n"
                + "@prefix d: <" + BASE + "/content/Dep#> .\n"
                + "@prefix ext: <http://purl.org/dc/terms/> .\n"
                + "@prefix reg: <https://registered.example/ns#> .\n"
                + "own:a rdfs:label \"x\" .\n";
        Map<Integer, VocabularyStatus> healthVersions =
                Map.of(1, VocabularyStatus.DEPRECATED, 2, VocabularyStatus.ACTIVE);
        ReferenceChecker.ReferenceData data = new ReferenceChecker.ReferenceData(
                Map.of(
                        "Health",
                        new ReferenceChecker.InternalState(VocabularyStatus.ACTIVE, 2, healthVersions),
                        "Dep",
                        new ReferenceChecker.InternalState(
                                VocabularyStatus.DEPRECATED, 1, Map.of(1, VocabularyStatus.ACTIVE))),
                Set.of("https://registered.example/ns"));
        ReferenceChecker.Result result = references.check(parse(turtle), "Self", (names, keys) -> {
            assertThat(names).containsExactlyInAnyOrder("Health", "Dep");
            assertThat(keys).containsExactlyInAnyOrder("http://purl.org/dc/terms", "https://registered.example/ns");
            return data;
        });
        assertThat(result.errors())
                .extracting(Issue::detail)
                .containsExactly(
                        "Referenced internal vocabulary 'Health' version 3 does not exist.",
                        "Referenced internal vocabulary 'Health' version 99999999999 does not exist.");
        assertThat(result.warnings())
                .extracting(ValidationWarning::description)
                .containsExactly(
                        "Referenced internal vocabulary 'Health' version 1 is deprecated."
                                + " Use version 2, the latest ACTIVE version.",
                        "Referenced internal vocabulary 'Dep' version 1 is deprecated."
                                + " The vocabulary 'Dep' is deprecated.",
                        "Cannot resolve external namespace 'http://purl.org/dc/terms/' because no cached external"
                                + " vocabulary is registered.");
        assertThat(result.warnings().getFirst().location())
                .isEqualTo("prefix 'h1' -> '" + BASE + "/content/Health/versions/1#'");
    }

    @Test
    void deprecatedVersionWithoutOtherActiveVersionRecommendsNothing() {
        String turtle = "@prefix h: <" + BASE + "/content/Health#> .\n@prefix own: <https://o.example/x#> .\n"
                + "own:a own:b h:c .\n";
        ReferenceChecker.ReferenceData data = new ReferenceChecker.ReferenceData(
                Map.of(
                        "Health",
                        new ReferenceChecker.InternalState(
                                VocabularyStatus.ACTIVE, 1, Map.of(1, VocabularyStatus.DEPRECATED))),
                Set.of());
        ReferenceChecker.Result result = references.check(parse(turtle), "Self", (names, keys) -> data);
        assertThat(result.warnings())
                .extracting(ValidationWarning::description)
                .containsExactly("Referenced internal vocabulary 'Health' version 1 is deprecated.");
        assertThat(references
                        .check(parse("@prefix own: <https://o.example/x#> . own:a own:b own:c ."), "S", (n, k) -> {
                            throw new AssertionError("no lookup expected");
                        })
                        .warnings())
                .isEmpty();
    }
}
