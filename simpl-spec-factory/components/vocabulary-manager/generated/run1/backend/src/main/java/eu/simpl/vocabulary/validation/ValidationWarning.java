package eu.simpl.vocabulary.validation;

/** A non-blocking validation finding, shaped as the OpenAPI {@code VocabularyValidationWarning}. */
public record ValidationWarning(String code, String description, String location) {

    public static final String EXTERNAL_REFERENCE_SKIPPED = "externalReferenceSkipped";
}
