package eu.simpl.vocabularymanager.validation;

/**
 * A validation warning (D10): never blocks, returned only in the 201 response.
 *
 * @param code the warning code, from the closed set
 * @param description human-readable text
 * @param location where it applies
 */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
}
