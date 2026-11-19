package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary (CDM InternalVocabulary) with the values it takes from its latest version (decision D25).
 *
 * @param name the vocabulary name, its identity
 * @param status the vocabulary status (decision D3)
 * @param createdBy creator of version 1
 * @param creationDate creation time of version 1
 * @param lastUpdateDate last change to the vocabulary or any of its versions
 * @param latestVersion highest version number (decision D5)
 * @param latestDescription description of the latest version (decision D28)
 */
public record Vocabulary(
        String name,
        VocabularyStatus status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        int latestVersion,
        String latestDescription) {}
