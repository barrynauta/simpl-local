package eu.simpl.vocabularymanager.content;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The two validation steps of D9 on POST /vocabularies and POST .../versions: syntax, then
 * semantic (S1 to S7). Errors give the 400 validation report (D10, D14, D125); warnings are
 * returned to the caller.
 */
@Component
public class ContentValidator {

    public static final String FIELD = "vocabularyFile";
    static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax.";

    private final TurtleParser parser;
    private final SemanticRules rules;
    private final ReferenceChecker references;
    private final int maxIssues;

    public ContentValidator(
            TurtleParser parser, SemanticRules rules, ReferenceChecker references, AppProperties properties) {
        this.parser = parser;
        this.rules = rules;
        this.references = references;
        this.maxIssues = properties.limits().maxIssues();
    }

    /**
     * Validates a vocabulary version's content.
     *
     * @param base the content URL of the new version (D57)
     * @param ownName the vocabulary being created or versioned
     * @return the warnings (D10), empty when there are none
     * @throws ApiException 400 with the validation report when there are errors
     */
    public List<ValidationWarning> validate(byte[] content, String base, String ownName) {
        TurtleDocument document = parseOrReject(content, base);
        List<ContentIssue> errors = new ArrayList<>(rules.check(document));
        ReferenceChecker.Outcome outcome = document.triples().isEmpty()
                ? new ReferenceChecker.Outcome(List.of(), List.of())
                : references.check(document, ownName);
        errors.addAll(outcome.errors());
        if (!errors.isEmpty()) {
            errors.sort(Comparator.comparingInt(ContentIssue::sequence));
            throw report(errors);
        }
        return outcome.warnings();
    }

    /**
     * Syntax check only, for external vocabularies (D30, D70, D124).
     *
     * @throws ApiException 400 on a syntax error or a file without statements
     */
    public void validateSyntaxAndNotEmpty(byte[] content, String base) {
        TurtleDocument document = parseOrReject(content, base);
        if (document.triples().isEmpty()) {
            throw report(List.of(new ContentIssue(SemanticRules.S6, "document", 0)));
        }
    }

    private TurtleDocument parseOrReject(byte[] content, String base) {
        try {
            return parser.parse(content, base);
        } catch (TurtleSyntaxException ex) {
            String detail = SYNTAX_DETAIL + " " + ex.getMessage();
            throw report(List.of(new ContentIssue(detail.trim(), ex.location(), 0)));
        }
    }

    private ApiException report(List<ContentIssue> errors) {
        boolean truncated = errors.size() > maxIssues;
        List<Issue> issues = errors.stream()
                .limit(maxIssues)
                .map(e -> Issue.invalidInput(FIELD, e.detail(), e.location()))
                .toList();
        return new ApiException(ProblemType.BAD_REQUEST, "The input message is incorrect", issues, Map.of(), truncated);
    }
}
