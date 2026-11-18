package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.error.RequestErrorCode;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import eu.simpl.vocabularymanager.service.VocabularyCommands.Content;
import eu.simpl.vocabularymanager.service.VocabularyCommands.CreateVersion;
import eu.simpl.vocabularymanager.service.VocabularyCommands.CreateVocabulary;
import eu.simpl.vocabularymanager.service.VocabularyCommands.Created;
import eu.simpl.vocabularymanager.service.VocabularyCommands.SearchReference;
import eu.simpl.vocabularymanager.service.VocabularyCommands.SearchResult;
import eu.simpl.vocabularymanager.service.VocabularyCommands.VersionPage;
import eu.simpl.vocabularymanager.service.VocabularyCommands.VersionView;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.InternalVocabularyState;
import eu.simpl.vocabularymanager.validation.ReferenceLookup;
import eu.simpl.vocabularymanager.validation.ValidationOutcome;
import eu.simpl.vocabularymanager.validation.VocabularyValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Use cases on vocabularies and vocabulary versions (BP02B, decisions D3 to D12, D25 to D29, D38, D43, D46). */
@Service
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyRepository repository;
    private final ExternalVocabularyRepository externalRepository;
    private final VocabularyValidator validator;
    private final VocabularyLocks locks;
    private final Clock clock;
    private final String contentBase;

    public VocabularyService(
            VocabularyRepository repository,
            ExternalVocabularyRepository externalRepository,
            VocabularyValidator validator,
            VocabularyLocks locks,
            Clock clock,
            VocabularyManagerProperties properties) {
        this.repository = repository;
        this.externalRepository = externalRepository;
        this.validator = validator;
        this.locks = locks;
        this.clock = clock;
        this.contentBase = properties.contentBaseUrl();
    }

    /**
     * Lists vocabularies (decisions D19 to D23).
     *
     * @throws VocabularyManagerException 503 when the store is not available
     */
    public Page<Vocabulary> list(VocabularyFilter filter, Comparator<Vocabulary> order, PageRequest pageRequest) {
        List<Vocabulary> all =
                repository.findAll().stream().filter(filter).sorted(order).toList();
        return Page.of(all, pageRequest);
    }

    /**
     * Gets one vocabulary.
     *
     * @throws VocabularyManagerException 404 when the vocabulary does not exist
     */
    public Vocabulary get(String name) {
        return repository.findByName(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    /**
     * Lists the versions of a vocabulary.
     *
     * @throws VocabularyManagerException 404 when the vocabulary does not exist
     */
    public VersionPage listVersions(String name, Comparator<VocabularyVersion> order, PageRequest pageRequest) {
        Vocabulary vocabulary = get(name);
        List<VocabularyVersion> all =
                repository.findVersions(name).stream().sorted(order).toList();
        return new VersionPage(Page.of(all, pageRequest), vocabulary.latestVersion());
    }

    /**
     * Gets one version.
     *
     * @throws VocabularyManagerException 404 when the vocabulary or the version does not exist
     */
    public VersionView getVersion(String name, int version) {
        Vocabulary vocabulary = get(name);
        VocabularyVersion found =
                repository.findVersion(name, version).orElseThrow(() -> Problems.versionNotFound(name, version));
        return new VersionView(found, vocabulary.latestVersion());
    }

    /**
     * Content of the latest version (decision D29).
     *
     * @throws VocabularyManagerException 404 when the vocabulary does not exist
     */
    public Content latestContent(String name) {
        Vocabulary vocabulary = get(name);
        return content(name, vocabulary.latestVersion());
    }

    /**
     * Content of one version (decision D29).
     *
     * @throws VocabularyManagerException 404 when the vocabulary or the version does not exist
     */
    public Content versionContent(String name, int version) {
        get(name);
        return content(name, version);
    }

    private Content content(String name, int version) {
        byte[] bytes = repository.findContent(name, version).orElseThrow(() -> Problems.versionNotFound(name, version));
        return new Content(name, version, bytes);
    }

    /**
     * Creates a vocabulary with version 1 ACTIVE (decisions D6, D7, D8, D37).
     *
     * @throws VocabularyManagerException 409 for a duplicate name, 400 for validation errors
     */
    public Created create(CreateVocabulary command) {
        return locks.withLock(command.name(), () -> {
            if (repository.findNameIgnoringCase(command.name()).isPresent()) {
                throw Problems.conflict("Vocabulary with name '" + command.name() + "' already exists.");
            }
            ValidationOutcome outcome = validate(command.content(), command.name(), 1);
            Instant now = now();
            VocabularyVersion first = new VocabularyVersion(
                    command.name(),
                    1,
                    command.description(),
                    command.changelog(),
                    VocabularyStatus.ACTIVE,
                    command.caller().id(),
                    now,
                    now);
            Vocabulary vocabulary = new Vocabulary(
                    command.name(), VocabularyStatus.ACTIVE, command.caller().id(), now, now, 1, command.description());
            repository.create(vocabulary, first, command.content());
            LOG.info(
                    "Vocabulary created: name={} warnings={}",
                    command.name(),
                    outcome.warnings().size());
            return new Created(first, outcome.warnings());
        });
    }

    /**
     * Creates the next version; older ACTIVE versions become DEPRECATED (decisions D4, D7, D43, D46).
     *
     * @throws VocabularyManagerException 404 unknown vocabulary, 409 identical version, 400 validation errors
     */
    public Created createVersion(String name, CreateVersion command) {
        return locks.withLock(name, () -> {
            Vocabulary vocabulary = get(name);
            int latest = vocabulary.latestVersion();
            VocabularyVersion latestVersion =
                    repository.findVersion(name, latest).orElseThrow(() -> Problems.versionNotFound(name, latest));
            byte[] latestContent = repository.findContent(name, latest).orElse(new byte[0]);
            if (Arrays.equals(latestContent, command.content())
                    && latestVersion.description().trim().equals(command.description())) {
                throw Problems.conflict("Version " + latest + " of vocabulary '" + name
                        + "' already has this content and description.");
            }
            int next = latest + 1;
            ValidationOutcome outcome = validate(command.content(), name, next);
            Instant now = now();
            VocabularyVersion created = new VocabularyVersion(
                    name,
                    next,
                    command.description(),
                    command.changelog(),
                    VocabularyStatus.ACTIVE,
                    command.caller().id(),
                    now,
                    now);
            repository.addVersion(created, command.content());
            LOG.info("Vocabulary version created: name={} version={}", name, next);
            return new Created(created, outcome.warnings());
        });
    }

    /**
     * Sets the vocabulary status; versions are not changed (decisions D3, D4).
     *
     * @throws VocabularyManagerException 404 unknown vocabulary, 409 when already in that status
     */
    public Vocabulary updateStatus(String name, VocabularyStatus status) {
        return locks.withLock(name, () -> {
            Vocabulary vocabulary = get(name);
            if (vocabulary.status() == status) {
                throw Problems.conflict("Vocabulary is already in status " + status.name());
            }
            repository.updateVocabularyStatus(name, status, now());
            LOG.info("Vocabulary status changed: name={} status={}", name, status);
            return get(name);
        });
    }

    /**
     * Sets the status of one version (decision D4).
     *
     * @throws VocabularyManagerException 404 unknown vocabulary or version, 409 when already in that status
     */
    public VersionView updateVersionStatus(String name, int version, VocabularyStatus status) {
        return locks.withLock(name, () -> {
            VersionView current = getVersion(name, version);
            if (current.version().status() == status) {
                throw Problems.conflict("Vocabulary version is already in status " + status.name());
            }
            repository.updateVersionStatus(name, version, status, now());
            LOG.info("Vocabulary version status changed: name={} version={} status={}", name, version, status);
            return getVersion(name, version);
        });
    }

    /** Resolves search references in request order; missing ones are left out (decision D38). */
    public List<SearchResult> search(List<SearchReference> references) {
        Set<SearchReference> unique = new LinkedHashSet<>(references);
        Map<String, Optional<Vocabulary>> vocabularies = new HashMap<>();
        List<SearchResult> results = new ArrayList<>();
        for (SearchReference reference : unique) {
            Optional<Vocabulary> vocabulary = vocabularies.computeIfAbsent(reference.name(), repository::findByName);
            if (vocabulary.isEmpty()) {
                continue;
            }
            if (reference.version() == null) {
                Vocabulary found = vocabulary.get();
                results.add(new SearchResult(found.name(), found.latestVersion(), found.status(), true));
            } else {
                repository
                        .findVersion(reference.name(), reference.version())
                        .ifPresent(v -> results.add(new SearchResult(v.name(), v.version(), v.status(), false)));
            }
        }
        return results;
    }

    private ValidationOutcome validate(byte[] content, String name, int version) {
        String base = contentBase + "/content/" + name + "/versions/" + version;
        ValidationOutcome outcome = validator.validate(content, base, name, new StoreReferenceLookup());
        if (outcome.hasErrors()) {
            LOG.info(
                    "Vocabulary content rejected: name={} errors={}",
                    name,
                    outcome.errors().size());
            throw new VocabularyManagerException(
                    RequestErrorCode.BAD_REQUEST, Problems.INPUT_INCORRECT, outcome.errors());
        }
        return outcome;
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    /** Reads stored vocabularies for the reference check; external vocabularies are read once per validation. */
    private final class StoreReferenceLookup implements ReferenceLookup {
        private List<ExternalVocabulary> externals;

        @Override
        public Optional<InternalVocabularyState> internalVocabulary(String name) {
            return repository
                    .findByName(name)
                    .map(vocabulary -> new InternalVocabularyState(
                            vocabulary.name(),
                            vocabulary.status(),
                            repository.findVersions(name).stream()
                                    .collect(Collectors.toMap(VocabularyVersion::version, VocabularyVersion::status))));
        }

        @Override
        public boolean isRegisteredExternal(String namespace) {
            if (externals == null) {
                externals = externalRepository.findAll();
            }
            return externals.stream().anyMatch(external -> external.matches(namespace));
        }
    }
}
