package eu.simpl.vocabularymanager.support;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** In-memory store for unit tests of the services. */
public class InMemoryVocabularyStore implements VocabularyStore {

    private final Map<String, Vocabulary> vocabularies = new LinkedHashMap<>();
    private final Map<String, TreeMap<Integer, VocabularyVersion>> versions = new LinkedHashMap<>();
    private final Map<String, byte[]> contents = new LinkedHashMap<>();
    private final Map<String, ExternalVocabulary> externals = new LinkedHashMap<>();
    private final Map<String, byte[]> externalContents = new LinkedHashMap<>();
    public boolean available = true;
    public int writes;

    @Override
    public synchronized Optional<Vocabulary> findVocabulary(String name) {
        Vocabulary v = vocabularies.get(name);
        if (v == null) {
            return Optional.empty();
        }
        String description = versions.get(name).get(v.latestVersion()).description();
        return Optional.of(new Vocabulary(
                v.name(),
                v.status(),
                v.createdBy(),
                v.creationDate(),
                v.lastUpdateDate(),
                v.latestVersion(),
                description));
    }

    @Override
    public synchronized boolean vocabularyNameTaken(String name) {
        return vocabularies.keySet().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    @Override
    public synchronized List<Vocabulary> listVocabularies() {
        return vocabularies.keySet().stream()
                .map(n -> findVocabulary(n).orElseThrow())
                .toList();
    }

    @Override
    public synchronized List<VocabularyVersion> listVersions(String name) {
        return new ArrayList<>(versions.getOrDefault(name, new TreeMap<>()).values());
    }

    @Override
    public synchronized Optional<VocabularyVersion> findVersion(String name, int version) {
        return Optional.ofNullable(versions.getOrDefault(name, new TreeMap<>()).get(version));
    }

    @Override
    public synchronized Optional<byte[]> findContent(String name, int version) {
        return Optional.ofNullable(contents.get(name + "/" + version));
    }

    @Override
    public synchronized void createVocabulary(Vocabulary vocabulary, VocabularyVersion first, byte[] content) {
        writes++;
        vocabularies.put(vocabulary.name(), vocabulary);
        versions.computeIfAbsent(vocabulary.name(), k -> new TreeMap<>()).put(first.version(), first);
        contents.put(vocabulary.name() + "/" + first.version(), content.clone());
    }

    @Override
    public synchronized void createVersion(VocabularyVersion version, byte[] content, String now) {
        writes++;
        TreeMap<Integer, VocabularyVersion> all = versions.get(version.name());
        all.replaceAll((n, v) -> v.status() == Status.ACTIVE ? with(v, Status.DEPRECATED, now) : v);
        all.put(version.version(), version);
        contents.put(version.name() + "/" + version.version(), content.clone());
        Vocabulary v = vocabularies.get(version.name());
        vocabularies.put(
                v.name(),
                new Vocabulary(v.name(), v.status(), v.createdBy(), v.creationDate(), now, version.version(), null));
    }

    @Override
    public synchronized void updateVocabularyStatus(String name, Status status, String now) {
        writes++;
        Vocabulary v = vocabularies.get(name);
        vocabularies.put(
                name, new Vocabulary(name, status, v.createdBy(), v.creationDate(), now, v.latestVersion(), null));
    }

    @Override
    public synchronized void updateVersionStatus(String name, int version, Status status, String now) {
        writes++;
        versions.get(name).computeIfPresent(version, (n, v) -> with(v, status, now));
        Vocabulary v = vocabularies.get(name);
        vocabularies.put(
                name, new Vocabulary(name, v.status(), v.createdBy(), v.creationDate(), now, v.latestVersion(), null));
    }

    private static VocabularyVersion with(VocabularyVersion v, Status status, String now) {
        return new VocabularyVersion(
                v.name(), v.version(), status, v.description(), v.changelog(), v.createdBy(), v.creationDate(), now);
    }

    @Override
    public synchronized List<ExternalVocabulary> listExternalVocabularies() {
        return new ArrayList<>(externals.values());
    }

    @Override
    public synchronized Optional<ExternalVocabulary> findExternalVocabulary(String name) {
        return Optional.ofNullable(externals.get(name));
    }

    @Override
    public synchronized Optional<byte[]> findExternalContent(String name) {
        return Optional.ofNullable(externalContents.get(name));
    }

    @Override
    public synchronized void createExternalVocabulary(ExternalVocabulary vocabulary, byte[] content) {
        writes++;
        externals.put(vocabulary.name(), vocabulary);
        externalContents.put(vocabulary.name(), content);
    }

    @Override
    public synchronized void deleteExternalVocabulary(String name) {
        writes++;
        externals.remove(name);
        externalContents.remove(name);
    }

    @Override
    public boolean isAvailable(Duration timeout) {
        return available;
    }

    /** Number of vocabularies whose name contains the text, ignoring case. */
    public synchronized long countLike(String text) {
        return vocabularies.keySet().stream()
                .filter(n -> n.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT)))
                .count();
    }
}
