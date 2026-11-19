package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Port to the store for the reference check: one store query per upload (Q5, D85). */
public interface ReferenceLookup {

    /**
     * @param internalNames names of referenced internal vocabularies
     * @param versions explicitly referenced version numbers
     * @param externalKeys normalised external namespaces to look up
     * @return what the store knows about them
     */
    State lookup(Set<String> internalNames, Set<Long> versions, Set<String> externalKeys);

    /**
     * @param internal stored internal vocabularies by exact name
     * @param registeredExternalKeys the external keys that match a registered namespace or alias
     */
    record State(Map<String, InternalVocabularyState> internal, Set<String> registeredExternalKeys) {}

    /**
     * @param name the vocabulary name
     * @param status the vocabulary status
     * @param latestVersion the latest version number
     * @param versionStatuses statuses of the requested, latest and ACTIVE versions
     */
    record InternalVocabularyState(
            String name, VocabularyStatus status, long latestVersion, Map<Long, VocabularyStatus> versionStatuses) {

        /**
         * @return the latest ACTIVE version number, when one exists (D5)
         */
        public Optional<Long> latestActiveVersion() {
            return versionStatuses.entrySet().stream()
                    .filter(entry -> entry.getValue() == VocabularyStatus.ACTIVE)
                    .map(Map.Entry::getKey)
                    .max(Long::compare);
        }
    }
}
