package eu.simpl.vocabularymanager.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/**
 * RFC 9457 problem details body with the Belgif issues extension.
 *
 * @param type problem type URN
 * @param href link to the problem type documentation
 * @param title short title
 * @param status HTTP status
 * @param detail human-readable detail
 * @param instance unique URN of this occurrence
 * @param issues issues, or null when there are none
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(
        String type, String href, String title, int status, String detail, String instance, List<Issue> issues) {

    /**
     * Creates a problem with a fresh instance id.
     *
     * @param type problem type URN
     * @param href link to the problem type documentation, may be null
     * @param title short title
     * @param status HTTP status
     * @param detail detail text
     * @param issues issues, may be null or empty
     * @return the problem
     */
    public static Problem of(String type, String href, String title, int status, String detail, List<Issue> issues) {
        List<Issue> list = issues == null || issues.isEmpty() ? null : List.copyOf(issues);
        return new Problem(type, href, title, status, detail, "urn:uuid:" + UUID.randomUUID(), list);
    }
}
