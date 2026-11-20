package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.domain.SearchReference;
import eu.simpl.vocabularymanager.domain.SearchResult;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyFilter;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.validation.ReferenceData;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Storage of vocabularies and vocabulary versions. Writes are conditional (D82). */
public interface VocabularyRepository {

    /** Finds a vocabulary by exact name (D54). */
    Optional<Vocabulary> find(String name);

    /** Whether a vocabulary with this name exists, compared case-insensitively (D37). */
    boolean existsIgnoreCase(String name);

    /** One page of the vocabulary list (D85). */
    Page<Vocabulary> list(VocabularyFilter filter, PageRequest page);

    /** One page of the versions of a vocabulary; the vocabulary must exist. */
    Page<VocabularyVersion> listVersions(
            String name, int latestVersion, VocabularyFilter.SortField sort, boolean descending, PageRequest page);

    /** Finds one version. */
    Optional<VocabularyVersion> findVersion(String name, int version);

    /** Content of a version, or of the latest version when version is null. */
    Optional<byte[]> content(String name, Integer version);

    /** Everything the reference check needs, in one store query (Q5, D12). */
    ReferenceData referenceData(Set<String> names, Set<Integer> versions, Set<String> externalKeys);

    /** Resolves the references of a vocabulary search in one store query (D38). */
    List<SearchResult> search(List<SearchReference> references);

    /** Inserts a vocabulary with version 1 unless the name exists case-insensitively. */
    boolean insertVocabulary(NewVersion first);

    /** Inserts a version when the latest version is still the expected one, deprecating older ACTIVE versions. */
    boolean insertVersion(NewVersion version, int expectedLatest);

    /** Changes the vocabulary status when it is still the expected one. */
    boolean updateVocabularyStatus(String name, VocabularyStatus expected, VocabularyStatus next, Instant now);

    /** Changes a version status when it is still the expected one. */
    boolean updateVersionStatus(
            String name, int version, VocabularyStatus expected, VocabularyStatus next, Instant now);
}
