package eu.simpl.vocabularymanager.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * RFC 9457 problem details body (REUSE-04), with the Belgif issues extension.
 *
 * @param type problem type URN
 * @param href documentation link of the type
 * @param title short title
 * @param status HTTP status
 * @param detail human readable detail, never with internal details (BLG-err-sanit)
 * @param instance urn:uuid of this occurrence
 * @param issues issues, or null when there are none
 * @param truncated true when the validation report was cut at 1000 issues (D125)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "href", "title", "status", "detail", "instance", "issues", "truncated"})
public record Problem(
        String type,
        String href,
        String title,
        int status,
        String detail,
        String instance,
        List<Issue> issues,
        Boolean truncated) {}
