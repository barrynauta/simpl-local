package eu.simpl.vocabulary.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Wire form of an external vocabulary: schema ExternalVocabulary (D41).
 *
 * @param href link
 * @param name name
 * @param namespaceUri namespace URI as sent
 * @param aliasUris alias URIs as sent, in order
 * @param sourceUri source URI, omitted when not given
 * @param description description, omitted when not given
 * @param createdBy creator
 * @param creationDate registration time
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExternalVocabularyDto(
        String href,
        String name,
        String namespaceUri,
        List<String> aliasUris,
        String sourceUri,
        String description,
        String createdBy,
        String creationDate) {}
