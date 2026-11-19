package eu.simpl.vocabulary.domain.model;

import java.time.Instant;

/**
 * A vocabulary version (CDM InternalVocabularyVersion, D26).
 *
 * @param name vocabulary name
 * @param version version number (D7)
 * @param description description (D28)
 * @param changelog changelog
 * @param status version status (D3, D4)
 * @param createdBy creator (D27)
 * @param creationDate creation time
 * @param lastUpdateDate last status change or creation time
 * @param contentSha256 hash of the stored content bytes, for the identical-version check (D46)
 */
public record VocabularyVersion(
        String name,
        long version,
        String description,
        String changelog,
        VocabularyStatus status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        String contentSha256) {}
