package eu.simpl.vocabularymanager.support;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.store.SortOrder;
import eu.simpl.vocabularymanager.store.VocabularyQuery;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store for unit tests of the services and validators. */
public class InMemoryStore implements VocabularyStore {

    private final Map<String, Vocabulary> vocabularies = new ConcurrentHashMap<>();
    private final Map<String, VocabularyVersion> versions = new ConcurrentHashMap<>();
    private final Map<String, byte[]> contents = new ConcurrentHashMap<>();
    private final Map<String, ExternalVocabulary> externals = new LinkedHashMap<>();
    private final Map<String, byte[]> externalContents = new ConcurrentHashMap<>();
    public volatile boolean available = true;

    private static String key(String name, long version) {
        return name + "#" + version;
    }

    @Override
    public Optional<Vocabulary> findVocabulary(String name) {
        return Optional.ofNullable(vocabularies.get(name));
    }

    @Override
    public boolean vocabularyNameTaken(String name) {
        return vocabularies.keySet().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    @Override
    public Page<Vocabulary> listVocabularies(VocabularyQuery q) {
        List<Vocabulary> all = vocabularies.values().stream()
                .filter(v -> q.nameContains() == null
                        || v.name()
                                .toLowerCase(Locale.ROOT)
                                .contains(q.nameContains().toLowerCase(Locale.ROOT)))
                .filter(v -> q.status() == null || v.status() == q.status())
                .sorted(Comparator.comparing(Vocabulary::name))
                .toList();
        return new Page<>(all.stream().skip(q.offset()).limit(q.limit()).toList(), all.size());
    }

    @Override
    public Optional<VocabularyVersion> findVersion(String name, long version) {
        return Optional.ofNullable(versions.get(key(name, version)));
    }

    @Override
    public Page<VocabularyVersion> listVersions(String name, SortOrder sort, long offset, int limit) {
        List<VocabularyVersion> all = versions.values().stream()
                .filter(v -> v.name().equals(name))
                .sorted(Comparator.comparingInt(VocabularyVersion::version).reversed())
                .toList();
        return new Page<>(all.stream().skip(offset).limit(limit).toList(), all.size());
    }

    @Override
    public Optional<Integer> latestActiveVersion(String name) {
        return versions.values().stream()
                .filter(v -> v.name().equals(name) && v.status() == VocabularyStatus.ACTIVE)
                .map(VocabularyVersion::version)
                .max(Integer::compare);
    }

    @Override
    public Optional<byte[]> versionContent(String name, long version) {
        return Optional.ofNullable(contents.get(key(name, version)));
    }

    @Override
    public void createVocabulary(Vocabulary vocabulary, VocabularyVersion firstVersion, byte[] content) {
        vocabularies.put(vocabulary.name(), vocabulary);
        versions.put(key(firstVersion.name(), 1), firstVersion);
        contents.put(key(firstVersion.name(), 1), content);
    }

    @Override
    public synchronized void createVersion(VocabularyVersion version, byte[] content) {
        Instant now = version.creationDate();
        for (VocabularyVersion old : new ArrayList<>(versions.values())) {
            if (old.name().equals(version.name()) && old.status() == VocabularyStatus.ACTIVE) {
                versions.put(key(old.name(), old.version()), withStatus(old, VocabularyStatus.DEPRECATED, now));
            }
        }
        versions.put(key(version.name(), version.version()), version);
        contents.put(key(version.name(), version.version()), content);
        Vocabulary v = vocabularies.get(version.name());
        vocabularies.put(
                v.name(),
                new Vocabulary(
                        v.name(),
                        v.status(),
                        v.createdBy(),
                        v.creationDate(),
                        now,
                        version.version(),
                        version.description()));
    }

    @Override
    public void updateVocabularyStatus(String name, VocabularyStatus status, Instant now) {
        Vocabulary v = vocabularies.get(name);
        vocabularies.put(
                name,
                new Vocabulary(
                        v.name(),
                        status,
                        v.createdBy(),
                        v.creationDate(),
                        now,
                        v.latestVersion(),
                        v.latestDescription()));
    }

    @Override
    public void updateVersionStatus(String name, int version, VocabularyStatus status, Instant now) {
        versions.put(key(name, version), withStatus(versions.get(key(name, version)), status, now));
        Vocabulary v = vocabularies.get(name);
        vocabularies.put(
                name,
                new Vocabulary(
                        v.name(),
                        v.status(),
                        v.createdBy(),
                        v.creationDate(),
                        now,
                        v.latestVersion(),
                        v.latestDescription()));
    }

    private static VocabularyVersion withStatus(VocabularyVersion v, VocabularyStatus status, Instant now) {
        return new VocabularyVersion(
                v.name(),
                v.version(),
                status,
                v.description(),
                v.changelog(),
                v.createdBy(),
                v.creationDate(),
                now,
                v.contentSha256());
    }

    @Override
    public Optional<ExternalVocabulary> findExternal(String name) {
        return Optional.ofNullable(externals.get(name));
    }

    @Override
    public boolean externalNameTaken(String name) {
        return externals.keySet().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    @Override
    public Optional<ExternalVocabulary> findExternalByUri(String normalisedUri) {
        return findExternalsByUris(List.of(normalisedUri)).stream().findFirst();
    }

    @Override
    public List<ExternalVocabulary> findExternalsByUris(List<String> keys) {
        return externals.values().stream()
                .filter(e -> keys.contains(ExternalVocabulary.normalise(e.namespaceUri()))
                        || e.aliasUris().stream().anyMatch(a -> keys.contains(ExternalVocabulary.normalise(a))))
                .toList();
    }

    @Override
    public Page<ExternalVocabulary> listExternals(long offset, int limit) {
        List<ExternalVocabulary> all = externals.values().stream()
                .sorted(Comparator.comparing(ExternalVocabulary::name))
                .toList();
        return new Page<>(all.stream().skip(offset).limit(limit).toList(), all.size());
    }

    @Override
    public void createExternal(ExternalVocabulary external, byte[] content) {
        externals.put(external.name(), external);
        externalContents.put(external.name(), content);
    }

    @Override
    public void deleteExternal(String name) {
        externals.remove(name);
        externalContents.remove(name);
    }

    @Override
    public Optional<byte[]> externalContent(String name) {
        return Optional.ofNullable(externalContents.get(name));
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
