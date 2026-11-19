package eu.simpl.vocabularymanager.web.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * RFC 9457 problem details, in the Belgif profile used by the OpenAPI of this component.
 *
 * @param type problem type URN
 * @param href link to the problem type description, or null
 * @param title short summary
 * @param status HTTP status
 * @param detail human-readable explanation
 * @param instance unique URN of this occurrence
 * @param issues input issues, or null when the problem type has none
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(
        String type,
        String href,
        String title,
        int status,
        String detail,
        String instance,
        List<ProblemIssue> issues) {}
