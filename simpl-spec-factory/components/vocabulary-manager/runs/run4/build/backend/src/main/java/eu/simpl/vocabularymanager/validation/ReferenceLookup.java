package eu.simpl.vocabularymanager.validation;

import java.util.Optional;

/** Read access the reference check needs: internal vocabularies by name, registered external namespaces. */
public interface ReferenceLookup {

    Optional<InternalVocabularyState> internalVocabulary(String name);

    /** True when the URI, normalised, is the namespace URI or an alias URI of a registered external vocabulary. */
    boolean isRegisteredExternal(String namespace);
}
