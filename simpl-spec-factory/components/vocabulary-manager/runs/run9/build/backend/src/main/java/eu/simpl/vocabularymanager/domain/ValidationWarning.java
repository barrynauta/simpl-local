package eu.simpl.vocabularymanager.domain;

/**
 * A non-blocking validation warning (D10). Codes are a closed set.
 *
 * @param code warning code
 * @param description human-readable text
 * @param location location text (D65)
 */
public record ValidationWarning(String code, String description, String location) {

    /** Warning code: an external namespace with no registered copy. */
    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";

    /** Warning code: a reference to an effectively deprecated internal version. */
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
}
