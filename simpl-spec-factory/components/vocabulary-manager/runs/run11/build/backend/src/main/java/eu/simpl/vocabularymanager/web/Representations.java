package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.content.ValidationWarning;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.store.StoreSchema;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Wire format of the decided contract (D25, D26, D27, D5, D10, D38, D78, D110, D111). Field names
 * stay as in the OpenAPI; hrefs are relative to the API base (E2).
 */
@Component
public class Representations {

    private final String contentBase;

    public Representations(AppProperties properties) {
        this.contentBase = properties.contentBaseTrimmed();
    }

    /** VocabularyCategory: the vocabulary (D25). */
    public Map<String, Object> vocabulary(Vocabulary v) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("href", "/vocabularies/" + v.name());
        out.put("contentHref", contentBase + "/content/" + v.name());
        out.put("name", v.name());
        out.put("version", v.latestVersion());
        out.put("description", v.description());
        out.put("status", v.status().name());
        out.put("createdBy", v.createdBy());
        out.put("creationDate", StoreSchema.format(v.creationDate()));
        out.put("lastUpdateDate", StoreSchema.format(v.lastUpdateDate()));
        return out;
    }

    /** VocabularyMetadata: the vocabulary version (D26, D27, D5). */
    public Map<String, Object> version(VocabularyVersion v) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("href", versionHref(v.name(), v.version()));
        out.put("contentHref", contentBase + "/content/" + v.name() + "/versions/" + v.version());
        out.put("latestVersionHref", versionHref(v.name(), v.latestVersion()));
        out.put("name", v.name());
        out.put("version", v.version());
        out.put("description", v.description());
        out.put("changelog", v.changelog());
        out.put("status", v.status().name());
        out.put("createdBy", v.createdBy());
        out.put("creationDate", StoreSchema.format(v.creationDate()));
        out.put("lastUpdateDate", StoreSchema.format(v.lastUpdateDate()));
        return out;
    }

    /** VocabularyVersionMetadataResponse: the version plus validationWarnings, always present (D10). */
    public Map<String, Object> created(VocabularyVersion v, List<ValidationWarning> warnings) {
        Map<String, Object> out = version(v);
        out.put(
                "validationWarnings",
                warnings.stream()
                        .map(w -> {
                            Map<String, Object> one = new LinkedHashMap<>();
                            one.put("code", w.code());
                            one.put("description", w.description());
                            one.put("location", w.location());
                            return one;
                        })
                        .toList());
        return out;
    }

    /** VocabularySearchItem (D38). */
    public Map<String, Object> searchItem(String name, int version, VocabularyStatus status, boolean latest) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("href", latest ? "/vocabularies/" + name : versionHref(name, version));
        out.put(
                "contentHref",
                latest ? contentBase + "/content/" + name : contentBase + "/content/" + name + "/versions/" + version);
        out.put("name", name);
        out.put("version", version);
        out.put("status", status.name());
        out.put("latest", latest);
        return out;
    }

    /** ExternalVocabulary; aliasUris always present (D110). */
    public Map<String, Object> external(ExternalVocabulary e) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("href", "/external-vocabularies/" + e.name());
        out.put("name", e.name());
        out.put("namespaceUri", e.namespaceUri());
        out.put("aliasUris", e.aliasUris());
        if (e.sourceUri() != null) {
            out.put("sourceUri", e.sourceUri());
        }
        if (e.description() != null) {
            out.put("description", e.description());
        }
        out.put("createdBy", e.createdBy());
        out.put("creationDate", StoreSchema.format(e.creationDate()));
        return out;
    }

    /** A paged collection with links (D22, D59, D78, D110, D111). */
    public Map<String, Object> page(
            String path, Map<String, String> query, long page, int pageSize, long total, List<?> items) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("page", page);
        out.put("totalPages", totalPages);
        out.put("total", total);
        out.put("pageSize", pageSize);
        out.put("self", link(path, query, page, pageSize));
        out.put("first", link(path, query, 1, pageSize));
        if (total > 0 && page > 1) {
            out.put("prev", link(path, query, Math.min(page - 1, totalPages), pageSize));
        }
        if (page < totalPages) {
            out.put("next", link(path, query, page + 1, pageSize));
        }
        if (total > 0) {
            out.put("last", link(path, query, totalPages, pageSize));
        }
        out.put("items", items);
        return out;
    }

    /** A search result: not paged (D38). */
    public Map<String, Object> searchResult(List<?> items) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("page", 1);
        out.put("totalPages", items.isEmpty() ? 0 : 1);
        out.put("total", items.size());
        out.put("pageSize", 100);
        out.put("items", items);
        return out;
    }

    static String link(String path, Map<String, String> query, long page, int pageSize) {
        StringBuilder out = new StringBuilder(path)
                .append("?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);
        query.forEach((name, value) -> {
            if (!"page".equals(name) && !"pageSize".equals(name)) {
                out.append('&')
                        .append(URLEncoder.encode(name, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });
        return out.toString();
    }

    private static String versionHref(String name, int version) {
        return "/vocabularies/" + name + "/versions/" + version;
    }
}
