package eu.simpl.vocabularymanager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Wire form of an external vocabulary (OpenAPI ExternalVocabulary). aliasUris is always present (D110). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExternalVocabularyResponse(
        String href,
        String name,
        String namespaceUri,
        @JsonInclude(JsonInclude.Include.ALWAYS) List<String> aliasUris,
        String sourceUri,
        String description,
        String createdBy,
        String creationDate) {}
