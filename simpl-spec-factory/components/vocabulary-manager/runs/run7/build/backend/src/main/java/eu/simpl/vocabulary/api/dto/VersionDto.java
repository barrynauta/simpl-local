package eu.simpl.vocabulary.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wire form of a vocabulary version: schema VocabularyMetadata (D26), with createdBy (D27).
 *
 * @param href link to the version
 * @param contentHref content URL of the version
 * @param latestVersionHref link to the latest version, always filled (D5)
 * @param name name
 * @param version version number
 * @param description description
 * @param changelog changelog
 * @param status version status
 * @param createdBy creator
 * @param creationDate creation time
 * @param lastUpdateDate last update time
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionDto(
        String href,
        String contentHref,
        String latestVersionHref,
        String name,
        long version,
        String description,
        String changelog,
        String status,
        String createdBy,
        String creationDate,
        String lastUpdateDate) {}
