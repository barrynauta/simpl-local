package eu.simpl.vocabularymanager.domain;

import java.util.Optional;

/** Status of a vocabulary and of a vocabulary version (glossary state tables, D3, D4). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /** Parses the exact upper-case wire value. */
    public static Optional<VocabularyStatus> parse(String value) {
        for (VocabularyStatus status : values()) {
            if (status.name().equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
