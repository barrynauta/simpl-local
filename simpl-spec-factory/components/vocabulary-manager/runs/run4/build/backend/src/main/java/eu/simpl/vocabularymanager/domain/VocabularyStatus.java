package eu.simpl.vocabularymanager.domain;

import java.util.Optional;

/** Status of a vocabulary or of a vocabulary version (glossary: vocabulary status, version status). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /** Parses the exact wire value; anything else is empty. */
    public static Optional<VocabularyStatus> fromWire(String value) {
        for (VocabularyStatus status : values()) {
            if (status.name().equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
