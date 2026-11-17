package eu.simpl.vocabulary.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One entry of the {@code issues} array of a Problem Details response. */
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
    public static final String SCHEMA_VIOLATION_HREF = "https://www.belgif.be/specification/rest/api-guide/issues/schemaViolation.html";
    public static final String INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput";
    public static final String INVALID_INPUT_HREF = "https://www.belgif.be/specification/rest/api-guide/issues/invalidInput.html";

    /** A request schema problem (query, path, form field or JSON body). */
    public static ProblemIssue schemaViolation(String in, String name, Object value, String detail) {
        return new ProblemIssue(SCHEMA_VIOLATION, SCHEMA_VIOLATION_HREF, "Input isn't valid with respect to schema",
                detail, in, name, value, null);
    }

    /** A problem with the uploaded vocabulary content (syntax, structure or semantics). */
    public static ProblemIssue invalidContent(String field, String detail, String location) {
        return new ProblemIssue(INVALID_INPUT, INVALID_INPUT_HREF, "Invalid input", detail, "body", field, null, location);
    }

    public static ProblemIssue simple(String in, String name, String detail) {
        return new ProblemIssue(null, null, null, detail, in, name, null, null);
    }
}
