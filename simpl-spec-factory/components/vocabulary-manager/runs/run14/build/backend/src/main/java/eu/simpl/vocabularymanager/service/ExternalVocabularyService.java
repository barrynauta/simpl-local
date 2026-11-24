package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.References;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Use cases of external vocabularies (D30, D38, D41, D42, D56, D66, D73, D147, D148). */
@Service
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    private final ExternalVocabularyRepository repository;
    private final ContentValidator validator;
    private final ConditionalWrite conditionalWrite;
    private final Clock clock;

    public ExternalVocabularyService(
            ExternalVocabularyRepository repository,
            ContentValidator validator,
            ConditionalWrite conditionalWrite,
            Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.conditionalWrite = conditionalWrite;
        this.clock = clock;
    }

    /**
     * A registration request after the field checks.
     *
     * @param name name
     * @param namespaceUri namespace URI as sent
     * @param aliasUris alias URIs as sent
     * @param sourceUri source URI or null
     * @param description description or null
     * @param content the file bytes
     */
    public record Registration(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            byte[] content) {}

    /**
     * Registers an external vocabulary.
     *
     * @throws ApiException 409 on a name or URI conflict (D147 order), 400 on invalid content
     */
    public ExternalVocabulary register(Registration registration, AuthenticatedUser user) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(References.normalise(registration.namespaceUri()));
        for (String alias : registration.aliasUris()) {
            keys.add(References.normalise(alias));
        }
        checkConflicts(registration, keys);
        validator.validateExternal(registration.content(), registration.namespaceUri());
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        ExternalVocabulary vocabulary = new ExternalVocabulary(
                registration.name(),
                registration.namespaceUri(),
                List.copyOf(registration.aliasUris()),
                registration.sourceUri(),
                registration.description(),
                user.creator(),
                now);
        conditionalWrite.run(() -> {
            if (repository.insert(vocabulary, keys, registration.content())) {
                return Boolean.TRUE;
            }
            checkConflicts(registration, keys);
            return null;
        });
        LOG.info("External vocabulary registered by user {}", user.subject());
        return vocabulary;
    }

    private void checkConflicts(Registration registration, Set<String> keys) {
        if (repository.nameTaken(registration.name())) {
            throw ApiException.conflict(
                    "An external vocabulary with name '" + registration.name() + "' is already registered.");
        }
        Set<String> taken = new HashSet<>(repository.registeredKeys(keys));
        if (taken.isEmpty()) {
            return;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(registration.namespaceUri());
        candidates.addAll(registration.aliasUris());
        for (String uri : candidates) {
            if (taken.contains(References.normalise(uri))) {
                throw ApiException.conflict(
                        "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
            }
        }
    }

    /** Lists external vocabularies in name order (D41). */
    public VocabularyService.Slice<ExternalVocabulary> list(long offset, int limit) {
        long total = repository.count();
        if (offset >= total) {
            return new VocabularyService.Slice<>(List.of(), total);
        }
        return new VocabularyService.Slice<>(repository.list(offset, limit), total);
    }

    /**
     * Gets an external vocabulary.
     *
     * @throws ApiException 404 with the text of D41
     */
    public ExternalVocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> notFound(name));
    }

    /**
     * Reads the content by name.
     *
     * @throws ApiException 404 with the text of D41
     */
    public byte[] content(String name) {
        get(name);
        return repository.content(name).orElseThrow(() -> notFound(name));
    }

    /**
     * Reads the content by namespace or alias URI (D66).
     *
     * @param namespaceUri the URI as sent
     * @return the vocabulary and its bytes
     * @throws ApiException 404 with the text of D66
     */
    public Map.Entry<ExternalVocabulary, byte[]> contentByNamespace(String namespaceUri) {
        Map<String, ExternalVocabulary> found = repository.findByKeys(List.of(References.normalise(namespaceUri)));
        if (found.isEmpty()) {
            throw namespaceNotFound(namespaceUri);
        }
        ExternalVocabulary vocabulary = found.values().iterator().next();
        byte[] bytes = repository.content(vocabulary.name()).orElseThrow(() -> namespaceNotFound(namespaceUri));
        return Map.entry(vocabulary, bytes);
    }

    /**
     * Searches by URIs (D38): results in the order of the first matching URI, namespaceUris before aliasUris,
     * each external vocabulary once.
     *
     * @param uris the URIs in that order
     * @return the matching external vocabularies
     */
    public List<ExternalVocabulary> search(List<String> uris) {
        Set<String> keys = new LinkedHashSet<>();
        for (String uri : uris) {
            keys.add(References.normalise(uri));
        }
        Map<String, ExternalVocabulary> byKey = repository.findByKeys(keys);
        Map<String, ExternalVocabulary> results = new LinkedHashMap<>();
        for (String key : keys) {
            ExternalVocabulary match = byKey.get(key);
            if (match != null) {
                results.putIfAbsent(match.name(), match);
            }
        }
        return List.copyOf(results.values());
    }

    /**
     * Removes an external vocabulary without a reference check (D41, D42).
     *
     * @throws ApiException 404 with the text of D41
     */
    public void delete(String name, AuthenticatedUser user) {
        get(name);
        repository.delete(name);
        LOG.info("External vocabulary removed by user {}", user.subject());
    }

    static ApiException notFound(String name) {
        return ApiException.notFound("External vocabulary with name '" + name + "' was not found.", "path", "name");
    }

    private static ApiException namespaceNotFound(String uri) {
        return ApiException.notFound(
                "External vocabulary with namespaceUri '" + uri + "' was not found.", "body", "namespaceUri");
    }
}
