package eu.simpl.vocabulary.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StructureValidatorTest {

    private final TurtleSyntaxValidator parser = new TurtleSyntaxValidator();
    private final StructureValidator validator = new StructureValidator();

    @Test
    void ontologyDeclarationIsEnough() { // D16
        var g = parser.parse("<https://e.org/o> a <http://www.w3.org/2002/07/owl#Ontology> .", "https://e.org/").graph();
        assertThat(validator.validate(g)).isEmpty();
    }

    @Test
    void conceptSchemeDeclarationIsEnough() { // D16
        var g = parser.parse("<https://e.org/s> a <http://www.w3.org/2004/02/skos/core#ConceptScheme> .",
                "https://e.org/").graph();
        assertThat(validator.validate(g)).isEmpty();
    }

    @Test
    void emptyGraphFails() {
        var g = parser.parse("# only a comment\n", "https://e.org/").graph();
        assertThat(validator.validate(g)).extracting(ValidationIssue::detail)
                .anyMatch(d -> d.contains("no RDF statements"));
    }

    @Test
    void missingDeclarationFails() {
        var g = parser.parse("<https://e.org/a> <https://e.org/p> \"x\" .", "https://e.org/").graph();
        assertThat(validator.validate(g)).singleElement()
                .satisfies(i -> assertThat(i.location()).isEqualTo("document"));
    }
}
