package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ContentValidationException;
import eu.simpl.vocabularymanager.domain.ValidationIssue;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.ReferenceAnalysis.InternalReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/** Runs syntax validation, then semantic validation S1 to S7, on uploaded vocabulary content (D9 to D12). */
@Component
public class ContentValidator {

    private final TurtleParser parser;
    private final SemanticRules semanticRules;
    private final VocabularyRepository repository;
    private final ReferenceAnalyzer analyzer;
    private final String contentBase;

    /**
     * @param parser Turtle parser
     * @param semanticRules rules S1 to S6
     * @param repository store lookup for S7 and the warnings
     * @param properties component configuration
     */
    public ContentValidator(
            TurtleParser parser,
            SemanticRules semanticRules,
            VocabularyRepository repository,
            VocabularyManagerProperties properties) {
        this.parser = parser;
        this.semanticRules = semanticRules;
        this.repository = repository;
        this.contentBase = properties.contentBase();
        this.analyzer = new ReferenceAnalyzer(contentBase);
    }

    /**
     * Validates content for a vocabulary version.
     *
     * @param content uploaded bytes
     * @param name vocabulary name
     * @param version the version number the content is meant for (base IRI, D57)
     * @return the warnings; never null
     * @throws ContentValidationException with every error when validation fails
     */
    public List<ValidationWarning> validate(byte[] content, String name, int version) {
        ParsedContent parsed = parser.parse(content, contentBase + "/content/" + name + "/versions/" + version);
        List<ValidationIssue> issues = new ArrayList<>(semanticRules.check(parsed.graph()));
        ReferenceAnalysis analysis = analyzer.analyze(parsed.prefixes(), parsed.subjectIris(), name);
        ReferenceData data = analysis.isEmpty()
                ? ReferenceData.empty()
                : repository.referenceData(analysis.internalNames(), versionNumbers(analysis), analysis.externalKeys());
        ReferenceEvaluator.Result references = ReferenceEvaluator.evaluate(analysis, data);
        issues.addAll(references.issues());
        if (!issues.isEmpty()) {
            throw new ContentValidationException(issues);
        }
        return references.warnings();
    }

    /**
     * Validates content of an external vocabulary: syntax only and at least one statement (D30, D70).
     *
     * @param content uploaded bytes
     * @param name external vocabulary name
     * @throws ContentValidationException with one issue when the content is not valid or empty
     */
    public void validateExternal(byte[] content, String name) {
        ParsedContent parsed = parser.parse(content, contentBase + "/external-vocabularies/" + name);
        if (parsed.graph().isEmpty()) {
            throw new ContentValidationException(List.of(new ValidationIssue(SemanticRules.S6, "document")));
        }
    }

    private static Set<Integer> versionNumbers(ReferenceAnalysis analysis) {
        Set<Integer> numbers = new TreeSet<>();
        analysis.internal().stream()
                .map(InternalReference::versionNumber)
                .filter(Objects::nonNull)
                .forEach(numbers::add);
        return numbers;
    }
}
