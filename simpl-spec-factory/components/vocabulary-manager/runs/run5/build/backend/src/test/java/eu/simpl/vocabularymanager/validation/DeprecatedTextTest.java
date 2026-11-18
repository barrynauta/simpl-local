package eu.simpl.vocabularymanager.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Texts of D47. */
class DeprecatedTextTest {

    @Test
    void missingTexts() {
        assertThat(ReferenceChecker.missingText("A", null))
                .isEqualTo("Referenced internal vocabulary 'A' does not exist.");
        assertThat(ReferenceChecker.missingText("A", 2))
                .isEqualTo("Referenced internal vocabulary 'A' version 2 does not exist.");
    }

    @Test
    void prefixLocation() {
        assertThat(new PrefixDeclaration("p", "http://x#").location()).isEqualTo("prefix 'p' -> 'http://x#'");
    }
}
