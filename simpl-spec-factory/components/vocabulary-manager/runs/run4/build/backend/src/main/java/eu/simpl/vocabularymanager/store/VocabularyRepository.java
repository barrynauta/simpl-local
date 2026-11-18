package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Data access for vocabularies and vocabulary versions. Every write is one store transaction. */
public interface VocabularyRepository {

    List<Vocabulary> findAll();

    Optional<Vocabulary> findByName(String name);

    /** Finds the stored name that equals the given name ignoring case (decision D37). */
    Optional<String> findNameIgnoringCase(String name);

    List<VocabularyVersion> findVersions(String name);

    Optional<VocabularyVersion> findVersion(String name, int version);

    Optional<byte[]> findContent(String name, int version);

    /** Stores a new vocabulary with its version 1. */
    void create(Vocabulary vocabulary, VocabularyVersion firstVersion, byte[] content);

    /**
     * Stores a new version: the new version ACTIVE, every older ACTIVE version DEPRECATED, the latest version number and
     * the last update date of the vocabulary set, all in one transaction (decision D4).
     */
    void addVersion(VocabularyVersion version, byte[] content);

    void updateVocabularyStatus(String name, VocabularyStatus status, Instant now);

    void updateVersionStatus(String name, int version, VocabularyStatus status, Instant now);
}
