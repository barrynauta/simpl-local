package eu.simpl.vocabularymanager.store;

/**
 * A sort field and its direction.
 *
 * @param field the field
 * @param descending true for descending order
 */
public record SortOrder(Field field, boolean descending) {

    /** Fields a list can be sorted on (decision D23). */
    public enum Field {
        NAME,
        STATUS,
        CREATION_DATE,
        LAST_UPDATED,
        VERSION
    }
}
