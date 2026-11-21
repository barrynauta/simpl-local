package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.ExternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.InternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.Reference;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReferenceAnalyzerTest {

    static final String BASE = "https://authority.example/vocabulary-manager";
    private final ReferenceAnalyzer analyzer = new ReferenceAnalyzer(BASE);

    private List<Reference> references(String turtle, String own) {
        return analyzer.references(TurtleSyntax.parse(turtle.getBytes(StandardCharsets.UTF_8), "http://x/"), own);
    }

    @Test
    void recognisesInternalReferenceShapes() {
        String turtle = "@prefix a: <" + BASE + "/content/Health> .\n"
                + "@prefix b: <" + BASE + "/content/Health/versions/2#> .\n"
                + "@prefix c: <" + BASE + "/content/Health/versions/abc/> .\n"
                + "@prefix d: <" + BASE + "/content/Legal/terms/> .\n"
                + "@prefix e: <" + BASE + "/content/lower/> .\n"
                + "@prefix f: <" + BASE + "/content/> .\n"
                + "@prefix g: <" + BASE + "/content/Health/versions/> .\n";
        List<Reference> found = references(turtle, "Own");
        assertThat(found).hasSize(7);
        InternalReference latest = (InternalReference) found.get(0);
        assertThat(latest.name()).isEqualTo("Health");
        assertThat(latest.versionText()).isNull();
        InternalReference versioned = (InternalReference) found.get(1);
        assertThat(versioned.version()).isEqualTo(2);
        InternalReference badVersion = (InternalReference) found.get(2);
        assertThat(badVersion.versionText()).isEqualTo("abc");
        assertThat(badVersion.version()).isNull();
        assertThat(((InternalReference) found.get(3)).name()).isEqualTo("Legal");
        assertThat(found.get(4)).isInstanceOf(ExternalReference.class);
        assertThat(found.get(5)).isInstanceOf(ExternalReference.class);
        assertThat(((InternalReference) found.get(6)).versionText()).isNull();
        assertThat(found.get(1).location()).isEqualTo("prefix 'b' -> '" + BASE + "/content/Health/versions/2#'");
    }

    @Test
    void ignoresOwnVocabularyW3cAndOwnNamespaces() {
        String turtle = "@prefix own: <" + BASE + "/content/Own/versions/1#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix ex: <https://example.eu/own#> .\n"
                + "@prefix ex2: <https://example.eu/own#> .\n"
                + "@prefix dc: <http://purl.org/dc/terms/> .\n"
                + "ex:a rdfs:label \"a\" .\n";
        List<Reference> found = references(turtle, "Own");
        assertThat(found).singleElement().isInstanceOf(ExternalReference.class);
        assertThat(((ExternalReference) found.get(0)).key()).isEqualTo("http://purl.org/dc/terms");
    }

    @Test
    void hugeVersionNumberHasNoVersion() {
        InternalReference reference =
                (InternalReference) references("@prefix a: <" + BASE + "/content/Health/versions/99999999999> .", "Own")
                        .get(0);
        assertThat(reference.version()).isNull();
        assertThat(reference.versionText()).isEqualTo("99999999999");
    }
}
