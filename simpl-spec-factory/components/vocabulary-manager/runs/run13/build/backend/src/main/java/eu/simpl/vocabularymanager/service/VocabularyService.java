package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.store.ReferenceState;
import eu.simpl.vocabularymanager.store.VocabularyQuery;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.store.VocabularyRepository.NewVersion;
import eu.simpl.vocabularymanager.store.VocabularyRepository.VersionSort;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.ParsedContent;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.ExternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.InternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.Reference;
import eu.simpl.vocabularymanager.validation.TurtleSyntax;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * The vocabulary lifecycle (D3 to D8, D43, D46, D82): creation, new versions, status changes and reads.
 * The checks of a write run in the order of D51 across the controller and this service.
 */
public class VocabularyService {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyService.class);

    /** Result of a creation: the stored version and its warnings. */
    public record Created(VocabularyVersion version, List<ValidationWarning> warnings) {}

    /** A version with the latest version number of its vocabulary. */
    public record VersionView(VocabularyVersion version, int latestVersion) {}

    /** Fields of a create or new version request after the field checks. */
    public record Submission(String name, String description, String changelog, byte[] content) {}

    private final VocabularyRepository repository;
    private final ReferenceAnalyzer referenceAnalyzer;
    private final String contentBase;
    private final WriteRetry writeRetry;
    private final Clock clock;

    public VocabularyService(VocabularyRepository repository, String contentBase, WriteRetry writeRetry, Clock clock) {
        this.repository = repository;
        this.referenceAnalyzer = new ReferenceAnalyzer(contentBase);
        this.contentBase = contentBase;
        this.writeRetry = writeRetry;
        this.clock = clock;
    }

    public Page<Vocabulary> list(VocabularyQuery query) {
        return repository.list(query);
    }

    public Vocabulary get(String name) {
        return repository.findVocabulary(name).orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    public Page<VocabularyVersion> versions(
            Vocabulary vocabulary, VersionSort sort, boolean descending, int page, int pageSize) {
        return repository.versions(vocabulary.name(), sort, descending, page, pageSize, vocabulary.latestVersion());
    }

    public VersionView version(String name, int version) {
        Vocabulary vocabulary = get(name);
        VocabularyVersion found =
                repository.findVersion(name, version).orElseThrow(() -> Problems.versionNotFound(name, version));
        return new VersionView(found, vocabulary.latestVersion());
    }

    /** The content of the latest version (D5, D29). */
    public byte[] latestContent(String name) {
        Vocabulary vocabulary = get(name);
        return repository
                .content(name, vocabulary.latestVersion())
                .orElseThrow(() -> Problems.vocabularyNotFound(name));
    }

    public byte[] content(String name, int version) {
        get(name);
        return repository.content(name, version).orElseThrow(() -> Problems.versionNotFound(name, version));
    }

    /** POST /vocabularies after the field checks: 409, validation, then the conditional insert. */
    public Created create(Submission submission, AuthenticatedUser user) {
        String name = submission.name();
        if (repository.nameTaken(name)) {
            throw Problems.conflict("Vocabulary with name '" + name + "' already exists.");
        }
        List<ValidationWarning> warnings = validate(submission.content(), name, 1);
        Instant now = now();
        NewVersion data = new NewVersion(
                name,
                1,
                submission.description(),
                submission.changelog(),
                user.creator(),
                now,
                sha256(submission.content()),
                submission.content());
        if (!repository.insertVocabulary(data, UUID.randomUUID().toString())) {
            throw Problems.conflict("Vocabulary with name '" + name + "' already exists.");
        }
        MDC.put("version", "1");
        LOG.info("Vocabulary created");
        return new Created(toVersion(data), warnings);
    }

    /** POST .../versions after the field checks: 404, identical check 409, validation, conditional insert. */
    public Created createVersion(Submission submission, AuthenticatedUser user) {
        String name = submission.name();
        Vocabulary vocabulary = get(name);
        rejectIdentical(vocabulary, submission);
        List<ValidationWarning> warnings = validate(submission.content(), name, vocabulary.latestVersion() + 1);
        String sha = sha256(submission.content());
        String token = UUID.randomUUID().toString();
        int[] firstAttempt = {vocabulary.latestVersion()};
        NewVersion stored = writeRetry.run(() -> {
            int latest = firstAttempt[0] > 0 ? firstAttempt[0] : get(name).latestVersion();
            firstAttempt[0] = 0;
            NewVersion data = new NewVersion(
                    name,
                    latest + 1,
                    submission.description(),
                    submission.changelog(),
                    user.creator(),
                    now(),
                    sha,
                    submission.content());
            return repository.insertVersion(data, latest, token) ? data : null;
        });
        MDC.put("version", Integer.toString(stored.version()));
        LOG.info("Vocabulary version created");
        return new Created(toVersion(stored), warnings);
    }

    /** D46: same bytes as the latest version and the same trimmed description. */
    private void rejectIdentical(Vocabulary vocabulary, Submission submission) {
        if (!vocabulary.description().equals(submission.description())) {
            return;
        }
        Optional<VocabularyVersion> latest = repository.findVersion(vocabulary.name(), vocabulary.latestVersion());
        if (latest.isPresent() && latest.get().contentSha256().equals(sha256(submission.content()))) {
            throw Problems.conflict("Version " + vocabulary.latestVersion() + " of vocabulary '" + vocabulary.name()
                    + "' already has this content and description.");
        }
    }

    /** Syntax, then semantic validation with one store query for the references (D9, D12, Q5). */
    private List<ValidationWarning> validate(byte[] content, String name, int version) {
        String base = contentBase + "/content/" + name + "/versions/" + version;
        ParsedContent parsed = TurtleSyntax.parse(content, base);
        List<Reference> references = referenceAnalyzer.references(parsed, name);
        Set<String> names = new LinkedHashSet<>();
        Set<String> externalKeys = new LinkedHashSet<>();
        for (Reference reference : references) {
            if (reference instanceof InternalReference internal) {
                if (internal.versionText() == null || internal.version() != null) {
                    names.add(internal.name());
                }
            } else if (reference instanceof ExternalReference external) {
                externalKeys.add(external.key());
            }
        }
        ReferenceState state = repository.referenceState(names, externalKeys);
        return ContentValidator.validate(parsed, references, state);
    }

    public Vocabulary setStatus(String name, VocabularyStatus target) {
        String token = UUID.randomUUID().toString();
        writeRetry.run(() -> {
            Vocabulary current = get(name);
            if (current.status() == target) {
                throw Problems.conflict("Vocabulary is already in status " + target.name());
            }
            return repository.setVocabularyStatus(name, current.status(), target, now(), token) ? Boolean.TRUE : null;
        });
        LOG.info("Vocabulary status changed to {}", target);
        return get(name);
    }

    public VersionView setVersionStatus(String name, int version, VocabularyStatus target) {
        String token = UUID.randomUUID().toString();
        writeRetry.run(() -> {
            VersionView current = version(name, version);
            if (current.version().status() == target) {
                throw Problems.conflict("Vocabulary version is already in status " + target.name());
            }
            return repository.setVersionStatus(name, version, current.version().status(), target, now(), token)
                    ? Boolean.TRUE
                    : null;
        });
        LOG.info("Vocabulary version status changed to {}", target);
        return version(name, version);
    }

    /** The store state of the searched vocabularies, in one query (D38). */
    public ReferenceState searchState(Set<String> names) {
        return repository.referenceState(names, Set.of());
    }

    private VocabularyVersion toVersion(NewVersion data) {
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

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
