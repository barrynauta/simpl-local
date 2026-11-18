package eu.simpl.vocabularymanager.domain;

/**
 * A vocabulary (CDM InternalVocabulary) with the values it takes from its latest version (D25).
 *
 * @param name vocabulary name
 * @param status vocabulary status (D3)
 * @param createdBy creator of version 1 (D27)
 * @param creationDate creation time of version 1
 * @param lastUpdateDate last change to the vocabulary or any version
 * @param latestVersion highest version number (D5)
 * @param latestDescription description of the latest version (D28)
 */
public record Vocabulary(
        String name,
        Status status,
        String createdBy,
        String creationDate,
        String lastUpdateDate,
        int latestVersion,
        String latestDescription) {}
