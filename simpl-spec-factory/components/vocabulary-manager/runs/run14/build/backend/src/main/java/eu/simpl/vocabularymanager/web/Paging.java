package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.PageResponse;
import java.util.List;

/**
 * Paging fields and links (D22, D59, D78, D111): every link carries page and pageSize first, then the other query
 * parameters of the request in request order with the value as sent; only page changes.
 */
public final class Paging {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private Paging() {}

    /**
     * Builds a page response.
     *
     * @param path the collection path relative to the API base, for example /vocabularies
     * @param query the request query
     * @param page the page number
     * @param pageSize the page size
     * @param total the total count
     * @param items the items
     * @param <T> the item type
     * @return the page
     */
    public static <T> PageResponse<T> page(
            String path, QueryParameters query, int page, int pageSize, long total, List<T> items) {
        long totalPages = (total + pageSize - 1) / pageSize;
        String others = others(query);
        String self = link(path, page, pageSize, others);
        String first = link(path, 1, pageSize, others);
        String prev = null;
        if (total > 0 && page > 1) {
            prev = link(path, page > totalPages ? totalPages : page - 1, pageSize, others);
        }
        String next = page < totalPages ? link(path, page + 1L, pageSize, others) : null;
        String last = total > 0 ? link(path, totalPages, pageSize, others) : null;
        return new PageResponse<>(page, pageSize, total, totalPages, self, first, prev, next, last, items);
    }

    /**
     * A search result page (D38, D153): page 1, pageSize 100, no links.
     *
     * @param items the results
     * @param <T> the item type
     * @return the page
     */
    public static <T> PageResponse<T> searchPage(List<T> items) {
        return new PageResponse<>(
                1, MAX_PAGE_SIZE, items.size(), items.isEmpty() ? 0 : 1, null, null, null, null, null, items);
    }

    private static String others(QueryParameters query) {
        StringBuilder text = new StringBuilder();
        for (String name : query.names()) {
            if ("page".equals(name) || "pageSize".equals(name)) {
                continue;
            }
            text.append('&').append(name).append('=').append(query.raw(name));
        }
        return text.toString();
    }

    private static String link(String path, long page, int pageSize, String others) {
        return path + "?page=" + page + "&pageSize=" + pageSize + others;
    }
}
