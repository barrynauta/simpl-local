package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.SearchResult;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.service.CreationResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Wire representations of the decided contract (D5, D25 to D27, D38, D41, D58, D78). */
public final class Representations {

    private final String contentBase;

    /** @param contentBase content base URL without trailing slash (E2) */
    public Representations(String contentBase) {
        this.contentBase = contentBase;
    }

    /** href of a vocabulary. */
    public static String vocabularyHref(String name) {
        return "/vocabularies/" + name;
    }

    /** href of a vocabulary version; also the Location header (D26). */
    public static String versionHref(String name, int version) {
        return vocabularyHref(name) + "/versions/" + version;
    }

    /** href of an external vocabulary (D41). */
    public static String externalHref(String name) {
        return "/external-vocabularies/" + name;
    }

    /** VocabularyCategory, the vocabulary (D25). */
    public Map<String, Object> vocabulary(Vocabulary vocabulary) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("href", vocabularyHref(vocabulary.name()));
        map.put("contentHref", contentBase + "/content/" + vocabulary.name());
        map.put("name", vocabulary.name());
        map.put("version", vocabulary.latestVersion());
        map.put("description", vocabulary.latestDescription());
        map.put("status", vocabulary.status().name());
        map.put("createdBy", vocabulary.createdBy());
        map.put("creationDate", time(vocabulary.creationDate()));
        map.put("lastUpdateDate", time(vocabulary.lastUpdateDate()));
        return map;
    }

    /** VocabularyMetadata, the vocabulary version (D26, D27, D5). */
    public Map<String, Object> version(VocabularyVersion version) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("href", versionHref(version.name(), version.version()));
        map.put("contentHref", contentBase + "/content/" + version.name() + "/versions/" + version.version());
        map.put("latestVersionHref", versionHref(version.name(), version.latestVersion()));
        map.put("name", version.name());
        map.put("version", version.version());
        map.put("description", version.description());
        map.put("changelog", version.changelog());
        map.put("status", version.status().name());
        map.put("createdBy", version.createdBy());
        map.put("creationDate", time(version.creationDate()));
        map.put("lastUpdateDate", time(version.lastUpdateDate()));
        return map;
    }

    /** VocabularyVersionMetadataResponse: the version plus validationWarnings, always present (D10). */
    public Map<String, Object> created(CreationResult result) {
        Map<String, Object> map = version(result.version());
        map.put(
                "validationWarnings",
                result.warnings().stream().map(Representations::warning).toList());
        return map;
    }

    private static Map<String, Object> warning(ValidationWarning warning) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", warning.code());
        map.put("description", warning.description());
        map.put("location", warning.location());
        return map;
    }

    /** VocabularySearchItem (D38). */
    public Map<String, Object> searchItem(SearchResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (result.latest()) {
            map.put("href", vocabularyHref(result.name()));
            map.put("contentHref", contentBase + "/content/" + result.name());
        } else {
            map.put("href", versionHref(result.name(), result.version()));
            map.put("contentHref", contentBase + "/content/" + result.name() + "/versions/" + result.version());
        }
        map.put("name", result.name());
        map.put("version", result.version());
        map.put("status", result.status().name());
        map.put("latest", result.latest());
        return map;
    }

    /** ExternalVocabulary (D41, D73). */
    public Map<String, Object> external(ExternalVocabulary vocabulary) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("href", externalHref(vocabulary.name()));
        map.put("name", vocabulary.name());
        map.put("namespaceUri", vocabulary.namespaceUri());
        map.put("aliasUris", vocabulary.aliasUris());
        if (vocabulary.sourceUri() != null) {
            map.put("sourceUri", vocabulary.sourceUri());
        }
        if (vocabulary.description() != null) {
            map.put("description", vocabulary.description());
        }
        map.put("createdBy", vocabulary.createdBy());
        map.put("creationDate", time(vocabulary.creationDate()));
        return map;
    }

    /** UTC to the second with 'Z' (D58). */
    public static String time(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /**
     * A collection page with links that keep the request's parameters (D22, D59, D78).
     *
     * @param path collection path relative to the API base
     * @param parameters request parameters other than page and pageSize, in request order
     * @param page page number
     * @param pageSize page size
     * @param total total items
     * @param items the items of this page
     * @return the page object
     */
    public static Map<String, Object> page(
            String path, Map<String, String> parameters, int page, int pageSize, long total, List<?> items) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("page", page);
        map.put("totalPages", totalPages);
        map.put("total", total);
        map.put("pageSize", pageSize);
        map.put("self", link(path, parameters, page, pageSize));
        map.put("first", link(path, parameters, 1, pageSize));
        if (total > 0 && page > 1) {
            long previous = page > totalPages ? totalPages : page - 1L;
            map.put("prev", link(path, parameters, previous, pageSize));
        }
        if (page < totalPages) {
            map.put("next", link(path, parameters, page + 1L, pageSize));
        }
        if (total > 0) {
            map.put("last", link(path, parameters, totalPages, pageSize));
        }
        map.put("items", items);
        return map;
    }

    /** An unpaged search result (D38): page 1, pageSize 100, no links. */
    public static Map<String, Object> searchPage(List<?> items) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("page", 1);
        map.put("totalPages", items.isEmpty() ? 0 : 1);
        map.put("total", items.size());
        map.put("pageSize", 100);
        map.put("items", items);
        return map;
    }

    private static String link(String path, Map<String, String> parameters, long page, int pageSize) {
        StringBuilder link = new StringBuilder(path)
                .append("?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);
        parameters.forEach((name, value) -> {
            if (!"page".equals(name) && !"pageSize".equals(name)) {
                link.append('&')
                        .append(URLEncoder.encode(name, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });
        return link.toString();
    }
}
