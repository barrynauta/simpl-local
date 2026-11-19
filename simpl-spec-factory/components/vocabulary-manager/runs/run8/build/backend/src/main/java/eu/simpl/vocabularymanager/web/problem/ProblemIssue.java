package eu.simpl.vocabularymanager.web.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One issue of a problem (Belgif InputValidationIssue or VocabularyValidationIssue).
 *
 * @param type issue type URN
 * @param href link to the issue type description
 * @param title short summary of the issue type
 * @param detail explanation of this issue
 * @param in where the input was: body, header, path or query
 * @param name name of the input element
 * @param value offending value, when it is safe and useful to echo it
 * @param location location inside uploaded content (vocabulary validation only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemIssue(
        String type,
        String href,
        String title,
        String detail,
        @JsonProperty("in") String in,
        String name,
        Object value,
        String location) {

    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";
    private static final String SCHEMA_VIOLATION_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/schemaViolation.html";
    private static final String INVALID_INPUT_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/invalidInput.html";

    /**
     * An input that breaks the request schema (types, formats, limits, unknown names).
     *
     * @param in location kind
     * @param name input name
     * @param detail explanation
     * @return the issue
     */
    public static ProblemIssue schemaViolation(String in, String name, String detail) {
        return new ProblemIssue(
                SCHEMA_VIOLATION,
                SCHEMA_VIOLATION_HREF,
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                null,
                null);
    }

    /**
     * An input that has the right shape but invalid content (Turtle syntax or semantics).
     *
     * @param in location kind
     * @param name input name
     * @param detail explanation
     * @param location location inside the content, or null
     * @return the issue
     */
    public static ProblemIssue invalidInput(String in, String name, String detail, String location) {
        return new ProblemIssue(INVALID_INPUT, INVALID_INPUT_HREF, "Invalid input", detail, in, name, null, location);
    }

    /**
     * A plain issue without a type, as used by the not-found responses of the OpenAPI.
     *
     * @param in location kind
     * @param name input name
     * @param detail explanation
     * @return the issue
     */
    public static ProblemIssue plain(String in, String name, String detail) {
        return new ProblemIssue(null, null, null, detail, in, name, null, null);
    }
}
