package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ConflictException;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ExternalVocabularyNotFoundException;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.UriKeys;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Registration, lookup and removal of external vocabularies (D30, D38, D41, D42, D56, D66, D73). */
@Service
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    private final ExternalVocabularyRepository repository;
    private final ContentValidator validator;
    private final Clock clock;

    /**
     * @param repository storage
     * @param validator content validation
     * @param clock time source
     */
    public ExternalVocabularyService(ExternalVocabularyRepository repository, ContentValidator validator, Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Registers an external vocabulary: conflicts first, then the syntax check (D51).
     *
     * @param command checked request fields
     * @return the stored external vocabulary
     * @throws ConflictException when the name, namespace or an alias is taken
     */
    public ExternalVocabulary register(RegisterExternalCommand command) {
        checkConflicts(command);
        validator.validateExternal(command.content(), command.name());
        ExternalVocabulary vocabulary = new ExternalVocabulary(
                command.name(),
                command.namespaceUri(),
                List.copyOf(command.aliasUris()),
                command.sourceUri(),
                command.description(),
                command.createdBy(),
                clock.instant().truncatedTo(ChronoUnit.SECONDS));
        if (!repository.insert(vocabulary, command.content())) {
            checkConflicts(command);
            throw new ConflictException(
                    "External vocabulary with name '" + command.name() + "' could not be registered: it conflicts.");
        }
        LOG.info("External vocabulary registered");
        return vocabulary;
    }

    private void checkConflicts(RegisterExternalCommand command) {
        if (repository.existsIgnoreCase(command.name())) {
            throw new ConflictException("External vocabulary with name '" + command.name() + "' already exists.");
        }
        List<String> uris = new ArrayList<>();
        uris.add(command.namespaceUri());
        uris.addAll(command.aliasUris());
        Set<String> taken =
                repository.existingKeys(uris.stream().map(UriKeys::key).toList());
        for (String uri : uris) {
            if (taken.contains(UriKeys.key(uri))) {
                throw new ConflictException(
                        "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
            }
        }
    }

    /**
     * @param page page selection
     * @return one page ordered by name
     */
    public Page<ExternalVocabulary> list(PageRequest page) {
        return repository.list(page);
    }

    /**
     * @param name exact name
     * @return the external vocabulary
     * @throws ExternalVocabularyNotFoundException when unknown
     */
    public ExternalVocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> ExternalVocabularyNotFoundException.byName(name));
    }

    /**
     * @param name exact name
     * @return stored content
     * @throws ExternalVocabularyNotFoundException when unknown
     */
    public byte[] content(String name) {
        return repository.content(name).orElseThrow(() -> ExternalVocabularyNotFoundException.byName(name));
    }

    /**
     * Content by namespace or alias URI (D66).
     *
     * @param uri the URI as sent
     * @return the external vocabulary and its content
     * @throws ExternalVocabularyNotFoundException when no namespace or alias matches
     */
    public Map.Entry<ExternalVocabulary, byte[]> contentByNamespace(String uri) {
        ExternalVocabulary vocabulary =
                repository.findByKeys(List.of(UriKeys.key(uri))).get(UriKeys.key(uri));
        if (vocabulary == null) {
            throw ExternalVocabularyNotFoundException.byNamespace(uri);
        }
        byte[] content = repository
                .content(vocabulary.name())
                .orElseThrow(() -> ExternalVocabularyNotFoundException.byNamespace(uri));
        return Map.entry(vocabulary, content);
    }

    /**
     * Removes an external vocabulary without a reference check (D42).
     *
     * @param name exact name
     * @throws ExternalVocabularyNotFoundException when unknown
     */
    public void delete(String name) {
        get(name);
        repository.delete(name);
        LOG.info("External vocabulary removed");
    }

    /**
     * Search by URIs in the decided order (D38): each external vocabulary once, at its first matching URI.
     *
     * @param uris namespace URIs first, then alias URIs, as sent
     * @return matches
     */
    public List<ExternalVocabulary> search(List<String> uris) {
        Set<String> keys = new LinkedHashSet<>();
        uris.forEach(uri -> keys.add(UriKeys.key(uri)));
        Map<String, ExternalVocabulary> byKey = repository.findByKeys(keys);
        Map<String, ExternalVocabulary> result = new LinkedHashMap<>();
        for (String key : keys) {
            ExternalVocabulary vocabulary = byKey.get(key);
            if (vocabulary != null) {
                result.putIfAbsent(vocabulary.name(), vocabulary);
            }
        }
        return List.copyOf(result.values());
    }

    /** @return now, to the second */
    Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }
}
