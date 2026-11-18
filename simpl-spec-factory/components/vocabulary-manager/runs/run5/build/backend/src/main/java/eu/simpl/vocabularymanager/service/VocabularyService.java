package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.InternalVocabularyState;
import eu.simpl.vocabularymanager.validation.ReferenceContext;
import eu.simpl.vocabularymanager.validation.ValidationWarning;
import eu.simpl.vocabularymanager.web.ApiException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Use cases of vocabularies and vocabulary versions (BP02B, D3 to D8, D43, D46). */
@Service
public class VocabularyService {

    /** Detail of the 503 when the store is not available (D44). */
    public static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyStore store;
    private final ContentValidator validator;
    private final NameLocks locks;
    private final Clock clock;
    private final String contentBase;

    public VocabularyService(
            VocabularyStore store,
            ContentValidator validator,
            NameLocks locks,
            Clock clock,
            VocabularyProperties properties) {
        this.store = store;
        this.validator = validator;
        this.locks = locks;
        this.clock = clock;
        this.contentBase = properties.contentBase();
    }

    /** Current time in UTC to the second (D58). */
    public String now() {
        return DateTimeFormatter.ISO_INSTANT.format(clock.instant().truncatedTo(ChronoUnit.SECONDS));
    }

    /** All vocabularies, unsorted. */
    public List<Vocabulary> listVocabularies() {
        return store.listVocabularies();
    }

    /** One vocabulary by exact name, or 404. */
    public Vocabulary getVocabulary(String name) {
        return store.findVocabulary(name).orElseThrow(() -> vocabularyNotFound(name));
    }

    /** All versions of a vocabulary, or 404 when the vocabulary does not exist. */
    public List<VocabularyVersion> listVersions(String name) {
        getVocabulary(name);
        return store.listVersions(name);
    }

    /** One version, or 404. */
    public VocabularyVersion getVersion(String name, int version) {
        getVocabulary(name);
        return store.findVersion(name, version).orElseThrow(() -> versionNotFound(name, version));
    }

    /** Content of the latest version (D5, D29). */
    public byte[] latestContent(String name) {
        Vocabulary vocabulary = getVocabulary(name);
        return store.findContent(name, vocabulary.latestVersion())
                .orElseThrow(() -> versionNotFound(name, vocabulary.latestVersion()));
    }

    /** Content of one version. */
    public byte[] content(String name, int version) {
        getVocabulary(name);
        return store.findContent(name, version).orElseThrow(() -> versionNotFound(name, version));
    }

    /**
     * Creates a vocabulary with version 1 (D6, D7). Checks in the order of D51 after the request shape: 409 duplicate
     * name, then validation.
     */
    public CreationResult createVocabulary(
            String name, String description, String changelog, byte[] content, String creator) {
        return locks.withLock(name, () -> {
            if (store.vocabularyNameTaken(name)) {
                throw ApiException.conflict("Vocabulary with name '" + name + "' already exists.");
            }
            List<ValidationWarning> warnings =
                    validator.validate(content, versionContentUrl(name, 1), referenceContext(name));
            String now = now();
            Vocabulary vocabulary = new Vocabulary(name, Status.ACTIVE, creator, now, now, 1, description);
            VocabularyVersion first =
                    new VocabularyVersion(name, 1, Status.ACTIVE, description, changelog, creator, now, now);
            store.createVocabulary(vocabulary, first, content);
            LOG.info("Vocabulary {} created", name);
            return new CreationResult(first, 1, warnings);
        });
    }

    /**
     * Creates a new version (D4, D7, D43, D46): 404, 409 identical version, validation, then one store transaction
     * that also deprecates every older ACTIVE version.
     */
    public CreationResult createVersion(
            String name, String description, String changelog, byte[] content, String creator) {
        return locks.withLock(name, () -> {
            Vocabulary vocabulary = getVocabulary(name);
            int latest = vocabulary.latestVersion();
            VocabularyVersion latestVersion =
                    store.findVersion(name, latest).orElseThrow(() -> versionNotFound(name, latest));
            byte[] latestContent = store.findContent(name, latest).orElse(new byte[0]);
            if (Arrays.equals(latestContent, content) && description.equals(latestVersion.description())) {
                throw ApiException.conflict("Version " + latest + " of vocabulary '" + name
                        + "' already has this content and description.");
            }
            int number = latest + 1;
            List<ValidationWarning> warnings =
                    validator.validate(content, versionContentUrl(name, number), referenceContext(name));
            String now = now();
            VocabularyVersion created =
                    new VocabularyVersion(name, number, Status.ACTIVE, description, changelog, creator, now, now);
            store.createVersion(created, content, now);
            LOG.info("Version {} of vocabulary {} created", number, name);
            return new CreationResult(created, number, warnings);
        });
    }

    /** Sets the vocabulary status (D3, D4): 404, 409 same status, then the change. */
    public Vocabulary updateVocabularyStatus(String name, Status status, String actor) {
        return locks.withLock(name, () -> {
            Vocabulary vocabulary = getVocabulary(name);
            if (vocabulary.status() == status) {
                throw ApiException.conflict("Vocabulary is already in status " + status.name());
            }
            store.updateVocabularyStatus(name, status, now());
            LOG.info("Vocabulary {} set to {} by {}", name, status, actor);
            return getVocabulary(name);
        });
    }

    /** Sets one version status (D4): 404, 409 same status, then the change. */
    public VocabularyVersion updateVersionStatus(String name, int version, Status status, String actor) {
        return locks.withLock(name, () -> {
            VocabularyVersion current = getVersion(name, version);
            if (current.status() == status) {
                throw ApiException.conflict("Vocabulary version is already in status " + status.name());
            }
            store.updateVersionStatus(name, version, status, now());
            LOG.info("Version {} of vocabulary {} set to {} by {}", version, name, status, actor);
            return getVersion(name, version);
        });
    }

    /**
     * Resolves search references (D38): request order, missing references left out, repeats given once.
     *
     * @param references the references
     * @return the results
     */
    public List<SearchResult> search(List<SearchReference> references) {
        Set<SearchReference> seen = new LinkedHashSet<>(references);
        return seen.stream().map(this::resolve).flatMap(Optional::stream).toList();
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

    /** Content URL of a version, also the base IRI of its content (D26, D57). */
    public String versionContentUrl(String name, int version) {
        return contentBase + "/content/" + name + "/versions/" + version;
    }

    /** Content URL of the latest version (D25). */
    public String vocabularyContentUrl(String name) {
        return contentBase + "/content/" + name;
    }

    private ReferenceContext referenceContext(String selfName) {
        Set<String> externalKeys = new HashSet<>();
        for (ExternalVocabulary external : store.listExternalVocabularies()) {
            externalKeys.addAll(external.lookupKeys());
        }
        return new ReferenceContext(
                selfName,
                referenced -> store.findVocabulary(referenced)
                        .map(v -> new InternalVocabularyState(v, store.listVersions(referenced))),
                externalKeys);
    }

    /** 404 for an unknown vocabulary (VocabularyNotFoundResponse). */
    public static ApiException vocabularyNotFound(String name) {
        return ApiException.notFound("Vocabulary with name '" + name + "' was not found.", "vocabularyName");
    }

    /** 404 for an unknown version (D55). */
    public static ApiException versionNotFound(String name, int version) {
        return ApiException.notFound("Version " + version + " of vocabulary '" + name + "' was not found.", "version");
    }
}
