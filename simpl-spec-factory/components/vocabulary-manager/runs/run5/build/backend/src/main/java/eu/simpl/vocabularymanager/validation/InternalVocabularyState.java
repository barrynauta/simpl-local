package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.util.List;
import java.util.Optional;

/**
 * What the reference check needs to know of a referenced internal vocabulary.
 *
 * @param vocabulary the vocabulary
 * @param versions all its versions
 */
public record InternalVocabularyState(Vocabulary vocabulary, List<VocabularyVersion> versions) {

    /** One version by number. */
    public Optional<VocabularyVersion> version(int number) {
        return versions.stream().filter(v -> v.version() == number).findFirst();
    }

    /** The highest-numbered version with version status ACTIVE (D5). */
    public Optional<Integer> latestActiveVersion() {
        return versions.stream()
                .filter(v -> v.status() == Status.ACTIVE)
                .map(VocabularyVersion::version)
                .max(Integer::compare);
    }
}
