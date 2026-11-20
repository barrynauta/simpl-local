package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary (CDM InternalVocabulary) with the values the API takes from its latest version (D25).
 *
 * @param name identity
 * @param status vocabulary status (D3)
 * @param latestVersion highest version number (D5)
 * @param latestDescription description of the latest version (D28)
 * @param latestContentSha256 hash of the latest version's content, for the identical-version check (D46)
 * @param createdBy creator of version 1 (D27)
 * @param creationDate creation time of the vocabulary
 * @param lastUpdateDate last change of the vocabulary or any version
 */
public record Vocabulary(
        String name,
        VocabularyStatus status,
        int latestVersion,
        String latestDescription,
        String latestContentSha256,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate) {}
