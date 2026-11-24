package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyNames;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.nio.charset.StandardCharsets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Use cases of internal vocabularies (BP02B, D3 to D8, D43, D46, D51, D82). Request shape checks are done before
 * these methods run; the methods run the not-found, conflict and validation checks in the decided order.
 */
@Service
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyRepository repository;
    private final ContentValidator validator;
    private final ConditionalWrite conditionalWrite;
    private final Clock clock;

    public VocabularyService(
            VocabularyRepository repository,
            ContentValidator validator,
            ConditionalWrite conditionalWrite,
            Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.conditionalWrite = conditionalWrite;
        this.clock = clock;
    }

    /**
     * The outcome of a creation.
     *
     * @param version the stored version
     * @param latestVersion the latest version number after the creation
     * @param warnings the validation warnings
     */
    public record Created(VocabularyVersion version, long latestVersion, List<ValidationWarning> warnings) {}

    /**
     * A page of a list.
     *
     * @param items the items
     * @param total the total count
     * @param <T> item type
     */
    public record Slice<T>(List<T> items, long total) {}

    /**
     * Lists vocabularies (SIMPL-18162, SIMPL-18164).
     *
     * @return the page and the total
     */
    public Slice<Vocabulary> list(VocabularyRepository.ListFilter filter, String orderBy, long offset, int limit) {
        long total = repository.count(filter);
        if (offset >= total) {
            return new Slice<>(List.of(), total);
        }
        return new Slice<>(repository.list(filter, orderBy, offset, limit), total);
    }

    /**
     * Gets a vocabulary.
     *
     * @throws ApiException 404 when the name is unknown
     */
    public Vocabulary get(String name) {
        return repository.findVocabulary(name).orElseThrow(() -> vocabularyNotFound(name));
    }

    /**
     * Lists versions of a vocabulary (SIMPL-18168).
     *
     * @throws ApiException 404 when the name is unknown
     */
    public Slice<VocabularyVersion> versions(String name, String orderBy, long offset, int limit) {
        Vocabulary vocabulary = get(name);
        if (offset >= vocabulary.latestVersion()) {
            return new Slice<>(List.of(), vocabulary.latestVersion());
        }
        return new Slice<>(repository.listVersions(name, orderBy, offset, limit), vocabulary.latestVersion());
    }

    /**
     * Gets one version together with the latest version number (SIMPL-18166).
     *
     * @throws ApiException 404 when the name or version is unknown
     */
    public Created version(String name, long version) {
        Vocabulary vocabulary = get(name);
        VocabularyVersion found = findVersion(vocabulary, version);
        return new Created(found, vocabulary.latestVersion(), List.of());
    }

    /**
     * Creates a vocabulary with version 1 (SIMPL-18170, SIMPL-18187 AC1).
     *
     * @throws ApiException 409 for a duplicate name, 400 for invalid content, 503 when the store is down
     */
    public Created create(String name, String description, String changelog, byte[] content, AuthenticatedUser user) {
        if (repository.nameTaken(VocabularyNames.key(name))) {
            throw duplicateName(name);
        }
        List<ValidationWarning> warnings = validator.validateInternal(content, name, 1);
        Instant now = now();
        String hash = sha256(content);
        Vocabulary vocabulary = new Vocabulary(name, VocabularyStatus.ACTIVE, 1, description, user.creator(), now, now);
        VocabularyVersion version = new VocabularyVersion(
                name, 1, VocabularyStatus.ACTIVE, description, changelog, user.creator(), now, now, hash);
        conditionalWrite.run(() -> {
            if (repository.insertVocabulary(vocabulary, version, content)) {
                return Boolean.TRUE;
            }
            if (repository.nameTaken(VocabularyNames.key(name))) {
                throw duplicateName(name);
            }
            return null;
        });
        LOG.info("Vocabulary created with version 1 by user {}", user.subject());
        return new Created(version, 1, warnings);
    }

    /**
     * Creates a new version (SIMPL-18173, SIMPL-18187 AC2, SIMPL-18190, D4, D43, D46).
     *
     * @throws ApiException 404 for an unknown name, 409 for an identical version, 400 for invalid content
     */
    public Created createVersion(
            String name, String description, String changelog, byte[] content, AuthenticatedUser user) {
        Vocabulary vocabulary = get(name);
        String hash = sha256(content);
        VocabularyVersion latest = findVersion(vocabulary, vocabulary.latestVersion());
        if (latest.contentSha256().equals(hash) && latest.description().equals(description)) {
            throw ApiException.conflict("Version " + latest.version() + " of vocabulary '" + name
                    + "' already has this content and description.");
        }
        List<ValidationWarning> warnings = validator.validateInternal(content, name, vocabulary.latestVersion() + 1);
        Created created = conditionalWrite.run(() -> {
            Vocabulary current = get(name);
            long number = current.latestVersion() + 1;
            Instant now = now();
            VocabularyVersion version = new VocabularyVersion(
                    name, number, VocabularyStatus.ACTIVE, description, changelog, user.creator(), now, now, hash);
            if (repository.insertVersion(name, current.latestVersion(), version, content)) {
                return new Created(version, number, warnings);
            }
            return null;
        });
        LOG.info("Version {} created by user {}", created.version().version(), user.subject());
        return created;
    }

    /**
     * Changes the vocabulary status (SIMPL-18175, D3, D4).
     *
     * @throws ApiException 404 for an unknown name, 409 for the current status
     */
    public Vocabulary changeStatus(String name, VocabularyStatus target, AuthenticatedUser user) {
        conditionalWrite.run(() -> {
            Vocabulary current = get(name);
            if (current.status() == target) {
                throw ApiException.conflict("Vocabulary is already in status " + target.name());
            }
            return repository.updateVocabularyStatus(name, current.status(), target, now()) ? Boolean.TRUE : null;
        });
        LOG.info("Vocabulary status changed to {} by user {}", target, user.subject());
        return get(name);
    }

    /**
     * Changes a version status (D4, D160).
     *
     * @throws ApiException 404 for an unknown name or version, 409 for the current status
     */
    public Created changeVersionStatus(
            String name, long versionNumber, VocabularyStatus target, AuthenticatedUser user) {
        conditionalWrite.run(() -> {
            Vocabulary vocabulary = get(name);
            VocabularyVersion current = findVersion(vocabulary, versionNumber);
            if (current.status() == target) {
                throw ApiException.conflict("Vocabulary version is already in status " + target.name());
            }
            return repository.updateVersionStatus(name, versionNumber, current.status(), target, now())
                    ? Boolean.TRUE
                    : null;
        });
        LOG.info("Version status changed to {} by user {}", target, user.subject());
        return version(name, versionNumber);
    }

    /**
     * Reads the content of the latest or a given version (D29).
     *
     * @param name the vocabulary name
     * @param version the version, or null for the latest
     * @return the version number and its bytes
     * @throws ApiException 404 when the name or version is unknown
     */
    public Content content(String name, Long version) {
        Vocabulary vocabulary = get(name);
        long number = version == null ? vocabulary.latestVersion() : version;
        if (number < 1 || number > vocabulary.latestVersion()) {
            throw versionNotFound(name, number);
        }
        byte[] bytes = repository.content(name, number).orElseThrow(() -> versionNotFound(name, number));
        return new Content(number, bytes);
    }

    /**
     * Content of a version.
     *
     * @param version version number
     * @param bytes the stored bytes
     */
    public record Content(long version, byte[] bytes) {}

    /**
     * A search reference (D38).
     *
     * @param name vocabulary name
     * @param version version, or null for the latest
     */
    public record Reference(String name, Long version) {}

    /**
     * A search result.
     *
     * @param name vocabulary name
     * @param version version number
     * @param status vocabulary status for latest results, else version status
     * @param latest true for a reference without version
     */
    public record SearchResult(String name, long version, VocabularyStatus status, boolean latest) {}

    /**
     * Resolves references in request order, without missing ones and repeated ones (D38). One store query.
     *
     * @param references the references, already validated
     * @return the results
     */
    public List<SearchResult> search(List<Reference> references) {
        Set<Reference> unique = new LinkedHashSet<>(references);
        Set<String> names = new LinkedHashSet<>();
        Set<Long> versions = new LinkedHashSet<>();
        for (Reference reference : unique) {
            names.add(reference.name());
            if (reference.version() != null) {
                versions.add(reference.version());
            }
        }
        VocabularyRepository.Lookup lookup = repository.lookup(names, versions, List.of());
        List<SearchResult> results = new ArrayList<>();
        for (Reference reference : unique) {
            resolve(reference, lookup.rows()).ifPresent(results::add);
        }
        return results;
    }

    private static Optional<SearchResult> resolve(Reference reference, List<VocabularyRepository.VersionRow> rows) {
        for (VocabularyRepository.VersionRow row : rows) {
            if (!row.name().equals(reference.name())) {
                continue;
            }
            if (reference.version() == null) {
                return Optional.of(new SearchResult(row.name(), row.latestVersion(), row.vocabularyStatus(), true));
            }
            if (row.version() == reference.version()) {
                return Optional.of(new SearchResult(row.name(), row.version(), row.versionStatus(), false));
            }
        }
        return Optional.empty();
    }

    private VocabularyVersion findVersion(Vocabulary vocabulary, long version) {
        return repository
                .findVersion(vocabulary.name(), version)
                .orElseThrow(() -> versionNotFound(vocabulary.name(), version));
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    static ApiException vocabularyNotFound(String name) {
        return ApiException.notFound("Vocabulary with name '" + name + "' was not found.", "path", "vocabularyName");
    }

    static ApiException versionNotFound(String name, long version) {
        return ApiException.notFound(
                "Version " + version + " of vocabulary '" + name + "' was not found.", "path", "version");
    }

    private static ApiException duplicateName(String name) {
        return ApiException.conflict("Vocabulary with name '" + name + "' already exists.");
    }

    /** SHA-256 of the content bytes as hex (D194). */
    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** UTF-8 text of bytes. */
    static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
