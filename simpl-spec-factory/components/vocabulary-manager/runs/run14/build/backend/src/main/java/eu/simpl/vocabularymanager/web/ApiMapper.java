package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.api.ExternalVocabularyResponse;
import eu.simpl.vocabularymanager.api.SearchItemResponse;
import eu.simpl.vocabularymanager.api.VersionResponse;
import eu.simpl.vocabularymanager.api.VocabularyResponse;
import eu.simpl.vocabularymanager.api.WarningResponse;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps the domain to the wire format of the OpenAPI (D25, D26, D5, D184). */
@Component
public class ApiMapper {

    private final String contentBase;

    public ApiMapper(ContentValidator validator) {
        this.contentBase = validator.contentBase();
    }

    /** The vocabulary object. */
    public VocabularyResponse vocabulary(Vocabulary vocabulary) {
        return new VocabularyResponse(
                "/vocabularies/" + vocabulary.name(),
                contentBase + "/content/" + vocabulary.name(),
                vocabulary.name(),
                vocabulary.latestVersion(),
                vocabulary.latestDescription(),
                vocabulary.status().name(),
                vocabulary.createdBy(),
                vocabulary.creationDate().toString(),
                vocabulary.lastUpdateDate().toString());
    }

    /** The version object; warnings null leaves validationWarnings out. */
    public VersionResponse version(VocabularyVersion version, long latestVersion, List<ValidationWarning> warnings) {
        String name = version.name();
        return new VersionResponse(
                versionHref(name, version.version()),
                contentBase + "/content/" + name + "/versions/" + version.version(),
                versionHref(name, latestVersion),
                name,
                version.version(),
                version.description(),
                version.changelog(),
                version.status().name(),
                version.createdBy(),
                version.creationDate().toString(),
                version.lastUpdateDate().toString(),
                warnings == null
                        ? null
                        : warnings.stream()
                                .map(w -> new WarningResponse(w.code(), w.description(), w.location()))
                                .toList());
    }

    /** A search result. */
    public SearchItemResponse searchItem(VocabularyService.SearchResult result) {
        String contentHref = result.latest()
                ? contentBase + "/content/" + result.name()
                : contentBase + "/content/" + result.name() + "/versions/" + result.version();
        return new SearchItemResponse(
                versionHref(result.name(), result.version()),
                contentHref,
                result.name(),
                result.version(),
                result.status().name(),
                result.latest());
    }

    /** The external vocabulary object. */
    public ExternalVocabularyResponse external(ExternalVocabulary vocabulary) {
        return new ExternalVocabularyResponse(
                "/external-vocabularies/" + vocabulary.name(),
                vocabulary.name(),
                vocabulary.namespaceUri(),
                vocabulary.aliasUris(),
                vocabulary.sourceUri(),
                vocabulary.description(),
                vocabulary.createdBy(),
                vocabulary.creationDate().toString());
    }

    /** The href of a version, also used as Location (D26). */
    public static String versionHref(String name, long version) {
        return "/vocabularies/" + name + "/versions/" + version;
    }
}
