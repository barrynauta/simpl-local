package eu.simpl.catalogue.common;

/** Paging rules of D31: 1-based page, default 20, capped at 100, invalid values give 400. */
public record Paging(int page, int pageSize) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static Paging parse(String page, String pageSize) {
        return parse(page, pageSize, MAX_SIZE);
    }

    public static Paging parse(String page, String pageSize, int max) {
        int p = parseInt(page, 1);
        int s = parseInt(pageSize, DEFAULT_SIZE);
        if (s > max) s = max;
        return new Paging(p, s);
    }

    private static int parseInt(String v, int def) {
        if (v == null || v.isEmpty()) return def;
        try {
            int i = Integer.parseInt(v.trim());
            if (i < 1) throw invalid();
            return i;
        } catch (NumberFormatException e) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return ApiException.invalidInput("page and pageSize must be integers greater than 0.");
    }

    public int offset() { return (page - 1) * pageSize; }

    public int totalPages(long total) {
        return (int) ((total + pageSize - 1) / pageSize);
    }
}
