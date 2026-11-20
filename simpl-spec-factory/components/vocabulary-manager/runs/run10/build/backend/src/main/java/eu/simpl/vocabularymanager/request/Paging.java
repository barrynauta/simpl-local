package eu.simpl.vocabularymanager.request;

import eu.simpl.vocabularymanager.error.Issue;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Page and page size of a paged list (D22) and its links (D59, D78, D110, D111): every link carries page and
 * pageSize first, then the other parameters of the request in request order.
 */
public record Paging(int page, int pageSize) {

    public static Paging from(QueryParameters parameters, List<Issue> issues) {
        int page = 1;
        int pageSize = FieldRules.PAGE_SIZE_DEFAULT;
        if (parameters.has("page")) {
            Integer value = FieldRules.positiveInt(parameters.get("page"), "query", "page", Integer.MAX_VALUE, issues);
            page = value == null ? page : value;
        }
        if (parameters.has("pageSize")) {
            Integer value = FieldRules.positiveInt(
                    parameters.get("pageSize"), "query", "pageSize", FieldRules.PAGE_SIZE_MAX, issues);
            pageSize = value == null ? pageSize : value;
        }
        return new Paging(page, pageSize);
    }

    public long offset() {
        return (long) (page - 1) * pageSize;
    }

    public int totalPages(long total) {
        return (int) ((total + pageSize - 1) / pageSize);
    }

    /** The paging links of one page. */
    public record Links(String self, String first, String prev, String next, String last) {}

    public Links links(String path, long total, QueryParameters parameters) {
        int totalPages = totalPages(total);
        String prev = null;
        if (total > 0 && page > 1) {
            prev = link(path, page > totalPages ? totalPages : page - 1, parameters);
        }
        String next = page < totalPages ? link(path, page + 1, parameters) : null;
        String last = total > 0 ? link(path, totalPages, parameters) : null;
        return new Links(link(path, page, parameters), link(path, 1, parameters), prev, next, last);
    }

    private String link(String path, int target, QueryParameters parameters) {
        StringBuilder link = new StringBuilder(path)
                .append("?page=")
                .append(target)
                .append("&pageSize=")
                .append(pageSize);
        for (Map.Entry<String, String> entry : parameters.inOrder().entrySet()) {
            if (entry.getKey().equals("page") || entry.getKey().equals("pageSize")) {
                continue;
            }
            link.append('&')
                    .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return link.toString();
    }
}
