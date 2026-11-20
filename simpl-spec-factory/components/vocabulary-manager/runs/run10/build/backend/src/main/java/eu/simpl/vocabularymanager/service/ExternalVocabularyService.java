package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.validation.ParsedContent;
import eu.simpl.vocabularymanager.validation.TurtleSyntaxValidator;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Registration, lookup and removal of external vocabularies (D30, D41, D42, D56, D66, D70, D73). */
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    private final ExternalVocabularyRepository repository;
    private final TurtleSyntaxValidator syntax;
    private final Clock clock;

    public ExternalVocabularyService(
            ExternalVocabularyRepository repository, TurtleSyntaxValidator syntax, Clock clock) {
        this.repository = repository;
        this.syntax = syntax;
        this.clock = clock;
    }

    /** A registration request after the field checks. */
    public record Registration(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            byte[] content,
            String createdBy) {}

    /**
     * Registers an external vocabulary: conflicts first (409), then a Turtle syntax check only (D30, D51).
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 409 or 400
     */
    public ExternalVocabulary register(Registration request) {
        checkConflicts(request);
        ParsedContent parsed = syntax.parse(request.content(), request.namespaceUri());
        if (parsed.graph().isEmpty()) {
            throw Problems.badRequest(
                    Issue.invalidInput("vocabularyFile", "Vocabulary contains no statements", "document"));
        }
        ExternalVocabularyRepository.Registration data = new ExternalVocabularyRepository.Registration(
                request.name(),
                request.namespaceUri(),
                request.aliasUris(),
                request.sourceUri(),
                request.description(),
                request.createdBy(),
                clock.instant().truncatedTo(ChronoUnit.SECONDS),
                request.content());
        if (!repository.insert(data, UUID.randomUUID().toString())) {
            checkConflicts(request);
            throw Problems.conflict("External vocabulary with name '" + request.name() + "' already exists.");
        }
        MDC.put("vocabularyName", request.name());
        LOG.info("External vocabulary registered: vocabularyName={}", request.name());
        return new ExternalVocabulary(
                data.name(),
                data.namespaceUri(),
                data.aliasUris(),
                data.sourceUri(),
                data.description(),
                data.createdBy(),
                data.now());
    }

    private void checkConflicts(Registration request) {
        if (repository.existsIgnoreCase(request.name())) {
            throw Problems.conflict("External vocabulary with name '" + request.name() + "' already exists.");
        }
        List<String> uris = new ArrayList<>();
        uris.add(request.namespaceUri());
        uris.addAll(request.aliasUris());
        Map<String, String> taken =
                repository.namesByKey(uris.stream().map(ExternalVocabulary::key).toList());
        for (String uri : uris) {
            if (taken.containsKey(ExternalVocabulary.key(uri))) {
                throw Problems.conflict(
                        "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
            }
        }
    }

    public ExternalVocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    public byte[] content(String name) {
        return repository.content(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /** Content by namespace or alias URI (D66); returns the name for the file name and the bytes. */
    public Map.Entry<String, byte[]> contentByNamespace(String uri) {
        String name =
                repository.namesByKey(List.of(ExternalVocabulary.key(uri))).get(ExternalVocabulary.key(uri));
        if (name == null) {
            throw Problems.externalNamespaceNotFound(uri);
        }
        byte[] content = repository.content(name).orElseThrow(() -> Problems.externalNamespaceNotFound(uri));
        return Map.entry(name, content);
    }

    /** Hard delete of metadata and content, without a reference check (D41, D42). */
    public void delete(String name) {
        get(name);
        repository.delete(name);
        LOG.info("External vocabulary removed: vocabularyName={}", name);
    }

    public VocabularyService.Page<ExternalVocabulary> list(long offset, int limit) {
        long total = repository.count();
        List<ExternalVocabulary> items = offset >= total ? List.of() : repository.list(offset, limit);
        return new VocabularyService.Page<>(items, total);
    }
}
