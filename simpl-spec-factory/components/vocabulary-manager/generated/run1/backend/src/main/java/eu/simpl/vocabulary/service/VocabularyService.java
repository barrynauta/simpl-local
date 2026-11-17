package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.Status;
import eu.simpl.vocabulary.domain.Vocabulary;
import eu.simpl.vocabulary.domain.VocabularyLifecycle;
import eu.simpl.vocabulary.problem.ApiException;
import eu.simpl.vocabulary.problem.ProblemIssue;
import eu.simpl.vocabulary.store.JenaStore;
import eu.simpl.vocabulary.validation.ContentValidator;
import eu.simpl.vocabulary.validation.ValidationIssue;
import eu.simpl.vocabulary.validation.ValidationReport;
import eu.simpl.vocabulary.validation.ValidationWarning;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

/** Use cases for internal vocabularies. */
@Service
public class VocabularyService {

    private final JenaStore store;
    private final ContentValidator validator;
    private final MonotonicClock clock;
    private final AppProperties props;
    /** Serialises read-modify-write sequences (single backend instance). */
    private final ReentrantLock writeLock = new ReentrantLock();

    public VocabularyService(JenaStore store, ContentValidator validator, MonotonicClock clock, AppProperties props) {
        this.store = store;
        this.validator = validator;
        this.clock = clock;
        this.props = props;
    }

    /** Result of a create call: the new state and the non-blocking warnings (D20). */
    public record Created(Vocabulary vocabulary, int version, List<ValidationWarning> warnings) {}

    public List<Vocabulary> findAll() {
        return store.findAll();
    }

    public Vocabulary get(String name) {
        return store.findVocabulary(name).orElseThrow(() -> vocabularyNotFound(name));
    }

    public Optional<Vocabulary> find(String name) {
        return store.findVocabulary(name);
    }

    public String content(String name, Integer version) {
        Vocabulary vocabulary = get(name);
        int number = version == null ? vocabulary.latestVersionNumber() : version;
        if (vocabulary.version(number).isEmpty()) {
            throw versionNotFound(name, number);
        }
        return store.rawContent(name, number).orElseThrow(() -> versionNotFound(name, number));
    }

    public Created create(String name, String description, String changelog, Upload file, String user) {
        writeLock.lock();
        try {
            if (store.findVocabulary(name).isPresent()) {
                throw ApiException.conflict("Vocabulary with name '" + name + "' already exists.");
            }
            ValidationReport report = checkUpload(name, 1, file);
            Vocabulary vocabulary = VocabularyLifecycle.create(name, description, changelog, user, clock.now());
            store.save(vocabulary, new JenaStore.NewContent(1, report.graph(), report.text()));
            return new Created(vocabulary, 1, report.warnings());
        } finally {
            writeLock.unlock();
        }
    }

    public Created createVersion(String name, String description, String changelog, Upload file, String user) {
        writeLock.lock();
        try {
            Vocabulary current = get(name);
            // D11: checked before the content, so a deprecated vocabulary is rejected regardless of the file.
            if (current.status() == Status.DEPRECATED) {
                throw ApiException.conflict("Vocabulary '" + name
                        + "' is DEPRECATED. Set the vocabulary status to ACTIVE before creating a new version.");
            }
            int next = current.latestVersionNumber() + 1;
            ValidationReport report = checkUpload(name, next, file);
            Vocabulary updated = VocabularyLifecycle.addVersion(current, description, changelog, user, clock.now());
            store.save(updated, new JenaStore.NewContent(next, report.graph(), report.text()));
            return new Created(updated, next, report.warnings());
        } finally {
            writeLock.unlock();
        }
    }

    public Vocabulary changeStatus(String name, Status status) {
        writeLock.lock();
        try {
            Vocabulary updated = VocabularyLifecycle.changeStatus(get(name), status, clock.now());
            store.save(updated, null);
            return updated;
        } finally {
            writeLock.unlock();
        }
    }

    public Vocabulary changeVersionStatus(String name, int version, Status status) {
        writeLock.lock();
        try {
            Vocabulary current = get(name);
            if (current.version(version).isEmpty()) {
                throw versionNotFound(name, version);
            }
            Vocabulary updated = VocabularyLifecycle.changeVersionStatus(current, version, status, clock.now());
            store.save(updated, null);
            return updated;
        } finally {
            writeLock.unlock();
        }
    }

    /** Media type (415), size (413), then content validation (400). */
    private ValidationReport checkUpload(String name, int version, Upload file) {
        if (!file.looksLikeTurtle()) {
            throw ApiException.unsupportedMediaType(
                    "Unsupported vocabulary file format. Only Turtle (text/turtle, .ttl) is supported.",
                    ProblemIssue.simple("body", "vocabularyFile", "Only Turtle (text/turtle, .ttl) is supported."));
        }
        if (file.bytes().length > props.maxUploadBytes()) {
            throw ApiException.payloadTooLarge(props.maxUploadBytes());
        }
        ValidationReport report = validator.validate(name, version, file.bytes(), store);
        if (report.hasErrors()) {
            throw ApiException.badRequest(toIssues(report.errors()), report.warnings());
        }
        return report;
    }

    public static List<ProblemIssue> toIssues(List<ValidationIssue> errors) {
        return errors.stream()
                .map(e -> ProblemIssue.invalidContent("vocabularyFile", e.detail(), e.location()))
                .toList();
    }

    public static ApiException vocabularyNotFound(String name) {
        String detail = "Vocabulary with name '" + name + "' was not found.";
        return ApiException.notFound(detail, ProblemIssue.simple("path", "vocabularyName", detail));
    }

    public static ApiException versionNotFound(String name, int version) {
        String detail = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return ApiException.notFound(detail, ProblemIssue.simple("path", "version", detail));
    }
}
