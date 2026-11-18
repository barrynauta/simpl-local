package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** One issue of a problem response (Belgif InputValidationIssue, VocabularyValidationIssue). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "href", "title", "detail", "in", "name", "value", "location"})
public record Issue(
        String type, String href, String title, String detail, String in, String name, Object value, String location) {

    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";
    private static final String BELGIF_ISSUES = "https://www.belgif.be/specification/rest/api-guide/issues/";

    /** A request value that does not meet the schema of the operation. */
    public static Issue schemaViolation(String in, String name, Object value, String detail) {
        return new Issue(
                SCHEMA_VIOLATION,
                BELGIF_ISSUES + "schemaViolation.html",
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                value,
                null);
    }

    /** A validation error found in uploaded vocabulary content. */
    public static Issue invalidInput(String name, String detail, String location) {
        return new Issue(
                INVALID_INPUT,
                BELGIF_ISSUES + "invalidInput.html",
                "Invalid input",
                detail,
                "body",
                name,
                null,
                location);
    }

    /** A plain issue that only says where the problem is (used by not found responses). */
    public static Issue at(String in, String name, Object value, String detail) {
        return new Issue(null, null, null, detail, in, name, value, null);
    }
}
