package eu.simpl.vocabulary.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Wire form of a collection page: schema CollectionPage with items (D22, D38, D59, D78).
 *
 * @param page page number
 * @param totalPages number of pages
 * @param total number of items
 * @param pageSize page size
 * @param self link to this page
 * @param first link to the first page
 * @param prev link to the previous page
 * @param next link to the next page
 * @param last link to the last page
 * @param items the items
 * @param <T> item type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageDto<T>(
        long page,
        long totalPages,
        long total,
        int pageSize,
        String self,
        String first,
        String prev,
        String next,
        String last,
        List<T> items) {}
