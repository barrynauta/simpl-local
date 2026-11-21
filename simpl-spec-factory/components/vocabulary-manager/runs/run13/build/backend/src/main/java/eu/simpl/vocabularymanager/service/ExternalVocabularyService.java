package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.UriKeys;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository.Conflicts;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository.ExternalMatch;
import eu.simpl.vocabularymanager.validation.ParsedContent;
import eu.simpl.vocabularymanager.validation.TurtleSyntax;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registration, lookup and removal of external vocabularies (D30, D38, D41, D42, D56, D66, D147). */
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);

    /** A registration request after the field checks. */
    public record Registration(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            byte[] content) {}

    private final ExternalVocabularyRepository repository;
    private final Clock clock;

    public ExternalVocabularyService(ExternalVocabularyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Page<ExternalVocabulary> list(int page, int pageSize) {
        return repository.list(page, pageSize);
    }

    public ExternalVocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    public byte[] content(String name) {
        get(name);
        return repository.content(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /** POST /external-vocabularies/content: lookup by namespace or alias URI (D66). */
    public Map.Entry<ExternalVocabulary, byte[]> contentByNamespace(String namespaceUri) {
        Map<String, ExternalMatch> matches = repository.findByKeys(List.of(UriKeys.key(namespaceUri)));
        if (matches.isEmpty()) {
            throw Problems.externalNamespaceNotFound(namespaceUri);
        }
        ExternalVocabulary external = matches.values().iterator().next().vocabulary();
        byte[] content =
                repository.content(external.name()).orElseThrow(() -> Problems.externalNamespaceNotFound(namespaceUri));
        return Map.entry(external, content);
    }

    /** External search: results in the order of the first matching URI, each vocabulary once (D38). */
    public List<ExternalVocabulary> search(List<String> uris) {
        List<String> keys = uris.stream().map(UriKeys::key).toList();
        Map<String, ExternalMatch> matches = repository.findByKeys(new LinkedHashSet<>(keys));
        Map<String, ExternalVocabulary> ordered = new LinkedHashMap<>();
        for (String key : keys) {
            for (ExternalMatch match : matches.values()) {
                if (match.keys().contains(key)) {
                    ordered.putIfAbsent(match.vocabulary().name(), match.vocabulary());
                }
            }
        }
        return new ArrayList<>(ordered.values());
    }

    /** Checks of D51 after the field checks: 409 (D147 order), then the syntax check (D30, D70, D124). */
    public ExternalVocabulary register(Registration registration, String creator) {
        rejectConflicts(registration);
        ParsedContent parsed = TurtleSyntax.parse(registration.content(), registration.namespaceUri());
        if (parsed.triples().isEmpty()) {
            throw Problems.badRequest(ProblemIssue.invalidInput(
                    "body", "vocabularyFile", "Vocabulary contains no statements", "document"));
        }
        ExternalVocabulary external = new ExternalVocabulary(
                registration.name(),
                registration.namespaceUri(),
                registration.aliasUris(),
                registration.sourceUri(),
                registration.description(),
                creator,
                clock.instant().truncatedTo(ChronoUnit.SECONDS));
        if (!repository.insert(
                external, registration.content(), UUID.randomUUID().toString())) {
            rejectConflicts(registration);
            throw Problems.conflict(
                    "An external vocabulary with name '" + registration.name() + "' is already registered.");
        }
        LOG.info("External vocabulary registered");
        return external;
    }

    private void rejectConflicts(Registration registration) {
        List<String> uris = new ArrayList<>();
        uris.add(registration.namespaceUri());
        uris.addAll(registration.aliasUris());
        Set<String> keys = new LinkedHashSet<>(uris.stream().map(UriKeys::key).toList());
        Conflicts conflicts = repository.conflicts(registration.name(), keys);
        if (conflicts.nameTaken()) {
            throw Problems.conflict(
                    "An external vocabulary with name '" + registration.name() + "' is already registered.");
        }
        for (String uri : uris) {
            if (conflicts.takenKeys().contains(UriKeys.key(uri))) {
                throw Problems.conflict(
                        "An external vocabulary with namespace or alias '" + uri + "' is already registered.");
            }
        }
    }

    public void delete(String name) {
        get(name);
        repository.delete(name);
        LOG.info("External vocabulary removed");
    }
}
