package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary version (CDM InternalVocabularyVersion), with the latest version number of its vocabulary for
 * latestVersionHref (D5).
 *
 * @param name the vocabulary name
 * @param version the version number
 * @param status the version status
 * @param description the version description
 * @param changelog the changelog
 * @param createdBy the creator (D27)
 * @param creationDate the creation time
 * @param lastUpdateDate the last status change, or the creation time
 * @param latestVersion the latest version number of the vocabulary
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
        long latestVersion) {}
