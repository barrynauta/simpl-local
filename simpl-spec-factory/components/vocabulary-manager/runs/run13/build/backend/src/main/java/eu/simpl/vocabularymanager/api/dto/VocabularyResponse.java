package eu.simpl.vocabularymanager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire form of a vocabulary: the OpenAPI schema VocabularyCategory (D25). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VocabularyResponse(
        String href,
        String contentHref,
        String name,
        Integer version,
        String description,
        String status,
        String createdBy,
        String creationDate,
        String lastUpdateDate) {}
