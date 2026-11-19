package eu.simpl.vocabularymanager.domain;

/**
 * A non-blocking validation warning returned with a created version (decision D10).
 *
 * @param code the warning code, from the closed set
 * @param description human-readable description
 * @param location where in the content
 */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
}
