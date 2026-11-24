package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Wire form of a vocabulary version (OpenAPI schemas VocabularyMetadata and, with validationWarnings,
 * VocabularyVersionMetadataResponse; D26, D27, D5).
 *
 * @param href link to the version
 * @param contentHref link to the content of the version
 * @param latestVersionHref link to the latest version, always filled (D5)
 * @param name vocabulary name
 * @param version version number
 * @param description description
 * @param changelog changelog
 * @param status version status
 * @param createdBy creator (D27)
 * @param creationDate creation date
 * @param lastUpdateDate last update date
 * @param validationWarnings warnings of a creation, null on other operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "href",
    "contentHref",
    "latestVersionHref",
    "name",
    "version",
    "description",
    "changelog",
    "status",
    "createdBy",
    "creationDate",
    "lastUpdateDate",
    "validationWarnings"
})
public record VersionResponse(
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
        String lastUpdateDate,
        List<WarningResponse> validationWarnings) {}
