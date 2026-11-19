package eu.simpl.vocabulary.domain.model;

import java.time.Instant;

/**
 * A vocabulary (CDM InternalVocabulary) with the values of its latest version that lists show (D25, D28).
 *
 * @param name the identity
 * @param status vocabulary status (D3)
 * @param createdBy creator of version 1 (D27)
 * @param creationDate creation time of version 1
 * @param lastUpdateDate last change of the vocabulary or any version
 * @param latestVersion highest version number (D5)
 * @param latestDescription description of the latest version (D28)
 */
public record Vocabulary(
        String name,
        VocabularyStatus status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        long latestVersion,
        String latestDescription) {}
