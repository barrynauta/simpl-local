package eu.simpl.vocabularymanager.domain;

import java.util.Optional;

/** Status of a vocabulary and of a vocabulary version (glossary state tables, decision D3). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /**
     * Parses the wire value; only the exact upper-case values are accepted.
     *
     * @param value the value from the request
     * @return the status, or empty when the value is not a status
     */
    public static Optional<VocabularyStatus> fromWire(String value) {
        for (VocabularyStatus status : values()) {
            if (status.name().equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
