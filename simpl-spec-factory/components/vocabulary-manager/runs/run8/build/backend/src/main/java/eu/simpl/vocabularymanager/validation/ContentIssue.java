package eu.simpl.vocabularymanager.validation;

/**
 * A validation error (D10): blocks creation.
 *
 * @param detail the decided detail text
 * @param location the decided location text (D65)
 */
public record ContentIssue(String detail, String location) {}
