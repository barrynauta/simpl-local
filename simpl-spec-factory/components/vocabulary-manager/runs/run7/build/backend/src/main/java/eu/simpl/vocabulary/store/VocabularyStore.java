package eu.simpl.vocabulary.store;

import eu.simpl.vocabulary.domain.model.ExternalVocabulary;
import eu.simpl.vocabulary.domain.model.StoredContent;
import eu.simpl.vocabulary.domain.model.Vocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.domain.model.VocabularyVersion;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.ReferenceFacts;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Persistence port of the component (MSV-02: the component owns its store). Every write is one store
 * transaction (D8, D43). Implementations throw {@link StoreUnavailableException} when the store does not
 * answer and {@link StoreException} on other store errors (D44).
 */
public interface VocabularyStore {

    /**
     * @param name exact name (D54)
     * @return the vocabulary with its latest version values
     */
    Optional<Vocabulary> findVocabulary(String name);

    /**
     * @param name a name
     * @return true when a vocabulary has this name, compared case-insensitively (D37)
     */
    boolean vocabularyNameTaken(String name);

    /** @return every vocabulary */
    List<Vocabulary> listVocabularies();

    /**
     * @param name exact name
     * @return every version of the vocabulary, in no particular order
     */
    List<VocabularyVersion> listVersions(String name);

    /**
     * @param name exact name
     * @param version version number
     * @return the version
     */
    Optional<VocabularyVersion> findVersion(String name, long version);

    /**
     * @param name exact name
     * @param version version number
     * @return the stored bytes
     */
    Optional<StoredContent> findContent(String name, long version);

    /**
     * @param name exact name
     * @return the stored bytes of the latest version (D5)
     */
    Optional<StoredContent> findLatestContent(String name);

    /**
     * Stores a vocabulary and its version 1 in one transaction.
     *
     * @param vocabulary the vocabulary
     * @param first version 1
     * @param content the bytes as uploaded
     */
    void createVocabulary(Vocabulary vocabulary, VocabularyVersion first, byte[] content);

    /**
     * Stores a new version and, in the same transaction, deprecates every ACTIVE version and updates the
     * vocabulary's latest version and last update date (D4).
     *
     * @param version the new version
     * @param content the bytes as uploaded
     */
    void createVersion(VocabularyVersion version, byte[] content);

    /**
     * Sets the vocabulary status and last update date (D3).
     *
     * @param name exact name
     * @param status new status
     * @param now change time
     */
    void updateVocabularyStatus(String name, VocabularyStatus status, Instant now);

    /**
     * Sets a version status and the last update dates of the version and the vocabulary (D4).
     *
     * @param name exact name
     * @param version version number
     * @param status new status
     * @param now change time
     */
    void updateVersionStatus(String name, long version, VocabularyStatus status, Instant now);

    /**
     * Loads, in one query, what the reference check needs (Q5).
     *
     * @param internalNames internal vocabulary names referenced
     * @param includeExternal whether registered external keys are needed
     * @return the facts
     */
    ReferenceFacts referenceFacts(Set<String> internalNames, boolean includeExternal);

    /**
     * @param name a name
     * @return true when an external vocabulary has this name, compared case-insensitively (D56)
     */
    boolean externalNameTaken(String name);

    /** @return every external vocabulary */
    List<ExternalVocabulary> listExternal();

    /**
     * @param name exact name
     * @return the external vocabulary
     */
    Optional<ExternalVocabulary> findExternal(String name);

    /**
     * @param name exact name
     * @return the stored bytes
     */
    Optional<StoredContent> findExternalContent(String name);

    /**
     * Registers an external vocabulary in one transaction.
     *
     * @param external the metadata
     * @param content the bytes as uploaded
     */
    void createExternal(ExternalVocabulary external, byte[] content);

    /**
     * Removes metadata and content of an external vocabulary in one transaction.
     *
     * @param name exact name
     */
    void deleteExternal(String name);

    /** @return true when the store answers within the health timeout */
    boolean ping();
}
