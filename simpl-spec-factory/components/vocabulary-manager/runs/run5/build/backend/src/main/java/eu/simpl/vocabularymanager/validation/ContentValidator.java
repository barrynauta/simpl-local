package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Runs the two validation steps of D9: syntax, then semantic (S1 to S7). */
@Component
public class ContentValidator {

    private final TurtleParser parser;
    private final SemanticRules rules;
    private final ReferenceChecker references;

    public ContentValidator(TurtleParser parser, SemanticRules rules, ReferenceChecker references) {
        this.parser = parser;
        this.rules = rules;
        this.references = references;
    }

    /**
     * Validates content of a new vocabulary version.
     *
     * @param content uploaded bytes
     * @param baseIri content URL of the new version (D57)
     * @param context reference lookups
     * @return the warnings, possibly empty
     * @throws ContentValidationException when there is at least one error
     */
    public List<ValidationWarning> validate(byte[] content, String baseIri, ReferenceContext context) {
        ParsedContent parsed = parser.parse(content, baseIri);
        List<ValidationError> errors = new ArrayList<>(rules.check(parsed.graph()));
        List<ValidationWarning> warnings = new ArrayList<>();
        references.check(parsed, context, errors, warnings);
        if (!errors.isEmpty()) {
            throw new ContentValidationException(errors);
        }
        return warnings;
    }

    /**
     * Syntax check only, for external vocabulary registration (D30).
     *
     * @param content uploaded bytes
     * @param baseIri base for relative IRIs
     * @throws ContentValidationException when the content is not valid Turtle
     */
    public void checkSyntax(byte[] content, String baseIri) {
        parser.parse(content, baseIri);
    }
}
