package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.Status;

/**
 * One result of POST /vocabularies/searches (VocabularySearchItem).
 *
 * @param name vocabulary name
 * @param version latest version number or the requested one
 * @param status vocabulary status when latest, else the version status (D38)
 * @param latest true when the reference had no version
 */
public record SearchResult(String name, int version, Status status, boolean latest) {}
