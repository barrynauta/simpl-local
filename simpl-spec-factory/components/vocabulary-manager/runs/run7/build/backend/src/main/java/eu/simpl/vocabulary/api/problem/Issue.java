package eu.simpl.vocabulary.api.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One issue of a problem response (Belgif input validation issue, RFC 9457 extension).
 *
 * @param type issue type URN
 * @param href link to the issue type documentation
 * @param title short title of the issue type
 * @param detail human readable detail
 * @param in where the input is: body, header, path or query
 * @param name name of the input element
 * @param value offending value, when it is safe to echo
 * @param location location inside uploaded content (validation issues only)
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

    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";
    private static final String ISSUE_HREF = "https://www.belgif.be/specification/rest/api-guide/issues/";

    /** In-locations used by issues. */
    public static final String IN_BODY = "body";

    public static final String IN_QUERY = "query";
    public static final String IN_PATH = "path";
    public static final String IN_HEADER = "header";

    /**
     * Builds a schema violation issue for a request element.
     *
     * @param in where the element is
     * @param name element name
     * @param detail explanation
     * @return the issue
     */
    public static Issue schemaViolation(String in, String name, String detail) {
        return new Issue(
                SCHEMA_VIOLATION,
                ISSUE_HREF + "schemaViolation.html",
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                null,
                null);
    }

    /**
     * Builds an invalid input issue for uploaded vocabulary content.
     *
     * @param name the form field
     * @param detail explanation
     * @param location location inside the content
     * @return the issue
     */
    public static Issue invalidInput(String name, String detail, String location) {
        return new Issue(
                INVALID_INPUT,
                ISSUE_HREF + "invalidInput.html",
                "Invalid input",
                detail,
                IN_BODY,
                name,
                null,
                location);
    }

    /**
     * Builds a plain issue without a type, as in the not-found examples of the contract.
     *
     * @param in where the element is
     * @param name element name
     * @param detail explanation
     * @return the issue
     */
    public static Issue plain(String in, String name, String detail) {
        return new Issue(null, null, null, detail, in, name, null, null);
    }
}
