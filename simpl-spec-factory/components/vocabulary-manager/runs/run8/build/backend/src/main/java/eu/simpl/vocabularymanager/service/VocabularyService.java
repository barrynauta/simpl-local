package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.store.VocabularyFilter;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.store.VocabularyRepository.ChangeRecord;
import eu.simpl.vocabularymanager.store.VocabularyRepository.VersionWrite;
import eu.simpl.vocabularymanager.validation.ContentIssue;
import eu.simpl.vocabularymanager.validation.ReferenceLookup;
import eu.simpl.vocabularymanager.validation.ValidationOutcome;
import eu.simpl.vocabularymanager.validation.ValidationWarning;
import eu.simpl.vocabularymanager.validation.VocabularyContentValidator;
import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.problem.Problems;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Vocabulary and version lifecycle (D3 to D8, D43, D46, D82): create, new version, status changes, reads, search.
 * Request shape is checked by the caller; this class runs the 404 and 409 checks and validation, in the order of D51.
 */
@Service
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyRepository repository;
    private final VocabularyContentValidator validator;
    private final ConditionalWrite conditionalWrite;
    private final Clock clock;
    private final String contentBase;

    /**
     * @param repository store access
     * @param validator content validation
     * @param conditionalWrite retry of conditional writes
     * @param clock clock
     * @param properties component settings
     */
    public VocabularyService(
            VocabularyRepository repository,
            VocabularyContentValidator validator,
            ConditionalWrite conditionalWrite,
            Clock clock,
            VocabularyManagerProperties properties) {
        this.repository = repository;
        this.validator = validator;
        this.conditionalWrite = conditionalWrite;
        this.clock = clock;
        this.contentBase = properties.contentBase();
    }

    /**
     * @return contentBase of E2
     */
    public String contentBase() {
        return contentBase;
    }

    /**
     * @param filter the filters
     * @param sort the ORDER BY clause
     * @param offset rows to skip
     * @param limit page size
     * @return the page
     */
    public Page<Vocabulary> list(VocabularyFilter filter, String sort, long offset, int limit) {
        return repository.listVocabularies(filter, sort, offset, limit);
    }

    /**
     * @param name exact name
     * @return the vocabulary
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404
     */
    public Vocabulary get(String name) {
        return repository.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    /**
     * @param name exact name
     * @param version version number
     * @return the version
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404 for an unknown vocabulary or version
     */
    public VocabularyVersion getVersion(String name, long version) {
        Optional<VocabularyVersion> found = repository.findVersion(name, version);
        if (found.isPresent()) {
            return found.get();
        }
        get(name);
        throw Problems.versionNotFound(name, version);
    }

    /**
     * @param name exact name
     * @param sort the ORDER BY clause
     * @param offset rows to skip
     * @param limit page size
     * @return the versions and the total
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404
     */
    public Page<VocabularyVersion> versions(String name, String sort, long offset, int limit) {
        Vocabulary vocabulary = get(name);
        long total = vocabulary.latestVersion();
        if (offset >= total) {
            return new Page<>(List.of(), total);
        }
        return new Page<>(repository.listVersions(name, sort, offset, limit), total);
    }

    /**
     * @param name exact name
     * @param version version number, or null for the latest version (D5)
     * @return the stored bytes (D29)
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 404
     */
    public byte[] content(String name, Long version) {
        Optional<byte[]> content = repository.content(name, version);
        if (content.isPresent()) {
            return content.get();
        }
        get(name);
        throw Problems.versionNotFound(name, version == null ? 0 : version);
    }

    /**
     * POST /vocabularies: 409 on a taken name, then validation, then version 1 ACTIVE (D6, D15, D51).
     *
     * @param command the checked request
     * @param user the caller
     * @return the created version 1 and the warnings
     */
    public Created create(NewVocabulary command, AuthenticatedUser user) {
        return conditionalWrite.run(() -> {
            if (repository.nameTaken(command.name())) {
                throw Problems.conflict("Vocabulary with name '" + command.name() + "' already exists.");
            }
            ValidationOutcome outcome = validate(command.content(), command.name(), 1);
            VersionWrite write = write(command.name(), command.description(), command.changelog(), command, user);
            if (!repository.insertVocabulary(write)) {
                return Optional.empty();
            }
            LOG.info("Vocabulary created with version 1");
            return Optional.of(new Created(created(write, 1), outcome.warnings()));
        });
    }

    /**
     * POST .../versions: 404, 409 identical version (D46), validation, then the next version ACTIVE and every older
     * ACTIVE version DEPRECATED in one transaction (D4, D7).
     *
     * @param command the checked request
     * @param user the caller
     * @return the created version and the warnings
     */
    public Created createVersion(NewVersion command, AuthenticatedUser user) {
        return conditionalWrite.run(() -> {
            Vocabulary vocabulary = get(command.name());
            String hash = sha256(command.content());
            if (hash.equals(vocabulary.latestContentSha256())
                    && command.description().equals(vocabulary.latestDescription())) {
                throw Problems.conflict("Version " + vocabulary.latestVersion() + " of vocabulary '" + command.name()
                        + "' already has this content and description.");
            }
            long next = vocabulary.latestVersion() + 1;
            ValidationOutcome outcome = validate(command.content(), command.name(), next);
            VersionWrite write = write(command.name(), command.description(), command.changelog(), command, user);
            if (!repository.insertVersion(write, vocabulary.latestVersion())) {
                return Optional.empty();
            }
            LOG.info("Vocabulary version {} created", next);
            return Optional.of(new Created(created(write, next), outcome.warnings()));
        });
    }

    /**
     * PATCH /vocabularies/{name} (D3, D4): versions are not touched.
     *
     * @param name exact name
     * @param target the requested status
     * @param user the caller
     * @return the vocabulary after the change
     */
    public Vocabulary changeStatus(String name, VocabularyStatus target, AuthenticatedUser user) {
        return conditionalWrite.run(() -> {
            Vocabulary vocabulary = get(name);
            if (vocabulary.status() == target) {
                throw Problems.conflict("Vocabulary is already in status " + target.name());
            }
            ChangeRecord change = change(user);
            if (!repository.updateVocabularyStatus(name, vocabulary.status(), target, change)) {
                return Optional.empty();
            }
            LOG.info("Vocabulary status changed to {}", target);
            return Optional.of(new Vocabulary(
                    vocabulary.name(),
                    target,
                    vocabulary.latestVersion(),
                    vocabulary.latestDescription(),
                    vocabulary.latestContentSha256(),
                    vocabulary.createdBy(),
                    vocabulary.creationDate(),
                    change.time()));
        });
    }

    /**
     * PATCH .../versions/{version} (D4): only that version's status.
     *
     * @param name exact name
     * @param version version number
     * @param target the requested status
     * @param user the caller
     * @return the version after the change
     */
    public VocabularyVersion changeVersionStatus(
            String name, long version, VocabularyStatus target, AuthenticatedUser user) {
        return conditionalWrite.run(() -> {
            VocabularyVersion current = getVersion(name, version);
            if (current.status() == target) {
                throw Problems.conflict("Vocabulary version is already in status " + target.name());
            }
            ChangeRecord change = change(user);
            if (!repository.updateVersionStatus(name, version, current.status(), target, change)) {
                return Optional.empty();
            }
            LOG.info("Vocabulary version {} status changed to {}", version, target);
            return Optional.of(new VocabularyVersion(
                    current.name(),
                    current.version(),
                    target,
                    current.description(),
                    current.changelog(),
                    current.createdBy(),
                    current.creationDate(),
                    change.time(),
                    current.latestVersion()));
        });
    }

    /**
     * POST /vocabularies/searches (D38): one store query; missing references left out; results in request order;
     * repeated references once.
     *
     * @param references the references, already checked
     * @return the results
     */
    public List<SearchResult> search(List<SearchReference> references) {
        Set<String> names = new LinkedHashSet<>();
        Set<Long> versions = new LinkedHashSet<>();
        for (SearchReference reference : references) {
            names.add(reference.name());
            if (reference.version() != null) {
                versions.add(reference.version());
            }
        }
        ReferenceLookup.State state = repository.lookup(names, versions, Set.of());
        Set<SearchReference> seen = new LinkedHashSet<>();
        List<SearchResult> results = new ArrayList<>();
        for (SearchReference reference : references) {
            if (!seen.add(reference)) {
                continue;
            }
            ReferenceLookup.InternalVocabularyState vocabulary =
                    state.internal().get(reference.name());
            if (vocabulary == null) {
                continue;
            }
            if (reference.version() == null) {
                results.add(new SearchResult(reference.name(), vocabulary.latestVersion(), vocabulary.status(), true));
            } else {
                VocabularyStatus status = vocabulary.versionStatuses().get(reference.version());
                if (status != null && reference.version() <= vocabulary.latestVersion()) {
                    results.add(new SearchResult(reference.name(), reference.version(), status, false));
                }
            }
        }
        return results;
    }

    private ValidationOutcome validate(byte[] content, String name, long version) {
        String base = contentBase + "/content/" + name + "/versions/" + version;
        ValidationOutcome outcome = validator.validate(content, name, base, contentBase, repository);
        if (!outcome.valid()) {
            LOG.info("Vocabulary content rejected with {} validation errors", outcome.errors().size());
            List<ProblemIssue> issues = new ArrayList<>();
            for (ContentIssue issue : outcome.errors()) {
                issues.add(ProblemIssue.invalidInput("body", "vocabularyFile", issue.detail(), issue.location()));
            }
            throw Problems.badRequest(issues);
        }
        return outcome;
    }

    private VersionWrite write(
            String name, String description, String changelog, ContentCommand command, AuthenticatedUser user) {
        return new VersionWrite(
                name,
                description,
                changelog,
                user.creator(),
                user.subject(),
                command.content(),
                sha256(command.content()),
                now(),
                UUID.randomUUID().toString());
    }

    private static VocabularyVersion created(VersionWrite write, long number) {
        return new VocabularyVersion(
                write.name(),
                number,
                VocabularyStatus.ACTIVE,
                write.description(),
                write.changelog(),
                write.createdBy(),
                write.time(),
                write.time(),
                number);
    }

    private ChangeRecord change(AuthenticatedUser user) {
        return new ChangeRecord(UUID.randomUUID().toString(), user.subject(), now());
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * @param content bytes
     * @return lower-case hex SHA-256
     */
    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    /** A request that carries vocabulary content. */
    public interface ContentCommand {
        /**
         * @return the uploaded bytes
         */
        byte[] content();
    }

    /**
     * @param name name
     * @param description trimmed description
     * @param changelog trimmed changelog
     * @param content uploaded bytes
     */
    public record NewVocabulary(String name, String description, String changelog, byte[] content)
            implements ContentCommand {}

    /**
     * @param name exact vocabulary name
     * @param description trimmed description
     * @param changelog trimmed changelog
     * @param content uploaded bytes
     */
    public record NewVersion(String name, String description, String changelog, byte[] content)
            implements ContentCommand {}

    /**
     * @param version the created version
     * @param warnings the validation warnings
     */
    public record Created(VocabularyVersion version, List<ValidationWarning> warnings) {}

    /**
     * @param name exact name
     * @param version version number, or null for the latest version
     */
    public record SearchReference(String name, Long version) {}

    /**
     * @param name name
     * @param version version number
     * @param status vocabulary status (latest) or version status
     * @param latest true when resolved as latest
     */
    public record SearchResult(String name, long version, VocabularyStatus status, boolean latest) {}
}
