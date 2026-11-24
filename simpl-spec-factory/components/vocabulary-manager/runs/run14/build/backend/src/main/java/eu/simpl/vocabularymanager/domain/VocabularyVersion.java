package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary version (CDM InternalVocabularyVersion, D26).
 *
 * @param name vocabulary name
 * @param version version number (D7)
 * @param status version status (D3)
 * @param description description of this version (D28)
 * @param changelog changelog of this version
 * @param createdBy creator (D27)
 * @param creationDate creation time
 * @param lastUpdateDate time of the last status change of this version
 * @param contentSha256 SHA-256 of the stored content bytes (D194)
 */
public record VocabularyVersion(
        String name,
        long version,
        VocabularyStatus status,
        String description,
        String changelog,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        String contentSha256) {}
