package eu.simpl.vocabularymanager.domain;

/** Vocabulary status and version status (glossary state tables, D3, D4). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /** The other status, the only target of a status change (D4). */
    public VocabularyStatus other() {
        return this == ACTIVE ? DEPRECATED : ACTIVE;
    }

    /** Parses an exact upper-case value, or returns null. */
    public static VocabularyStatus parse(String value) {
        for (VocabularyStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
