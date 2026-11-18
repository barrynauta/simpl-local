package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.Issue;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Runs the two validation steps of decision D9: syntax, then semantic (S1 to S7). */
@Component
public class VocabularyValidator {

    private final TurtleSyntaxValidator syntax;
    private final SemanticRules semanticRules;
    private final ReferenceRules referenceRules;

    public VocabularyValidator(
            TurtleSyntaxValidator syntax, SemanticRules semanticRules, ReferenceRules referenceRules) {
        this.syntax = syntax;
        this.semanticRules = semanticRules;
        this.referenceRules = referenceRules;
    }

    /**
     * Validates uploaded content.
     *
     * @param content the uploaded bytes
     * @param baseIri base for relative IRIs
     * @param ownName the vocabulary being created or versioned
     * @param lookup read access to stored vocabularies
     */
    public ValidationOutcome validate(byte[] content, String baseIri, String ownName, ReferenceLookup lookup) {
        ParsedTurtle parsed = syntax.parse(content, baseIri);
        if (!parsed.isValid()) {
            return new ValidationOutcome(List.of(parsed.syntaxError()), List.of());
        }
        List<Issue> errors = new ArrayList<>(semanticRules.check(parsed.graph()));
        ReferenceRules.Result references = referenceRules.check(parsed.prefixes(), parsed.graph(), ownName, lookup);
        errors.addAll(references.errors());
        return new ValidationOutcome(List.copyOf(errors), references.warnings());
    }

    /** Syntax only, for external vocabulary registration (decision D30). */
    public ValidationOutcome validateSyntax(byte[] content, String baseIri) {
        ParsedTurtle parsed = syntax.parse(content, baseIri);
        return parsed.isValid()
                ? new ValidationOutcome(List.of(), List.of())
                : new ValidationOutcome(List.of(parsed.syntaxError()), List.of());
    }
}
