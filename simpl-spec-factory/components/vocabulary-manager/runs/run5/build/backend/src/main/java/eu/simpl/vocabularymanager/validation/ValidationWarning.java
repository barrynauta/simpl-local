package eu.simpl.vocabularymanager.validation;

/**
 * A non-blocking validation warning (D10), returned only in the 201 response.
 *
 * @param code warning code from the closed set
 * @param description human readable text
 * @param location where in the content the warning is
 */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
    public static final String SEMANTIC_REASONING_SKIPPED = "semanticReasoningSkipped";
}
