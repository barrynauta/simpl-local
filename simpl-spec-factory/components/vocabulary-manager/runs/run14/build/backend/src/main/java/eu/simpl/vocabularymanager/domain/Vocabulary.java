package eu.simpl.vocabularymanager.domain;

import java.time.Instant;

/**
 * A vocabulary (CDM InternalVocabulary, D25). The latest version number and its description are kept on the
 * vocabulary so lists need no join (D85, D188).
 *
 * @param name vocabulary name, the identity
 * @param status vocabulary status (D3)
 * @param latestVersion number of the latest version (D5)
 * @param latestDescription description of the latest version (D28)
 * @param createdBy creator of version 1 (D27)
 * @param creationDate creation time of version 1
 * @param lastUpdateDate time of the last change to the vocabulary or any version
 */
public record Vocabulary(
        String name,
        VocabularyStatus status,
        long latestVersion,
        String latestDescription,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate) {}
