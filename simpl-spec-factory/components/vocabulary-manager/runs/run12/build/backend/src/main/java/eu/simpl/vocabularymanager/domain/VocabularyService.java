package eu.simpl.vocabularymanager.domain;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.Model.ContentFingerprint;
import eu.simpl.vocabularymanager.domain.Model.Page;
import eu.simpl.vocabularymanager.domain.Model.Status;
import eu.simpl.vocabularymanager.domain.Model.Vocabulary;
import eu.simpl.vocabularymanager.domain.Model.VocabularyVersion;
import eu.simpl.vocabularymanager.domain.Model.Warning;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.store.Content;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import eu.simpl.vocabularymanager.store.VocabularyStore.ListFilter;
import eu.simpl.vocabularymanager.store.VocabularyStore.NewVersion;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The vocabulary lifecycle of BP02B: create (version 1 ACTIVE), new version (deprecates every older
 * ACTIVE version), status changes, reads (decisions D3 to D8, D43, D46, D82, D160). Request-shape
 * checks happen before; this class runs the 404, 409, validation and write steps of D51.
 */
@Service
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    /**
     * A created version and its validation warnings.
     *
     * @param version the stored version
     * @param warnings validation warnings (not stored, D10)
     */
    public record Created(VocabularyVersion version, List<Warning> warnings) {}

    private final VocabularyStore store;
    private final ContentValidator validator;
    private final Duration writeBudget;
    private final Clock clock;

    @Autowired
    public VocabularyService(VocabularyStore store, ContentValidator validator, AppProperties props) {
        this(store, validator, props.store().writeBudget(), Clock.systemUTC());
    }

    VocabularyService(VocabularyStore store, ContentValidator validator, Duration writeBudget, Clock clock) {
        this.store = store;
        this.validator = validator;
        this.writeBudget = writeBudget == null ? Duration.ofSeconds(30) : writeBudget;
        this.clock = clock;
    }

    public static ApiException vocabularyNotFound(String name) {
        String d = "Vocabulary with name '" + name + "' was not found.";
        return ApiException.notFound(d, Issue.notFound("path", "vocabularyName", d));
    }

    public static ApiException versionNotFound(String name, int version) {
        String d = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return ApiException.notFound(d, Issue.notFound("path", "version", d));
    }

    public Vocabulary get(String name) {
        return store.find(name).orElseThrow(() -> vocabularyNotFound(name));
    }

    public Page<Vocabulary> list(ListFilter filter, String sort, int page, int pageSize) {
        return store.list(filter, sort, page, pageSize);
    }

    public VocabularyVersion getVersion(String name, int version) {
        Vocabulary v = get(name);
        return store.findVersion(name, version, v.latestVersion()).orElseThrow(() -> versionNotFound(name, version));
    }

    public Page<VocabularyVersion> versions(String name, String sort, int page, int pageSize) {
        Vocabulary v = get(name);
        return store.listVersions(name, v.latestVersion(), sort, page, pageSize);
    }

    /** Content of the latest version (D5, D29). */
    public byte[] latestContent(String name) {
        Vocabulary v = get(name);
        return store.content(name, v.latestVersion()).orElseThrow(() -> vocabularyNotFound(name));
    }

    public byte[] content(String name, int version) {
        get(name);
        return store.content(name, version).orElseThrow(() -> versionNotFound(name, version));
    }

    /** POST /vocabularies after the shape checks: 409, validation, conditional write. */
    public Created create(String name, String description, String changelog, byte[] bytes, String creator) {
        if (store.nameKeyExists(name)) {
            throw duplicate(name);
        }
        List<Warning> warnings = validator.validateInternal(bytes, name, 1);
        Instant now = now();
        String token = UUID.randomUUID().toString();
        NewVersion nv = new NewVersion(description.strip(), changelog.strip(), creator, now, bytes);
        if (!store.insertVocabulary(name, nv, token)) {
            throw duplicate(name);
        }
        LOG.info("vocabulary created");
        return new Created(
                new VocabularyVersion(name, 1, nv.description(), nv.changelog(), Status.ACTIVE, creator, now, now, 1),
                warnings);
    }

    private static ApiException duplicate(String name) {
        return ApiException.conflict("Vocabulary with name '" + name + "' already exists.");
    }

    /** POST .../versions after the shape checks: 404, 409 identical (D46), validation, write. */
    public Created createVersion(String name, String description, String changelog, byte[] bytes, String creator) {
        Vocabulary v = get(name);
        String desc = description.strip();
        ContentFingerprint fp = store.fingerprint(name, v.latestVersion()).orElse(null);
        if (fp != null
                && fp.size() == bytes.length
                && fp.sha256().equals(Content.sha256(bytes))
                && fp.description().equals(desc)) {
            throw ApiException.conflict("Version "
                    + v.latestVersion()
                    + " of vocabulary '"
                    + name
                    + "' already has this content and description.");
        }
        List<Warning> warnings = validator.validateInternal(bytes, name, v.latestVersion() + 1);
        String token = UUID.randomUUID().toString();
        WriteRetry retry = new WriteRetry(writeBudget);
        int expected = v.latestVersion();
        while (true) {
            Instant now = now();
            NewVersion nv = new NewVersion(desc, changelog.strip(), creator, now, bytes);
            if (store.insertVersion(name, expected, nv, token)) {
                int created = expected + 1;
                MDC.put("version", Integer.toString(created));
                LOG.info("vocabulary version created");
                return new Created(
                        new VocabularyVersion(
                                name,
                                created,
                                nv.description(),
                                nv.changelog(),
                                Status.ACTIVE,
                                creator,
                                now,
                                now,
                                created),
                        warnings);
            }
            retry.beforeRetry();
            expected = get(name).latestVersion();
        }
    }

    /** PATCH /vocabularies/{name}: 404, 409 same status, conditional write (D3, D4, D43). */
    public Vocabulary changeStatus(String name, Status to) {
        WriteRetry retry = new WriteRetry(writeBudget);
        String token = UUID.randomUUID().toString();
        while (true) {
            Vocabulary v = get(name);
            if (v.status() == to) {
                throw ApiException.conflict("Vocabulary is already in status " + to.name());
            }
            if (store.changeVocabularyStatus(name, v.status(), to, now(), token)) {
                LOG.info("vocabulary status changed to {}", to);
                return get(name);
            }
            retry.beforeRetry();
        }
    }

    /** PATCH .../versions/{version}: 404, 409 same status, conditional write (D4, D160). */
    public VocabularyVersion changeVersionStatus(String name, int version, Status to) {
        WriteRetry retry = new WriteRetry(writeBudget);
        String token = UUID.randomUUID().toString();
        while (true) {
            VocabularyVersion current = getVersion(name, version);
            if (current.status() == to) {
                throw ApiException.conflict("Vocabulary version is already in status " + to.name());
            }
            if (store.changeVersionStatus(name, version, current.status(), to, now(), token)) {
                LOG.info("vocabulary version status changed to {}", to);
                return getVersion(name, version);
            }
            retry.beforeRetry();
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }
}
