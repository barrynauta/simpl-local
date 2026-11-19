package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary version (CDM InternalVocabularyVersion), immutable apart from its status (decision D26).
 *
 * @param name the vocabulary name
 * @param version the version number (decision D7)
 * @param status the version status
 * @param description the version description (decision D28)
 * @param changelog what changed in this version
 * @param createdBy the creator (decision D27)
 * @param creationDate creation time
 * @param lastUpdateDate last status change, or creation time
 * @param contentSha256 SHA-256 of the stored content bytes, hex
 */
public record VocabularyVersion(
        String name,
        int version,
        VocabularyStatus status,
        String description,
        String changelog,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        String contentSha256) {}
