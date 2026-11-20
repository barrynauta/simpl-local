package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/** A vocabulary version (CDM InternalVocabularyVersion, D26). Its content is stored apart and never changes. */
public record VocabularyVersion(
        String name,
        int version,
        String description,
        String changelog,
        VocabularyStatus status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        String contentSha256) {}
