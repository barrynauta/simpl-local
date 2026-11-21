package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/** A vocabulary version (CDM InternalVocabularyVersion, D26). The content is stored apart (D29, D106). */
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
