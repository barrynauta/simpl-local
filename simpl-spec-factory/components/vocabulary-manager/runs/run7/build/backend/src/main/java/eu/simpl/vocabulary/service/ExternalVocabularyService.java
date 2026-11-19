package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.api.request.FieldRules;
import eu.simpl.vocabulary.domain.model.ExternalVocabulary;
import eu.simpl.vocabulary.domain.model.StoredContent;
import eu.simpl.vocabulary.store.VocabularyStore;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registration and removal of external vocabularies (D30, D41, D42, D56, D73). */
public class ExternalVocabularyService {

    /** All external registrations share one lock, because namespace and aliases are unique across them. */
    static final String LOCK_KEY = "#external-vocabularies";

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    private final VocabularyStore store;
    private final ContentValidation validation;
    private final NameLocks locks;
    private final Clock clock;

    public ExternalVocabularyService(
            VocabularyStore store, ContentValidation validation, NameLocks locks, Clock clock) {
        this.store = store;
        this.validation = validation;
        this.locks = locks;
        this.clock = clock;
    }

    /**
     * Registers an external vocabulary: 409 on name, namespace or alias, then the syntax check.
     *
     * @param registration the metadata as sent
     * @param content the Turtle bytes
     * @return the stored external vocabulary
     */
    public ExternalVocabulary register(ExternalVocabulary registration, byte[] content) {
        return locks.withLock(LOCK_KEY, () -> {
            if (store.externalNameTaken(registration.name())) {
                throw Problems.conflict("External vocabulary with name '" + registration.name() + "' already exists.");
            }
            Set<String> taken = new HashSet<>();
            for (ExternalVocabulary existing : store.listExternal()) {
                taken.add(FieldRules.normaliseNamespace(existing.namespaceUri()));
                existing.aliasUris().forEach(a -> taken.add(FieldRules.normaliseNamespace(a)));
            }
            List<String> candidates = new ArrayList<>();
            candidates.add(registration.namespaceUri());
            candidates.addAll(registration.aliasUris());
            for (String uri : candidates) {
                if (taken.contains(FieldRules.normaliseNamespace(uri))) {
                    throw Problems.conflict(
                            "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
                }
            }
            validation.validateExternal(registration.name(), content);
            ExternalVocabulary stored = new ExternalVocabulary(
                    registration.name(),
                    registration.namespaceUri(),
                    registration.aliasUris(),
                    registration.sourceUri(),
                    registration.description(),
                    registration.createdBy(),
                    clock.instant().truncatedTo(ChronoUnit.SECONDS));
            store.createExternal(stored, content);
            LOG.info("External vocabulary {} registered", stored.name());
            return stored;
        });
    }

    /**
     * Lists external vocabularies by name ascending, code point order (D41, D58).
     *
     * @param page page number
     * @param pageSize page size
     * @return the page
     */
    public Page<ExternalVocabulary> list(long page, int pageSize) {
        List<ExternalVocabulary> sorted = store.listExternal().stream()
                .sorted(Comparator.comparing(ExternalVocabulary::name))
                .toList();
        return Page.of(sorted, page, pageSize);
    }

    /**
     * @param name exact name
     * @return the external vocabulary
     */
    public ExternalVocabulary get(String name) {
        return store.findExternal(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /**
     * @param name exact name
     * @return the stored bytes
     */
    public StoredContent content(String name) {
        return store.findExternalContent(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /**
     * Finds the content by namespace or alias URI (D66).
     *
     * @param namespaceUri the URI as sent
     * @param searchService lookup by URI
     * @return the external vocabulary and its bytes
     */
    public StoredContent contentByNamespace(String namespaceUri, SearchService searchService) {
        List<ExternalVocabulary> found = searchService.searchExternal(List.of(namespaceUri));
        if (found.isEmpty()) {
            throw Problems.externalNamespaceNotFound(namespaceUri);
        }
        return store.findExternalContent(found.get(0).name())
                .orElseThrow(() -> Problems.externalNamespaceNotFound(namespaceUri));
    }

    /**
     * Removes metadata and content, without reference checks (D42).
     *
     * @param name exact name
     */
    public void delete(String name) {
        locks.withLock(LOCK_KEY, () -> {
            get(name);
            store.deleteExternal(name);
            LOG.info("External vocabulary {} removed", name);
            return null;
        });
    }
}
