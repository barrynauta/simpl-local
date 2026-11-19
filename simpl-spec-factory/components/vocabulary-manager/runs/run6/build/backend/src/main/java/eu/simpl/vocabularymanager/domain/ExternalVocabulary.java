package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/**
 * A registered cached copy of a vocabulary Simpl-Open does not control (decisions D30, D41, D56).
 *
 * @param name the name, its identity
 * @param namespaceUri the canonical namespace URI as registered
 * @param aliasUris alias URIs as registered, in order
 * @param sourceUri where the copy came from, may be null
 * @param description description, may be null
 * @param createdBy the creator (decision D27)
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

    /** Normalises a namespace or alias URI for comparison: one trailing '#' or '/' is removed (D30). */
    public static String normalise(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}
