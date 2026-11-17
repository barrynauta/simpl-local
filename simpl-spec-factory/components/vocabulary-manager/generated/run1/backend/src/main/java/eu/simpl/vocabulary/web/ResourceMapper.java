package eu.simpl.vocabulary.web;

import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.ExternalVocabulary;
import eu.simpl.vocabulary.domain.Vocabulary;
import eu.simpl.vocabulary.domain.VocabularyVersion;
import eu.simpl.vocabulary.validation.ValidationWarning;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps domain objects to response bodies and builds href / contentHref values. */
@Component
public class ResourceMapper {

    private final String publicBase;

    public ResourceMapper(AppProperties props) {
        this.publicBase = props.normalizedPublicBaseUrl();
    }

    public static String vocabularyHref(String name) {
        return "/vocabularies/" + name;
    }

    public static String versionHref(String name, int version) {
        return "/vocabularies/" + name + "/versions/" + version;
    }

    public Dtos.VocabularyCategory category(Vocabulary v) {
        return new Dtos.VocabularyCategory(vocabularyHref(v.name()), publicBase + "/content/" + v.name(), v.name(),
                v.latestVersionNumber(), v.description(), v.status(), v.createdBy(), v.creationDate(),
                v.lastUpdateDate());
    }

    public Dtos.VocabularyMetadata metadata(Vocabulary v, VocabularyVersion ver) {
        return new Dtos.VocabularyMetadata(versionHref(v.name(), ver.version()),
                publicBase + "/content/" + v.name() + "/versions/" + ver.version(),
                versionHref(v.name(), v.latestVersionNumber()), v.name(), ver.version(), ver.description(),
                ver.changelog(), ver.status(), ver.createdBy(), ver.creationDate(), ver.lastUpdateDate());
    }

    public Dtos.VocabularyVersionMetadataResponse created(Vocabulary v, int number, List<ValidationWarning> warnings) {
        VocabularyVersion ver = v.version(number).orElseThrow();
        return new Dtos.VocabularyVersionMetadataResponse(versionHref(v.name(), number),
                publicBase + "/content/" + v.name() + "/versions/" + number, v.name(), number, ver.description(),
                ver.changelog(), ver.status(), ver.createdBy(), ver.creationDate(), ver.lastUpdateDate(),
                warnings == null ? List.of() : warnings);
    }

    public Dtos.VocabularySearchItem searchLatest(Vocabulary v) {
        return new Dtos.VocabularySearchItem(vocabularyHref(v.name()), publicBase + "/content/" + v.name(), v.name(),
                v.latestVersionNumber(), v.status(), true);
    }

    public Dtos.VocabularySearchItem searchVersion(Vocabulary v, VocabularyVersion ver) {
        return new Dtos.VocabularySearchItem(versionHref(v.name(), ver.version()),
                publicBase + "/content/" + v.name() + "/versions/" + ver.version(), v.name(), ver.version(),
                ver.status(), false);
    }

    public Dtos.ExternalVocabularyDto external(ExternalVocabulary x) {
        return new Dtos.ExternalVocabularyDto("/external-vocabularies/" + x.name(), x.name(), x.namespaceUri(),
                x.aliasUris(), x.sourceUri(), x.description(), x.createdBy(), x.creationDate());
    }
}
