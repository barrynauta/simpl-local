package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Persistence of vocabularies, vocabulary versions and external vocabularies. Each write method is one store
 * transaction (D8, D43): it stores the whole change or nothing.
 */
public interface VocabularyStore {

    /** Finds a vocabulary by its exact name (D54). */
    Optional<Vocabulary> findVocabulary(String name);

    /** True when a vocabulary with this name, compared case-insensitively, exists (D37). */
    boolean vocabularyNameTaken(String name);

    /** All vocabularies. */
    List<Vocabulary> listVocabularies();

    /** All versions of a vocabulary, without content. */
    List<VocabularyVersion> listVersions(String name);

    /** One version, without content. */
    Optional<VocabularyVersion> findVersion(String name, int version);

    /** The stored content bytes of a version. */
    Optional<byte[]> findContent(String name, int version);

    /** Stores a new vocabulary with its version 1. */
    void createVocabulary(Vocabulary vocabulary, VocabularyVersion first, byte[] content);

    /**
     * Stores a new version, sets every older ACTIVE version to DEPRECATED with last update date {@code now}, and
     * updates the vocabulary's latest version and last update date (D4).
     */
    void createVersion(VocabularyVersion version, byte[] content, String now);

    /** Sets the vocabulary status and its last update date. */
    void updateVocabularyStatus(String name, Status status, String now);

    /** Sets a version status and the last update dates of the version and the vocabulary. */
    void updateVersionStatus(String name, int version, Status status, String now);

    /** All external vocabularies. */
    List<ExternalVocabulary> listExternalVocabularies();

    /** One external vocabulary by its exact name. */
    Optional<ExternalVocabulary> findExternalVocabulary(String name);

    /** The cached content of an external vocabulary. */
    Optional<byte[]> findExternalContent(String name);

    /** Registers an external vocabulary. */
    void createExternalVocabulary(ExternalVocabulary vocabulary, byte[] content);

    /** Removes an external vocabulary with its content. */
    void deleteExternalVocabulary(String name);

    /** True when the store answers within the timeout. */
    boolean isAvailable(Duration timeout);
}
