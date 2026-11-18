package eu.simpl.vocabularymanager.service;

/**
 * One reference of POST /vocabularies/searches.
 *
 * @param name exact vocabulary name
 * @param version version number, or null for the latest version
 */
public record SearchReference(String name, Integer version) {}
