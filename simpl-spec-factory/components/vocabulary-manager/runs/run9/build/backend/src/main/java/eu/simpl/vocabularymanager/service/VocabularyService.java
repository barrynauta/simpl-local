package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ConflictException;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.domain.SearchReference;
import eu.simpl.vocabularymanager.domain.SearchResult;
import eu.simpl.vocabularymanager.domain.ServiceBusyException;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VersionNotFoundException;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyFilter;
import eu.simpl.vocabularymanager.domain.VocabularyNotFoundException;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.store.NewVersion;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/** Vocabulary lifecycle: create, version, status changes, reads and search (D3 to D8, D43, D46, D82). */
@Service
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);
    private static final int MAX_RETRIES = 5;

    private final VocabularyRepository repository;
    private final ContentValidator validator;
    private final UploadSlots uploadSlots;
    private final Clock clock;
    private final long writeBudgetMillis;

    /**
     * @param repository storage
     * @param validator content validation
     * @param uploadSlots upload concurrency limit (D92)
     * @param clock time source
     * @param properties component configuration
     */
    public VocabularyService(
            VocabularyRepository repository,
            ContentValidator validator,
            UploadSlots uploadSlots,
            Clock clock,
            VocabularyManagerProperties properties) {
        this.repository = repository;
        this.validator = validator;
        this.uploadSlots = uploadSlots;
        this.clock = clock;
        this.writeBudgetMillis = properties.store().writeBudget().toMillis();
    }

    /**
     * @param name exact name
     * @return the vocabulary
     * @throws VocabularyNotFoundException when unknown
     */
    public Vocabulary get(String name) {
        return repository.find(name).orElseThrow(() -> new VocabularyNotFoundException(name));
    }

    /**
     * @param filter filters and sort
     * @param page page selection
     * @return one page of vocabularies
     */
    public Page<Vocabulary> list(VocabularyFilter filter, PageRequest page) {
        return repository.list(filter, page);
    }

    /**
     * @param name exact name
     * @param sort sort field
     * @param descending direction
     * @param page page selection
     * @return one page of versions
     * @throws VocabularyNotFoundException when unknown
     */
    public Page<VocabularyVersion> versions(
            String name, VocabularyFilter.SortField sort, boolean descending, PageRequest page) {
        Vocabulary vocabulary = get(name);
        return repository.listVersions(name, vocabulary.latestVersion(), sort, descending, page);
    }

    /**
     * @param name exact name
     * @param version version number
     * @return the version
     * @throws VocabularyNotFoundException when the vocabulary is unknown
     * @throws VersionNotFoundException when the version is unknown
     */
    public VocabularyVersion version(String name, int version) {
        return repository.findVersion(name, version).orElseThrow(() -> notFound(name, version));
    }

    /**
     * @param name exact name
     * @param version version number, or null for the latest version
     * @return the stored bytes
     * @throws VocabularyNotFoundException when the vocabulary is unknown
     * @throws VersionNotFoundException when the version is unknown
     */
    public byte[] content(String name, Integer version) {
        return repository.content(name, version).orElseThrow(() -> notFound(name, version));
    }

    private RuntimeException notFound(String name, Integer version) {
        if (version == null || repository.find(name).isEmpty()) {
            return new VocabularyNotFoundException(name);
        }
        return new VersionNotFoundException(name, version);
    }

    /**
     * Resolves search references; repeated references give one result at the first position (D38).
     *
     * @param references references in request order
     * @return results in request order, missing ones left out
     */
    public List<SearchResult> search(List<SearchReference> references) {
        Set<SearchReference> unique = new LinkedHashSet<>(references);
        return repository.search(new ArrayList<>(unique));
    }

    /**
     * Creates a vocabulary with version 1 (D6, D7, D8).
     *
     * @param command checked request fields
     * @return the stored version 1 and the warnings
     * @throws ConflictException when the name exists case-insensitively
     */
    public CreationResult create(CreateVocabularyCommand command) {
        try (UploadSlots.Slot ignored = uploadSlots.acquire()) {
            if (repository.existsIgnoreCase(command.name())) {
                throw duplicateName(command.name());
            }
            List<ValidationWarning> warnings = validator.validate(command.content(), command.name(), 1);
            Instant now = now();
            NewVersion first = new NewVersion(
                    command.name(),
                    1,
                    command.description(),
                    command.changelog(),
                    command.createdBy(),
                    now,
                    command.content(),
                    sha256(command.content()));
            if (!repository.insertVocabulary(first)) {
                throw duplicateName(command.name());
            }
            LOG.info("Vocabulary created");
            VocabularyVersion stored = new VocabularyVersion(
                    command.name(),
                    1,
                    command.description(),
                    command.changelog(),
                    VocabularyStatus.ACTIVE,
                    command.createdBy(),
                    now,
                    now,
                    1);
            return new CreationResult(stored, warnings);
        }
    }

    private static ConflictException duplicateName(String name) {
        return new ConflictException("Vocabulary with name '" + name + "' already exists.");
    }

    /**
     * Creates the next version; older ACTIVE versions become DEPRECATED in the same store transaction (D4, D43, D82).
     *
     * @param name exact vocabulary name
     * @param command checked request fields
     * @return the stored version and the warnings
     * @throws VocabularyNotFoundException when the vocabulary is unknown
     * @throws ConflictException when content and description equal the latest version (D46)
     * @throws ServiceBusyException when the write budget runs out
     */
    public CreationResult createVersion(String name, CreateVersionCommand command) {
        try (UploadSlots.Slot ignored = uploadSlots.acquire()) {
            Vocabulary vocabulary = get(name);
            String sha = sha256(command.content());
            if (sha.equals(vocabulary.latestContentSha256())
                    && command.description().equals(vocabulary.latestDescription())) {
                throw new ConflictException("Version " + vocabulary.latestVersion() + " of vocabulary '" + name
                        + "' already has this content and description.");
            }
            List<ValidationWarning> warnings =
                    validator.validate(command.content(), name, vocabulary.latestVersion() + 1);
            long deadline = System.currentTimeMillis() + writeBudgetMillis;
            int expectedLatest = vocabulary.latestVersion();
            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                Instant now = now();
                int number = expectedLatest + 1;
                NewVersion version = new NewVersion(
                        name,
                        number,
                        command.description(),
                        command.changelog(),
                        command.createdBy(),
                        now,
                        command.content(),
                        sha);
                if (repository.insertVersion(version, expectedLatest)) {
                    MDC.put("version", Integer.toString(number));
                    LOG.info("Vocabulary version created after {} retries", attempt);
                    VocabularyVersion stored = new VocabularyVersion(
                            name,
                            number,
                            command.description(),
                            command.changelog(),
                            VocabularyStatus.ACTIVE,
                            command.createdBy(),
                            now,
                            now,
                            number);
                    return new CreationResult(stored, warnings);
                }
                if (System.currentTimeMillis() > deadline) {
                    break;
                }
                backOff();
                expectedLatest = get(name).latestVersion();
            }
            LOG.warn("Vocabulary version not created: write condition kept failing");
            throw new ServiceBusyException(ServiceBusyException.STORE_DETAIL);
        }
    }

    /**
     * Changes the vocabulary status (D3, D4); versions are not touched.
     *
     * @param name exact name
     * @param next requested status
     * @return the vocabulary after the change
     * @throws VocabularyNotFoundException when unknown
     * @throws ConflictException when it already has this status
     * @throws ServiceBusyException when the write budget runs out
     */
    public Vocabulary changeStatus(String name, VocabularyStatus next) {
        long deadline = System.currentTimeMillis() + writeBudgetMillis;
        Vocabulary vocabulary = get(name);
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (vocabulary.status() == next) {
                throw new ConflictException("Vocabulary is already in status " + next.name());
            }
            if (repository.updateVocabularyStatus(name, vocabulary.status(), next, now())) {
                LOG.info("Vocabulary status changed to {}", next);
                return get(name);
            }
            if (System.currentTimeMillis() > deadline) {
                break;
            }
            backOff();
            vocabulary = get(name);
        }
        throw new ServiceBusyException(ServiceBusyException.STORE_DETAIL);
    }

    /**
     * Changes one version status (D4).
     *
     * @param name exact name
     * @param versionNumber version number
     * @param next requested status
     * @return the version after the change
     * @throws VocabularyNotFoundException when the vocabulary is unknown
     * @throws VersionNotFoundException when the version is unknown
     * @throws ConflictException when it already has this status
     * @throws ServiceBusyException when the write budget runs out
     */
    public VocabularyVersion changeVersionStatus(String name, int versionNumber, VocabularyStatus next) {
        long deadline = System.currentTimeMillis() + writeBudgetMillis;
        VocabularyVersion version = version(name, versionNumber);
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (version.status() == next) {
                throw new ConflictException("Vocabulary version is already in status " + next.name());
            }
            if (repository.updateVersionStatus(name, versionNumber, version.status(), next, now())) {
                MDC.put("version", Integer.toString(versionNumber));
                LOG.info("Vocabulary version status changed to {}", next);
                return version(name, versionNumber);
            }
            if (System.currentTimeMillis() > deadline) {
                break;
            }
            backOff();
            version = version(name, versionNumber);
        }
        throw new ServiceBusyException(ServiceBusyException.STORE_DETAIL);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private static void backOff() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10, 60));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceBusyException(ServiceBusyException.STORE_DETAIL, ex);
        }
    }

    /** Hex SHA-256 of the content (identical-version check, D46). */
    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
