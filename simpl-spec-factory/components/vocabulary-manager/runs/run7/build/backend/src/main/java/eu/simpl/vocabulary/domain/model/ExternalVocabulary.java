package eu.simpl.vocabulary.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * A registered cached copy of an external vocabulary (D30, D41, D56, D73).
 *
 * @param name identity
 * @param namespaceUri namespace URI as sent
 * @param aliasUris alias URIs as sent, in order
 * @param sourceUri source URI as sent, may be null
 * @param description description, may be null
 * @param createdBy creator (D27)
 * @param creationDate registration time
 */
public record ExternalVocabulary(
        String name,
        String namespaceUri,
        List<String> aliasUris,
        String sourceUri,
        String description,
        String createdBy,
        Instant creationDate) {

    public ExternalVocabulary {
        aliasUris = aliasUris == null ? List.of() : List.copyOf(aliasUris);
    }
}
