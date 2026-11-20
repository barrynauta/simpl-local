package eu.simpl.vocabularymanager.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.content.ContentIssue;
import eu.simpl.vocabularymanager.content.ContentValidator;
import eu.simpl.vocabularymanager.content.ReferenceChecker;
import eu.simpl.vocabularymanager.content.SemanticRules;
import eu.simpl.vocabularymanager.content.TurtleDocument;
import eu.simpl.vocabularymanager.content.TurtleParser;
import eu.simpl.vocabularymanager.content.TurtleSyntaxException;
import eu.simpl.vocabularymanager.content.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.store.ReferenceState;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Syntax, semantic rules S1 to S7, warnings and the report limit (D9 to D12, D47, D53, D65, D69, D125, D140). */
class ContentRulesTest {

    private static final String BASE = "https://authority.example/vocabulary-manager";
    private final TurtleParser parser = new TurtleParser();
    private final SemanticRules rules = new SemanticRules();

    private TurtleDocument parse(String text) throws TurtleSyntaxException {
        return parser.parse(text.getBytes(StandardCharsets.UTF_8), BASE + "/content/Own/versions/1");
    }

    private static AppProperties properties() {
        return new AppProperties(BASE, null, null, new AppProperties.Limits(5242880, 6291456, 1048576, 4, null, 3));
    }

    @Test
    void syntaxErrorHasLineAndColumn() {
        assertThatThrownBy(() -> parse("@prefix ex: <http://ex.org/> .\nex:a ex:b ."))
                .isInstanceOfSatisfying(TurtleSyntaxException.class, ex -> {
                    assertThat(ex.line()).isEqualTo(2);
                    assertThat(ex.location()).startsWith("line 2");
                });
        assertThat(new TurtleSyntaxException("m", 3, 0).location()).isEqualTo("line 3");
        assertThat(new TurtleSyntaxException("m", -1, -1).location()).isEqualTo("document");
    }

    @Test
    void relativeIrisResolveAgainstTheContentUrl() throws Exception {
        TurtleDocument doc = parse("<#a> <#b> <c> .");
        assertThat(doc.triples().get(0).getSubject().getURI()).isEqualTo(BASE + "/content/Own/versions/1#a");
    }

    @Test
    void untaggedLabelsCountAsOneLanguageAndTaggedDiffer() throws Exception {
        List<ContentIssue> issues = rules.check(parse("""
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                <http://ex/s> skos:prefLabel "a" , "b"@en , "c"@EN , "d"@fr .
                <http://ex/t> skos:prefLabel "a" , "b"@en .
                """));
        assertThat(issues).extracting(ContentIssue::location).containsExactly("resource 'http://ex/s', language 'en'");
    }

    @Test
    void literalsAreShownInNTriplesForm() throws Exception {
        List<ContentIssue> issues = rules.check(parse("""
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                <http://ex/p> a owl:ObjectProperty .
                <http://ex/s> <http://ex/p> "4"^^xsd:integer , "q\\"x" .
                _:b <http://ex/p> "y" .
                """));
        assertThat(issues).extracting(ContentIssue::location).hasSize(3);
        assertThat(issues.get(0).location())
                .isEqualTo("statement 'http://ex/s http://ex/p \"4\"^^<http://www.w3.org/2001/XMLSchema#integer>'");
        assertThat(issues.get(1).location()).isEqualTo("statement 'http://ex/s http://ex/p \"q\\\"x\"'");
        assertThat(issues.get(2).location()).startsWith("statement '_:");
    }

    @Test
    void reportIsTruncatedAtTheLimit() {
        VocabularyRepository repository = mock(VocabularyRepository.class);
        when(repository.references(any(), any())).thenReturn(new ReferenceState(List.of(), List.of()));
        ContentValidator validator =
                new ContentValidator(parser, rules, new ReferenceChecker(properties(), repository), properties());
        StringBuilder content = new StringBuilder(
                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n<http://ex/p> a owl:ObjectProperty .\n");
        for (int i = 0; i < 5; i++) {
            content.append("<http://ex/s> <http://ex/p> \"").append(i).append("\" .\n");
        }
        assertThatThrownBy(() -> validator.validate(content.toString().getBytes(), BASE, "Own"))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.issues()).hasSize(3);
                    assertThat(ex.truncated()).isTrue();
                });
        assertThatThrownBy(() -> validator.validateSyntaxAndNotEmpty(new byte[0], "http://ns/"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void referenceShapesAndWarnings() throws Exception {
        VocabularyRepository repository = mock(VocabularyRepository.class);
        when(repository.references(any(), any()))
                .thenReturn(new ReferenceState(
                        List.of(
                                new ReferenceState.VersionRow(
                                        "Lib", VocabularyStatus.ACTIVE, 3, 1, VocabularyStatus.DEPRECATED),
                                new ReferenceState.VersionRow(
                                        "Lib", VocabularyStatus.ACTIVE, 3, 2, VocabularyStatus.ACTIVE),
                                new ReferenceState.VersionRow(
                                        "Lib", VocabularyStatus.ACTIVE, 3, 3, VocabularyStatus.DEPRECATED)),
                        List.of("http://registered.example/ns")));
        ReferenceChecker checker = new ReferenceChecker(properties(), repository);
        String content = "@prefix v1: <" + BASE + "/content/Lib/versions/1#> .\n"
                + "@prefix lat: <" + BASE + "/content/Lib/> .\n"
                + "@prefix bad: <" + BASE + "/content/Lib/versions/x1/> .\n"
                + "@prefix own: <" + BASE + "/content/Own#> .\n"
                + "@prefix low: <" + BASE + "/content/lower#> .\n"
                + "@prefix reg: <http://registered.example/ns#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix me: <https://me.example/x#> .\n"
                + "me:a me:b me:c .\n";
        ReferenceChecker.Outcome outcome = checker.check(parse(content), "Own");
        assertThat(outcome.errors())
                .extracting(ContentIssue::detail)
                .containsExactly("Referenced internal vocabulary 'Lib' version x1 does not exist.");
        assertThat(outcome.warnings())
                .extracting(ValidationWarning::description)
                .containsExactly(
                        "Referenced internal vocabulary 'Lib' version 1 is deprecated. Use version 2, the latest ACTIVE version.",
                        "Referenced internal vocabulary 'Lib' version 3 is deprecated. Use version 2, the latest ACTIVE version.",
                        "Cannot resolve external namespace '" + BASE
                                + "/content/lower#' because no cached external vocabulary is registered.");
    }
}
