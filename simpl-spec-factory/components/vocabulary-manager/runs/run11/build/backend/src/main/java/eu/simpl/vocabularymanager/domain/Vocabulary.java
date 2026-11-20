package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary (CDM InternalVocabulary, D25) with the values it takes from its latest version.
 *
 * @param name the identity
 * @param latestVersion highest version number (D5)
 * @param description description of the latest version (D28)
 * @param status vocabulary status (D3)
 * @param createdBy creator of version 1 (D27)
 * @param creationDate creation time of version 1
 * @param lastUpdateDate last change to the vocabulary or any version
 */
public record Vocabulary(
        String name,
        int latestVersion,
        String description,
        VocabularyStatus status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate) {}
