package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Rules S1 to S6 of D9 with the refinements D53, D65, D140, D142, D143, D144. */
class SemanticRulesTest {

    static final String PREFIXES = "@prefix ex: <http://ex.org/> .\n"
            + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
            + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";

    static List<SemanticRules.Finding> check(String body) throws Exception {
        return SemanticRules.check(
                ParsedTurtle.parse((PREFIXES + body).getBytes(StandardCharsets.UTF_8), "http://base.org/"));
    }

    @Test
    void validFileHasNoFindings() throws Exception {
        assertThat(check("ex:a a skos:Concept ; skos:prefLabel \"A\"@en , \"A\"@fr , \"A\" ."))
                .isEmpty();
    }

    @Test
    void s1LiteralForObjectProperty() throws Exception {
        List<SemanticRules.Finding> f =
                check("ex:p a owl:ObjectProperty . ex:s ex:p \"v\\\"q\"@en . ex:s ex:p \"5\"^^xsd:integer .");
        assertThat(f).hasSize(2);
        assertThat(f.get(0).detail()).isEqualTo(SemanticRules.S1);
        assertThat(f.get(0).location()).isEqualTo("statement 'http://ex.org/s http://ex.org/p \"v\\\"q\"@en'");
        assertThat(f.get(1).location())
                .isEqualTo(
                        "statement 'http://ex.org/s http://ex.org/p \"5\"^^<http://www.w3.org/2001/XMLSchema#integer>'");
    }

    @Test
    void s2ResourceForDatatypeProperty() throws Exception {
        List<SemanticRules.Finding> f = check("ex:d a owl:DatatypeProperty . ex:s ex:d ex:o . ex:s ex:d [] .");
        assertThat(f).hasSize(2);
        assertThat(f.get(0).detail()).isEqualTo(SemanticRules.S2);
        assertThat(f.get(1).location()).contains("_:");
    }

    @Test
    void s3OneIssuePerPropertyWhateverTheOrder() throws Exception {
        List<SemanticRules.Finding> f = check(
                "ex:p a owl:DatatypeProperty . ex:p a owl:ObjectProperty . ex:q a owl:ObjectProperty , owl:DatatypeProperty .");
        assertThat(f).extracting(SemanticRules.Finding::detail).containsOnly(SemanticRules.S3);
        assertThat(f)
                .extracting(SemanticRules.Finding::location)
                .containsExactly("resource 'http://ex.org/p'", "resource 'http://ex.org/q'");
    }

    @Test
    void s4LanguagesCaseInsensitiveAndUntagged() throws Exception {
        List<SemanticRules.Finding> f = check(
                "ex:s skos:prefLabel \"a\"@EN , \"b\"@en , \"c\"@en . ex:t skos:prefLabel \"a\" , \"b\" . ex:u skos:prefLabel \"a\" , \"b\"@de .");
        assertThat(f)
                .extracting(SemanticRules.Finding::location)
                .containsExactly(
                        "resource 'http://ex.org/s', language 'en'", "resource 'http://ex.org/t', language 'none'");
    }

    @Test
    void s5OneIssuePerResource() throws Exception {
        List<SemanticRules.Finding> f = check(
                "ex:i a ex:A , ex:B , ex:C . ex:A owl:disjointWith ex:B . ex:C owl:disjointWith ex:A . ex:j a ex:A .");
        assertThat(f).hasSize(1);
        assertThat(f.get(0).location()).isEqualTo("resource 'http://ex.org/i'");
    }

    @Test
    void s6EmptyDocument() throws Exception {
        List<SemanticRules.Finding> f = check("");
        assertThat(f).hasSize(1);
        assertThat(f.get(0).location()).isEqualTo("document");
        assertThat(SemanticRules.check(ParsedTurtle.parse(new byte[0], "http://b/")))
                .hasSize(1);
    }

    @Test
    void syntaxErrorHasLineAndColumn() {
        assertThatThrownBy(() -> ParsedTurtle.parse("ex:a ex:b .".getBytes(), "http://b/"))
                .isInstanceOf(ParsedTurtle.SyntaxError.class)
                .satisfies(e ->
                        assertThat(((ParsedTurtle.SyntaxError) e).location()).startsWith("line 1"));
        assertThatThrownBy(() -> ParsedTurtle.parse(new byte[] {(byte) 0xff, 0x00, 0x13}, "http://b/"))
                .isInstanceOf(ParsedTurtle.SyntaxError.class);
        assertThat(new ParsedTurtle.SyntaxError("m", -1, -1).location()).isEqualTo("document");
        assertThat(new ParsedTurtle.SyntaxError("m", 3, -1).location()).isEqualTo("line 3");
    }

    @Test
    void relativeIrisResolveAgainstBase() throws Exception {
        ParsedTurtle p = ParsedTurtle.parse("<a> <b> <c> .".getBytes(), "https://x.org/content/N/versions/2");
        assertThat(p.triples().get(0).getSubject().getURI()).isEqualTo("https://x.org/content/N/versions/a");
    }
}
