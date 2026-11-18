package eu.simpl.vocabularymanager.validation;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Inputs of the reference check (D12).
 *
 * @param selfName name of the vocabulary being created or versioned; its own references are ignored
 * @param internalLookup finds a stored vocabulary by exact name
 * @param externalKeys normalised namespace and alias URIs of all registered external vocabularies
 */
public record ReferenceContext(
        String selfName,
        Function<String, Optional<InternalVocabularyState>> internalLookup,
        Set<String> externalKeys) {}
