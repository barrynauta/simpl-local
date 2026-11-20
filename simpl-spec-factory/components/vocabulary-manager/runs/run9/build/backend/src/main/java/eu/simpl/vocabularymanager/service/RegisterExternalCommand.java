package eu.simpl.vocabularymanager.service;

import java.util.List;

/**
 * Request to register an external vocabulary; fields already checked (D56, D73).
 *
 * @param name name
 * @param namespaceUri namespace URI as sent
 * @param aliasUris alias URIs as sent, in order
 * @param sourceUri source URI or null
 * @param description trimmed description or null
 * @param content uploaded content
 * @param createdBy creator (D27)
 */
public record RegisterExternalCommand(
        String name,
        String namespaceUri,
        List<String> aliasUris,
        String sourceUri,
        String description,
        byte[] content,
        String createdBy) {}
