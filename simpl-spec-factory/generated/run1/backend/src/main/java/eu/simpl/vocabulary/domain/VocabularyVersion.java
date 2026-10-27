package eu.simpl.vocabulary.domain;

import java.time.Instant;

/** One version of an internal vocabulary. */
public record VocabularyVersion(
        int version,
        String description,
        String changelog,
        Status status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate) {

    public VocabularyVersion withStatus(Status newStatus, Instant now) {
        if (newStatus == status) {
            return this;
        }
        return new VocabularyVersion(version, description, changelog, newStatus, createdBy, creationDate, now);
    }
}
