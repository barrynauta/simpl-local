package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.util.List;

/** Inputs and outputs of the vocabulary service. */
public final class VocabularyCommands {

    private VocabularyCommands() {}

    /** POST /vocabularies after the field checks; texts are trimmed. */
    public record CreateVocabulary(String name, String description, String changelog, byte[] content, Caller caller) {}

    /** POST .../versions after the field checks; texts are trimmed. */
    public record CreateVersion(String description, String changelog, byte[] content, Caller caller) {}

    /** A created version with its validation warnings. */
    public record Created(VocabularyVersion version, List<ValidationWarning> warnings) {}

    /** A version together with the latest version number of its vocabulary (for latestVersionHref). */
    public record VersionView(VocabularyVersion version, int latestVersion) {}

    /** A page of versions together with the latest version number of the vocabulary. */
    public record VersionPage(Page<VocabularyVersion> page, int latestVersion) {}

    /** Downloaded content of one version. */
    public record Content(String name, int version, byte[] bytes) {}

    /** One reference of a vocabulary search; version null means the latest version. */
    public record SearchReference(String name, Integer version) {}

    /** One result of a vocabulary search (decision D38). */
    public record SearchResult(String name, int version, VocabularyStatus status, boolean latest) {}
}
