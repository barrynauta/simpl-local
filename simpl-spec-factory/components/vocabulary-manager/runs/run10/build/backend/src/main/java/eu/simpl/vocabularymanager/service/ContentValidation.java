package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.Rdf;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.ParsedContent;
import eu.simpl.vocabularymanager.validation.ReferenceChecker;
import eu.simpl.vocabularymanager.validation.SemanticValidator;
import eu.simpl.vocabularymanager.validation.TurtleSyntaxValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.QuerySolution;

/**
 * The two validation steps of D9 on every create and new version: syntax first (stops on failure), then semantic
 * rules S1 to S6 and the reference check S7, all errors in one response. Warnings come back for the 201 body.
 */
public class ContentValidation {

    private final TurtleSyntaxValidator syntax;
    private final SemanticValidator semantic;
    private final ReferenceChecker references;
    private final VocabularyRepository repository;
    private final String contentBase;

    public ContentValidation(
            TurtleSyntaxValidator syntax,
            SemanticValidator semantic,
            ReferenceChecker references,
            VocabularyRepository repository,
            String contentBase) {
        this.syntax = syntax;
        this.semantic = semantic;
        this.references = references;
        this.repository = repository;
        this.contentBase = contentBase;
    }

    /**
     * Validates the content of version {@code version} of vocabulary {@code name}.
     *
     * @return the validation warnings
     * @throws eu.simpl.vocabularymanager.error.ProblemException 400 with the validation report
     */
    public List<ValidationWarning> validate(byte[] content, String name, int version) {
        String base = contentBase + "/content/" + name + "/versions/" + version;
        ParsedContent parsed = syntax.parse(content, base);
        List<Issue> errors = new ArrayList<>(semantic.validate(parsed.graph()));
        ReferenceChecker.Result result = references.check(parsed, name, this::lookup);
        errors.addAll(result.errors());
        if (!errors.isEmpty()) {
            throw Problems.badRequest(
                    errors.size() > SemanticValidator.MAX_ISSUES
                            ? errors.subList(0, SemanticValidator.MAX_ISSUES)
                            : errors);
        }
        return result.warnings();
    }

    private ReferenceChecker.ReferenceData lookup(Set<String> names, Set<String> keys) {
        Map<String, ReferenceChecker.InternalState> internal = new HashMap<>();
        Set<String> registered = new HashSet<>();
        if (names.isEmpty() && keys.isEmpty()) {
            return new ReferenceChecker.ReferenceData(internal, registered);
        }
        Map<String, Map<Integer, VocabularyStatus>> versions = new HashMap<>();
        for (QuerySolution row : repository.referenceData(names, keys)) {
            if ("external".equals(Rdf.string(row, "kind"))) {
                registered.add(Rdf.string(row, "key"));
                continue;
            }
            String name = Rdf.string(row, "name");
            Map<Integer, VocabularyStatus> byVersion = versions.computeIfAbsent(name, key -> new HashMap<>());
            byVersion.put(Rdf.integer(row, "version"), VocabularyStatus.valueOf(Rdf.string(row, "versionStatus")));
            internal.put(
                    name,
                    new ReferenceChecker.InternalState(
                            VocabularyStatus.valueOf(Rdf.string(row, "vstatus")),
                            Rdf.integer(row, "latest"),
                            byVersion));
        }
        return new ReferenceChecker.ReferenceData(internal, registered);
    }
}
