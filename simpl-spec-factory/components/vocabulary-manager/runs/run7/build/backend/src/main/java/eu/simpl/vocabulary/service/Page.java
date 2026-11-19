package eu.simpl.vocabulary.service;

import java.util.List;

/**
 * One page of a sorted list (D22).
 *
 * @param items items of the page
 * @param total number of items in the whole list
 * @param page page number, from 1
 * @param pageSize page size
 * @param <T> item type
 */
public record Page<T>(List<T> items, long total, long page, int pageSize) {

    /**
     * Cuts a page out of a full, sorted list.
     *
     * @param all the full list
     * @param page page number, from 1
     * @param pageSize page size
     * @param <T> item type
     * @return the page, empty when beyond the last page
     */
    public static <T> Page<T> of(List<T> all, long page, int pageSize) {
        if (page - 1 >= (all.size() + (long) pageSize - 1) / pageSize) {
            return new Page<>(List.of(), all.size(), page, pageSize);
        }
        long from = (page - 1) * pageSize;
        int to = (int) Math.min(all.size(), from + pageSize);
        return new Page<>(List.copyOf(all.subList((int) from, to)), all.size(), page, pageSize);
    }

    /** Number of pages; 0 when the list is empty. */
    public long totalPages() {
        return (total + pageSize - 1) / pageSize;
    }
}
