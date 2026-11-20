package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/** A registered external vocabulary (CDM ExternalVocabulary, D30). URIs are kept exactly as sent (D73). */
public record ExternalVocabulary(
        String name,
        String namespaceUri,
        List<String> aliasUris,
        String sourceUri,
        String description,
        String createdBy,
        Instant creationDate) {

    /** Comparison form of a namespace URI: one trailing '#' or '/' removed (D30). */
    public static String key(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}
