package eu.simpl.vocabulary.domain.validation;

/**
 * A non-blocking validation warning (D10). Codes form a closed set.
 *
 * @param code warning code
 * @param description human readable text
 * @param location where it is (D65)
 */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
    /** Reserved, not emitted in this build (D10). */
    public static final String SEMANTIC_REASONING_SKIPPED = "semanticReasoningSkipped";
}
