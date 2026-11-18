package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/** A vocabulary version (CDM InternalVocabularyVersion) without its content. */
public record VocabularyVersion(
        String name,
        int version,
        String description,
        String changelog,
        VocabularyStatus status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate) {}
