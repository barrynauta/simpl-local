package eu.simpl.vocabularymanager.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * RFC 9457 problem details body (OpenAPI Problem).
 *
 * @param type problem type URN
 * @param href documentation link, may be null
 * @param title short title
 * @param status HTTP status
 * @param detail human readable detail
 * @param instance unique id of this occurrence
 * @param issues issues, may be null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(
        String type, String href, String title, int status, String detail, String instance, List<Issue> issues) {}
