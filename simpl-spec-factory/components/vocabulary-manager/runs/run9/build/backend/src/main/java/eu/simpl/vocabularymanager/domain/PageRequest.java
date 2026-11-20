package eu.simpl.vocabularymanager.domain;

/**
 * Page selection (D22).
 *
 * @param page page number from 1
 * @param pageSize items per page, 1 to 100
 */
public record PageRequest(int page, int pageSize) {

    /** Offset of the first item of this page. */
    public long offset() {
        return (long) (page - 1) * pageSize;
    }
}
