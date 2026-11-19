package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.problem.Problems;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Registration, lookup and removal of external vocabularies (decisions D30, D38, D41, D42, D56). */
@Service
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    private final VocabularyStore store;
    private final ContentValidator validator;
    private final VocabularyLocks locks;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param store the store
     * @param validator the content validator
     * @param locks the locks
     * @param clock the clock
     */
    public ExternalVocabularyService(
            VocabularyStore store, ContentValidator validator, VocabularyLocks locks, Clock clock) {
        this.store = store;
        this.validator = validator;
        this.locks = locks;
        this.clock = clock;
    }

    /**
     * Registers an external vocabulary after a syntax check (decision D30). Conflicts are checked before the
     * content (decision D51).
     *
     * @param name the name
     * @param namespaceUri the namespace URI
     * @param aliasUris the alias URIs, already checked against D56
     * @param sourceUri the source URI or null
     * @param description the trimmed description or null
     * @param content the content bytes
     * @param createdBy the creator
     * @return the stored external vocabulary
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 409 or 400
     */
    public ExternalVocabulary register(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            byte[] content,
            String createdBy) {
        return locks.withExternalRegistry(() -> {
            if (store.externalNameTaken(name)) {
                throw Problems.conflict("External vocabulary with name '" + name + "' already exists.");
            }
            List<String> uris = new ArrayList<>();
            uris.add(namespaceUri);
            uris.addAll(aliasUris);
            for (String uri : uris) {
                if (store.findExternalByUri(ExternalVocabulary.normalise(uri)).isPresent()) {
                    throw Problems.conflict(
                            "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
                }
            }
            ContentValidator.Outcome outcome = validator.validateSyntaxOnly(content, name);
            if (!outcome.passed()) {
                throw Problems.badRequest(outcome.errors());
            }
            ExternalVocabulary external = new ExternalVocabulary(
                    name,
                    namespaceUri,
                    List.copyOf(aliasUris),
                    sourceUri,
                    description,
                    createdBy,
                    clock.instant().truncatedTo(ChronoUnit.SECONDS));
            store.createExternal(external, content);
            LOG.info("External vocabulary {} registered by {}", name, createdBy);
            return external;
        });
    }

    /**
     * Removes an external vocabulary; no reference check (decision D42).
     *
     * @param name the name
     * @param user who asked, for the log
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public void remove(String name, String user) {
        locks.withExternalRegistry(() -> {
            get(name);
            store.deleteExternal(name);
            LOG.info("External vocabulary {} removed by {}", name, user);
            return null;
        });
    }

    /**
     * Reads an external vocabulary.
     *
     * @param name the exact name
     * @return the external vocabulary
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public ExternalVocabulary get(String name) {
        return store.findExternal(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /**
     * Lists external vocabularies by name ascending.
     *
     * @param offset items to skip
     * @param limit items to return
     * @return the page
     */
    public Page<ExternalVocabulary> list(long offset, int limit) {
        return store.listExternals(offset, limit);
    }

    /**
     * Reads the content by name.
     *
     * @param name the name
     * @return the bytes
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public byte[] contentByName(String name) {
        return store.externalContent(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /**
     * Resolves a namespace or alias URI to the external vocabulary.
     *
     * @param namespaceUri the URI as used in a prefix
     * @return the external vocabulary
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public ExternalVocabulary byNamespace(String namespaceUri) {
        return store.findExternalByUri(ExternalVocabulary.normalise(namespaceUri))
                .orElseThrow(() -> Problems.externalNamespaceNotFound(namespaceUri));
    }

    /**
     * Searches by URIs (decision D38): namespace URIs first, then alias URIs; each match once, in the order of the
     * first URI that matched it.
     *
     * @param uris the URIs in search order
     * @return the matches
     */
    public List<ExternalVocabulary> search(List<String> uris) {
        Map<String, Boolean> keys = new LinkedHashMap<>();
        uris.forEach(uri -> keys.put(ExternalVocabulary.normalise(uri), Boolean.TRUE));
        List<ExternalVocabulary> found = store.findExternalsByUris(List.copyOf(keys.keySet()));
        Map<String, ExternalVocabulary> byKey = new HashMap<>();
        for (ExternalVocabulary external : found) {
            byKey.put(ExternalVocabulary.normalise(external.namespaceUri()), external);
            external.aliasUris().forEach(alias -> byKey.put(ExternalVocabulary.normalise(alias), external));
        }
        Map<String, ExternalVocabulary> ordered = new LinkedHashMap<>();
        for (String key : keys.keySet()) {
            Optional.ofNullable(byKey.get(key)).ifPresent(e -> ordered.putIfAbsent(e.name(), e));
        }
        return List.copyOf(ordered.values());
    }
}
