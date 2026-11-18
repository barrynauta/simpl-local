package eu.simpl.vocabularymanager.service;

import java.util.List;

/**
 * One page of a sorted, filtered list.
 *
 * @param items the items of this page
 * @param page the requested page
 * @param pageSize the requested page size
 * @param total the number of items over all pages
 */
public record Page<T>(List<T> items, int page, int pageSize, long total) {

    public int totalPages() {
        return (int) ((total + pageSize - 1) / pageSize);
    }

    /** Cuts the page out of the complete sorted list. */
    public static <T> Page<T> of(List<T> all, PageRequest request) {
        long from = (long) (request.page() - 1) * request.pageSize();
        List<T> items = from >= all.size()
                ? List.of()
                : all.subList((int) from, (int) Math.min(all.size(), from + request.pageSize()));
        return new Page<>(List.copyOf(items), request.page(), request.pageSize(), all.size());
    }
}
