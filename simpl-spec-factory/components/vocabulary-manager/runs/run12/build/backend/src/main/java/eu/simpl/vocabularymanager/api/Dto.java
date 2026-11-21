package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.simpl.vocabularymanager.domain.Model;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** Wire objects of the decided contract (OpenAPI schemas plus D5, D10, D27, D79). */
public final class Dto {

    private Dto() {}

    /** UTC to the second with Z (D58). */
    public static String time(Instant t) {
        return t == null ? null : DateTimeFormatter.ISO_INSTANT.format(t.truncatedTo(ChronoUnit.SECONDS));
    }

    /** VocabularyCategory: the vocabulary (D25). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VocabularyDto(
            String href,
            String contentHref,
            String name,
            int version,
            String description,
            String status,
            String createdBy,
            String creationDate,
            String lastUpdateDate) {

        public static VocabularyDto of(Model.Vocabulary v, String contentBase) {
            return new VocabularyDto(
                    "/vocabularies/" + v.name(),
                    contentBase + "/content/" + v.name(),
                    v.name(),
                    v.latestVersion(),
                    v.description(),
                    v.status().name(),
                    v.createdBy(),
                    time(v.creationDate()),
                    time(v.lastUpdateDate()));
        }
    }

    /**
     * VocabularyMetadata and VocabularyVersionMetadataResponse: the vocabulary version (D26, D27,
     * D5). validationWarnings is present only on the creation response, always as an array (D10).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
        "href",
        "contentHref",
        "latestVersionHref",
        "name",
        "version",
        "description",
        "changelog",
        "status",
        "createdBy",
        "creationDate",
        "lastUpdateDate",
        "validationWarnings"
    })
    public record VersionDto(
            String href,
            String contentHref,
            String latestVersionHref,
            String name,
            int version,
            String description,
            String changelog,
            String status,
            String createdBy,
            String creationDate,
            String lastUpdateDate,
            List<WarningDto> validationWarnings) {

        public static VersionDto of(Model.VocabularyVersion v, String contentBase, List<WarningDto> warnings) {
            return new VersionDto(
                    "/vocabularies/" + v.name() + "/versions/" + v.version(),
                    contentBase + "/content/" + v.name() + "/versions/" + v.version(),
                    "/vocabularies/" + v.name() + "/versions/" + v.latestVersion(),
                    v.name(),
                    v.version(),
                    v.description(),
                    v.changelog(),
                    v.status().name(),
                    v.createdBy(),
                    time(v.creationDate()),
                    time(v.lastUpdateDate()),
                    warnings);
        }
    }

    /** VocabularyValidationWarning (D10). */
    public record WarningDto(String code, String description, String location) {
        public static WarningDto of(Model.Warning w) {
            return new WarningDto(w.code(), w.description(), w.location());
        }
    }

    /** ExternalVocabulary (D41, D110: aliasUris always present). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExternalDto(
            String href,
            String name,
            String namespaceUri,
            @JsonInclude(JsonInclude.Include.ALWAYS) List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            String creationDate) {

        public static ExternalDto of(Model.ExternalVocabulary e) {
            return new ExternalDto(
                    "/external-vocabularies/" + e.name(),
                    e.name(),
                    e.namespaceUri(),
                    e.aliasUris(),
                    e.sourceUri(),
                    e.description(),
                    e.createdBy(),
                    time(e.creationDate()));
        }
    }

    /** VocabularySearchItem (D38). */
    public record SearchItemDto(
            String href, String contentHref, String name, int version, String status, boolean latest) {}

    /** HealthResponse. */
    public record HealthDto(String status) {}

    /** CollectionPage with items; links are left out when absent (D22). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"page", "totalPages", "total", "pageSize", "self", "first", "prev", "next", "last", "items"})
    public record PageDto<T>(
            int page,
            long totalPages,
            long total,
            int pageSize,
            String self,
            String first,
            String prev,
            String next,
            String last,
            @JsonInclude(JsonInclude.Include.ALWAYS) List<T> items) {

        /** A paged list with links that keep the request's other parameters (D22, D59, D78, D111). */
        public static <T> PageDto<T> paged(
                String path, Map<String, String> params, int page, int pageSize, long total, List<T> items) {
            long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
            String self = link(path, params, page, pageSize);
            String first = link(path, params, 1, pageSize);
            String prev = null;
            if (total > 0 && page > 1) {
                prev = link(path, params, (int) Math.min(page - 1L, totalPages), pageSize);
            }
            String next = page < totalPages ? link(path, params, page + 1, pageSize) : null;
            String last = total > 0 ? link(path, params, (int) totalPages, pageSize) : null;
            return new PageDto<>(page, totalPages, total, pageSize, self, first, prev, next, last, items);
        }

        /** A search result: one page, no links (D38, D153). */
        public static <T> PageDto<T> search(List<T> items) {
            return new PageDto<>(1, items.isEmpty() ? 0 : 1, items.size(), 100, null, null, null, null, null, items);
        }

        static String link(String path, Map<String, String> params, int page, int pageSize) {
            StringBuilder sb = new StringBuilder(path);
            sb.append("?page=").append(page).append("&pageSize=").append(pageSize);
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getKey().equals("page") || e.getKey().equals("pageSize")) {
                    continue;
                }
                sb.append('&').append(enc(e.getKey())).append('=').append(enc(e.getValue()));
            }
            return sb.toString();
        }

        private static String enc(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }
}
