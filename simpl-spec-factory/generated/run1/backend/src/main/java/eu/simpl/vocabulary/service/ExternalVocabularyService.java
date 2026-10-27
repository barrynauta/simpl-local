package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.config.AppProperties;
import eu.simpl.vocabulary.domain.ExternalVocabulary;
import eu.simpl.vocabulary.problem.ApiException;
import eu.simpl.vocabulary.problem.ProblemIssue;
import eu.simpl.vocabulary.store.JenaStore;
import eu.simpl.vocabulary.validation.ContentValidator;
import eu.simpl.vocabulary.validation.ValidationReport;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

/** Use cases for cached external vocabularies (OpenAPI only, no story). */
@Service
public class ExternalVocabularyService {

    private final JenaStore store;
    private final ContentValidator validator;
    private final MonotonicClock clock;
    private final AppProperties props;
    private final ReentrantLock writeLock = new ReentrantLock();

    public ExternalVocabularyService(JenaStore store, ContentValidator validator, MonotonicClock clock,
                                     AppProperties props) {
        this.store = store;
        this.validator = validator;
        this.clock = clock;
        this.props = props;
    }

    public List<ExternalVocabulary> findAll() {
        return store.externalVocabularies();
    }

    public ExternalVocabulary get(String name) {
        return store.findExternal(name).orElseThrow(() -> notFound(name));
    }

    public String content(String name) {
        get(name);
        return store.externalRaw(name).orElseThrow(() -> notFound(name));
    }

    public ExternalVocabulary byNamespaceOrAlias(String uri) {
        return store.externalVocabularies().stream().filter(x -> x.matchesAny(uri)).findFirst()
                .orElseThrow(() -> ApiException.notFound("External vocabulary with namespaceUri '" + uri
                        + "' was not found.", ProblemIssue.simple("body", "namespaceUri",
                        "No cached external vocabulary is registered for this namespace.")));
    }

    public ExternalVocabulary create(String name, String namespaceUri, List<String> aliases, String sourceUri,
                                     String description, Upload file, String user) {
        writeLock.lock();
        try {
            if (store.findExternal(name).isPresent()) {
                throw ApiException.conflict("External vocabulary with name '" + name + "' already exists.");
            }
            for (ExternalVocabulary other : store.externalVocabularies()) {
                for (String candidate : concat(namespaceUri, aliases)) {
                    if (other.matchesAny(candidate)) {
                        throw ApiException.conflict("Namespace '" + candidate
                                + "' is already registered by external vocabulary '" + other.name() + "'.");
                    }
                }
            }
            if (!file.looksLikeTurtle()) {
                throw ApiException.unsupportedMediaType(
                        "Unsupported vocabulary file format. Only Turtle (text/turtle, .ttl) is supported.",
                        ProblemIssue.simple("body", "vocabularyFile", "Only Turtle is supported."));
            }
            if (file.bytes().length > props.maxUploadBytes()) {
                throw ApiException.payloadTooLarge(props.maxUploadBytes());
            }
            ValidationReport report = validator.validateSyntaxOnly(name, file.bytes());
            if (report.hasErrors()) {
                throw ApiException.badRequest(VocabularyService.toIssues(report.errors()));
            }
            ExternalVocabulary x = new ExternalVocabulary(name, namespaceUri, aliases, sourceUri, description, user,
                    clock.now());
            store.saveExternal(x, report.graph(), report.text());
            return x;
        } finally {
            writeLock.unlock();
        }
    }

    public void delete(String name) {
        writeLock.lock();
        try {
            get(name);
            store.deleteExternal(name);
        } finally {
            writeLock.unlock();
        }
    }

    private static List<String> concat(String first, List<String> rest) {
        java.util.ArrayList<String> all = new java.util.ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    private static ApiException notFound(String name) {
        String detail = "External vocabulary with name '" + name + "' was not found.";
        return ApiException.notFound(detail, ProblemIssue.simple("path", "name", detail));
    }
}
