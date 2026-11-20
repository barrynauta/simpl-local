package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.util.List;

/**
 * A created version with its validation warnings (D10, D26).
 *
 * @param version the stored version
 * @param warnings the warnings, possibly empty
 */
public record CreationResult(VocabularyVersion version, List<ValidationWarning> warnings) {}
