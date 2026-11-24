package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/**
 * A registered external vocabulary (CDM ExternalVocabulary, D30, D56).
 *
 * @param name name, the identity
 * @param namespaceUri namespace URI as sent (D73)
 * @param aliasUris alias URIs as sent, in the order sent
 * @param sourceUri source URI, or null
 * @param description description, or null (D148)
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
