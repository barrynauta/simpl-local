package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.Model.Status;
import eu.simpl.vocabularymanager.domain.Model.Warning;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.store.ReferenceStore;
import eu.simpl.vocabularymanager.store.ReferenceStore.InternalInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Reference classification (D12, D69, D145, D146) and the warning texts (D11, D47). */
class ReferencesTest {

    static final String BASE = "https://authority.example/vocabulary-manager";

    static ParsedTurtle.Prefix p(String prefix, String ns, int pos) {
        return new ParsedTurtle.Prefix(prefix, ns, pos);
    }

    @Test
    void classify() {
        List<References.Ref> refs = References.classify(
                List.of(
                        p("a", BASE + "/content/Health", 1),
                        p("b", BASE + "/content/Health/versions/2#", 2),
                        p("c", BASE + "/content/Health/other/path", 3),
                        p("d", BASE + "/content/Health/versions/x", 4),
                        p("e", BASE + "/content/", 5),
                        p("f", BASE + "/content/lower#", 6),
                        p("g", BASE + "/content/Health", 7),
                        p("h", "http://purl.org/dc/terms/", 8)),
                BASE);
        assertThat(refs).hasSize(7);
        assertThat(refs.get(0).internalName()).isEqualTo("Health");
        assertThat(refs.get(0).versionText()).isNull();
        assertThat(refs.get(1).versionNumber()).isEqualTo(2);
        assertThat(refs.get(2).versionText()).isNull();
        assertThat(refs.get(3).versionNumber()).isEqualTo(-1);
        assertThat(refs.get(4).internal()).isFalse();
        assertThat(refs.get(5).internal()).isFalse();
        assertThat(refs.get(6).location()).isEqualTo("prefix 'h' -> 'http://purl.org/dc/terms/'");
        assertThat(References.normalise("http://a/#")).isEqualTo("http://a/");
        assertThat(new References.Ref("p", "n", 1, "N", "99999999999").versionNumber())
                .isEqualTo(-1);
    }

    ContentValidator validator(ReferenceStore.Lookup lookup) {
        ReferenceStore store = mock(ReferenceStore.class);
        when(store.lookup(any(), any(), anyBoolean(), any())).thenReturn(lookup);
        return new ContentValidator(store, new AppProperties(BASE, null, null, null));
    }

    static byte[] file(String prefixes) {
        return (prefixes + "@prefix own: <http://own.org/> .\nown:a own:b own:c .\n").getBytes();
    }

    @Test
    void deprecatedReferenceTexts() {
        InternalInfo info = new InternalInfo(
                Status.ACTIVE, 3, Map.of(1, Status.DEPRECATED, 2, Status.ACTIVE, 3, Status.DEPRECATED));
        ContentValidator v = validator(new ReferenceStore.Lookup(Map.of("Old", info), Set.of()));
        List<Warning> w = v.validateInternal(
                file("@prefix o: <" + BASE + "/content/Old/versions/1#> .\n@prefix l: <" + BASE + "/content/Old> .\n"),
                "Mine",
                1);
        assertThat(w).hasSize(2);
        assertThat(w.get(0).description())
                .isEqualTo(
                        "Referenced internal vocabulary 'Old' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(w.get(1).description())
                .isEqualTo(
                        "Referenced internal vocabulary 'Old' version 3 is deprecated. Use version 2, the latest ACTIVE version.");

        InternalInfo deprecatedVocabulary = new InternalInfo(Status.DEPRECATED, 1, Map.of(1, Status.ACTIVE));
        ContentValidator v2 = validator(new ReferenceStore.Lookup(Map.of("Old", deprecatedVocabulary), Set.of()));
        List<Warning> w2 = v2.validateInternal(file("@prefix o: <" + BASE + "/content/Old> .\n"), "Mine", 1);
        assertThat(w2.get(0).description())
                .isEqualTo(
                        "Referenced internal vocabulary 'Old' version 1 is deprecated. The vocabulary 'Old' is deprecated.");

        InternalInfo noActive = new InternalInfo(Status.ACTIVE, 1, Map.of(1, Status.DEPRECATED));
        ContentValidator v3 = validator(new ReferenceStore.Lookup(Map.of("Old", noActive), Set.of()));
        assertThat(v3.validateInternal(file("@prefix o: <" + BASE + "/content/Old> .\n"), "Mine", 1)
                        .get(0)
                        .description())
                .isEqualTo("Referenced internal vocabulary 'Old' version 1 is deprecated.");
    }

    @Test
    void missingReferencesAreErrors() {
        ContentValidator v = validator(new ReferenceStore.Lookup(Map.of(), Set.of("http://reg.org/ns")));
        try {
            v.validateInternal(
                    file("@prefix a: <"
                            + BASE
                            + "/content/Gone> .\n@prefix b: <"
                            + BASE
                            + "/content/Gone/versions/x> .\n@prefix r: <http://reg.org/ns#> .\n@prefix s: <"
                            + BASE
                            + "/content/Mine/versions/9> .\n"),
                    "Mine",
                    1);
        } catch (ApiException e) {
            assertThat(e.issues()).hasSize(2);
            assertThat(e.issues().get(0).detail()).isEqualTo("Referenced internal vocabulary 'Gone' does not exist.");
            assertThat(e.issues().get(1).detail())
                    .isEqualTo("Referenced internal vocabulary 'Gone' version x does not exist.");
            return;
        }
        throw new AssertionError("expected 400");
    }

    @Test
    void externalAndW3cNamespaces() {
        ContentValidator v = validator(new ReferenceStore.Lookup(Map.of(), Set.of()));
        List<Warning> w = v.validateInternal(
                file(
                        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix x: <http://x.org/> .\n@prefix y: <http://x.org/> .\n"),
                "Mine",
                1);
        assertThat(w).hasSize(1);
        assertThat(w.get(0).code()).isEqualTo("externalReferenceSkipped");
        assertThat(w.get(0).location()).isEqualTo("prefix 'x' -> 'http://x.org/'");
    }

    @Test
    void externalSyntaxOnly() {
        ContentValidator v = validator(new ReferenceStore.Lookup(Map.of(), Set.of()));
        v.validateExternal("<a> <b> <c> .".getBytes(), "http://ns.org/");
        try {
            v.validateExternal("".getBytes(), "http://ns.org/");
        } catch (ApiException e) {
            assertThat(e.issues().get(0).location()).isEqualTo("document");
            return;
        }
        throw new AssertionError("expected 400");
    }

    @Test
    void reportIsTruncatedAt1000() {
        StringBuilder sb = new StringBuilder(
                "@prefix ex: <http://ex.org/> .\n@prefix owl: <http://www.w3.org/2002/07/owl#> .\nex:p a owl:ObjectProperty .\n");
        for (int i = 0; i < 1100; i++) {
            sb.append("ex:s").append(i).append(" ex:p \"x\" .\n");
        }
        ContentValidator v = validator(new ReferenceStore.Lookup(Map.of(), Set.of()));
        try {
            v.validateInternal(sb.toString().getBytes(), "Mine", 1);
        } catch (ApiException e) {
            assertThat(e.issues()).hasSize(1000);
            assertThat(e.truncated()).isTrue();
            return;
        }
        throw new AssertionError("expected 400");
    }
}
