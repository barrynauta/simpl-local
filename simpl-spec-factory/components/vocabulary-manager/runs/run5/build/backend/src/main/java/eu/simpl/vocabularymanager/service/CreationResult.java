package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.validation.ValidationWarning;
import java.util.List;

/**
 * A created vocabulary version with its validation warnings (VocabularyVersionMetadataResponse, D26).
 *
 * @param version the stored version
 * @param latestVersion latest version number after the creation
 * @param warnings validation warnings, possibly empty
 */
public record CreationResult(VocabularyVersion version, int latestVersion, List<ValidationWarning> warnings) {}
