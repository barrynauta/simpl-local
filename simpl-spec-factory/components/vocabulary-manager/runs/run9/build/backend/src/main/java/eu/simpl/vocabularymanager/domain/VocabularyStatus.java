package eu.simpl.vocabularymanager.domain;

import java.util.Optional;

/** Vocabulary status and version status share one value set (glossary state tables, D3, D4). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /** Parses the wire value; only the exact upper-case names are accepted. */
    public static Optional<VocabularyStatus> fromWire(String value) {
        for (VocabularyStatus status : values()) {
            if (status.name().equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
