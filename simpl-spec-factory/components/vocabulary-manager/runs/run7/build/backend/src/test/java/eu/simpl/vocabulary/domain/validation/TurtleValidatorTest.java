package eu.simpl.vocabulary.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Syntax step and rules S1 to S6 (D9, D53, D57, D65). */
class TurtleValidatorTest {

    private final TurtleValidator validator = new TurtleValidator();
    private static final String BASE = "https://authority.example/vocabulary-manager/content/Test/versions/1";

    private ParsedContent parse(String text) throws SyntaxError {
        return validator.parse(text.getBytes(StandardCharsets.UTF_8), BASE);
    }

    private List<ValidationError> semantic(String text) throws SyntaxError {
        return validator.semanticErrors(parse(text));
    }

    @Test
    void syntaxErrorHasLineAndColumn() {
        assertThatThrownBy(() -> parse("@prefix ex: <http://e.org/> .\nex:a ex:b .\n"))
                .isInstanceOfSatisfying(SyntaxError.class, e -> {
                    assertThat(e.error().detail()).startsWith("Encountered invalid Turtle syntax");
                    assertThat(e.error().location()).isEqualTo("line 2, column 11");
                });
    }

    @Test
    void missingFinalDotIsASyntaxError() {
        assertThatThrownBy(() -> parse("<http://a/s> <http://a/p> <http://a/o>"))
                .isInstanceOf(SyntaxError.class);
    }

    @Test
    void binaryContentIsASyntaxErrorWithPrintableDetail() {
        byte[] bytes = {(byte) 0xff, (byte) 0xd8, 0x01, 0x02, 'x'};
        assertThatThrownBy(() -> validator.parse(bytes, BASE))
                .isInstanceOfSatisfying(
                        SyntaxError.class, e -> assertThat(e.error().detail()).matches("[\\x20-\\x7E]+"));
    }

    @Test
    void illFormedLiteralIsOnlyAWarning() throws SyntaxError {
        ParsedContent parsed = parse("<http://a/s> <http://a/p> \"abc\"^^<http://www.w3.org/2001/XMLSchema#integer> .");
        assertThat(parsed.graph().size()).isEqualTo(1);
    }

    @Test
    void prefixesAreCollectedAndResolvedAgainstTheBase() throws SyntaxError {
        ParsedContent parsed =
                parse("@prefix a: <http://a.org/> .\nPREFIX b: <#>\n@prefix a: <http://a.org/> .\na:x a:y b:z .");
        assertThat(parsed.prefixes())
                .containsExactly(new PrefixDeclaration("a", "http://a.org/"), new PrefixDeclaration("b", BASE + "#"));
        assertThat(parsed.prefixes().get(0).location()).isEqualTo("prefix 'a' -> 'http://a.org/'");
    }

    @Test
    void validContentHasNoErrors() throws SyntaxError {
        assertThat(semantic("""
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                <http://e.org/c> a skos:Concept ; skos:prefLabel "A"@en , "B"@fr , "C" .
                """)).isEmpty();
    }

    @Test
    void emptyContentIsS6() throws SyntaxError {
        assertThat(semantic("@prefix a: <http://a.org/> ."))
                .containsExactly(new ValidationError(TurtleValidator.S6_DETAIL, "document"));
    }

    @Test
    void allRulesRunAndReportEveryError() throws SyntaxError {
        List<ValidationError> errors = semantic("""
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix ex: <http://example.org/> .
                ex:op a owl:ObjectProperty . ex:dp a owl:DatatypeProperty .
                ex:both a owl:ObjectProperty , owl:DatatypeProperty .
                ex:A owl:disjointWith ex:B .
                ex:s ex:op "lit" ; ex:dp ex:o ; ex:dp [ ex:q 1 ] ;
                  skos:prefLabel "a"@en , "b"@EN , "c" , "d" ; a ex:A , ex:B .
                """);
        assertThat(errors)
                .extracting(ValidationError::detail)
                .containsExactly(
                        TurtleValidator.S1_DETAIL,
                        TurtleValidator.S2_DETAIL,
                        TurtleValidator.S2_DETAIL,
                        TurtleValidator.S3_DETAIL,
                        TurtleValidator.S4_DETAIL,
                        TurtleValidator.S4_DETAIL,
                        TurtleValidator.S5_DETAIL);
        assertThat(errors.get(0).location()).isEqualTo("statement 'http://example.org/s http://example.org/op lit'");
        assertThat(errors.get(3).location()).isEqualTo("resource 'http://example.org/both'");
        assertThat(errors)
                .extracting(ValidationError::location)
                .contains("resource 'http://example.org/s', language 'none'", "resource 'http://example.org/s'");
    }

    @Test
    void oneUntaggedAndOneTaggedLabelIsFine() throws SyntaxError {
        assertThat(semantic("""
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                <http://e.org/c> skos:prefLabel "A" , "B"@en .
                """)).isEmpty();
    }

    @Test
    void locationTexts() {
        assertThat(TurtleValidator.location(3, 14)).isEqualTo("line 3, column 14");
        assertThat(TurtleValidator.location(3, -1)).isEqualTo("line 3");
        assertThat(TurtleValidator.location(-1, -1)).isEqualTo("document");
    }

    @Test
    void subjectIrisIgnoreBlankNodes() throws SyntaxError {
        ParsedContent parsed = parse("<http://a/s> <http://a/p> [ <http://a/q> 1 ] .");
        assertThat(TurtleValidator.subjectIris(parsed.graph())).containsExactly("http://a/s");
    }
}
