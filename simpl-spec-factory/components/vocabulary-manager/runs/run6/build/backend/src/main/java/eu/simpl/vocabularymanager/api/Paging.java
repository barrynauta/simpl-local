package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.problem.Issue;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.MultiValueMap;

/**
 * Page parameters and paging links of decisions D22 and D59: page minimum 1, default 1; pageSize 1 to 100,
 * default 20; self and first always, prev, next and last when they exist.
 *
 * @param page the page number, may be far beyond the last page
 * @param pageSize the page size
 */
public record Paging(BigInteger page, int pageSize) {

    /** Largest page size (decision D22). */
    public static final int MAX_PAGE_SIZE = 100;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");

    /**
     * Reads page and pageSize from the query parameters.
     *
     * @param parameters the query parameters
     * @param issues the issue list
     * @return the paging, with defaults for invalid values (the caller throws on issues)
     */
    public static Paging from(MultiValueMap<String, String> parameters, List<Issue> issues) {
        BigInteger page = BigInteger.ONE;
        int pageSize = DEFAULT_PAGE_SIZE;
        String pageText = parameters.getFirst("page");
        if (pageText != null) {
            if (DIGITS.matcher(pageText).matches() && new BigInteger(pageText).signum() > 0) {
                page = new BigInteger(pageText);
            } else {
                issues.add(Issue.schemaViolation(
                        Issue.IN_QUERY, "page", "Query parameter 'page' must be an integer of at least 1."));
            }
        }
        String sizeText = parameters.getFirst("pageSize");
        if (sizeText != null) {
            if (DIGITS.matcher(sizeText).matches()
                    && sizeText.length() <= 3
                    && Integer.parseInt(sizeText) >= 1
                    && Integer.parseInt(sizeText) <= MAX_PAGE_SIZE) {
                pageSize = Integer.parseInt(sizeText);
            } else {
                issues.add(Issue.schemaViolation(
                        Issue.IN_QUERY,
                        "pageSize",
                        "Query parameter 'pageSize' must be an integer from 1 to " + MAX_PAGE_SIZE + "."));
            }
        }
        return new Paging(page, pageSize);
    }

    /**
     * Number of items to skip; saturates at Long.MAX_VALUE for pages far beyond any collection.
     *
     * @return the offset
     */
    public long offset() {
        BigInteger offset = page.subtract(BigInteger.ONE).multiply(BigInteger.valueOf(pageSize));
        return offset.bitLength() < 63 ? offset.longValue() : Long.MAX_VALUE;
    }

    /**
     * Builds the paged response with its links.
     *
     * @param path the collection path relative to the API base, for example /vocabularies
     * @param otherParameters filter and sort parameters to repeat in the links, in order
     * @param total the number of items in the collection
     * @param items the items of this page
     * @param <T> the item type
     * @return the response body
     */
    public <T> PageDto<T> response(String path, Map<String, String> otherParameters, long total, List<T> items) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        BigInteger last = BigInteger.valueOf(totalPages);
        String prev = null;
        if (total > 0 && page.compareTo(BigInteger.ONE) > 0) {
            prev = page.compareTo(last) > 0
                    ? link(path, last, otherParameters)
                    : link(path, page.subtract(BigInteger.ONE), otherParameters);
        }
        String next = page.compareTo(last) < 0 ? link(path, page.add(BigInteger.ONE), otherParameters) : null;
        String lastLink = total > 0 ? link(path, last, otherParameters) : null;
        return new PageDto<>(
                page,
                totalPages,
                total,
                pageSize,
                link(path, page, otherParameters),
                link(path, BigInteger.ONE, otherParameters),
                prev,
                next,
                lastLink,
                items);
    }

    private String link(String path, BigInteger pageNumber, Map<String, String> otherParameters) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("page", pageNumber.toString());
        parameters.put("pageSize", Integer.toString(pageSize));
        parameters.putAll(otherParameters);
        StringBuilder link = new StringBuilder(path).append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!first) {
                link.append('&');
            }
            first = false;
            link.append(entry.getKey()).append('=').append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return link.toString();
    }
}
