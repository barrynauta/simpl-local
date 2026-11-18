package eu.simpl.vocabularymanager.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** The closed set of validation warning codes (decision D10). */
public enum WarningCode {
    EXTERNAL_REFERENCE_SKIPPED("externalReferenceSkipped"),
    DEPRECATED_VOCABULARY_REFERENCE("deprecatedVocabularyReference"),
    /** Reserved; not emitted in this build. */
    SEMANTIC_REASONING_SKIPPED("semanticReasoningSkipped");

    private final String wire;

    WarningCode(String wire) {
        this.wire = wire;
    }

    @JsonValue
    public String wire() {
        return wire;
    }
}
