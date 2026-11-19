package eu.simpl.vocabulary.api.dto;

import java.util.List;

/**
 * Wire form of a creation: schema VocabularyVersionMetadataResponse (D26) with createdBy (D27) and
 * validationWarnings always present (D10).
 *
 * @param href link to the version
 * @param contentHref content URL
 * @param latestVersionHref link to the latest version (the version itself)
 * @param name name
 * @param version version number
 * @param description description
 * @param changelog changelog
 * @param status version status
 * @param createdBy creator
 * @param creationDate creation time
 * @param lastUpdateDate last update time
 * @param validationWarnings warnings, empty array when none
 */
public record CreatedVersionDto(
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
        List<WarningDto> validationWarnings) {}
