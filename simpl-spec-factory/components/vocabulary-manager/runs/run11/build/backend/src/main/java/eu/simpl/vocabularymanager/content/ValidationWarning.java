package eu.simpl.vocabularymanager.content;

/**
 * A validation warning (D10): never blocks, returned only in the 201 response. The code is one of
 * the closed set externalReferenceSkipped, deprecatedVocabularyReference, semanticReasoningSkipped.
 */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
}
