package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigInteger;
import java.util.List;

/**
 * CollectionPage of the OpenAPI file with its items. Absent links are left out (decision D22).
 *
 * @param page the page number
 * @param totalPages the number of pages
 * @param total the number of items in the collection
 * @param pageSize the page size
 * @param self link to this page
 * @param first link to the first page
 * @param prev link to the previous page
 * @param next link to the next page
 * @param last link to the last page
 * @param items the items
 * @param <T> the item type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageDto<T>(
        BigInteger page,
        long totalPages,
        long total,
        int pageSize,
        String self,
        String first,
        String prev,
        String next,
        String last,
        List<T> items) {

    /**
     * An unpaged search result (decision D38): page 1, pageSize 100, no links.
     *
     * @param items the results
     * @param <T> the item type
     * @return the body
     */
    public static <T> PageDto<T> unpaged(List<T> items) {
        return new PageDto<>(
                BigInteger.ONE,
                items.isEmpty() ? 0 : 1,
                items.size(),
                Paging.MAX_PAGE_SIZE,
                null,
                null,
                null,
                null,
                null,
                items);
    }
}
