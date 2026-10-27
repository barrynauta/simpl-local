package eu.simpl.vocabulary.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** RFC 9457 Problem Details, shaped as the OpenAPI {@code Problem} schema (D5). */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Problem(
        String type,
        String href,
        String title,
        int status,
        String detail,
        String instance,
        List<ProblemIssue> issues,
        List<?> validationWarnings) {
}
