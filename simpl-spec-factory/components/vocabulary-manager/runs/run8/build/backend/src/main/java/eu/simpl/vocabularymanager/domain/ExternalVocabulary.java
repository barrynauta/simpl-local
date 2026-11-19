package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/**
 * A registered external vocabulary (CDM ExternalVocabulary): a cached copy used to resolve references (D30).
 *
 * @param name the identity
 * @param namespaceUri the namespace URI as sent
 * @param aliasUris the alias URIs as sent, in the order sent (D73)
 * @param sourceUri where it was copied from, or null
 * @param description the description, or null
 * @param createdBy the creator (D27)
 * @param creationDate the registration time
 */
public record ExternalVocabulary(
        String name,
        String namespaceUri,
        List<String> aliasUris,
        String sourceUri,
        String description,
        String createdBy,
        Instant creationDate) {}
