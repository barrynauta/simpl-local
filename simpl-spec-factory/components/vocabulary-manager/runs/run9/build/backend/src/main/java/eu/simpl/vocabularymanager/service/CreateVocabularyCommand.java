package eu.simpl.vocabularymanager.service;

/**
 * Request to create a vocabulary; fields already checked (D37).
 *
 * @param name vocabulary name
 * @param description trimmed description
 * @param changelog trimmed changelog
 * @param content uploaded content
 * @param createdBy creator (D27)
 */
public record CreateVocabularyCommand(
        String name, String description, String changelog, byte[] content, String createdBy) {}
