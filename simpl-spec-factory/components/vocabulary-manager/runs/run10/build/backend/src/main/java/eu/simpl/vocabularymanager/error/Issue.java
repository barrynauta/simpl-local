package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One issue of an RFC 9457 problem, in the Belgif shape of the OpenAPI (ProblemIssue).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Issue(
        String type, String href, String title, String detail, String in, String name, Object value, String location) {

    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";
    private static final String SCHEMA_VIOLATION_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/schemaViolation.html";
    private static final String INVALID_INPUT_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/invalidInput.html";

    /** A request value that breaks the schema of the decided contract. */
    public static Issue schemaViolation(String in, String name, String detail, Object value) {
        return new Issue(
                SCHEMA_VIOLATION,
                SCHEMA_VIOLATION_HREF,
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                value,
                null);
    }

    /** A validation error of uploaded content or a too large part. */
    public static Issue invalidInput(String name, String detail, String location) {
        return new Issue(INVALID_INPUT, INVALID_INPUT_HREF, "Invalid input", detail, "body", name, null, location);
    }

    /** The short issue of a not-found problem. */
    public static Issue notFound(String in, String name, String detail) {
        return new Issue(null, null, null, detail, in, name, null, null);
    }
}
