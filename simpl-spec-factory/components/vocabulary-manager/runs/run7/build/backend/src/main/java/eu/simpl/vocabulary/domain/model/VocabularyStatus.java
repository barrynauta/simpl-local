package eu.simpl.vocabulary.domain.model;

import java.util.Optional;

/** Vocabulary status and version status share these values (D3, D6, glossary state tables). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /**
     * Parses a wire value; only the exact upper-case values are accepted.
     *
     * @param value the value
     * @return the status, empty when unknown
     */
    public static Optional<VocabularyStatus> parse(String value) {
        for (VocabularyStatus status : values()) {
            if (status.name().equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
