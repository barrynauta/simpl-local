package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One issue of a problem detail (Belgif InputValidationIssue and VocabularyValidationIssue). */
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

    private static final String SCHEMA_VIOLATION = "urn:problem-type:belgif:input-validation:schemaViolation";
    private static final String SCHEMA_VIOLATION_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/schemaViolation.html";
    private static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";
    private static final String INVALID_INPUT_HREF =
            "https://www.belgif.be/specification/rest/api-guide/issues/invalidInput.html";

    /** A request value that breaks the decided contract (D21, D37, D49, D52). */
    public static ProblemIssue schemaViolation(String in, String name, String detail, Object value) {
        return new ProblemIssue(
                SCHEMA_VIOLATION,
                SCHEMA_VIOLATION_HREF,
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                value,
                null);
    }

    /** A validation error in uploaded content (D9, D10). */
    public static ProblemIssue invalidInput(String in, String name, String detail, String location) {
        return new ProblemIssue(INVALID_INPUT, INVALID_INPUT_HREF, "Invalid input", detail, in, name, null, location);
    }

    /** A plain issue that only names the request element (404 issues, D41, D66). */
    public static ProblemIssue reference(String in, String name, String detail) {
        return new ProblemIssue(null, null, null, detail, in, name, null, null);
    }
}
