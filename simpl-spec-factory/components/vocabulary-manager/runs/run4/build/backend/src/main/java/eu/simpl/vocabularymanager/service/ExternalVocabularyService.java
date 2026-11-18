package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Namespaces;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.error.RequestErrorCode;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.validation.ValidationOutcome;
import eu.simpl.vocabularymanager.validation.VocabularyValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Registration, lookup and removal of external vocabularies (decisions D30, D38, D41, D42). */
@Service
public class ExternalVocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalVocabularyService.class);
    private static final String LOCK_KEY = "external-vocabularies";

    private final ExternalVocabularyRepository repository;
    private final VocabularyValidator validator;
    private final VocabularyLocks locks;
    private final Clock clock;

    public ExternalVocabularyService(
            ExternalVocabularyRepository repository,
            VocabularyValidator validator,
            VocabularyLocks locks,
            Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.locks = locks;
        this.clock = clock;
    }

    /** Registration input after the field checks. */
    public record Register(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            byte[] content,
            Caller caller) {}

    /**
     * Registers an external vocabulary after a Turtle syntax check.
     *
     * @throws VocabularyManagerException 409 when the name, namespace URI or an alias is taken; 400 on invalid Turtle
     */
    public ExternalVocabulary register(Register command) {
        return locks.withLock(LOCK_KEY, () -> {
            List<ExternalVocabulary> existing = repository.findAll();
            String key = command.name().toLowerCase(Locale.ROOT);
            if (existing.stream()
                    .anyMatch(e -> e.name().toLowerCase(Locale.ROOT).equals(key))) {
                throw Problems.conflict("External vocabulary with name '" + command.name() + "' already exists.");
            }
            List<String> uris = new ArrayList<>();
            uris.add(command.namespaceUri());
            uris.addAll(command.aliasUris());
            for (String uri : uris) {
                existing.stream().filter(e -> e.matches(uri)).findFirst().ifPresent(e -> {
                    throw Problems.conflict("Namespace URI '" + uri
                            + "' is already registered for external vocabulary '" + e.name() + "'.");
                });
            }
            ValidationOutcome outcome = validator.validateSyntax(command.content(), baseIri(command.namespaceUri()));
            if (outcome.hasErrors()) {
                throw new VocabularyManagerException(
                        RequestErrorCode.BAD_REQUEST, Problems.INPUT_INCORRECT, outcome.errors());
            }
            Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
            ExternalVocabulary vocabulary = new ExternalVocabulary(
                    command.name(),
                    command.namespaceUri(),
                    command.aliasUris(),
                    command.sourceUri(),
                    command.description(),
                    command.caller().id(),
                    now);
            repository.create(vocabulary, command.content());
            LOG.info("External vocabulary registered: name={}", command.name());
            return vocabulary;
        });
    }

    /** Lists external vocabularies, name ascending (decision D41). */
    public Page<ExternalVocabulary> list(PageRequest pageRequest) {
        List<ExternalVocabulary> all = repository.findAll().stream()
                .sorted(Comparator.comparing(ExternalVocabulary::name))
                .toList();
        return Page.of(all, pageRequest);
    }

    /**
     * Gets one external vocabulary.
     *
     * @throws VocabularyManagerException 404 when it does not exist
     */
    public ExternalVocabulary get(String name) {
        return repository.findByName(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /**
     * Content of an external vocabulary by name.
     *
     * @throws VocabularyManagerException 404 when it does not exist
     */
    public byte[] content(String name) {
        get(name);
        return repository.findContent(name).orElseThrow(() -> Problems.externalNotFound(name));
    }

    /** Result of a content lookup by namespace URI. */
    public record NamedContent(String name, byte[] bytes) {}

    /**
     * Content of the external vocabulary whose namespace URI or alias matches.
     *
     * @throws VocabularyManagerException 404 when none matches
     */
    public NamedContent contentByNamespace(String namespaceUri) {
        ExternalVocabulary match = repository.findAll().stream()
                .filter(e -> e.matches(namespaceUri))
                .findFirst()
                .orElseThrow(() -> Problems.externalNamespaceNotFound(namespaceUri));
        byte[] bytes = repository
                .findContent(match.name())
                .orElseThrow(() -> Problems.externalNamespaceNotFound(namespaceUri));
        return new NamedContent(match.name(), bytes);
    }

    /**
     * Removes an external vocabulary; no reference check (decision D42).
     *
     * @throws VocabularyManagerException 404 when it does not exist
     */
    public void delete(String name) {
        locks.withLock(LOCK_KEY, () -> {
            get(name);
            repository.delete(name);
            LOG.info("External vocabulary removed: name={}", name);
            return null;
        });
    }

    /** Looks the URIs up in order, namespace URIs before alias URIs; each match appears once (decision D38). */
    public List<ExternalVocabulary> search(List<String> uris) {
        List<ExternalVocabulary> all = repository.findAll();
        Set<String> seen = new LinkedHashSet<>();
        List<ExternalVocabulary> results = new ArrayList<>();
        for (String uri : uris) {
            for (ExternalVocabulary candidate : all) {
                if (candidate.matches(uri) && seen.add(candidate.name())) {
                    results.add(candidate);
                }
            }
        }
        return results;
    }

    private static String baseIri(String namespaceUri) {
        String normalised = Namespaces.normalise(namespaceUri);
        return normalised.isEmpty() ? "urn:simpl:external" : namespaceUri;
    }
}
