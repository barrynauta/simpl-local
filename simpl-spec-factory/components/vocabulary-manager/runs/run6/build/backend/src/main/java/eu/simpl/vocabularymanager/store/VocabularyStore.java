package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The store of vocabularies, versions and external vocabularies (port to the RDF store, decision E3). Each write
 * method is one store transaction (decisions D8, D43, D44). Read methods throw {@link StoreUnavailableException}
 * or {@link StoreFailureException} on store failures.
 */
public interface VocabularyStore {

    /**
     * Finds a vocabulary by its exact name (decision D54).
     *
     * @param name the name
     * @return the vocabulary
     */
    Optional<Vocabulary> findVocabulary(String name);

    /**
     * Tells whether a vocabulary name is taken, compared case-insensitively (decision D37).
     *
     * @param name the name
     * @return true when taken
     */
    boolean vocabularyNameTaken(String name);

    /**
     * Lists vocabularies.
     *
     * @param query filters, order and page
     * @return the page
     */
    Page<Vocabulary> listVocabularies(VocabularyQuery query);

    /**
     * Finds one version.
     *
     * @param name the vocabulary name
     * @param version the version number
     * @return the version
     */
    Optional<VocabularyVersion> findVersion(String name, long version);

    /**
     * Lists the versions of a vocabulary.
     *
     * @param name the vocabulary name
     * @param sort the order
     * @param offset items to skip
     * @param limit items to return
     * @return the page
     */
    Page<VocabularyVersion> listVersions(String name, SortOrder sort, long offset, int limit);

    /**
     * Finds the highest version number with version status ACTIVE (decision D5).
     *
     * @param name the vocabulary name
     * @return the version number
     */
    Optional<Integer> latestActiveVersion(String name);

    /**
     * Reads the stored content bytes of a version.
     *
     * @param name the vocabulary name
     * @param version the version number
     * @return the bytes
     */
    Optional<byte[]> versionContent(String name, long version);

    /**
     * Stores a new vocabulary with its version 1.
     *
     * @param vocabulary the vocabulary
     * @param firstVersion version 1
     * @param content the content bytes
     */
    void createVocabulary(Vocabulary vocabulary, VocabularyVersion firstVersion, byte[] content);

    /**
     * Stores a new version, deprecates every older ACTIVE version and updates the vocabulary (decision D4).
     *
     * @param version the new version
     * @param content the content bytes
     */
    void createVersion(VocabularyVersion version, byte[] content);

    /**
     * Sets the vocabulary status.
     *
     * @param name the vocabulary name
     * @param status the new status
     * @param now the change time
     */
    void updateVocabularyStatus(String name, VocabularyStatus status, Instant now);

    /**
     * Sets a version status and the vocabulary last update date.
     *
     * @param name the vocabulary name
     * @param version the version number
     * @param status the new status
     * @param now the change time
     */
    void updateVersionStatus(String name, int version, VocabularyStatus status, Instant now);

    /**
     * Finds an external vocabulary by exact name.
     *
     * @param name the name
     * @return the external vocabulary
     */
    Optional<ExternalVocabulary> findExternal(String name);

    /**
     * Tells whether an external name is taken, case-insensitively (decision D56).
     *
     * @param name the name
     * @return true when taken
     */
    boolean externalNameTaken(String name);

    /**
     * Finds the external vocabulary whose namespace URI or an alias URI equals the normalised URI (decision D30).
     *
     * @param normalisedUri the URI without a trailing '#' or '/'
     * @return the external vocabulary
     */
    Optional<ExternalVocabulary> findExternalByUri(String normalisedUri);

    /**
     * Finds the external vocabularies matching any of the normalised URIs.
     *
     * @param normalisedUris the URIs
     * @return the matches, unordered
     */
    List<ExternalVocabulary> findExternalsByUris(List<String> normalisedUris);

    /**
     * Lists external vocabularies by name ascending.
     *
     * @param offset items to skip
     * @param limit items to return
     * @return the page
     */
    Page<ExternalVocabulary> listExternals(long offset, int limit);

    /**
     * Stores an external vocabulary.
     *
     * @param external the metadata
     * @param content the content bytes
     */
    void createExternal(ExternalVocabulary external, byte[] content);

    /**
     * Removes an external vocabulary, metadata and content.
     *
     * @param name the name
     */
    void deleteExternal(String name);

    /**
     * Reads the stored content of an external vocabulary.
     *
     * @param name the name
     * @return the bytes
     */
    Optional<byte[]> externalContent(String name);

    /**
     * Probes the store for the health endpoint.
     *
     * @return true when the store answers in time
     */
    boolean isAvailable();
}
