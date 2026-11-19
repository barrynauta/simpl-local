package eu.simpl.vocabulary.api;

import eu.simpl.vocabulary.api.dto.CreatedVersionDto;
import eu.simpl.vocabulary.api.dto.ExternalVocabularyDto;
import eu.simpl.vocabulary.api.dto.PageDto;
import eu.simpl.vocabulary.api.dto.SearchItemDto;
import eu.simpl.vocabulary.api.dto.VersionDto;
import eu.simpl.vocabulary.api.dto.VocabularyDto;
import eu.simpl.vocabulary.api.dto.WarningDto;
import eu.simpl.vocabulary.api.request.QueryParameters;
import eu.simpl.vocabulary.domain.model.ExternalVocabulary;
import eu.simpl.vocabulary.domain.model.Vocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyVersion;
import eu.simpl.vocabulary.service.CreatedVersion;
import eu.simpl.vocabulary.service.Page;
import eu.simpl.vocabulary.service.SearchService.SearchResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

/**
 * Maps the domain to the wire format of the contract (D25, D26, D27, D38) and builds paging links (D22,
 * D59, D78). href values are relative to the API base (E2).
 */
public class ApiMapper {

    private final String contentBaseUrl;

    public ApiMapper(String contentBaseUrl) {
        this.contentBaseUrl = contentBaseUrl;
    }

    public static String vocabularyHref(String name) {
        return "/vocabularies/" + name;
    }

    public static String versionHref(String name, long version) {
        return "/vocabularies/" + name + "/versions/" + version;
    }

    public static String externalHref(String name) {
        return "/external-vocabularies/" + name;
    }

    public String contentHref(String name) {
        return contentBaseUrl + "/content/" + name;
    }

    public String contentHref(String name, long version) {
        return contentBaseUrl + "/content/" + name + "/versions/" + version;
    }

    public VocabularyDto vocabulary(Vocabulary vocabulary) {
        return new VocabularyDto(
                vocabularyHref(vocabulary.name()),
                contentHref(vocabulary.name()),
                vocabulary.name(),
                vocabulary.latestVersion(),
                vocabulary.latestDescription(),
                vocabulary.status().name(),
                vocabulary.createdBy(),
                time(vocabulary.creationDate()),
                time(vocabulary.lastUpdateDate()));
    }

    public VersionDto version(VocabularyVersion version, long latestVersion) {
        return new VersionDto(
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

    public CreatedVersionDto created(CreatedVersion created) {
        VocabularyVersion version = created.version();
        List<WarningDto> warnings = created.warnings().stream()
                .map(w -> new WarningDto(w.code(), w.description(), w.location()))
                .toList();
        return new CreatedVersionDto(
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
                warnings);
    }

    public SearchItemDto searchItem(SearchResult result) {
        if (result.latest()) {
            return new SearchItemDto(
                    vocabularyHref(result.name()),
                    contentHref(result.name()),
                    result.name(),
                    result.version(),
                    result.status().name(),
                    true);
        }
        return new SearchItemDto(
                versionHref(result.name(), result.version()),
                contentHref(result.name(), result.version()),
                result.name(),
                result.version(),
                result.status().name(),
                false);
    }

    public ExternalVocabularyDto external(ExternalVocabulary external) {
        return new ExternalVocabularyDto(
                externalHref(external.name()),
                external.name(),
                external.namespaceUri(),
                external.aliasUris(),
                external.sourceUri(),
                external.description(),
                external.createdBy(),
                time(external.creationDate()));
    }

    /**
     * A paged collection with links (D22, D59, D78).
     *
     * @param path collection path relative to the API base
     * @param query request query parameters
     * @param page the page
     * @param mapper item mapper
     * @param <S> domain type
     * @param <T> wire type
     * @return the wire page
     */
    public static <S, T> PageDto<T> page(String path, QueryParameters query, Page<S> page, Function<S, T> mapper) {
        long totalPages = page.totalPages();
        long number = page.page();
        String prev = null;
        if (page.total() > 0 && number > 1) {
            prev = link(path, query, number > totalPages ? totalPages : number - 1, page.pageSize());
        }
        String next = number < totalPages ? link(path, query, number + 1, page.pageSize()) : null;
        String last = page.total() > 0 ? link(path, query, totalPages, page.pageSize()) : null;
        return new PageDto<>(
                number,
                totalPages,
                page.total(),
                page.pageSize(),
                link(path, query, number, page.pageSize()),
                link(path, query, 1, page.pageSize()),
                prev,
                next,
                last,
                page.items().stream().map(mapper).toList());
    }

    /**
     * A search result collection: one page holding everything, no links (D38).
     *
     * @param items the items
     * @param <T> item type
     * @return the wire page
     */
    public static <T> PageDto<T> unpaged(List<T> items) {
        return new PageDto<>(1, items.isEmpty() ? 0 : 1, items.size(), 100, null, null, null, null, null, items);
    }

    static String link(String path, QueryParameters query, long page, int pageSize) {
        StringBuilder link = new StringBuilder(path)
                .append("?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);
        for (QueryParameters.Parameter parameter : query.parameters()) {
            if ("page".equals(parameter.name()) || "pageSize".equals(parameter.name())) {
                continue;
            }
            link.append('&').append(encode(parameter.name())).append('=').append(encode(parameter.value()));
        }
        return link.toString();
    }

    private static String encode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if ((c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '-'
                    || c == '.'
                    || c == '_'
                    || c == '~'
                    || c == ':') {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", b & 0xff));
            }
        }
        return out.toString();
    }

    /**
     * UTC time to the second with 'Z' (D58).
     *
     * @param instant the time
     * @return ISO 8601 text
     */
    public static String time(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
