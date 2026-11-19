package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.domain.model.Vocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyVersion;

/**
 * A version with its vocabulary, for latestVersionHref (D5).
 *
 * @param vocabulary the vocabulary
 * @param version the version
 */
public record VersionView(Vocabulary vocabulary, VocabularyVersion version) {}
