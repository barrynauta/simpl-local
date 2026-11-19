package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.domain.model.StoredContent;
import eu.simpl.vocabulary.domain.model.Vocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.domain.model.VocabularyVersion;
import eu.simpl.vocabulary.domain.validation.ValidationWarning;
import eu.simpl.vocabulary.store.VocabularyStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle of vocabularies and versions (D3 to D8, D43, D46). Changes to one vocabulary are serialised by
 * {@link NameLocks}; every change is one store transaction.
 */
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyStore store;
    private final ContentValidation validation;
    private final NameLocks locks;
    private final Clock clock;

    public VocabularyService(VocabularyStore store, ContentValidation validation, NameLocks locks, Clock clock) {
        this.store = store;
        this.validation = validation;
        this.locks = locks;
        this.clock = clock;
    }

    /**
     * Creates a vocabulary with version 1, ACTIVE (D6, D7). Checks after the request shape: 409 duplicate
     * name, then validation (D51).
     *
     * @param name vocabulary name
     * @param description description of version 1
     * @param changelog changelog of version 1
     * @param content the Turtle bytes
     * @param creator the creator (D27)
     * @return the created version and warnings
     */
    public CreatedVersion createVocabulary(
            String name, String description, String changelog, byte[] content, String creator) {
        return locks.withLock(name, () -> {
            if (store.vocabularyNameTaken(name)) {
                throw Problems.conflict("Vocabulary with name '" + name + "' already exists.");
            }
            List<ValidationWarning> warnings = validation.validateInternal(name, 1, content);
            Instant now = now();
            VocabularyVersion first = new VocabularyVersion(
                    name,
                    1,
                    description.strip(),
                    changelog.strip(),
                    VocabularyStatus.ACTIVE,
                    creator,
                    now,
                    now,
                    sha256(content));
            Vocabulary vocabulary =
                    new Vocabulary(name, VocabularyStatus.ACTIVE, creator, now, now, 1, first.description());
            store.createVocabulary(vocabulary, first, content);
            LOG.info("Vocabulary {} created with version 1", name);
            return new CreatedVersion(first, warnings);
        });
    }

    /**
     * Creates the next version, ACTIVE, and deprecates every older ACTIVE version (D4, D7). Checks: 404,
     * 409 identical version (D46), then validation (D51).
     *
     * @param name vocabulary name
     * @param description description
     * @param changelog changelog
     * @param content the Turtle bytes
     * @param creator the creator (D27)
     * @return the created version and warnings
     */
    public CreatedVersion createVersion(
            String name, String description, String changelog, byte[] content, String creator) {
        return locks.withLock(name, () -> {
            Vocabulary vocabulary = store.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
            long latest = vocabulary.latestVersion();
            String hash = sha256(content);
            String trimmedDescription = description.strip();
            Optional<VocabularyVersion> latestVersion = store.findVersion(name, latest);
            if (latestVersion.isPresent()
                    && latestVersion.get().contentSha256().equals(hash)
                    && latestVersion.get().description().equals(trimmedDescription)) {
                throw Problems.conflict("Version " + latest + " of vocabulary '" + name
                        + "' already has this content and description.");
            }
            long next = latest + 1;
            List<ValidationWarning> warnings = validation.validateInternal(name, next, content);
            Instant now = now();
            VocabularyVersion version = new VocabularyVersion(
                    name,
                    next,
                    trimmedDescription,
                    changelog.strip(),
                    VocabularyStatus.ACTIVE,
                    creator,
                    now,
                    now,
                    hash);
            store.createVersion(version, content);
            LOG.info("Vocabulary {} version {} created", name, next);
            return new CreatedVersion(version, warnings);
        });
    }

    /**
     * @param name exact name (D54)
     * @return the vocabulary
     */
    public Vocabulary getVocabulary(String name) {
        return store.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    /**
     * Filters, sorts and pages the vocabulary list (D19, D20, D22, D23, D58).
     *
     * @param query the query
     * @return the page
     */
    public Page<Vocabulary> listVocabularies(VocabularyQuery query) {
        String needle =
                query.nameContains() == null ? null : query.nameContains().toLowerCase(Locale.ROOT);
        List<Vocabulary> matching = store.listVocabularies().stream()
                .filter(v -> needle == null || v.name().toLowerCase(Locale.ROOT).contains(needle))
                .filter(v -> query.status() == null || v.status() == query.status())
                .filter(v ->
                        query.creationDate() == null || query.creationDate().matches(v.creationDate()))
                .filter(v -> query.lastUpdated() == null || query.lastUpdated().matches(v.lastUpdateDate()))
                .sorted(vocabularyOrder(query.sort()))
                .toList();
        return Page.of(matching, query.page(), query.pageSize());
    }

    /**
     * Comparator for a sort value of D23; ties by name ascending, names by code point (D58).
     *
     * @param sort the sort value, null for the default
     * @return the comparator
     */
    static Comparator<Vocabulary> vocabularyOrder(String sort) {
        Comparator<Vocabulary> byName = Comparator.comparing(Vocabulary::name, VocabularyService::codePointOrder);
        if (sort == null) {
            return byName;
        }
        boolean descending = sort.startsWith("-");
        String field = descending ? sort.substring(1) : sort;
        Comparator<Vocabulary> primary = switch (field) {
            case "status" -> Comparator.comparing(v -> v.status().name());
            case "creationDate" -> Comparator.comparing(Vocabulary::creationDate);
            case "lastUpdated" -> Comparator.comparing(Vocabulary::lastUpdateDate);
            default -> byName;
        };
        if (descending) {
            primary = primary.reversed();
        }
        return primary.thenComparing(byName);
    }

    /**
     * Lists the versions of a vocabulary (D23: default version descending, ties version descending).
     *
     * @param name exact name
     * @param sort sort value or null
     * @param page page number
     * @param pageSize page size
     * @return the vocabulary and the page
     */
    public VersionPage listVersions(String name, String sort, long page, int pageSize) {
        Vocabulary vocabulary = getVocabulary(name);
        List<VocabularyVersion> sorted =
                store.listVersions(name).stream().sorted(versionOrder(sort)).toList();
        return new VersionPage(vocabulary, Page.of(sorted, page, pageSize));
    }

    /**
     * A page of versions with their vocabulary.
     *
     * @param vocabulary the vocabulary
     * @param page the page
     */
    public record VersionPage(Vocabulary vocabulary, Page<VocabularyVersion> page) {}

    static Comparator<VocabularyVersion> versionOrder(String sort) {
        Comparator<VocabularyVersion> byVersionDescending =
                Comparator.comparingLong(VocabularyVersion::version).reversed();
        if (sort == null) {
            return byVersionDescending;
        }
        boolean descending = sort.startsWith("-");
        String field = descending ? sort.substring(1) : sort;
        Comparator<VocabularyVersion> primary = switch (field) {
            case "status" -> Comparator.comparing(v -> v.status().name());
            case "creationDate" -> Comparator.comparing(VocabularyVersion::creationDate);
            default -> Comparator.comparing(VocabularyVersion::name, VocabularyService::codePointOrder);
        };
        if (descending) {
            primary = primary.reversed();
        }
        return primary.thenComparing(byVersionDescending);
    }

    /**
     * @param name exact name
     * @param version version number
     * @return the version with its vocabulary
     */
    public VersionView getVersion(String name, long version) {
        Vocabulary vocabulary = getVocabulary(name);
        VocabularyVersion found =
                store.findVersion(name, version).orElseThrow(() -> Problems.versionNotFound(name, version));
        return new VersionView(vocabulary, found);
    }

    /**
     * Sets the vocabulary status (D3); the same status gives 409 (D4).
     *
     * @param name exact name
     * @param status requested status
     * @return the updated vocabulary
     */
    public Vocabulary updateVocabularyStatus(String name, VocabularyStatus status) {
        return locks.withLock(name, () -> {
            Vocabulary vocabulary = getVocabulary(name);
            if (vocabulary.status() == status) {
                throw Problems.conflict("Vocabulary is already in status " + status.name());
            }
            store.updateVocabularyStatus(name, status, now());
            LOG.info("Vocabulary {} status changed to {}", name, status);
            return getVocabulary(name);
        });
    }

    /**
     * Sets a version status (D4); the same status gives 409.
     *
     * @param name exact name
     * @param version version number
     * @param status requested status
     * @return the updated version with its vocabulary
     */
    public VersionView updateVersionStatus(String name, long version, VocabularyStatus status) {
        return locks.withLock(name, () -> {
            VersionView current = getVersion(name, version);
            if (current.version().status() == status) {
                throw Problems.conflict("Vocabulary version is already in status " + status.name());
            }
            store.updateVersionStatus(name, version, status, now());
            LOG.info("Vocabulary {} version {} status changed to {}", name, version, status);
            return getVersion(name, version);
        });
    }

    /**
     * @param name exact name
     * @return the bytes of the latest version (D29)
     */
    public StoredContent latestContent(String name) {
        return store.findLatestContent(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    /**
     * @param name exact name
     * @param version version number
     * @return the bytes of the version
     */
    public StoredContent content(String name, long version) {
        Optional<StoredContent> content = store.findContent(name, version);
        if (content.isPresent()) {
            return content.get();
        }
        getVocabulary(name);
        throw Problems.versionNotFound(name, version);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Code point order of D58 ('AZ' before 'Ab').
     *
     * @param a first
     * @param b second
     * @return comparison
     */
    static int codePointOrder(String a, String b) {
        return a.compareTo(b);
    }

    /**
     * SHA-256 of content, hex.
     *
     * @param content the bytes
     * @return the hash
     */
    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Text form of bytes, for tests. */
    static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
