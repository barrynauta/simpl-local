package eu.simpl.vocabularymanager.domain;

/**
 * A vocabulary version (CDM InternalVocabularyVersion), without its content (D26).
 *
 * @param name vocabulary name
 * @param version version number
 * @param status version status
 * @param description description of this version
 * @param changelog changelog of this version
 * @param createdBy creator of this version
 * @param creationDate creation time
 * @param lastUpdateDate last status change or creation time
 */
public record VocabularyVersion(
        String name,
        int version,
        Status status,
        String description,
        String changelog,
        String createdBy,
        String creationDate,
        String lastUpdateDate) {}
