package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.content.ContentValidator;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * External vocabularies (D30, D41, D42, D56, D66, D73): registration after a Turtle syntax check,
 * lookups by name, namespace and alias, and hard delete.
 */
@Service
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    private final ExternalVocabularyRepository repository;
    private final ContentValidator validator;

    public ExternalVocabularyService(ExternalVocabularyRepository repository, ContentValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /**
     * Registers an external vocabulary. Order after the field checks: 409 (name, then namespace
     * and aliases), then the syntax check (D51).
     *
     * @throws ApiException 409, 400, 503
     */
    public ExternalVocabulary register(ExternalVocabulary data, byte[] content) {
        rejectConflicts(data);
        validator.validateSyntaxAndNotEmpty(content, data.namespaceUri());
        String writeId = UUID.randomUUID().toString();
        repository.create(data, content, writeId);
        if (!repository.writeApplied(data.name(), writeId)) {
            rejectConflicts(data);
            throw ApiException.conflict("External vocabulary with name '" + data.name() + "' already exists.");
        }
        LOG.info("External vocabulary registered");
        return repository.find(data.name()).orElseThrow(() -> notFoundByName(data.name()));
    }

    public Page<ExternalVocabulary> list(long page, int size) {
        return repository.list(page, size);
    }

    /**
     * One external vocabulary by name.
     *
     * @throws ApiException 404
     */
    public ExternalVocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> notFoundByName(name));
    }

    /**
     * Content by name.
     *
     * @throws ApiException 404
     */
    public byte[] content(String name) {
        return repository.content(name).orElseThrow(() -> notFoundByName(name));
    }

    /**
     * Content by namespace URI or alias (D66). Returns the name and the bytes.
     *
     * @throws ApiException 404 with the namespaceUri text
     */
    public Map.Entry<String, byte[]> contentByNamespace(String namespaceUri) {
        String name = repository
                .nameByKey(ExternalVocabularyRepository.normalise(namespaceUri))
                .orElseThrow(() -> notFoundByNamespace(namespaceUri));
        byte[] bytes = repository.content(name).orElseThrow(() -> notFoundByNamespace(namespaceUri));
        return Map.entry(name, bytes);
    }

    /**
     * Hard delete (D41, D42): no reference check.
     *
     * @throws ApiException 404
     */
    public void delete(String name) {
        get(name);
        repository.delete(name);
        LOG.info("External vocabulary removed");
    }

    /** The external search (D38): namespaceUris before aliasUris, each vocabulary once. */
    public List<ExternalVocabulary> search(List<String> uris) {
        Map<String, Boolean> keys = new LinkedHashMap<>();
        for (String uri : uris) {
            keys.put(ExternalVocabularyRepository.normalise(uri), Boolean.TRUE);
        }
        return repository.findByKeys(new ArrayList<>(keys.keySet()));
    }

    private void rejectConflicts(ExternalVocabulary data) {
        if (repository.nameTaken(data.name())) {
            throw ApiException.conflict("External vocabulary with name '" + data.name() + "' already exists.");
        }
        List<String> sent = new ArrayList<>();
        sent.add(data.namespaceUri());
        sent.addAll(data.aliasUris());
        List<String> keys =
                sent.stream().map(ExternalVocabularyRepository::normalise).toList();
        List<String> taken = repository.takenKeys(keys);
        for (String uri : sent) {
            if (taken.contains(ExternalVocabularyRepository.normalise(uri))) {
                throw ApiException.conflict(
                        "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
            }
        }
    }

    public static ApiException notFoundByName(String name) {
        String detail = "External vocabulary with name '" + name + "' was not found.";
        return new ApiException(ProblemType.NOT_FOUND, detail, List.of(Issue.plain("path", "name", detail)));
    }

    public static ApiException notFoundByNamespace(String uri) {
        String detail = "External vocabulary with namespaceUri '" + uri + "' was not found.";
        return new ApiException(ProblemType.NOT_FOUND, detail, List.of(Issue.plain("body", "namespaceUri", detail)));
    }
}
