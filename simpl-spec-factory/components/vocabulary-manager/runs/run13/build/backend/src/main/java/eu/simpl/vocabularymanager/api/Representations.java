package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.dto.CollectionResponse;
import eu.simpl.vocabularymanager.api.dto.ExternalVocabularyResponse;
import eu.simpl.vocabularymanager.api.dto.SearchItemResponse;
import eu.simpl.vocabularymanager.api.dto.VersionCreatedResponse;
import eu.simpl.vocabularymanager.api.dto.VersionResponse;
import eu.simpl.vocabularymanager.api.dto.VocabularyResponse;
import eu.simpl.vocabularymanager.api.dto.WarningResponse;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.UriUtils;

/**
 * Maps domain objects to their wire form (D25, D26, D29). href values and paging links are relative to the
 * API base (E2); contentHref is under the content base URL.
 */
public final class Representations {

    private final String contentBase;

    public Representations(String contentBase) {
        this.contentBase = contentBase;
    }

    public static String vocabularyHref(String name) {
        return "/vocabularies/" + name;
    }

    public static String versionHref(String name, int version) {
        return "/vocabularies/" + name + "/versions/" + version;
    }

    public static String externalHref(String name) {
        return "/external-vocabularies/" + name;
    }

    public String contentHref(String name) {
        return contentBase + "/content/" + name;
    }

    public String contentHref(String name, int version) {
        return contentBase + "/content/" + name + "/versions/" + version;
    }

    public VocabularyResponse vocabulary(Vocabulary vocabulary) {
        return new VocabularyResponse(
                vocabularyHref(vocabulary.name()),
                contentHref(vocabulary.name()),
                vocabulary.name(),
                vocabulary.latestVersion(),
                vocabulary.description(),
                vocabulary.status().name(),
                vocabulary.createdBy(),
                time(vocabulary.creationDate()),
                time(vocabulary.lastUpdateDate()));
    }

    public VersionResponse version(VocabularyVersion version, int latestVersion) {
        return new VersionResponse(
                versionHref(version.name(), version.version()),
                contentHref(version.name(), version.version()),
                versionHref(version.name(), latestVersion),
                version.name(),
                version.version(),
                version.description(),
                version.changelog(),
                version.status().name(),
                version.createdBy(),
                time(version.creationDate()),
                time(version.lastUpdateDate()));
    }

    public VersionCreatedResponse created(VocabularyVersion version, List<ValidationWarning> warnings) {
        return new VersionCreatedResponse(
                versionHref(version.name(), version.version()),
                contentHref(version.name(), version.version()),
                versionHref(version.name(), version.version()),
                version.name(),
                version.version(),
                version.description(),
                version.changelog(),
                version.status().name(),
                version.createdBy(),
                time(version.creationDate()),
                time(version.lastUpdateDate()),
                warnings.stream()
                        .map(w -> new WarningResponse(w.code(), w.description(), w.location()))
                        .toList());
    }

    public SearchItemResponse latestItem(String name, int latestVersion, VocabularyStatus vocabularyStatus) {
        return new SearchItemResponse(
                vocabularyHref(name), contentHref(name), name, latestVersion, vocabularyStatus.name(), true);
    }

    public SearchItemResponse versionItem(String name, int version, VocabularyStatus versionStatus) {
        return new SearchItemResponse(
                versionHref(name, version), contentHref(name, version), name, version, versionStatus.name(), false);
    }

    public static ExternalVocabularyResponse external(ExternalVocabulary external) {
        return new ExternalVocabularyResponse(
                externalHref(external.name()),
                external.name(),
                external.namespaceUri(),
                external.aliasUris(),
                external.sourceUri(),
                external.description(),
                external.createdBy(),
                time(external.creationDate()));
    }

    public static String time(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    /**
     * A page with its links (D22, D59, D78, D110, D111): page and pageSize first, then the other parameters
     * in request order; prev on a page beyond the last points to the last page; no last when total is 0.
     */
    public static <T> CollectionResponse<T> page(
            String path, int page, int pageSize, long total, Map<String, String> others, List<T> items) {
        int totalPages = (int) Math.min(Integer.MAX_VALUE, (total + pageSize - 1) / pageSize);
        String prev = null;
        if (total > 0 && page > 1) {
            prev = link(path, Math.min(page - 1, totalPages), pageSize, others);
        }
        String next = page < totalPages ? link(path, page + 1, pageSize, others) : null;
        String last = totalPages >= 1 ? link(path, totalPages, pageSize, others) : null;
        return new CollectionResponse<>(
                page,
                totalPages,
                total,
                pageSize,
                link(path, page, pageSize, others),
                link(path, 1, pageSize, others),
                prev,
                next,
                last,
                items);
    }

    /** A result list that is not paged (D38, D153). */
    public static <T> CollectionResponse<T> unpaged(List<T> items) {
        return new CollectionResponse<>(
                1, items.isEmpty() ? 0 : 1, items.size(), 100, null, null, null, null, null, items);
    }

    private static String link(String path, int page, int pageSize, Map<String, String> others) {
        StringBuilder link = new StringBuilder(path)
                .append("?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);
        others.forEach((name, value) -> link.append('&')
                .append(UriUtils.encodeQueryParam(name, StandardCharsets.UTF_8))
                .append('=')
                .append(UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8)));
        return link.toString();
    }
}
