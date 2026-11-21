package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One issue of a problem (Belgif InputValidationIssue / VocabularyValidationIssue). Null fields are
 * left out of the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Issue(
        String type,
        String href,
        String title,
        String detail,
        @JsonProperty("in") String in,
        String name,
        Object value,
        String location) {

    private static final String ISSUES = "https://www.belgif.be/specification/rest/api-guide/issues/";
    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";

    /** A request that breaks the decided contract (shape, pattern, limits). */
    public static Issue schemaViolation(String in, String name, String detail, Object value) {
        return new Issue(
                SCHEMA_VIOLATION,
                ISSUES + "schemaViolation.html",
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                value,
                null);
    }

    /** A validation error of uploaded content or another invalid input value. */
    public static Issue invalidInput(String in, String name, String detail, String location) {
        return new Issue(
                INVALID_INPUT, ISSUES + "invalidInput.html", "Invalid input", detail, in, name, null, location);
    }

    /** The short issue of a not-found problem (OpenAPI VocabularyNotFoundResponse example). */
    public static Issue notFound(String in, String name, String detail) {
        return new Issue(null, null, null, detail, in, name, null, null);
    }
}
