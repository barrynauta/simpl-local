package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.service.VocabularyService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps domain objects to the wire schemas of the OpenAPI file, with the changes of decision D39 (createdBy on
 * versions, latestVersionHref always filled, validationWarnings always present). The one place where domain
 * names meet wire names (DDD-14).
 */
@Component
public class Representations {

    private final String contentBase;

    /**
     * Creates the mapper.
     *
     * @param properties the configuration (content base, decision E2)
     */
    public Representations(AppProperties properties) {
        String base = properties.contentBaseUrl();
        this.contentBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /**
     * VocabularyCategory: the vocabulary (decision D25).
     *
     * @param href relative link
     * @param contentHref content link
     * @param name name
     * @param version latest version number
     * @param description latest version description
     * @param status vocabulary status
     * @param createdBy creator of version 1
     * @param creationDate creation date
     * @param lastUpdateDate last update date
     */
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
            String lastUpdateDate) {}

    /**
     * VocabularyMetadata: a vocabulary version (decision D26).
     *
     * @param href relative link
     * @param contentHref content link
     * @param latestVersionHref link to the latest version (decision D5)
     * @param name vocabulary name
     * @param version version number
     * @param description description
     * @param changelog changelog
     * @param status version status
     * @param createdBy creator (decision D27)
     * @param creationDate creation date
     * @param lastUpdateDate last update date
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
            String lastUpdateDate) {}

    /**
     * VocabularyVersionMetadataResponse: a created version with its warnings (decision D10).
     *
     * @param href relative link, also the Location header
     * @param contentHref content link
     * @param name vocabulary name
     * @param version version number
     * @param description description
     * @param changelog changelog
     * @param status version status
     * @param createdBy creator
     * @param creationDate creation date
     * @param lastUpdateDate last update date
     * @param validationWarnings warnings, empty array when none
     */
    public record CreatedVersionDto(
            String href,
            String contentHref,
            String name,
            int version,
            String description,
            String changelog,
            String status,
            String createdBy,
            String creationDate,
            String lastUpdateDate,
            List<ValidationWarning> validationWarnings) {}

    /**
     * VocabularySearchItem.
     *
     * @param href relative link
     * @param contentHref content link
     * @param name vocabulary name
     * @param version version number
     * @param status status
     * @param latest true when it came from the latest version
     */
    public record SearchItemDto(
            String href, String contentHref, String name, int version, String status, boolean latest) {}

    /**
     * ExternalVocabulary.
     *
     * @param href relative link
     * @param name name
     * @param namespaceUri namespace URI
     * @param aliasUris alias URIs
     * @param sourceUri source URI
     * @param description description
     * @param createdBy creator
     * @param creationDate registration date
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExternalDto(
            String href,
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            String creationDate) {}

    /**
     * Maps a vocabulary.
     *
     * @param v the vocabulary
     * @return the wire object
     */
    public VocabularyDto vocabulary(Vocabulary v) {
        return new VocabularyDto(
                vocabularyHref(v.name()),
                contentBase + "/content/" + v.name(),
                v.name(),
                v.latestVersion(),
                v.latestDescription(),
                v.status().name(),
                v.createdBy(),
                time(v.creationDate()),
                time(v.lastUpdateDate()));
    }

    /**
     * Maps a version.
     *
     * @param v the version
     * @param latestVersion the latest version number of its vocabulary
     * @return the wire object
     */
    public VersionDto version(VocabularyVersion v, int latestVersion) {
        return new VersionDto(
                versionHref(v.name(), v.version()),
                versionContentHref(v.name(), v.version()),
                versionHref(v.name(), latestVersion),
                v.name(),
                v.version(),
                v.description(),
                v.changelog(),
                v.status().name(),
                v.createdBy(),
                time(v.creationDate()),
                time(v.lastUpdateDate()));
    }

    /**
     * Maps a created version.
     *
     * @param created the created version and warnings
     * @return the wire object
     */
    public CreatedVersionDto created(VocabularyService.Created created) {
        VocabularyVersion v = created.version();
        return new CreatedVersionDto(
                versionHref(v.name(), v.version()),
                versionContentHref(v.name(), v.version()),
                v.name(),
                v.version(),
                v.description(),
                v.changelog(),
                v.status().name(),
                v.createdBy(),
                time(v.creationDate()),
                time(v.lastUpdateDate()),
                List.copyOf(created.warnings()));
    }

    /**
     * Maps a search result (decision D38).
     *
     * @param r the result
     * @return the wire object
     */
    public SearchItemDto searchItem(VocabularyService.SearchResult r) {
        if (r.latest()) {
            return new SearchItemDto(
                    vocabularyHref(r.name()),
                    contentBase + "/content/" + r.name(),
                    r.name(),
                    r.version(),
                    r.status().name(),
                    true);
        }
        return new SearchItemDto(
                versionHref(r.name(), r.version()),
                versionContentHref(r.name(), r.version()),
                r.name(),
                r.version(),
                r.status().name(),
                false);
    }

    /**
     * Maps an external vocabulary.
     *
     * @param e the external vocabulary
     * @return the wire object
     */
    public ExternalDto external(ExternalVocabulary e) {
        return new ExternalDto(
                externalHref(e.name()),
                e.name(),
                e.namespaceUri(),
                e.aliasUris(),
                e.sourceUri(),
                e.description(),
                e.createdBy(),
                time(e.creationDate()));
    }

    /**
     * Relative link of a vocabulary.
     *
     * @param name the name
     * @return the link
     */
    public static String vocabularyHref(String name) {
        return "/vocabularies/" + name;
    }

    /**
     * Relative link of a version.
     *
     * @param name the vocabulary name
     * @param version the version number
     * @return the link
     */
    public static String versionHref(String name, int version) {
        return "/vocabularies/" + name + "/versions/" + version;
    }

    /**
     * Relative link of an external vocabulary.
     *
     * @param name the name
     * @return the link
     */
    public static String externalHref(String name) {
        return "/external-vocabularies/" + name;
    }

    private String versionContentHref(String name, int version) {
        return contentBase + "/content/" + name + "/versions/" + version;
    }

    private static String time(Instant instant) {
        return instant.toString();
    }
}
