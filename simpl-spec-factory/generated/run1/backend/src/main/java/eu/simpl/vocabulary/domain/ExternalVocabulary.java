package eu.simpl.vocabulary.domain;

import java.time.Instant;
import java.util.List;

/** A cached copy of an external vocabulary, used as a validation dependency. */
public record ExternalVocabulary(
        String name,
        String namespaceUri,
        List<String> aliasUris,
        String sourceUri,
        String description,
        String createdBy,
        Instant creationDate) {

    /** Normalisation used for all namespace comparisons: trimmed, without trailing '/' or '#'. */
    public static String normalize(String uri) {
        if (uri == null) {
            return null;
        }
        String s = uri.trim();
        while (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public boolean matchesNamespace(String uri) {
        return normalize(namespaceUri).equals(normalize(uri));
    }

    public boolean matchesAlias(String uri) {
        String n = normalize(uri);
        return aliasUris.stream().map(ExternalVocabulary::normalize).anyMatch(n::equals);
    }

    public boolean matchesAny(String uri) {
        return matchesNamespace(uri) || matchesAlias(uri);
    }
}
