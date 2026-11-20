package eu.simpl.vocabularymanager.domain;

/**
 * One result of a vocabulary search (D38).
 *
 * @param name vocabulary name
 * @param version version number
 * @param status vocabulary status when latest, version status otherwise
 * @param latest whether the reference had no version
 */
public record SearchResult(String name, int version, VocabularyStatus status, boolean latest) {}
