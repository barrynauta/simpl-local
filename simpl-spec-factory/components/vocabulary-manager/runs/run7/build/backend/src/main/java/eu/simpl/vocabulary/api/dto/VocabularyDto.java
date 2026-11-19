package eu.simpl.vocabulary.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wire form of a vocabulary: schema VocabularyCategory (D25).
 *
 * @param href link to the vocabulary
 * @param contentHref content URL of the latest version
 * @param name name
 * @param version latest version number
 * @param description description of the latest version
 * @param status vocabulary status
 * @param createdBy creator of version 1
 * @param creationDate creation time
 * @param lastUpdateDate last update time
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VocabularyDto(
        String href,
        String contentHref,
        String name,
        long version,
        String description,
        String status,
        String createdBy,
        String creationDate,
        String lastUpdateDate) {}
