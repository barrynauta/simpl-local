package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.dto.Dtos;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.request.Paging;
import eu.simpl.vocabularymanager.request.QueryParameters;
import eu.simpl.vocabularymanager.service.SearchService;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Maps the domain to the wire representations; hrefs are relative to the API base (E2), contentHref uses E2. */
public class Representations {

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

    public Dtos.VocabularyCategory vocabulary(Vocabulary vocabulary) {
        return new Dtos.VocabularyCategory(
                vocabularyHref(vocabulary.name()),
                contentBase + "/content/" + vocabulary.name(),
                vocabulary.name(),
                vocabulary.latestVersion(),
                vocabulary.description(),
                vocabulary.status().name(),
                vocabulary.createdBy(),
                time(vocabulary.creationDate()),
                time(vocabulary.lastUpdateDate()));
    }

    public Dtos.VocabularyMetadata version(VocabularyVersion version, int latestVersion) {
        return new Dtos.VocabularyMetadata(
                versionHref(version.name(), version.version()),
                versionContentHref(version),
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

    public Dtos.VocabularyVersionMetadataResponse created(VocabularyVersion version, List<ValidationWarning> warnings) {
        return new Dtos.VocabularyVersionMetadataResponse(
                versionHref(version.name(), version.version()),
                versionContentHref(version),
                versionHref(version.name(), version.version()),
                version.name(),
                version.version(),
                version.description(),
                version.changelog(),
                version.status().name(),
                version.createdBy(),
                time(version.creationDate()),
                time(version.lastUpdateDate()),
                List.copyOf(warnings));
    }

    public Dtos.ExternalVocabulary external(ExternalVocabulary external) {
        return new Dtos.ExternalVocabulary(
                externalHref(external.name()),
                external.name(),
                external.namespaceUri(),
                List.copyOf(external.aliasUris()),
                external.sourceUri(),
                external.description(),
                external.createdBy(),
                time(external.creationDate()));
    }

    public Dtos.VocabularySearchItem searchItem(SearchService.Hit hit) {
        String href = hit.latest() ? vocabularyHref(hit.name()) : versionHref(hit.name(), hit.version());
        String contentHref = hit.latest()
                ? contentBase + "/content/" + hit.name()
                : contentBase + "/content/" + hit.name() + "/versions/" + hit.version();
        return new Dtos.VocabularySearchItem(
                href, contentHref, hit.name(), hit.version(), hit.status().name(), hit.latest());
    }

    /** A paged list with its links. */
    public static <T> Dtos.CollectionPage<T> page(
            List<T> items, long total, Paging paging, String path, QueryParameters parameters) {
        Paging.Links links = paging.links(path, total, parameters);
        return new Dtos.CollectionPage<>(
                paging.page(),
                paging.totalPages(total),
                total,
                paging.pageSize(),
                links.self(),
                links.first(),
                links.prev(),
                links.next(),
                links.last(),
                items);
    }

    /** A search result: not paged, one page that holds every result, no links (D38). */
    public static <T> Dtos.CollectionPage<T> searchPage(List<T> items) {
        return new Dtos.CollectionPage<>(
                1, items.isEmpty() ? 0 : 1, items.size(), 100, null, null, null, null, null, items);
    }

    private String versionContentHref(VocabularyVersion version) {
        return contentBase + "/content/" + version.name() + "/versions/" + version.version();
    }

    /** UTC to the second with 'Z' (D58). */
    public static String time(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
