package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * What the store knows about the vocabularies and namespaces an upload references, read with one store
 * query per upload (Q5, D12, D38).
 *
 * @param internal vocabularies by exact name
 * @param registeredExternalKeys URI keys (D30) of registered external namespaces and aliases that matched
 */
public record ReferenceState(Map<String, InternalState> internal, Set<String> registeredExternalKeys) {

    public ReferenceState {
        internal = Map.copyOf(internal);
        registeredExternalKeys = Set.copyOf(registeredExternalKeys);
    }

    /** One stored vocabulary with the status of each version. */
    public record InternalState(
            String name, VocabularyStatus status, int latestVersion, TreeMap<Integer, VocabularyStatus> versions) {

        /** The highest-numbered ACTIVE version, or null (D5). */
        public Integer latestActiveVersion() {
            for (Integer version : versions.descendingKeySet()) {
                if (versions.get(version) == VocabularyStatus.ACTIVE) {
                    return version;
                }
            }
            return null;
        }
    }
}
