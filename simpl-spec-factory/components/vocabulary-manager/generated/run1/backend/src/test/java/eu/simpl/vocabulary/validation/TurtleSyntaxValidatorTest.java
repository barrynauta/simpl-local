package eu.simpl.vocabulary.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TurtleSyntaxValidatorTest {

    private final TurtleSyntaxValidator validator = new TurtleSyntaxValidator();

    @Test
    void validTurtleHasNoErrors() {
        var out = validator.parse("""
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                <https://example.org/x> a owl:Ontology .
                """, "https://example.org/base");
        assertThat(out.errors()).isEmpty();
        assertThat(out.graph().size()).isEqualTo(1);
    }

    @Test
    void reportsLineAndColumn() { // SIMPL-18180 AC5, 18181
        var out = validator.parse("""
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                <https://example.org/x> a owl:Ontology .
                <https://example.org/y> a <https://example.org/C> <https://example.org/D> .
                """, "https://example.org/base");
        assertThat(out.errors()).isNotEmpty();
        assertThat(out.errors().get(0).location()).startsWith("line ");
        assertThat(out.errors().get(0).category()).isEqualTo(ValidationIssue.Category.SYNTAX);
    }

    @Test
    void reportsAllErrorsNotOnlyTheFirst() { // SIMPL-18180 AC7
        var out = validator.parse("""
                @prefix ex: <https://example.org/> .
                ex:a ex:p ex:b .
                ex:c ex:p undefined:x .
                ex:d ex:p ex:e .
                ex:f ex:p other:y .
                ex:g ex:p ex:h .
                """, "https://example.org/base");
        assertThat(out.errors()).hasSize(2);
        assertThat(out.errors().get(0).location()).startsWith("line 3,");
        assertThat(out.errors().get(1).location()).startsWith("line 5,");
    }

    @Test
    void multiLineStatementErrorsAreRecovered() {
        var out = validator.parse("""
                @prefix ex: <https://example.org/> .
                ex:a ex:p ex:b ;
                     ex:q "unterminated .
                ex:c ex:p bad:x .
                """, "https://example.org/base");
        assertThat(out.errors()).hasSizeGreaterThanOrEqualTo(1);
        // Jena reports a newline inside a string at the start of the next line.
        assertThat(out.errors().get(0).location()).matches("line [34], column \\d+");
    }
}
