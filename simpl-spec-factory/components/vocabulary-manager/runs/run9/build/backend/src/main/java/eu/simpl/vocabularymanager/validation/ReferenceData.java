package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.util.Map;
import java.util.Set;

/**
 * What the store knows about the references of one upload, read in one query (Q5).
 *
 * @param vocabularies per referenced name that exists
 * @param registeredExternalKeys comparison keys of registered namespaces and aliases that match
 */
public record ReferenceData(Map<String, ReferencedVocabulary> vocabularies, Set<String> registeredExternalKeys) {

    /** Data for an upload without references. */
    public static ReferenceData empty() {
        return new ReferenceData(Map.of(), Set.of());
    }

    /**
     * A referenced vocabulary.
     *
     * @param status vocabulary status
     * @param latestVersion latest version number
     * @param versions status of the versions read: the referenced ones, the latest and the ACTIVE ones
     */
    public record ReferencedVocabulary(
            VocabularyStatus status, int latestVersion, Map<Integer, VocabularyStatus> versions) {

        /** Highest-numbered ACTIVE version, or null (D5). */
        public Integer latestActiveVersion() {
            return versions.entrySet().stream()
                    .filter(e -> e.getValue() == VocabularyStatus.ACTIVE)
                    .map(Map.Entry::getKey)
                    .max(Integer::compare)
                    .orElse(null);
        }
    }
}
