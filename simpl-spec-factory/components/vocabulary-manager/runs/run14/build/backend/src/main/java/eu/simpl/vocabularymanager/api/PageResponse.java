package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * A collection page (OpenAPI schema CollectionPage, D22, D59, D78, D111, D153).
 *
 * @param page page number
 * @param pageSize page size
 * @param total number of matching items
 * @param totalPages number of pages
 * @param self link to this page
 * @param first link to the first page
 * @param prev link to the previous page
 * @param next link to the next page
 * @param last link to the last page
 * @param items the items
 * @param <T> the item type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"page", "pageSize", "total", "totalPages", "self", "first", "prev", "next", "last", "items"})
public record PageResponse<T>(
        int page,
        int pageSize,
        long total,
        long totalPages,
        String self,
        String first,
        String prev,
        String next,
        String last,
        List<T> items) {}
