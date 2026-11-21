package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TurtleSyntaxTest {

    private static ProblemIssue issue(byte[] content) {
        try {
            TurtleSyntax.parse(content, "http://base.example/content/X/versions/1");
        } catch (ApiException e) {
            assertThat(e.problemType().status()).isEqualTo(400);
            assertThat(e.issues()).hasSize(1);
            return e.issues().get(0);
        }
        throw new AssertionError("no syntax error");
    }

    @Test
    void firstErrorGivesLineAndColumn() {
        ProblemIssue issue =
                issue("@prefix ex: <http://e/> .\nex:a ex:b ex:c .\nex:a ex:b \n".getBytes(StandardCharsets.UTF_8));
        assertThat(issue.detail()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issue.location()).startsWith("line 4");
        assertThat(issue.in()).isEqualTo("body");
        assertThat(issue.name()).isEqualTo("vocabularyFile");
        assertThat(issue.type()).isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
    }

    @Test
    void undefinedPrefixIsASyntaxError() {
        ProblemIssue issue = issue("x:a x:b x:c .".getBytes(StandardCharsets.UTF_8));
        assertThat(issue.location()).isEqualTo("line 1, column 1");
    }

    @Test
    void invalidUtf8IsASyntaxErrorWithItsLine() {
        byte[] content = {'#', ' ', 'o', 'k', '\n', '#', ' ', (byte) 0xC3, (byte) 0x28, '\n'};
        ProblemIssue issue = issue(content);
        assertThat(issue.location()).isEqualTo("line 2");
        assertThat(issue.detail()).contains("UTF-8");
    }

    @Test
    void relativeIrisResolveAgainstTheBase() {
        ParsedContent parsed = TurtleSyntax.parse(
                "<a> <b> <c> .".getBytes(StandardCharsets.UTF_8), "http://base.example/content/X/versions/1");
        assertThat(parsed.triples().get(0).getSubject().getURI()).isEqualTo("http://base.example/content/X/versions/a");
    }

    @Test
    void byteOrderMarkIsAccepted() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "<http://a> <http://b> <http://c> .".getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, content, 0, 3);
        System.arraycopy(body, 0, content, 3, body.length);
        assertThat(TurtleSyntax.parse(content, "http://b/").triples()).hasSize(1);
    }

    @Test
    void prefixesAndTriplesKeepFileOrder() {
        ParsedContent parsed = TurtleSyntax.parse(
                ("@prefix a: <http://a/> .\na:x a:y a:z .\n@prefix b: <http://b/> .\nb:x b:y b:z .\na:x a:y a:z .")
                        .getBytes(StandardCharsets.UTF_8),
                "http://b/");
        assertThat(parsed.prefixes())
                .extracting(ParsedContent.PrefixDeclaration::prefix)
                .containsExactly("a", "b");
        assertThat(parsed.triples()).hasSize(2);
        assertThat(parsed.position(parsed.triples().get(0)))
                .isLessThan(parsed.prefixes().get(1).position());
    }

    @Test
    void binaryContentIsRejected() {
        assertThatThrownBy(() -> TurtleSyntax.parse(new byte[] {0, 1, 2, (byte) 0xFF}, "http://b/"))
                .isInstanceOf(ApiException.class);
    }
}
