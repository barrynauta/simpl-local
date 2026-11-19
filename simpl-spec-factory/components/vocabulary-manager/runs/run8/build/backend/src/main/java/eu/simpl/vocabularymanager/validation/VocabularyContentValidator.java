package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The two validation steps of D9: syntax, then semantic (S1 to S7). A syntax error stops validation with one issue;
 * otherwise every semantic rule runs and every error is returned. Error count is capped (NFR-04) at
 * {@value #MAX_ISSUES}.
 */
@Component
public class VocabularyContentValidator {

    public static final int MAX_ISSUES = 1000;

    /**
     * Validates content of an internal vocabulary version.
     *
     * @param content the uploaded bytes
     * @param vocabularyName the vocabulary being created or versioned
     * @param base the base IRI of the new version (D57)
     * @param contentBase contentBase of E2
     * @param lookup the store port for references
     * @return errors and warnings
     */
    public ValidationOutcome validate(
            byte[] content, String vocabularyName, String base, String contentBase, ReferenceLookup lookup) {
        ParsedContent parsed;
        try {
            parsed = TurtleParser.parse(content, base);
        } catch (TurtleSyntaxException e) {
            return new ValidationOutcome(List.of(new ContentIssue(e.detail(), e.location())), List.of());
        }
        List<ContentIssue> errors = new ArrayList<>(SemanticRules.check(parsed));
        List<ValidationWarning> warnings = new ArrayList<>();
        if (!parsed.triples().isEmpty() || !parsed.prefixes().isEmpty()) {
            ValidationOutcome references = ReferenceRules.check(parsed, vocabularyName, contentBase, lookup);
            errors.addAll(references.errors());
            warnings.addAll(references.warnings());
        }
        if (errors.size() > MAX_ISSUES) {
            errors = new ArrayList<>(errors.subList(0, MAX_ISSUES));
        }
        return new ValidationOutcome(List.copyOf(errors), List.copyOf(warnings));
    }

    /**
     * Validates content of an external vocabulary: syntax only, and not empty (D30, D70).
     *
     * @param content the uploaded bytes
     * @param base the base IRI
     * @return the errors, at most one
     */
    public List<ContentIssue> validateExternal(byte[] content, String base) {
        try {
            ParsedContent parsed = TurtleParser.parse(content, base);
            if (parsed.triples().isEmpty()) {
                return List.of(new ContentIssue(SemanticRules.S6, "document"));
            }
            return List.of();
        } catch (TurtleSyntaxException e) {
            return List.of(new ContentIssue(e.detail(), e.location()));
        }
    }
}
