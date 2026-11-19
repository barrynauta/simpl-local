package eu.simpl.vocabularymanager.domain;

import java.util.Optional;

/** Vocabulary status and version status share one value set (glossary, D3, D4, DDD-04). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /**
     * @param value a wire value
     * @return the status, empty when the value is not exactly ACTIVE or DEPRECATED
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
