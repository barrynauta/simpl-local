package eu.simpl.vocabularymanager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Wire form of a created version: VocabularyVersionMetadataResponse with createdBy, latestVersionHref and
 * validationWarnings always present (D10, D110).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionCreatedResponse(
        String href,
        String contentHref,
        String latestVersionHref,
        String name,
        Integer version,
        String description,
        String changelog,
        String status,
        String createdBy,
        String creationDate,
        String lastUpdateDate,
        @JsonInclude(JsonInclude.Include.ALWAYS) List<WarningResponse> validationWarnings) {}
