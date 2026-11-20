package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary version (CDM InternalVocabularyVersion), without its content.
 *
 * @param name vocabulary name
 * @param version version number
 * @param description description of this version
 * @param changelog changelog of this version
 * @param status version status
 * @param createdBy creator (D27)
 * @param creationDate creation time
 * @param lastUpdateDate last status change or creation time
 * @param latestVersion latest version number of the vocabulary, for latestVersionHref (D5)
 */
public record VocabularyVersion(
        String name,
        int version,
        String description,
        String changelog,
        VocabularyStatus status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        int latestVersion) {}
