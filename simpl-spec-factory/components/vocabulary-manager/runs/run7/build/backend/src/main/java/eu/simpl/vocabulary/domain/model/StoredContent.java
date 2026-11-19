package eu.simpl.vocabulary.domain.model;

/**
 * Stored Turtle bytes of a version or an external vocabulary (D29).
 *
 * @param name the vocabulary name
 * @param version the version number, 0 for external vocabularies
 * @param bytes the bytes as uploaded
 */
public record StoredContent(String name, long version, byte[] bytes) {}
