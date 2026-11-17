package eu.simpl.vocabulary.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A vocabulary aggregate: the vocabulary-level metadata plus all its versions (ascending by number).
 * The vocabulary status is stored separately from the version statuses (D10).
 */
public record Vocabulary(
        String name,
        Status status,
        String createdBy,
        Instant creationDate,
        Instant lastUpdateDate,
        List<VocabularyVersion> versions) {

    public Vocabulary {
        versions = versions.stream().sorted(Comparator.comparingInt(VocabularyVersion::version)).toList();
    }

    public VocabularyVersion latest() {
        return versions.get(versions.size() - 1);
    }

    public int latestVersionNumber() {
        return latest().version();
    }

    /** The description of a vocabulary is the description of its latest version. */
    public String description() {
        return latest().description();
    }

    public Optional<VocabularyVersion> version(int number) {
        return versions.stream().filter(v -> v.version() == number).findFirst();
    }

    /** Highest-numbered ACTIVE version, if any. */
    public Optional<VocabularyVersion> latestActive() {
        return versions.stream().filter(v -> v.status() == Status.ACTIVE)
                .max(Comparator.comparingInt(VocabularyVersion::version));
    }
}
