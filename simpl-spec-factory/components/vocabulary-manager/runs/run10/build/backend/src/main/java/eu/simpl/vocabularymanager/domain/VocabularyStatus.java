package eu.simpl.vocabularymanager.domain;

/** Vocabulary status and version status share these values (D3, glossary state tables). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /** Parses a stored or requested value; returns null for anything else. */
    public static VocabularyStatus parse(Object value) {
        if (value instanceof String text) {
            for (VocabularyStatus status : values()) {
                if (status.name().equals(text)) {
                    return status;
                }
            }
        }
        return null;
    }
}
