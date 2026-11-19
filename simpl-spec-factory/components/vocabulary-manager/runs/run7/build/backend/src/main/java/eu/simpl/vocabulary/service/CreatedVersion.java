package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.domain.model.VocabularyVersion;
import eu.simpl.vocabulary.domain.validation.ValidationWarning;
import java.util.List;

/**
 * Result of a successful creation: the stored version and the warnings (D10).
 *
 * @param version the new version
 * @param warnings validation warnings, possibly empty
 */
public record CreatedVersion(VocabularyVersion version, List<ValidationWarning> warnings) {}
