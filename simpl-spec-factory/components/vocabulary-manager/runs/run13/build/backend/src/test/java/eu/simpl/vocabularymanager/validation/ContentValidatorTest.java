package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.store.ReferenceState;
import eu.simpl.vocabularymanager.store.ReferenceState.InternalState;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class ContentValidatorTest {

    private static final String BASE = ReferenceAnalyzerTest.BASE;
    private final ReferenceAnalyzer analyzer = new ReferenceAnalyzer(BASE);

    private static InternalState state(String name, VocabularyStatus status, VocabularyStatus... versions) {
        TreeMap<Integer, VocabularyStatus> map = new TreeMap<>();
        for (int i = 0; i < versions.length; i++) {
            map.put(i + 1, versions[i]);
        }
        return new InternalState(name, status, versions.length, map);
    }

    private List<ValidationWarning> validate(String turtle, ReferenceState state) {
        ParsedContent parsed = TurtleSyntax.parse(turtle.getBytes(StandardCharsets.UTF_8), "http://x/");
        return ContentValidator.validate(parsed, analyzer.references(parsed, "Own"), state);
    }

    private static String content(String namespace) {
        return "@prefix r: <" + namespace + "> .\n<http://own.example/a> <http://own.example/b> r:c .\n";
    }

    @Test
    void deprecatedVersionRecommendsLatestActive() {
        ReferenceState state = new ReferenceState(
                Map.of(
                        "Health",
                        state("Health", VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED, VocabularyStatus.ACTIVE)),
                Set.of());
        List<ValidationWarning> warnings = validate(content(BASE + "/content/Health/versions/1#"), state);
        assertThat(warnings).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo("deprecatedVocabularyReference");
            assertThat(w.description())
                    .isEqualTo("Referenced internal vocabulary 'Health' version 1 is deprecated. Use version 2, the"
                            + " latest ACTIVE version.");
            assertThat(w.location()).isEqualTo("prefix 'r' -> '" + BASE + "/content/Health/versions/1#'");
        });
    }

    @Test
    void deprecatedVocabularyRecommendsNothing() {
        ReferenceState state = new ReferenceState(
                Map.of("Health", state("Health", VocabularyStatus.DEPRECATED, VocabularyStatus.ACTIVE)), Set.of());
        assertThat(validate(content(BASE + "/content/Health#"), state).get(0).description())
                .isEqualTo("Referenced internal vocabulary 'Health' version 1 is deprecated. The vocabulary 'Health'"
                        + " is deprecated.");
    }

    @Test
    void deprecatedWithoutActiveVersionHasOnlyTheFirstSentence() {
        ReferenceState state = new ReferenceState(
                Map.of("Health", state("Health", VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED)), Set.of());
        assertThat(validate(content(BASE + "/content/Health/versions/1#"), state)
                        .get(0)
                        .description())
                .isEqualTo("Referenced internal vocabulary 'Health' version 1 is deprecated.");
    }

    @Test
    void missingReferencesAreS7Errors() {
        ReferenceState state = new ReferenceState(
                Map.of("Health", state("Health", VocabularyStatus.ACTIVE, VocabularyStatus.ACTIVE)), Set.of());
        String turtle = "@prefix a: <" + BASE + "/content/Health/versions/9#> .\n"
                + "@prefix b: <" + BASE + "/content/Unknown#> .\n"
                + "@prefix c: <" + BASE + "/content/Unknown/versions/2#> .\n"
                + "@prefix d: <" + BASE + "/content/Health/versions/x1#> .\n"
                + "<http://own.example/a> <http://own.example/b> <http://own.example/c> .\n";
        ApiException error = catchThrowableOfType(ApiException.class, () -> validate(turtle, state));
        assertThat(error.issues())
                .extracting(i -> i.detail())
                .containsExactly(
                        "Referenced internal vocabulary 'Health' version 9 does not exist.",
                        "Referenced internal vocabulary 'Unknown' does not exist.",
                        "Referenced internal vocabulary 'Unknown' version 2 does not exist.",
                        "Referenced internal vocabulary 'Health' version x1 does not exist.");
    }

    @Test
    void externalNamespacesWarnUnlessRegistered() {
        String turtle = "@prefix dc: <http://purl.org/dc/terms/> .\n@prefix sc: <https://schema.org/> .\n"
                + "<http://own.example/a> dc:title sc:x .\n";
        List<ValidationWarning> warnings =
                validate(turtle, new ReferenceState(Map.of(), Set.of("http://purl.org/dc/terms")));
        assertThat(warnings).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo("externalReferenceSkipped");
            assertThat(w.description())
                    .isEqualTo("Cannot resolve external namespace 'https://schema.org/' because no cached external"
                            + " vocabulary is registered.");
        });
    }

    @Test
    void reportIsCappedAt1000IssuesInFileOrder() {
        StringBuilder turtle = new StringBuilder("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n");
        turtle.append("<http://e/p> a owl:ObjectProperty .\n");
        for (int i = 0; i < 1005; i++) {
            turtle.append("<http://e/s").append(i).append("> <http://e/p> \"v\" .\n");
        }
        ApiException error = catchThrowableOfType(
                ApiException.class, () -> validate(turtle.toString(), new ReferenceState(Map.of(), Set.of())));
        assertThat(error.issues()).hasSize(1000);
        assertThat(error.truncated()).isTrue();
        assertThat(error.issues().get(0).location()).contains("http://e/s0 ");
    }
}
