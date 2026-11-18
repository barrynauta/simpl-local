package eu.simpl.vocabularymanager.domain;

import java.util.List;

/**
 * A registered external vocabulary (cached copy, D30).
 *
 * @param name name, the identity
 * @param namespaceUri namespace URI as registered
 * @param aliasUris alias URIs as registered
 * @param sourceUri where the copy came from, may be null
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
        String creationDate) {

    /** Removes one trailing '#' or '/' (D30). */
    public static String normalise(String uri) {
        if (uri != null && (uri.endsWith("#") || uri.endsWith("/"))) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    /** All lookup keys: namespace and aliases, normalised. */
    public List<String> lookupKeys() {
        java.util.ArrayList<String> keys = new java.util.ArrayList<>();
        keys.add(normalise(namespaceUri));
        aliasUris.forEach(a -> keys.add(normalise(a)));
        return keys;
    }
}
