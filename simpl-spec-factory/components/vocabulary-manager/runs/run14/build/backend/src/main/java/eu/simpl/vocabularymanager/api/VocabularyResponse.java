package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Wire form of a vocabulary (OpenAPI schema VocabularyCategory, D25).
 *
 * @param href link to the vocabulary
 * @param contentHref link to the content of the latest version
 * @param name name
 * @param version latest version number
 * @param description description of the latest version
 * @param status vocabulary status
 * @param createdBy creator of version 1
 * @param creationDate creation date
 * @param lastUpdateDate last update date
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "href",
    "contentHref",
    "name",
    "version",
    "description",
    "status",
    "createdBy",
    "creationDate",
    "lastUpdateDate"
})
public record VocabularyResponse(
        String href,
        String contentHref,
        String name,
        long version,
        String description,
        String status,
        String createdBy,
        String creationDate,
        String lastUpdateDate) {}
