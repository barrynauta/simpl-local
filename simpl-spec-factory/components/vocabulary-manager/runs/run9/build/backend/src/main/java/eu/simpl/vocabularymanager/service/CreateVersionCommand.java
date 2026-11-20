package eu.simpl.vocabularymanager.service;

/**
 * Request to create a vocabulary version; fields already checked (D37).
 *
 * @param description trimmed description
 * @param changelog trimmed changelog
 * @param content uploaded content
 * @param createdBy creator (D27)
 */
public record CreateVersionCommand(String description, String changelog, byte[] content, String createdBy) {}
