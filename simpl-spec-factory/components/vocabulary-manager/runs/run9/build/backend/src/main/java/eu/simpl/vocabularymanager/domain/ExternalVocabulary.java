package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/**
 * A registered external vocabulary (CDM ExternalVocabulary), without its content.
 *
 * @param name identity
 * @param namespaceUri namespace URI as sent (D73)
 * @param aliasUris alias URIs as sent, in the order sent
 * @param sourceUri where the copy came from, or null
 * @param description description, or null
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
        Instant creationDate) {}
