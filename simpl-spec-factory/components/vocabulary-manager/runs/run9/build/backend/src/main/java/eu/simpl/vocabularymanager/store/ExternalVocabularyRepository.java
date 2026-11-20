package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Storage of registered external vocabularies. */
public interface ExternalVocabularyRepository {

    /** Finds by exact name. */
    Optional<ExternalVocabulary> find(String name);

    /** Whether an external vocabulary with this name exists, case-insensitively (D56). */
    boolean existsIgnoreCase(String name);

    /** One page, ordered by name (D41). */
    Page<ExternalVocabulary> list(PageRequest page);

    /** The keys (D30) among the given ones that a registered namespace or alias already uses. */
    Set<String> existingKeys(Collection<String> keys);

    /** Registered external vocabularies per matching key (namespace or alias). */
    Map<String, ExternalVocabulary> findByKeys(Collection<String> keys);

    /** Content by name. */
    Optional<byte[]> content(String name);

    /** Registers unless the name or a key is taken; returns whether it was stored. */
    boolean insert(ExternalVocabulary vocabulary, byte[] content);

    /** Removes metadata and content. */
    void delete(String name);
}
