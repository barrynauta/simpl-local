package eu.simpl.vocabularymanager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire form of a vocabulary version: the OpenAPI schema VocabularyMetadata plus createdBy (D26, D27). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionResponse(
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
        String lastUpdateDate) {}
