package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary (CDM InternalVocabulary) with the values D25 takes from its latest version.
 *
 * @param name the identity
 * @param status the vocabulary status (D3)
 * @param latestVersion the latest version number (D5)
 * @param latestDescription the description of the latest version (D28)
 * @param latestContentSha256 hash of the latest version's content bytes (D46)
 * @param createdBy creator of version 1 (D27)
 * @param creationDate creation time of version 1
 * @param lastUpdateDate time of the last change to the vocabulary or any version (D25)
 */
public record Vocabulary(
        String name,
        VocabularyStatus status,
        long latestVersion,
        String latestDescription,
        String latestContentSha256,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate) {}
