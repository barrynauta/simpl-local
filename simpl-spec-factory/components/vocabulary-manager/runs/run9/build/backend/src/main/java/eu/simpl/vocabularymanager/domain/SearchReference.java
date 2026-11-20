package eu.simpl.vocabularymanager.domain;

/**
 * One reference of a vocabulary search (D38).
 *
 * @param name vocabulary name, exact match (D54)
 * @param version version number, or null for the latest version
 */
public record SearchReference(String name, Integer version) {}
