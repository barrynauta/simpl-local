package eu.simpl.vocabularymanager.domain;

/** A validation warning (CDM VocabularyValidationWarning, D10). Returned with a created version, never stored. */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
    public static final String SEMANTIC_REASONING_SKIPPED = "semanticReasoningSkipped";
}
