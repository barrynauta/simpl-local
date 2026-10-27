package eu.simpl.vocabulary.validation;

import eu.simpl.vocabulary.domain.ExternalVocabulary;
import eu.simpl.vocabulary.domain.Vocabulary;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Read access to stored vocabularies, as needed by semantic validation. */
public interface ReferenceResolver {

    Optional<Vocabulary> findVocabulary(String name);

    /** Local names (fragment or last path segment) of all IRI subjects in the content of a version. */
    Set<String> localNamesOf(String name, int version);

    List<ExternalVocabulary> externalVocabularies();
}
