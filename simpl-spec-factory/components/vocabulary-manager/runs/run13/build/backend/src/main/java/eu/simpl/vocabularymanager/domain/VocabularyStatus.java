package eu.simpl.vocabularymanager.domain;

/** Status of a vocabulary or of a vocabulary version (D3, glossary state tables). */
public enum VocabularyStatus {
    ACTIVE,
    DEPRECATED;

    /** Parses a stored or requested value; returns null when it is not a status. */
    public static VocabularyStatus parse(String value) {
        for (VocabularyStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
