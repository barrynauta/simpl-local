package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticRulesTest {

    private static final String PREFIXES = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
            + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
            + "@prefix ex: <http://example.org/> .\n";

    private static List<Finding> check(String body) {
        ParsedContent parsed =
                TurtleSyntax.parse((PREFIXES + body).getBytes(StandardCharsets.UTF_8), "http://base.example/v/1");
        return SemanticRules.check(parsed);
    }

    @Test
    void literalForObjectPropertyIsS1WithStatementLocation() {
        List<Finding> findings = check("ex:p a owl:ObjectProperty .\nex:s ex:p \"text\"@en .\n");
        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.detail()).isEqualTo("Literal value for object property");
            assertThat(f.location()).isEqualTo("statement 'http://example.org/s http://example.org/p \"text\"@en'");
        });
    }

    @Test
    void typedLiteralIsWrittenInNTriplesForm() {
        List<Finding> findings = check("ex:p a owl:ObjectProperty .\nex:s ex:p 5 .\n");
        assertThat(findings.get(0).location())
                .isEqualTo("statement 'http://example.org/s http://example.org/p"
                        + " \"5\"^^<http://www.w3.org/2001/XMLSchema#integer>'");
    }

    @Test
    void resourceForDatatypePropertyIsS2() {
        List<Finding> findings = check("ex:d a owl:DatatypeProperty .\nex:s ex:d ex:o .\nex:t ex:d [] .\n");
        assertThat(findings).hasSize(2).allMatch(f -> f.detail().equals("Resource value for datatype property"));
        assertThat(findings.get(0).location())
                .isEqualTo("statement 'http://example.org/s http://example.org/d http://example.org/o'");
    }

    @Test
    void bothKindsIsOneS3PerProperty() {
        List<Finding> findings = check("ex:p a owl:DatatypeProperty .\nex:p a owl:ObjectProperty .\n");
        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.detail()).isEqualTo("Property declared as both object and datatype property");
            assertThat(f.location()).isEqualTo("resource 'http://example.org/p'");
        });
    }

    @Test
    void duplicatePrefLabelsPerLanguageCaseInsensitive() {
        List<Finding> findings = check("ex:s skos:prefLabel \"a\"@EN, \"b\"@en, \"c\"@en, \"d\"@fr .\n"
                + "ex:t skos:prefLabel \"x\", \"y\" .\nex:u skos:prefLabel \"x\", \"y\"@en .\n");
        assertThat(findings)
                .extracting(Finding::location)
                .containsExactly(
                        "resource 'http://example.org/s', language 'en'",
                        "resource 'http://example.org/t', language 'none'");
        assertThat(findings).allMatch(f -> f.detail().equals("Duplicate preferred label for the same language"));
    }

    @Test
    void disjointClassesIsOneS5PerResource() {
        List<Finding> findings = check("ex:A owl:disjointWith ex:B .\nex:B owl:disjointWith ex:C .\n"
                + "ex:x a ex:A, ex:B, ex:C .\nex:y a ex:A .\n");
        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.detail()).isEqualTo("Resource is an instance of disjoint classes");
            assertThat(f.location()).isEqualTo("resource 'http://example.org/x'");
        });
    }

    @Test
    void emptyContentIsS6() {
        List<Finding> findings = SemanticRules.check(TurtleSyntax.parse(new byte[0], "http://b/"));
        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.detail()).isEqualTo("Vocabulary contains no statements");
            assertThat(f.location()).isEqualTo("document");
        });
        assertThat(check("")).extracting(Finding::detail).containsExactly("Vocabulary contains no statements");
    }

    @Test
    void validContentHasNoFindings() {
        assertThat(check("ex:p a owl:ObjectProperty .\nex:s ex:p ex:o ; skos:prefLabel \"S\"@en, \"S\"@de .\n"))
                .isEmpty();
    }

    @Test
    void formatEscapesLiterals() {
        String formatted =
                SemanticRules.format(org.apache.jena.graph.NodeFactory.createLiteralString("a\"b\\c\nd\re\tf"));
        assertThat(formatted).isEqualTo("\"a\\\"b\\\\c\\nd\\re\\tf\"");
        assertThat(SemanticRules.format(org.apache.jena.graph.NodeFactory.createBlankNode("b1")))
                .isEqualTo("_:b1");
    }
}
