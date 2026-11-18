package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.ApiModel.CollectionPage;
import eu.simpl.vocabularymanager.api.ApiModel.ExternalVocabularyResponse;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularyCategory;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularyMetadata;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularySearchItem;
import eu.simpl.vocabularymanager.api.ApiModel.VocabularyVersionMetadataResponse;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.service.Page;
import eu.simpl.vocabularymanager.service.VocabularyCommands.SearchResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Maps domain objects to the wire schemas; builds href, contentHref and paging links (decisions D22, D25, D26). */
@Component
public class ApiMapper {

    private final String contentBase;

    public ApiMapper(VocabularyManagerProperties properties) {
        this.contentBase = properties.contentBaseUrl();
    }

    public static String vocabularyHref(String name) {
        return "/vocabularies/" + name;
    }

    public static String versionHref(String name, int version) {
        return vocabularyHref(name) + "/versions/" + version;
    }

    public String contentHref(String name) {
        return contentBase + "/content/" + name;
    }

    public String contentHref(String name, int version) {
        return contentHref(name) + "/versions/" + version;
    }

    public VocabularyCategory vocabulary(Vocabulary v) {
        return new VocabularyCategory(
                vocabularyHref(v.name()),
                contentHref(v.name()),
                v.name(),
                v.latestVersion(),
                v.description(),
                v.status(),
                v.createdBy(),
                v.creationDate(),
                v.lastUpdateDate());
    }

    public VocabularyMetadata version(VocabularyVersion v, int latestVersion) {
        return new VocabularyMetadata(
                versionHref(v.name(), v.version()),
                contentHref(v.name(), v.version()),
                versionHref(v.name(), latestVersion),
                v.name(),
                v.version(),
                v.description(),
                v.changelog(),
                v.status(),
                v.createdBy(),
                v.creationDate(),
                v.lastUpdateDate());
    }

    public VocabularyVersionMetadataResponse created(VocabularyVersion v, List<ValidationWarning> warnings) {
        return new VocabularyVersionMetadataResponse(
                versionHref(v.name(), v.version()),
                contentHref(v.name(), v.version()),
                versionHref(v.name(), v.version()),
                v.name(),
                v.version(),
                v.description(),
                v.changelog(),
                v.status(),
                v.createdBy(),
                v.creationDate(),
                v.lastUpdateDate(),
                List.copyOf(warnings));
    }

    public VocabularySearchItem searchItem(SearchResult r) {
        return r.latest()
                ? new VocabularySearchItem(
                        vocabularyHref(r.name()), contentHref(r.name()), r.name(), r.version(), r.status(), true)
                : new VocabularySearchItem(
                        versionHref(r.name(), r.version()),
                        contentHref(r.name(), r.version()),
                        r.name(),
                        r.version(),
                        r.status(),
                        false);
    }

    public ExternalVocabularyResponse external(ExternalVocabulary e) {
        return new ExternalVocabularyResponse(
                "/external-vocabularies/" + e.name(),
                e.name(),
                e.namespaceUri(),
                e.aliasUris(),
                e.sourceUri(),
                e.description(),
                e.createdBy(),
                e.creationDate());
    }

    /**
     * A paged collection with self, first, prev, next and last links (decision D22).
     *
     * @param path the collection path
     * @param query the query parameters to repeat in the links, in order, without page and pageSize
     */
    public <T, R> CollectionPage<R> page(Page<T> page, String path, Map<String, String> query, Function<T, R> mapper) {
        int totalPages = page.totalPages();
        String self = link(path, page.page(), page.pageSize(), query);
        String first = link(path, 1, page.pageSize(), query);
        String prev = page.page() > 1
                ? link(path, Math.min(page.page() - 1, Math.max(totalPages, 1)), page.pageSize(), query)
                : null;
        String next = page.page() < totalPages ? link(path, page.page() + 1, page.pageSize(), query) : null;
        String last = totalPages > 0 ? link(path, totalPages, page.pageSize(), query) : null;
        return new CollectionPage<>(
                page.page(),
                totalPages,
                page.total(),
                page.pageSize(),
                self,
                first,
                prev,
                next,
                last,
                page.items().stream().map(mapper).toList());
    }

    /** An unpaged search result: page 1, pageSize 100, no links (decision D38). */
    public <R> CollectionPage<R> searchResult(List<R> items) {
        return new CollectionPage<>(
                1, items.isEmpty() ? 0 : 1, items.size(), 100, null, null, null, null, null, List.copyOf(items));
    }

    private static String link(String path, int page, int pageSize, Map<String, String> query) {
        StringBuilder link = new StringBuilder(path)
                .append("?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);
        query.forEach((name, value) -> {
            if (value != null) {
                link.append('&').append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });
        return link.toString();
    }
}
