package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What the store knows about the vocabularies and namespaces an upload references (D12): one row
 * per version of each referenced internal vocabulary, and the registered external lookup keys found.
 */
public record ReferenceState(List<VersionRow> versions, List<String> externalKeys) {

    /** One version of a referenced internal vocabulary. */
    public record VersionRow(
            String name,
            VocabularyStatus vocabularyStatus,
            int latestVersion,
            int version,
            VocabularyStatus versionStatus) {}

    public ReferenceState {
        versions = List.copyOf(versions);
        externalKeys = List.copyOf(externalKeys);
    }

    public boolean vocabularyExists(String name) {
        return versions.stream().anyMatch(r -> r.name().equals(name));
    }

    public Optional<VersionRow> version(String name, int version) {
        return versions.stream()
                .filter(r -> r.name().equals(name) && r.version() == version)
                .findFirst();
    }

    public OptionalInt latestVersion(String name) {
        return versions.stream()
                .filter(r -> r.name().equals(name))
                .mapToInt(VersionRow::latestVersion)
                .findFirst();
    }

    /** The highest-numbered version with version status ACTIVE (D5). */
    public OptionalInt latestActiveVersion(String name) {
        return versions.stream()
                .filter(r -> r.name().equals(name) && r.versionStatus() == VocabularyStatus.ACTIVE)
                .mapToInt(VersionRow::version)
                .max();
    }

    public Set<String> externalKeySet() {
        return externalKeys.stream().collect(Collectors.toSet());
    }
}
