package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.content.ContentValidator;
import eu.simpl.vocabularymanager.content.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.store.ListQuery;
import eu.simpl.vocabularymanager.store.NewVersion;
import eu.simpl.vocabularymanager.store.SparqlClient;
import eu.simpl.vocabularymanager.store.StoreSchema;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.store.VocabularyRepository.WriteKind;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The vocabulary lifecycle (D3 to D8, D43, D46, D82): creation, new versions with automatic
 * deprecation, status changes, and reads. Writes are conditional store updates, retried at most 5
 * times within the write budget (D71, D82); no lock state in the service.
 */
@Service
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyRepository repository;
    private final ContentValidator validator;
    private final AppProperties properties;
    private final Clock clock;

    public VocabularyService(
            VocabularyRepository repository, ContentValidator validator, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.properties = properties;
        this.clock = clock;
    }

    /** A created version and its validation warnings. */
    public record Created(VocabularyVersion version, List<ValidationWarning> warnings) {}

    /** What the create and new version requests carry, after the field checks. */
    public record Submission(String name, String description, String changelog, byte[] content, String creator) {}

    /**
     * Creates a vocabulary with version 1 ACTIVE (SIMPL-18170, D6, D7). Order after the field
     * checks: 409 duplicate name, then content validation (D51).
     *
     * @throws ApiException 409, 400 validation report, 503
     */
    public Created create(Submission submission) {
        String name = submission.name();
        if (repository.nameTaken(name)) {
            throw duplicate(name);
        }
        List<ValidationWarning> warnings = validator.validate(submission.content(), contentUrl(name, 1), name);
        String writeId = UUID.randomUUID().toString();
        Instant now = StoreSchema.now();
        repository.create(newVersion(submission, now), writeId);
        if (!repository.writeApplied(StoreSchema.vocabularyIri(name), WriteKind.CREATION, writeId)) {
            throw duplicate(name);
        }
        LOG.info("Vocabulary created");
        return new Created(versionOrFail(name, 1), warnings);
    }

    /**
     * Creates the next version (SIMPL-18173, D4, D7, D46, D82). Order after the field checks: 404,
     * 409 identical version, then content validation, then the conditional write.
     *
     * @throws ApiException 404, 409, 400 validation report, 503
     */
    public Created addVersion(Submission submission) {
        String name = submission.name();
        Vocabulary vocabulary = repository.find(name).orElseThrow(() -> vocabularyNotFound(name));
        int latest = vocabulary.latestVersion();
        rejectIdenticalVersion(submission, latest);
        List<ValidationWarning> warnings = validator.validate(submission.content(), contentUrl(name, latest + 1), name);
        long deadline = clock.millis() + properties.limits().writeBudget().toMillis();
        String writeId = UUID.randomUUID().toString();
        for (int attempt = 0; attempt < properties.store().conditionalRetries(); attempt++) {
            Instant now = StoreSchema.now();
            repository.addVersion(newVersion(submission, now), latest, writeId);
            if (repository.writeApplied(StoreSchema.versionIri(name, latest + 1), WriteKind.CREATION, writeId)) {
                LOG.info("Vocabulary version created");
                return new Created(versionOrFail(name, latest + 1), warnings);
            }
            if (clock.millis() > deadline) {
                break;
            }
            pause(attempt);
            latest = repository
                    .find(name)
                    .orElseThrow(() -> vocabularyNotFound(name))
                    .latestVersion();
        }
        LOG.warn("Version creation gave up after conditional write retries");
        throw ApiException.unavailable(SparqlClient.STORE_UNAVAILABLE);
    }

    /**
     * Sets the vocabulary status (SIMPL-18175, D3, D4).
     *
     * @throws ApiException 404, 409 same status, 503
     */
    public Vocabulary setStatus(String name, VocabularyStatus target) {
        for (int attempt = 0; attempt < properties.store().conditionalRetries(); attempt++) {
            Vocabulary current = repository.find(name).orElseThrow(() -> vocabularyNotFound(name));
            if (current.status() == target) {
                throw ApiException.conflict("Vocabulary is already in status " + target.name());
            }
            String writeId = UUID.randomUUID().toString();
            repository.setStatus(name, current.status(), target, StoreSchema.now(), writeId);
            if (repository.writeApplied(StoreSchema.vocabularyIri(name), WriteKind.STATUS, writeId)) {
                LOG.info("Vocabulary status changed to {}", target);
                return repository.find(name).orElseThrow(() -> vocabularyNotFound(name));
            }
            pause(attempt);
        }
        throw ApiException.unavailable(SparqlClient.STORE_UNAVAILABLE);
    }

    /**
     * Sets a version status (D4, D41).
     *
     * @throws ApiException 404, 409 same status, 503
     */
    public VocabularyVersion setVersionStatus(String name, int version, VocabularyStatus target) {
        for (int attempt = 0; attempt < properties.store().conditionalRetries(); attempt++) {
            VocabularyVersion current = version(name, version);
            if (current.status() == target) {
                throw ApiException.conflict("Vocabulary version is already in status " + target.name());
            }
            String writeId = UUID.randomUUID().toString();
            repository.setVersionStatus(name, version, current.status(), target, StoreSchema.now(), writeId);
            if (repository.writeApplied(StoreSchema.versionIri(name, version), WriteKind.STATUS, writeId)) {
                LOG.info("Vocabulary version status changed to {}", target);
                return version(name, version);
            }
            pause(attempt);
        }
        throw ApiException.unavailable(SparqlClient.STORE_UNAVAILABLE);
    }

    /**
     * The vocabulary.
     *
     * @throws ApiException 404
     */
    public Vocabulary vocabulary(String name) {
        return repository.find(name).orElseThrow(() -> vocabularyNotFound(name));
    }

    /** The state of the named vocabularies and all their versions, in one store query (D38). */
    public eu.simpl.vocabularymanager.store.ReferenceState searchState(List<String> names) {
        return repository.references(names, List.of());
    }

    public Page<Vocabulary> list(ListQuery query) {
        return repository.list(query);
    }

    /**
     * The versions of a vocabulary.
     *
     * @throws ApiException 404 when the vocabulary does not exist
     */
    public Page<VocabularyVersion> versions(String name, String sortField, boolean ascending, long page, int size) {
        Page<VocabularyVersion> result = repository.listVersions(name, sortField, ascending, page, size);
        if (result.total() == 0) {
            throw vocabularyNotFound(name);
        }
        return result;
    }

    /**
     * One version.
     *
     * @throws ApiException 404 for an unknown vocabulary or version
     */
    public VocabularyVersion version(String name, int version) {
        return repository.findVersion(name, version).orElseThrow(() -> {
            if (repository.find(name).isEmpty()) {
                return vocabularyNotFound(name);
            }
            return versionNotFound(name, version);
        });
    }

    /**
     * Content of a version, or of the latest version when version is null (D5, D29).
     *
     * @throws ApiException 404
     */
    public byte[] content(String name, Integer version) {
        int number = version == null ? vocabulary(name).latestVersion() : version;
        return repository.content(name, number).orElseThrow(() -> {
            if (repository.find(name).isEmpty()) {
                return vocabularyNotFound(name);
            }
            return versionNotFound(name, number);
        });
    }

    public String contentUrl(String name, int version) {
        return properties.contentBaseTrimmed() + "/content/" + name + "/versions/" + version;
    }

    public static ApiException vocabularyNotFound(String name) {
        String detail = "Vocabulary with name '" + name + "' was not found.";
        return new ApiException(ProblemType.NOT_FOUND, detail, List.of(Issue.plain("path", "vocabularyName", detail)));
    }

    public static ApiException versionNotFound(String name, int version) {
        String detail = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return new ApiException(ProblemType.NOT_FOUND, detail, List.of(Issue.plain("path", "version", detail)));
    }

    private void rejectIdenticalVersion(Submission submission, int latest) {
        VocabularyVersion latestVersion = versionOrFail(submission.name(), latest);
        if (!latestVersion.description().equals(submission.description())) {
            return;
        }
        byte[] stored = repository.content(submission.name(), latest).orElse(new byte[0]);
        if (Arrays.equals(stored, submission.content())) {
            throw ApiException.conflict("Version " + latest + " of vocabulary '" + submission.name()
                    + "' already has this content and description.");
        }
    }

    private VocabularyVersion versionOrFail(String name, int version) {
        return repository
                .findVersion(name, version)
                .orElseThrow(() -> new ApiException(ProblemType.INTERNAL_SERVER_ERROR, "Internal Server Error"));
    }

    private static NewVersion newVersion(Submission submission, Instant now) {
        return new NewVersion(
                submission.name(),
                submission.description(),
                submission.changelog(),
                submission.creator(),
                submission.content(),
                now);
    }

    private static ApiException duplicate(String name) {
        return ApiException.conflict("Vocabulary with name '" + name + "' already exists.");
    }

    private static void pause(int attempt) {
        try {
            Thread.sleep(20L + (long) (Math.random() * 50 * (attempt + 1)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ApiException.unavailable(SparqlClient.STORE_UNAVAILABLE);
        }
    }
}
