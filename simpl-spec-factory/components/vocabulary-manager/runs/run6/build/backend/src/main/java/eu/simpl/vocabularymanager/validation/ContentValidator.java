package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Runs the two validation steps of decision D9 on uploaded content: syntax, then (only when syntax passes)
 * semantic validation with every rule. Errors block (400); warnings do not (decision D10).
 */
@Component
public class ContentValidator {

    /** Form field that carries the content. */
    public static final String FIELD = "vocabularyFile";

    private final TurtleParser parser;
    private final SemanticRules semanticRules;
    private final ReferenceChecker referenceChecker;
    private final String contentBase;
    private final int maxIssues;

    /**
     * Outcome of validation.
     *
     * @param errors validation errors as problem issues; empty when the content passed
     * @param warnings validation warnings
     */
    public record Outcome(List<Issue> errors, List<ValidationWarning> warnings) {

        /**
         * Tells whether validation passed.
         *
         * @return true when there are no errors
         */
        public boolean passed() {
            return errors.isEmpty();
        }
    }

    /**
     * Creates the validator.
     *
     * @param parser the Turtle parser
     * @param semanticRules rules S1 to S6
     * @param store the store, for references
     * @param properties the configuration
     */
    public ContentValidator(
            TurtleParser parser, SemanticRules semanticRules, VocabularyStore store, AppProperties properties) {
        this.parser = parser;
        this.semanticRules = semanticRules;
        this.contentBase = trimSlash(properties.contentBaseUrl());
        this.referenceChecker = new ReferenceChecker(store, contentBase);
        this.maxIssues = properties.limits().maxValidationIssues();
    }

    /**
     * Validates content of an internal vocabulary version: syntax, then semantic rules S1 to S7.
     *
     * @param content the uploaded bytes
     * @param name the vocabulary name
     * @param version the number the new version will get
     * @return the outcome
     */
    public Outcome validateVersion(byte[] content, String name, int version) {
        String base = contentBase + "/content/" + name + "/versions/" + version;
        TurtleParser.Parsed parsed;
        try {
            parsed = parser.parse(content, base);
        } catch (TurtleParser.SyntaxError e) {
            return new Outcome(List.of(syntaxIssue(e)), List.of());
        }
        List<Issue> errors = new ArrayList<>();
        semanticRules.check(parsed.graph()).forEach(f -> errors.add(issue(f)));
        ReferenceChecker.Result references = referenceChecker.check(parsed.prefixes(), parsed.graph(), name);
        references.errors().forEach(f -> errors.add(issue(f)));
        List<Issue> capped = errors.size() > maxIssues ? errors.subList(0, maxIssues) : errors;
        return new Outcome(List.copyOf(capped), errors.isEmpty() ? references.warnings() : List.of());
    }

    /**
     * Validates content of an external vocabulary: syntax only (decision D30).
     *
     * @param content the uploaded bytes
     * @param name the external vocabulary name
     * @return the outcome, never with warnings
     */
    public Outcome validateSyntaxOnly(byte[] content, String name) {
        try {
            parser.parse(content, contentBase + "/external-vocabularies/" + name);
            return new Outcome(List.of(), List.of());
        } catch (TurtleParser.SyntaxError e) {
            return new Outcome(List.of(syntaxIssue(e)), List.of());
        }
    }

    private static Issue syntaxIssue(TurtleParser.SyntaxError e) {
        String message = e.getMessage() == null ? "" : ": " + e.getMessage();
        return Issue.invalidInput(
                FIELD, "Encountered invalid Turtle syntax" + message, "line " + e.line() + ", column " + e.column());
    }

    private static Issue issue(SemanticRules.Finding finding) {
        return Issue.invalidInput(FIELD, finding.detail(), finding.location());
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
