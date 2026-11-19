package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.problem.Problems;
import eu.simpl.vocabularymanager.store.SortOrder;
import eu.simpl.vocabularymanager.store.VocabularyQuery;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Use cases of the vocabulary aggregate: the vocabulary is the root, its versions change only through it
 * (DDD-11). Implements the lifecycle of decisions D3 to D8, D43 and D46. Request shape checks (400, 413, 415)
 * happen before these methods are called (decision D51).
 */
@Service
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyStore store;
    private final ContentValidator validator;
    private final VocabularyLocks locks;
    private final Clock clock;

    /**
     * A created version with its warnings.
     *
     * @param version the stored version
     * @param warnings validation warnings (decision D10)
     */
    public record Created(VocabularyVersion version, List<ValidationWarning> warnings) {}

    /**
     * A vocabulary search reference (decision D38).
     *
     * @param name the vocabulary name
     * @param version the version number, or null for the latest version
     */
    public record SearchReference(String name, Long version) {}

    /**
     * A search result: a vocabulary (latest) or a version.
     *
     * @param name the vocabulary name
     * @param version the version number
     * @param status the vocabulary status (latest) or the version status
     * @param latest true when the result came from the latest version
     */
    public record SearchResult(String name, int version, VocabularyStatus status, boolean latest) {}

    /**
     * Creates the service.
     *
     * @param store the store
     * @param validator the content validator
     * @param locks the per-vocabulary locks
     * @param clock the clock
     */
    public VocabularyService(VocabularyStore store, ContentValidator validator, VocabularyLocks locks, Clock clock) {
        this.store = store;
        this.validator = validator;
        this.locks = locks;
        this.clock = clock;
    }

    /**
     * Creates a vocabulary with version 1, ACTIVE (decisions D3, D6, D7).
     *
     * @param name the vocabulary name, already checked against D37
     * @param description the trimmed description
     * @param changelog the trimmed changelog
     * @param content the content bytes
     * @param createdBy the creator (decision D27)
     * @return the created version
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 409 for a taken name, 400 for invalid content
     */
    public Created createVocabulary(
            String name, String description, String changelog, byte[] content, String createdBy) {
        return locks.withVocabulary(name, () -> {
            if (store.vocabularyNameTaken(name)) {
                throw Problems.conflict("Vocabulary with name '" + name + "' already exists.");
            }
            ContentValidator.Outcome outcome = validator.validateVersion(content, name, 1);
            if (!outcome.passed()) {
                throw Problems.badRequest(outcome.errors());
            }
            Instant now = now();
            VocabularyVersion first = new VocabularyVersion(
                    name, 1, VocabularyStatus.ACTIVE, description, changelog, createdBy, now, now, sha256(content));
            Vocabulary vocabulary = new Vocabulary(name, VocabularyStatus.ACTIVE, createdBy, now, now, 1, description);
            store.createVocabulary(vocabulary, first, content);
            LOG.info("Vocabulary {} created with version 1 by {}", name, createdBy);
            return new Created(first, outcome.warnings());
        });
    }

    /**
     * Creates the next version, ACTIVE, and deprecates every older ACTIVE version (decisions D4, D7, D43, D46).
     *
     * @param name the vocabulary name
     * @param description the trimmed description
     * @param changelog the trimmed changelog
     * @param content the content bytes
     * @param createdBy the creator
     * @return the created version
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404, 409 (identical version) or 400 (content)
     */
    public Created createVersion(String name, String description, String changelog, byte[] content, String createdBy) {
        return locks.withVocabulary(name, () -> {
            Vocabulary vocabulary = store.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
            int latestNumber = vocabulary.latestVersion();
            String hash = sha256(content);
            Optional<VocabularyVersion> latest = store.findVersion(name, latestNumber);
            if (latest.isPresent() && isIdentical(latest.get(), hash, description, content)) {
                throw Problems.conflict("Version " + latestNumber + " of vocabulary '" + name
                        + "' already has this content and description.");
            }
            int number = latestNumber + 1;
            ContentValidator.Outcome outcome = validator.validateVersion(content, name, number);
            if (!outcome.passed()) {
                throw Problems.badRequest(outcome.errors());
            }
            Instant now = now();
            VocabularyVersion version = new VocabularyVersion(
                    name, number, VocabularyStatus.ACTIVE, description, changelog, createdBy, now, now, hash);
            store.createVersion(version, content);
            LOG.info("Vocabulary {} version {} created by {}", name, number, createdBy);
            return new Created(version, outcome.warnings());
        });
    }

    private boolean isIdentical(VocabularyVersion latest, String hash, String description, byte[] content) {
        if (!latest.contentSha256().equals(hash) || !latest.description().equals(description)) {
            return false;
        }
        return store.versionContent(latest.name(), latest.version())
                .map(stored -> Arrays.equals(stored, content))
                .orElse(false);
    }

    /**
     * Changes the vocabulary status; versions are not touched (decision D3).
     *
     * @param name the vocabulary name
     * @param status the requested status
     * @param user who asked, for the log
     * @return the vocabulary after the change
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404, or 409 for the current status (D4)
     */
    public Vocabulary changeVocabularyStatus(String name, VocabularyStatus status, String user) {
        return locks.withVocabulary(name, () -> {
            Vocabulary vocabulary = store.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
            if (vocabulary.status() == status) {
                throw Problems.conflict("Vocabulary is already in status " + status.name());
            }
            store.updateVocabularyStatus(name, status, now());
            LOG.info("Vocabulary {} status changed to {} by {}", name, status, user);
            return store.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
        });
    }

    /**
     * Changes one version status (decision D4).
     *
     * @param name the vocabulary name
     * @param versionText the version number from the path, already checked to be a positive integer
     * @param status the requested status
     * @param user who asked, for the log
     * @return the version after the change
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404, or 409 for the current status
     */
    public VocabularyVersion changeVersionStatus(
            String name, String versionText, VocabularyStatus status, String user) {
        return locks.withVocabulary(name, () -> {
            store.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
            VocabularyVersion version = findVersionOrThrow(name, versionText);
            if (version.status() == status) {
                throw Problems.conflict("Vocabulary version is already in status " + status.name());
            }
            store.updateVersionStatus(name, version.version(), status, now());
            LOG.info("Vocabulary {} version {} status changed to {} by {}", name, version.version(), status, user);
            return findVersionOrThrow(name, versionText);
        });
    }

    /**
     * Reads a vocabulary.
     *
     * @param name the exact name (decision D54)
     * @return the vocabulary
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public Vocabulary getVocabulary(String name) {
        return store.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    /**
     * Lists vocabularies.
     *
     * @param query filters, order and page
     * @return the page
     */
    public Page<Vocabulary> listVocabularies(VocabularyQuery query) {
        return store.listVocabularies(query);
    }

    /**
     * Lists the versions of a vocabulary.
     *
     * @param name the vocabulary name
     * @param sort the order
     * @param offset items to skip
     * @param limit items to return
     * @return the page
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404 for an unknown vocabulary
     */
    public Page<VocabularyVersion> listVersions(String name, SortOrder sort, long offset, int limit) {
        getVocabulary(name);
        return store.listVersions(name, sort, offset, limit);
    }

    /**
     * Reads a version.
     *
     * @param name the vocabulary name
     * @param versionText the version number from the path
     * @return the version
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public VocabularyVersion getVersion(String name, String versionText) {
        getVocabulary(name);
        return findVersionOrThrow(name, versionText);
    }

    /**
     * Reads the content of the latest version (decision D29).
     *
     * @param name the vocabulary name
     * @return the bytes
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public byte[] latestContent(String name) {
        Vocabulary vocabulary = getVocabulary(name);
        return store.versionContent(name, vocabulary.latestVersion())
                .orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    /**
     * Reads the content of one version.
     *
     * @param name the vocabulary name
     * @param versionText the version number from the path
     * @return the bytes
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 404
     */
    public byte[] versionContent(String name, String versionText) {
        VocabularyVersion version = getVersion(name, versionText);
        return store.versionContent(name, version.version())
                .orElseThrow(() -> Problems.versionNotFound(name, versionText));
    }

    /**
     * Resolves search references (decision D38): request order, missing references left out, repeats once.
     *
     * @param references the references, already deduplicated by the caller
     * @return the results
     */
    public List<SearchResult> search(List<SearchReference> references) {
        return references.stream().map(this::resolve).flatMap(Optional::stream).toList();
    }

    private Optional<SearchResult> resolve(SearchReference reference) {
        Optional<Vocabulary> vocabulary = store.findVocabulary(reference.name());
        if (vocabulary.isEmpty()) {
            return Optional.empty();
        }
        if (reference.version() == null) {
            Vocabulary v = vocabulary.get();
            return Optional.of(new SearchResult(v.name(), v.latestVersion(), v.status(), true));
        }
        return store.findVersion(reference.name(), reference.version())
                .map(v -> new SearchResult(v.name(), v.version(), v.status(), false));
    }

    private VocabularyVersion findVersionOrThrow(String name, String versionText) {
        long number;
        try {
            number = Long.parseLong(versionText);
        } catch (NumberFormatException e) {
            throw Problems.versionNotFound(name, versionText);
        }
        if (number > Integer.MAX_VALUE) {
            throw Problems.versionNotFound(name, versionText);
        }
        return store.findVersion(name, number).orElseThrow(() -> Problems.versionNotFound(name, versionText));
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Computes the SHA-256 of content with the JDK provider (REUSE-01).
     *
     * @param content the bytes
     * @return the hex digest
     */
    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
