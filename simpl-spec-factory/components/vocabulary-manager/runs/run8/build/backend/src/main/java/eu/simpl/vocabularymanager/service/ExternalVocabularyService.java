package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository.ExternalWrite;
import eu.simpl.vocabularymanager.store.VocabularyRepository.ChangeRecord;
import eu.simpl.vocabularymanager.validation.ContentIssue;
import eu.simpl.vocabularymanager.validation.VocabularyContentValidator;
import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.problem.Problems;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** External vocabularies: register, read, search, download, delete (D30, D41, D42, D56, D66, D73). */
@Service
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    private final ExternalVocabularyRepository repository;
    private final VocabularyContentValidator validator;
    private final ConditionalWrite conditionalWrite;
    private final Clock clock;

    /**
     * @param repository store access
     * @param validator content validation
     * @param conditionalWrite retry of conditional writes
     * @param clock clock
     */
    public ExternalVocabularyService(
            ExternalVocabularyRepository repository,
            VocabularyContentValidator validator,
            ConditionalWrite conditionalWrite,
            Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.conditionalWrite = conditionalWrite;
        this.clock = clock;
    }

    /**
     * @param offset rows to skip
     * @param limit page size
     * @return the page
     */
    public Page<ExternalVocabulary> list(long offset, int limit) {
        return repository.list(offset, limit);
    }

    /**
     * @param name exact name
     * @return the external vocabulary
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404
     */
    public ExternalVocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> Problems.externalVocabularyNotFound(name));
    }

    /**
     * @param name exact name
     * @return the stored bytes
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404
     */
    public byte[] content(String name) {
        return repository.content(name).orElseThrow(() -> Problems.externalVocabularyNotFound(name));
    }

    /**
     * POST /external-vocabularies/content (D66).
     *
     * @param namespaceUri the namespace URI as sent
     * @return the external vocabulary and its bytes
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404
     */
    public NamedContent contentByNamespace(String namespaceUri) {
        String key = FieldRules.normalizeNamespace(namespaceUri);
        ExternalVocabulary vocabulary = repository.findByKeys(Set.of(key)).get(key);
        Optional<byte[]> content = vocabulary == null ? Optional.empty() : repository.content(vocabulary.name());
        if (content.isEmpty()) {
            throw Problems.notFound(
                    "External vocabulary with namespaceUri '" + namespaceUri + "' was not found.",
                    "body",
                    "namespaceUri");
        }
        return new NamedContent(vocabulary.name(), content.get());
    }

    /**
     * POST /external-vocabulary/searches (D38): results in the order of the first matching URI, each once.
     *
     * @param uris the URIs as sent, namespaceUris before aliasUris
     * @return the matches
     */
    public List<ExternalVocabulary> search(List<String> uris) {
        Set<String> keys = new LinkedHashSet<>();
        for (String uri : uris) {
            keys.add(FieldRules.normalizeNamespace(uri));
        }
        Map<String, ExternalVocabulary> byKey = repository.findByKeys(keys);
        Map<String, ExternalVocabulary> results = new LinkedHashMap<>();
        for (String key : keys) {
            ExternalVocabulary match = byKey.get(key);
            if (match != null) {
                results.putIfAbsent(match.name(), match);
            }
        }
        return new ArrayList<>(results.values());
    }

    /**
     * POST /external-vocabularies: 409 on a taken name, namespace or alias, then the syntax check (D30, D51, D70).
     *
     * @param command the checked request
     * @param user the caller
     * @return the registered external vocabulary
     */
    public ExternalVocabulary register(Registration command, AuthenticatedUser user) {
        Map<String, String> sentByKey = new LinkedHashMap<>();
        sentByKey.put(FieldRules.normalizeNamespace(command.namespaceUri()), command.namespaceUri());
        for (String alias : command.aliasUris()) {
            sentByKey.putIfAbsent(FieldRules.normalizeNamespace(alias), alias);
        }
        return conditionalWrite.run(() -> {
            Set<String> conflicts = repository.conflicts(FieldRules.nameKey(command.name()), sentByKey.keySet());
            if (conflicts.contains("name")) {
                throw Problems.conflict("External vocabulary with name '" + command.name() + "' already exists.");
            }
            for (Map.Entry<String, String> entry : sentByKey.entrySet()) {
                if (conflicts.contains(entry.getKey())) {
                    throw Problems.conflict("An external vocabulary with namespace or alias '" + entry.getValue()
                            + "' is already registered.");
                }
            }
            List<ContentIssue> errors = validator.validateExternal(command.content(), command.namespaceUri());
            if (!errors.isEmpty()) {
                ContentIssue error = errors.get(0);
                throw Problems.badRequest(List.of(
                        ProblemIssue.invalidInput("body", "vocabularyFile", error.detail(), error.location())));
            }
            ExternalVocabulary vocabulary = new ExternalVocabulary(
                    command.name(),
                    command.namespaceUri(),
                    command.aliasUris(),
                    command.sourceUri(),
                    command.description(),
                    user.creator(),
                    now());
            ExternalWrite write = new ExternalWrite(
                    vocabulary, sentByKey.keySet(), command.content(), user.subject(), UUID.randomUUID().toString());
            if (!repository.insert(write)) {
                return Optional.empty();
            }
            LOG.info("External vocabulary registered");
            return Optional.of(vocabulary);
        });
    }

    /**
     * DELETE /external-vocabularies/{name}: hard delete, no reference check (D41, D42).
     *
     * @param name exact name
     * @param user the caller
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404
     */
    public void delete(String name, AuthenticatedUser user) {
        get(name);
        boolean deleted = repository.delete(name, new ChangeRecord(UUID.randomUUID().toString(), user.subject(), now()));
        if (!deleted) {
            throw Problems.externalVocabularyNotFound(name);
        }
        LOG.info("External vocabulary deleted");
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * @param name name
     * @param namespaceUri namespace URI as sent
     * @param aliasUris alias URIs as sent, in order
     * @param sourceUri source URI or null
     * @param description trimmed description or null
     * @param content uploaded bytes
     */
    public record Registration(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            byte[] content) {}

    /**
     * @param name the external vocabulary name
     * @param content the stored bytes
     */
    public record NamedContent(String name, byte[] content) {}
}
