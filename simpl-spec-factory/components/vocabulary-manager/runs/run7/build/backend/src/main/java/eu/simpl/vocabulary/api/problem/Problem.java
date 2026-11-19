package eu.simpl.vocabulary.api.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * RFC 9457 problem details body, in the Belgif profile.
 *
 * @param type problem type URN
 * @param href link to the problem type documentation
 * @param title short title
 * @param status HTTP status
 * @param detail human readable detail
 * @param instance unique id of this occurrence
 * @param issues issues, omitted when empty
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Problem(
        String type, String href, String title, Integer status, String detail, String instance, List<Issue> issues) {}
