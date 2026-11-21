package eu.simpl.vocabularymanager.domain;

import java.time.Instant;
import java.util.List;

/** A registered external vocabulary (CDM ExternalVocabulary, D30, D56, D73). Optional values may be null. */
public record ExternalVocabulary(
        String name,
        String namespaceUri,
        List<String> aliasUris,
        String sourceUri,
        String description,
        String createdBy,
        Instant creationDate) {

    public ExternalVocabulary {
        aliasUris = List.copyOf(aliasUris);
    }
}
