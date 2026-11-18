package eu.simpl.vocabularymanager.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One issue of a problem response (OpenAPI ProblemIssue, InputValidationIssue, VocabularyValidationIssue).
 *
 * @param type issue type URN
 * @param href documentation link
 * @param title issue title
 * @param detail human readable detail
 * @param in where the input is: body, path, query or header
 * @param name name of the input
 * @param value offending value, may be null
 * @param location location inside uploaded content, may be null
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

    private static final String BELGIF_ISSUES = "https://www.belgif.be/specification/rest/api-guide/issues/";
    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";

    /** An input that breaks the request schema (D49, D52, D37). */
    public static Issue schemaViolation(String in, String name, String detail, Object value) {
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

    /** A validation error in uploaded content (D9, D10). */
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

    /** A plain issue pointing at an input, without a type (404 bodies of the OpenAPI). */
    public static Issue pointer(String in, String name, String detail) {
        return new Issue(null, null, null, detail, in, name, null, null);
    }
}
