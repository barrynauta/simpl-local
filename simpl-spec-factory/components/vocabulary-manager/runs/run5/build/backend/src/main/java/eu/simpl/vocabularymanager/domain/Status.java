package eu.simpl.vocabularymanager.domain;

/** Vocabulary status and version status (glossary state tables, D3, D4). */
public enum Status {
    ACTIVE,
    DEPRECATED;

    /** Returns the status for a wire value, or null when the value is not a status. */
    public static Status fromWire(String value) {
        for (Status s : values()) {
            if (s.name().equals(value)) {
                return s;
            }
        }
        return null;
    }
}
