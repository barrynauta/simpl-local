package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.util.Map;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * What the reference check needs to know about one internal vocabulary.
 *
 * @param name the vocabulary name
 * @param status the vocabulary status
 * @param versionStatuses version status by version number
 */
public record InternalVocabularyState(
        String name, VocabularyStatus status, Map<Integer, VocabularyStatus> versionStatuses) {

    public InternalVocabularyState {
        versionStatuses = new TreeMap<>(versionStatuses);
    }

    /** The highest version number (decision D5). */
    public int latestVersion() {
        return ((SortedMap<Integer, VocabularyStatus>) versionStatuses).lastKey();
    }

    /** The highest-numbered version whose version status is ACTIVE (decision D5). */
    public OptionalInt latestActiveVersion() {
        return versionStatuses.entrySet().stream()
                .filter(entry -> entry.getValue() == VocabularyStatus.ACTIVE)
                .mapToInt(Map.Entry::getKey)
                .max();
    }

    /** True when the version's own status or the vocabulary status is DEPRECATED (decision D3). */
    public boolean isEffectivelyDeprecated(int version) {
        return status == VocabularyStatus.DEPRECATED || versionStatuses.get(version) == VocabularyStatus.DEPRECATED;
    }
}
