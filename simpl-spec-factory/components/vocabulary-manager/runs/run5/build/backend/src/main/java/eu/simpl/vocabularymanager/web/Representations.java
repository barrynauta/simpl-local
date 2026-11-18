package eu.simpl.vocabularymanager.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.service.CreationResult;
import eu.simpl.vocabularymanager.service.SearchResult;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.validation.ValidationWarning;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps domain objects to the wire schemas of the OpenAPI, with the additions of D5, D10 and D27 (D39). */
@Component
public class Representations {

    private final VocabularyService vocabularies;

    public Representations(VocabularyService vocabularies) {
        this.vocabularies = vocabularies;
    }

    /** VocabularyCategory: the vocabulary (D25). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VocabularyBody(
            String href,
            String contentHref,
            String name,
            int version,
            String description,
            String status,
            String createdBy,
            String creationDate,
            String lastUpdateDate) {}

    /** VocabularyMetadata: the vocabulary version (D26), with createdBy (D27) and latestVersionHref (D5). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VersionBody(
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

    /** VocabularyVersionMetadataResponse: the created version plus validationWarnings, always present (D10). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreatedVersionBody(
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
            @JsonInclude(JsonInclude.Include.ALWAYS) List<ValidationWarning> validationWarnings) {}

    /** VocabularySearchItem. */
    public record SearchItemBody(
            String href, String contentHref, String name, int version, String status, boolean latest) {}

    /** ExternalVocabulary. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExternalBody(
            String href,
            String name,
            String namespaceUri,
            @JsonInclude(JsonInclude.Include.ALWAYS) List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            String creationDate) {}

    /** Path of a vocabulary relative to the API base. */
    public static String vocabularyHref(String name) {
        return "/vocabularies/" + name;
    }

    /** Path of a version relative to the API base. */
    public static String versionHref(String name, int version) {
        return "/vocabularies/" + name + "/versions/" + version;
    }

    /** Path of an external vocabulary relative to the API base. */
    public static String externalHref(String name) {
        return "/external-vocabularies/" + name;
    }

    public VocabularyBody vocabulary(Vocabulary v) {
        return new VocabularyBody(
                vocabularyHref(v.name()),
                vocabularies.vocabularyContentUrl(v.name()),
                v.name(),
                v.latestVersion(),
                v.latestDescription(),
                v.status().name(),
                v.createdBy(),
                v.creationDate(),
                v.lastUpdateDate());
    }

    public VersionBody version(VocabularyVersion v, int latestVersion) {
        return new VersionBody(
                versionHref(v.name(), v.version()),
                vocabularies.versionContentUrl(v.name(), v.version()),
                versionHref(v.name(), latestVersion),
                v.name(),
                v.version(),
                v.description(),
                v.changelog(),
                v.status().name(),
                v.createdBy(),
                v.creationDate(),
                v.lastUpdateDate());
    }

    public CreatedVersionBody created(CreationResult result) {
        VocabularyVersion v = result.version();
        return new CreatedVersionBody(
                versionHref(v.name(), v.version()),
                vocabularies.versionContentUrl(v.name(), v.version()),
                versionHref(v.name(), result.latestVersion()),
                v.name(),
                v.version(),
                v.description(),
                v.changelog(),
                v.status().name(),
                v.createdBy(),
                v.creationDate(),
                v.lastUpdateDate(),
                result.warnings());
    }

    public SearchItemBody searchItem(SearchResult r) {
        String href = r.latest() ? vocabularyHref(r.name()) : versionHref(r.name(), r.version());
        String contentHref = r.latest()
                ? vocabularies.vocabularyContentUrl(r.name())
                : vocabularies.versionContentUrl(r.name(), r.version());
        return new SearchItemBody(
                href, contentHref, r.name(), r.version(), r.status().name(), r.latest());
    }

    public ExternalBody external(ExternalVocabulary e) {
        return new ExternalBody(
                externalHref(e.name()),
                e.name(),
                e.namespaceUri(),
                e.aliasUris(),
                e.sourceUri(),
                e.description(),
                e.createdBy(),
                e.creationDate());
    }
}
