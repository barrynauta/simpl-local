package eu.simpl.vocabularymanager.web.api;

import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.routing.QueryParameters;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.web.util.UriUtils;

/**
 * Paging of D22, D59 and D78: page minimum 1 (default 1), pageSize 1 to 100 (default 20); links keep every query
 * parameter of the request and change only page.
 *
 * @param page the requested page
 * @param pageSize the page size
 */
public record Paging(long page, int pageSize) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final Pattern DIGITS = Pattern.compile("^[0-9]{1,18}$");

    /**
     * @param parameters the query parameters
     * @param issues collects invalid values
     * @return the paging, defaults where absent
     */
    public static Paging parse(QueryParameters parameters, List<ProblemIssue> issues) {
        long page = 1;
        int pageSize = DEFAULT_PAGE_SIZE;
        String pageValue = parameters.first("page");
        if (pageValue != null) {
            if (DIGITS.matcher(pageValue).matches() && Long.parseLong(pageValue) >= 1) {
                page = Long.parseLong(pageValue);
            } else {
                issues.add(ProblemIssue.schemaViolation(
                        "query", "page", "Query parameter 'page' must be an integer of at least 1."));
            }
        }
        String sizeValue = parameters.first("pageSize");
        if (sizeValue != null) {
            if (DIGITS.matcher(sizeValue).matches()
                    && Long.parseLong(sizeValue) >= 1
                    && Long.parseLong(sizeValue) <= MAX_PAGE_SIZE) {
                pageSize = Integer.parseInt(sizeValue);
            } else {
                issues.add(ProblemIssue.schemaViolation(
                        "query", "pageSize", "Query parameter 'pageSize' must be an integer from 1 to 100."));
            }
        }
        return new Paging(page, pageSize);
    }

    /**
     * @return rows to skip; saturates instead of overflowing
     */
    public long offset() {
        long pagesBefore = page - 1;
        if (pagesBefore > Long.MAX_VALUE / MAX_PAGE_SIZE) {
            return Long.MAX_VALUE / 2;
        }
        return pagesBefore * pageSize;
    }

    /**
     * @param path the collection path relative to the API base, for example /vocabularies
     * @param parameters the request query parameters
     * @param total the number of matching items
     * @param items the items of the page
     * @param <T> item type
     * @return the collection page with links
     */
    public <T> Representations.CollectionPage<T> page(
            String path, QueryParameters parameters, long total, List<T> items) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        String prev = null;
        if (total > 0 && page > 1) {
            prev = link(path, parameters, Math.min(page - 1, totalPages));
        }
        String next = page < totalPages ? link(path, parameters, page + 1) : null;
        String last = totalPages > 0 ? link(path, parameters, totalPages) : null;
        return new Representations.CollectionPage<>(
                page,
                totalPages,
                total,
                pageSize,
                link(path, parameters, page),
                link(path, parameters, 1),
                prev,
                next,
                last,
                items);
    }

    private String link(String path, QueryParameters parameters, long targetPage) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", Long.toString(targetPage));
        query.put("pageSize", Integer.toString(pageSize));
        for (String name : parameters.names()) {
            if (!"page".equals(name) && !"pageSize".equals(name)) {
                query.put(name, parameters.first(name));
            }
        }
        StringBuilder link = new StringBuilder(path).append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (!first) {
                link.append('&');
            }
            first = false;
            link.append(UriUtils.encodeQueryParam(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(encodeValue(entry.getValue()));
        }
        return link.toString();
    }

    private static String encodeValue(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8).replace("+", "%2B");
    }
}
