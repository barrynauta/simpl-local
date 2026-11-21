package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary (CDM InternalVocabulary, D25). Description is the latest version's description (D28),
 * createdBy and creationDate are those of version 1 (D27).
 */
public record Vocabulary(
        String name,
        VocabularyStatus status,
        int latestVersion,
        String description,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate) {}
