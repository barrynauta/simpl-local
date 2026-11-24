package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests of syntax and semantic validation (D9, D53, D65, D125, D140 to D145, D181, D199). */
class ValidationRulesTest {

    private static ParsedContent parse(String text) throws TurtleSyntax.SyntaxError {
        return TurtleSyntax.parse(text.getBytes(StandardCharsets.UTF_8), "https://base.example/content/X/versions/1");
    }

    private static List<String> details(String text) throws TurtleSyntax.SyntaxError {
        return SemanticRules.check(parse(text)).stream()
                .map(issue -> issue.issue().detail() + " @ " + issue.issue().location())
                .toList();
    }

    @Test
    void syntaxErrorHasLineAndColumn() {
        assertThatThrownBy(() -> parse("@prefix ex: <http://e/> .\nex:a ex:b ."))
                .isInstanceOfSatisfying(TurtleSyntax.SyntaxError.class, e -> {
                    assertThat(e.line()).isEqualTo(2);
                    assertThat(e.location()).startsWith("line 2");
                });
        assertThat(new TurtleSyntax.SyntaxError("x", 3, -1).location()).isEqualTo("line 3");
        assertThat(new TurtleSyntax.SyntaxError("x", -1, -1).location()).isEqualTo("document");
    }

    @Test
    void prefixesKeepTheDeclaredText() throws TurtleSyntax.SyntaxError {
        ParsedContent content = parse(
                "@base <https://b.example/> .\n@prefix rel: <rel/> .\nPREFIX up: <http://up/>\nrel:a up:b _:n1 .\n_:n1 up:c [] .");
        assertThat(content.prefixes()).hasSize(2);
        assertThat(content.prefixes().get(0).declared()).isEqualTo("rel/");
        assertThat(content.prefixes().get(0).resolved()).isEqualTo("https://b.example/rel/");
        assertThat(content.prefixes().get(1).prefix()).isEqualTo("up");
        assertThat(content.blankNodeLabels()).containsExactly("n1");
    }

    @Test
    void semanticRules() throws TurtleSyntax.SyntaxError {
        assertThat(details("")).containsExactly("Vocabulary contains no statements @ document");
        String owl =
                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix ex: <http://e/> .\n";
        assertThat(details(owl + "ex:p a owl:ObjectProperty . ex:s ex:p \"a \\\"q\\\"\" ."))
                .containsExactly(
                        "Literal value for object property @ statement '<http://e/s> <http://e/p> \"a \\\"q\\\"\"'");
        assertThat(details(owl + "ex:p a owl:DatatypeProperty . _:x ex:p [ ] ."))
                .containsExactly("Resource value for datatype property @ statement '_:x <http://e/p> []'");
        assertThat(details(owl + "ex:p a owl:DatatypeProperty , owl:ObjectProperty ."))
                .containsExactly("Property declared as both object and datatype property @ resource 'http://e/p'");
        assertThat(details(owl + "ex:s skos:prefLabel \"a\"@EN, \"b\"@en, \"c\", \"c\" ."))
                .containsExactly(
                        "Duplicate preferred label for the same language @ resource 'http://e/s', language 'en'");
        assertThat(details(owl + "ex:s skos:prefLabel \"a\", \"b\"@en ; skos:prefLabel \"x\"@de ."))
                .isEmpty();
        assertThat(
                        details(
                                owl
                                        + "ex:A owl:disjointWith ex:B . ex:B owl:disjointWith ex:C .\nex:s a ex:A , ex:B , ex:C . ex:t a ex:A , ex:D ."))
                .containsExactly("Resource is an instance of disjoint classes @ resource 'http://e/s'");
    }

    @Test
    void referencesAreClassified() throws TurtleSyntax.SyntaxError {
        String base = "https://authority.example/vocabulary-manager";
        String text = "@prefix a: <" + base + "/content/Health#> .\n"
                + "@prefix b: <" + base + "/content/Health/versions/2#> .\n"
                + "@prefix c: <" + base + "/content/Health/versions/x/> .\n"
                + "@prefix d: <" + base + "/content/Health/other/path> .\n"
                + "@prefix e: <" + base + "/content/lower> .\n"
                + "@prefix f: <" + base + "/content/Ab> .\n"
                + "@prefix g: <" + base + "/content/Self/versions/1#> .\n"
                + "@prefix h: <" + base + "/content/Health-x> .\n"
                + "@prefix rdfz: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix own: <http://own.example/ns#> .\n"
                + "@prefix dup: <http://own.example/ns#> .\n"
                + "@prefix dc: <http://purl.org/dc/terms/> .\n"
                + "own:a dc:title \"x\" .\n";
        References.Found found = References.find(parse(text), base, "Self");
        assertThat(found.internal())
                .extracting(References.Internal::name)
                .containsExactly("Health", "Health", "Health", "Health");
        assertThat(found.internal().get(1).version()).isEqualTo(2L);
        assertThat(found.internal().get(2).versionText()).isEqualTo("x");
        assertThat(found.internal().get(2).version()).isNull();
        assertThat(found.internal().get(3).versionText()).isNull();
        assertThat(found.external())
                .extracting(ParsedContent.PrefixDeclaration::prefix)
                .containsExactly("e", "f", "h", "dc");
        assertThat(References.normalise("http://x/")).isEqualTo("http://x");
        assertThat(References.normalise("http://x#")).isEqualTo("http://x");
        assertThat(References.normalise("http://x")).isEqualTo("http://x");
    }
}
