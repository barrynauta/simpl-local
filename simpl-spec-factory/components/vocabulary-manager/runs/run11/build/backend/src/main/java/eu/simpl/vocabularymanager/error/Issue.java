package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/** One issue of a problem response (Belgif InputValidationIssue or VocabularyValidationIssue). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Issue(String type, String href, String title, String detail, String in, String name, String location) {

    public static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";

    private static final String SCHEMA_VIOLATION_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/schemaViolation.html";
    private static final String INVALID_INPUT_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/invalidInput.html";

    /** A request shape issue: a parameter or field breaks the decided contract. */
    public static Issue schema(String in, String name, String detail) {
        return new Issue(
                SCHEMA_VIOLATION,
                SCHEMA_VIOLATION_HREF,
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                null);
    }

    /** A content validation issue (syntax or semantic error) with a location. */
    public static Issue invalidInput(String name, String detail, String location) {
        return new Issue(INVALID_INPUT, INVALID_INPUT_HREF, "Invalid input", detail, "body", name, location);
    }

    /** A plain issue without a type, as in the not-found examples of the contract. */
    public static Issue plain(String in, String name, String detail) {
        return new Issue(null, null, null, detail, in, name, null);
    }
}
