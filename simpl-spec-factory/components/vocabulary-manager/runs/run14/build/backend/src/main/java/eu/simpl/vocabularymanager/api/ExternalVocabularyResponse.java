package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Wire form of an external vocabulary (OpenAPI schema ExternalVocabulary, D41, D110, D148).
 *
 * @param href link
 * @param name name
 * @param namespaceUri namespace URI as sent
 * @param aliasUris alias URIs as sent, always present
 * @param sourceUri source URI, omitted when absent
 * @param description description, omitted when absent
 * @param createdBy creator
 * @param creationDate creation date
 */
@JsonPropertyOrder({"href", "name", "namespaceUri", "aliasUris", "sourceUri", "description", "createdBy", "creationDate"
})
public record ExternalVocabularyResponse(
        String href,
        String name,
        String namespaceUri,
        @JsonInclude(JsonInclude.Include.ALWAYS) List<String> aliasUris,
        @JsonInclude(JsonInclude.Include.NON_NULL) String sourceUri,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        String createdBy,
        String creationDate) {}
