package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import java.util.List;
import java.util.Optional;

/** Data access for external vocabularies. */
public interface ExternalVocabularyRepository {

    List<ExternalVocabulary> findAll();

    Optional<ExternalVocabulary> findByName(String name);

    Optional<byte[]> findContent(String name);

    void create(ExternalVocabulary vocabulary, byte[] content);

    void delete(String name);
}
