package eu.simpl.vocabularymanager.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Paged collection (OpenAPI CollectionPage). Links are omitted when absent (D22, D38, D59).
 *
 * @param page page number
 * @param totalPages number of pages
 * @param total number of items
 * @param pageSize page size
 * @param self link to this page
 * @param first link to page 1
 * @param prev link to the previous page
 * @param next link to the next page
 * @param last link to the last page
 * @param items the items of this page
 * @param <T> item type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollectionPage<T>(
        int page,
        int totalPages,
        long total,
        int pageSize,
        String self,
        String first,
        String prev,
        String next,
        String last,
        @JsonInclude(JsonInclude.Include.ALWAYS) List<T> items) {

    /**
     * Builds a page of a sorted list (D22, D59).
     *
     * @param all all items in order
     * @param page requested page, at least 1
     * @param pageSize requested page size
     * @param path collection path relative to the API base
     * @param extraQuery further query parameters for the links, already encoded, may be empty
     * @param <T> item type
     * @return the page
     */
    public static <T> CollectionPage<T> of(List<T> all, int page, int pageSize, String path, String extraQuery) {
        int total = all.size();
        int totalPages = (total + pageSize - 1) / pageSize;
        int from = (int) Math.min((long) (page - 1) * pageSize, total);
        int to = (int) Math.min((long) from + pageSize, total);
        List<T> items = all.subList(from, to);
        String prev = null;
        if (total > 0 && page > 1) {
            prev = link(path, Math.min(page - 1, totalPages), pageSize, extraQuery);
        }
        String next = page < totalPages ? link(path, page + 1, pageSize, extraQuery) : null;
        String last = total > 0 ? link(path, totalPages, pageSize, extraQuery) : null;
        return new CollectionPage<>(
                page,
                totalPages,
                total,
                pageSize,
                link(path, page, pageSize, extraQuery),
                link(path, 1, pageSize, extraQuery),
                prev,
                next,
                last,
                List.copyOf(items));
    }

    /** An unpaged search result (D38): page 1, pageSize 100, no links. */
    public static <T> CollectionPage<T> unpaged(List<T> items) {
        int total = items.size();
        return new CollectionPage<>(
                1, total == 0 ? 0 : 1, total, 100, null, null, null, null, null, List.copyOf(items));
    }

    private static String link(String path, int page, int pageSize, String extraQuery) {
        String link = path + "?page=" + page + "&pageSize=" + pageSize;
        return extraQuery.isEmpty() ? link : link + "&" + extraQuery;
    }
}
