package eu.simpl.vocabularymanager.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One issue of an RFC 9457 problem (Belgif InputValidationIssue or VocabularyValidationIssue).
 *
 * @param type issue type URN
 * @param href link to the issue type documentation
 * @param title short title of the issue type
 * @param detail human-readable detail
 * @param in where the input was found: body, header, path or query
 * @param name name of the input
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
        String location) {

    private static final String BELGIF_ISSUES = "https://www.belgif.be/specification/rest/api-guide/issues/";

    /** In-values of the problem issue schema. */
    public static final String IN_BODY = "body";

    public static final String IN_PATH = "path";
    public static final String IN_QUERY = "query";

    /**
     * An input that breaks the schema of the operation (type, format, pattern, limits, required).
     *
     * @param in where the input was found
     * @param name the input name
     * @param detail what is wrong
     * @return the issue
     */
    public static Issue schemaViolation(String in, String name, String detail) {
        return new Issue(
                "urn:problem-type:belgif:input-validation:schemaViolation",
                BELGIF_ISSUES + "schemaViolation.html",
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                null);
    }

    /**
     * An input the operation does not define (decision D52, D21).
     *
     * @param in where the input was found
     * @param name the unknown input name
     * @return the issue
     */
    public static Issue unknownInput(String in, String name) {
        return new Issue(
                "urn:problem-type:belgif:input-validation:unknownInput",
                BELGIF_ISSUES + "unknownInput.html",
                "Unknown input",
                "Input '" + name + "' is not defined for this operation.",
                in,
                name,
                null);
    }

    /**
     * A validation error in uploaded content (decision D10).
     *
     * @param name the form field that carried the content
     * @param detail what is wrong
     * @param location where in the content
     * @return the issue
     */
    public static Issue invalidInput(String name, String detail, String location) {
        return new Issue(
                "urn:problem-type:belgif:input-validation:invalidInput",
                BELGIF_ISSUES + "invalidInput.html",
                "Invalid input",
                detail,
                IN_BODY,
                name,
                location);
    }

    /**
     * A plain issue that names the input without an issue type (used by 404 responses).
     *
     * @param in where the input was found
     * @param name the input name
     * @param detail what is wrong
     * @return the issue
     */
    public static Issue plain(String in, String name, String detail) {
        return new Issue(null, null, null, detail, in, name, null);
    }
}
