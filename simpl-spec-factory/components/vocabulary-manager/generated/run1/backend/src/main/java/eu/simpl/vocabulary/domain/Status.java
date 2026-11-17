package eu.simpl.vocabulary.domain;

/** Status of a vocabulary and of a vocabulary version (OpenAPI VocabularyStatus). */
public enum Status {
    ACTIVE,
    DEPRECATED;

    public static Status parse(String value) {
        if (value == null) {
            return null;
        }
        for (Status s : values()) {
            if (s.name().equals(value)) {
                return s;
            }
        }
        return null;
    }
}
