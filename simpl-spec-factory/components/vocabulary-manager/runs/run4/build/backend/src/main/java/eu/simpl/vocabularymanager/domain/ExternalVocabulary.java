package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/** A registered cached copy of a vocabulary Simpl-Open does not control (decision D30). */
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

    /** True when the URI, normalised, equals the namespace URI or one of the alias URIs. */
    public boolean matches(String uri) {
        String key = Namespaces.normalise(uri);
        if (Namespaces.normalise(namespaceUri).equals(key)) {
            return true;
        }
        return aliasUris.stream().map(Namespaces::normalise).anyMatch(key::equals);
    }
}
