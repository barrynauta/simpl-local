package eu.simpl.vocabularymanager.domain;

/** A validation warning (CDM VocabularyValidationWarning, D10). Not stored. */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
    public static final String DEPRECATED_VOCABULARY_REFERENCE = "deprecatedVocabularyReference";
}
