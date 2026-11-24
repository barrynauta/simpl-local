package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One issue of a problem response (Belgif input validation issue, D10, D49).
 *
 * @param type issue type URN
 * @param href link to the issue type documentation
 * @param title short title of the issue type
 * @param detail human readable detail
 * @param in where the issue is: body, path, query or header
 * @param name the name of the offending field or parameter
 * @param value the offending value, when it is safe to echo
 * @param location location inside uploaded content (validation issues only)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "href", "title", "detail", "in", "name", "value", "location"})
public record Issue(
        String type, String href, String title, String detail, String in, String name, String value, String location) {

    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";
    private static final String SCHEMA_VIOLATION_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/schemaViolation.html";
    private static final String INVALID_INPUT_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/invalidInput.html";

    /** A schema violation of a request value (path, query or body field). */
    public static Issue schemaViolation(String in, String name, String detail) {
        return new Issue(
                SCHEMA_VIOLATION,
                SCHEMA_VIOLATION_HREF,
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                null,
                null);
    }

    /** An invalid input issue, used for content validation errors. */
    public static Issue invalidInput(String in, String name, String detail, String location) {
        return new Issue(INVALID_INPUT, INVALID_INPUT_HREF, "Invalid input", detail, in, name, null, location);
    }

    /** A not-found issue without type (as in the OpenAPI example of the 404 body). */
    public static Issue notFound(String in, String name, String detail) {
        return new Issue(null, null, null, detail, in, name, null, null);
    }
}
