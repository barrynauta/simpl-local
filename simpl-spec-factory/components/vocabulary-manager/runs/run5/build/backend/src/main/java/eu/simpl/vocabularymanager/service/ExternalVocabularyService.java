package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.web.ApiException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Registration, lookup and removal of external vocabularies (D30, D41, D42, D56). */
@Service
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);
    private static final String REGISTRATION_LOCK = "external-vocabulary-registry";

    private final VocabularyStore store;
    private final ContentValidator validator;
    private final NameLocks locks;
    private final VocabularyService vocabularies;
    private final String contentBase;

    public ExternalVocabularyService(
            VocabularyStore store,
            ContentValidator validator,
            NameLocks locks,
            VocabularyService vocabularies,
            VocabularyProperties properties) {
        this.store = store;
        this.validator = validator;
        this.locks = locks;
        this.vocabularies = vocabularies;
        this.contentBase = properties.contentBase();
    }

    /** All external vocabularies, name ascending by code point (D41, D58). */
    public List<ExternalVocabulary> list() {
        List<ExternalVocabulary> all = new ArrayList<>(store.listExternalVocabularies());
        all.sort(Comparator.comparing(ExternalVocabulary::name));
        return all;
    }

    /** One external vocabulary, or 404. */
    public ExternalVocabulary get(String name) {
        return store.findExternalVocabulary(name).orElseThrow(() -> notFound(name));
    }

    /** Cached content by name, or 404. */
    public byte[] content(String name) {
        get(name);
        return store.findExternalContent(name).orElseThrow(() -> notFound(name));
    }

    /**
     * Registers an external vocabulary: 409 on name, namespace or alias, then the Turtle syntax check (D30, D51).
     *
     * @param request the registration, fields already checked
     * @param content the Turtle bytes
     * @return the stored external vocabulary
     */
    public ExternalVocabulary register(ExternalVocabulary request, byte[] content) {
        return locks.withLock(REGISTRATION_LOCK, () -> {
            List<ExternalVocabulary> existing = store.listExternalVocabularies();
            String key = request.name().toLowerCase(Locale.ROOT);
            if (existing.stream()
                    .anyMatch(e -> e.name().toLowerCase(Locale.ROOT).equals(key))) {
                throw ApiException.conflict("External vocabulary with name '" + request.name() + "' already exists.");
            }
            List<String> taken =
                    existing.stream().flatMap(e -> e.lookupKeys().stream()).toList();
            List<String> wanted = new ArrayList<>();
            wanted.add(request.namespaceUri());
            wanted.addAll(request.aliasUris());
            for (String uri : wanted) {
                if (taken.contains(ExternalVocabulary.normalise(uri))) {
                    throw ApiException.conflict(
                            "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
                }
            }
            validator.checkSyntax(content, contentBase + "/external-vocabularies/" + request.name());
            ExternalVocabulary stored = new ExternalVocabulary(
                    request.name(),
                    request.namespaceUri(),
                    request.aliasUris(),
                    request.sourceUri(),
                    request.description(),
                    request.createdBy(),
                    vocabularies.now());
            store.createExternalVocabulary(stored, content);
            LOG.info("External vocabulary {} registered", stored.name());
            return stored;
        });
    }

    /** Removes an external vocabulary without a reference check (D42). */
    public void delete(String name, String actor) {
        locks.withLock(REGISTRATION_LOCK, () -> {
            get(name);
            store.deleteExternalVocabulary(name);
            LOG.info("External vocabulary {} removed by {}", name, actor);
            return null;
        });
    }

    /**
     * Finds the external vocabulary whose namespace URI or alias matches, after normalising (D30).
     *
     * @param uri namespace or alias URI
     * @return the match
     */
    public Optional<ExternalVocabulary> findByUri(String uri) {
        String key = ExternalVocabulary.normalise(uri);
        return list().stream().filter(e -> e.lookupKeys().contains(key)).findFirst();
    }

    /**
     * Searches by namespace URIs and alias URIs (D38): order of the first matching URI, namespaces before aliases;
     * each match once.
     *
     * @param namespaceUris namespace URI candidates
     * @param aliasUris alias URI candidates
     * @return matches
     */
    public List<ExternalVocabulary> search(List<String> namespaceUris, List<String> aliasUris) {
        List<ExternalVocabulary> all = list();
        Map<String, ExternalVocabulary> results = new LinkedHashMap<>();
        for (String uri : namespaceUris) {
            String key = ExternalVocabulary.normalise(uri);
            all.stream()
                    .filter(e -> ExternalVocabulary.normalise(e.namespaceUri()).equals(key)
                            || e.aliasUris().stream()
                                    .anyMatch(
                                            a -> ExternalVocabulary.normalise(a).equals(key)))
                    .forEach(e -> results.putIfAbsent(e.name(), e));
        }
        for (String uri : aliasUris) {
            String key = ExternalVocabulary.normalise(uri);
            all.stream().filter(e -> e.lookupKeys().contains(key)).forEach(e -> results.putIfAbsent(e.name(), e));
        }
        return new ArrayList<>(results.values());
    }

    /** 404 for an unknown external vocabulary (D41). */
    public static ApiException notFound(String name) {
        return ApiException.notFound("External vocabulary with name '" + name + "' was not found.", "name");
    }
}
