package eu.simpl.vocabularymanager.domain;

/**
 * A blocking validation error of uploaded content (D10).
 *
 * @param detail decided detail text
 * @param location decided location text (D65)
 */
public record ValidationIssue(String detail, String location) {}
