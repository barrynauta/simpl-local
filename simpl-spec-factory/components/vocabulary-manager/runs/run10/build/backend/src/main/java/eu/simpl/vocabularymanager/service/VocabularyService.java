package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.ListQuery;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * The vocabulary lifecycle (D3 to D8, D43, D46, D82): create, new version, status changes and reads. Each write is a
 * conditional store update; when the condition fails because of a concurrent change the service reads again and
 * retries, at most {@link #MAX_RETRIES} times, then answers 503.
 */
public class VocabularyService {

    static final int MAX_RETRIES = 5;
    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyRepository repository;
    private final ContentValidation validation;
    private final Clock clock;

    public VocabularyService(VocabularyRepository repository, ContentValidation validation, Clock clock) {
        this.repository = repository;
        this.validation = validation;
        this.clock = clock;
    }

    /** A created version, with the warnings of its validation. */
    public record Created(VocabularyVersion version, List<ValidationWarning> warnings) {}

    /** Data of a create or new-version request after the field checks. */
    public record Upload(String description, String changelog, byte[] content, String createdBy) {}

    /**
     * Creates a vocabulary with version 1 (D6, D8, D51).
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 409 duplicate name, 400 validation report
     */
    public Created create(String name, Upload upload) {
        if (repository.existsIgnoreCase(name)) {
            throw duplicate(name);
        }
        List<ValidationWarning> warnings = validation.validate(upload.content(), name, 1);
        VocabularyRepository.NewVersion data = newVersion(name, 1, upload);
        if (!repository.insertVocabulary(data, UUID.randomUUID().toString())) {
            throw duplicate(name);
        }
        MDC.put("vocabularyName", name);
        MDC.put("version", "1");
        LOG.info("Vocabulary created: vocabularyName={} version=1", name);
        return new Created(toVersion(data), warnings);
    }

    /**
     * Creates the next version of a vocabulary and deprecates every older ACTIVE version (D4, D7, D46).
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 404, 409 identical version, 400 validation report,
     *     503 when concurrent changes keep winning
     */
    public Created createVersion(String name, Upload upload) {
        Vocabulary vocabulary = repository.find(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
        repository.findVersion(name, vocabulary.latestVersion()).ifPresent(latest -> {
            if (sha256(upload.content()).equals(latest.contentSha256())
                    && upload.description().equals(latest.description())) {
                throw Problems.conflict("Version " + latest.version() + " of vocabulary '" + name
                        + "' already has this content and description.");
            }
        });
        List<ValidationWarning> warnings = validation.validate(upload.content(), name, vocabulary.latestVersion() + 1);
        int latest = vocabulary.latestVersion();
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            VocabularyRepository.NewVersion data = newVersion(name, latest + 1, upload);
            if (repository.insertVersion(data, latest, UUID.randomUUID().toString())) {
                MDC.put("version", Integer.toString(data.version()));
                LOG.info("Vocabulary version created: vocabularyName={} version={}", name, data.version());
                return new Created(toVersion(data), warnings);
            }
            latest = repository
                    .find(name)
                    .orElseThrow(() -> Problems.vocabularyNotFound(name))
                    .latestVersion();
        }
        throw Problems.serviceUnavailable(Problems.STORE_UNAVAILABLE);
    }

    /**
     * Sets the vocabulary status (D3, D4).
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 404, 409 same status
     */
    public Vocabulary changeStatus(String name, VocabularyStatus target) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            Vocabulary vocabulary = repository.find(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
            if (vocabulary.status() == target) {
                throw Problems.conflict("Vocabulary is already in status " + target.name());
            }
            if (repository.updateVocabularyStatus(
                    name, vocabulary.status(), target, now(), UUID.randomUUID().toString())) {
                LOG.info("Vocabulary status changed: vocabularyName={} status={}", name, target);
                return repository.find(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
            }
        }
        throw Problems.serviceUnavailable(Problems.STORE_UNAVAILABLE);
    }

    /**
     * Sets one version's status (D4); the vocabulary status is not touched.
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 404, 409 same status
     */
    public VersionView changeVersionStatus(String name, int version, VocabularyStatus target) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            Vocabulary vocabulary = repository.find(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
            VocabularyVersion current =
                    repository.findVersion(name, version).orElseThrow(() -> Problems.versionNotFound(name, version));
            if (current.status() == target) {
                throw Problems.conflict("Vocabulary version is already in status " + target.name());
            }
            if (repository.updateVersionStatus(
                    name,
                    version,
                    current.status(),
                    target,
                    now(),
                    UUID.randomUUID().toString())) {
                LOG.info("Version status changed: vocabularyName={} version={} status={}", name, version, target);
                VocabularyVersion updated = repository
                        .findVersion(name, version)
                        .orElseThrow(() -> Problems.versionNotFound(name, version));
                return new VersionView(updated, vocabulary.latestVersion());
            }
        }
        throw Problems.serviceUnavailable(Problems.STORE_UNAVAILABLE);
    }

    /** A version with the latest version number of its vocabulary (for latestVersionHref, D5). */
    public record VersionView(VocabularyVersion version, int latestVersion) {}

    public Vocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    public VersionView getVersion(String name, int version) {
        Vocabulary vocabulary = get(name);
        VocabularyVersion found =
                repository.findVersion(name, version).orElseThrow(() -> Problems.versionNotFound(name, version));
        return new VersionView(found, vocabulary.latestVersion());
    }

    /** A page of a list with its total. */
    public record Page<T>(List<T> items, long total) {}

    public Page<Vocabulary> list(ListQuery query) {
        long total = repository.count(query);
        List<Vocabulary> items = query.offset() >= total ? List.of() : repository.list(query);
        return new Page<>(items, total);
    }

    /** Versions of a vocabulary; the total is the latest version number, as versions are 1 to n (D7). */
    public Page<VocabularyVersion> versions(String name, String sort, long offset, int limit) {
        Vocabulary vocabulary = get(name);
        long total = vocabulary.latestVersion();
        List<VocabularyVersion> items =
                offset >= total ? List.of() : repository.listVersions(name, sort, offset, limit);
        return new Page<>(items, total);
    }

    /** The content of the latest version (D5, D29). */
    public byte[] latestContent(String name) {
        Vocabulary vocabulary = get(name);
        return repository
                .content(name, vocabulary.latestVersion())
                .orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    public byte[] content(String name, int version) {
        Optional<byte[]> content = repository.content(name, version);
        if (content.isPresent()) {
            return content.get();
        }
        get(name);
        throw Problems.versionNotFound(name, version);
    }

    private VocabularyRepository.NewVersion newVersion(String name, int version, Upload upload) {
        return new VocabularyRepository.NewVersion(
                name,
                version,
                upload.description(),
                upload.changelog(),
                upload.createdBy(),
                now(),
                upload.content(),
                sha256(upload.content()));
    }

    private static VocabularyVersion toVersion(VocabularyRepository.NewVersion data) {
        return new VocabularyVersion(
                data.name(),
                data.version(),
                data.description(),
                data.changelog(),
                VocabularyStatus.ACTIVE,
                data.createdBy(),
                data.now(),
                data.now(),
                data.sha256());
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private static RuntimeException duplicate(String name) {
        return Problems.conflict("Vocabulary with name '" + name + "' already exists.");
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
