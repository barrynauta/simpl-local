package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.api.request.MultipartForm;
import eu.simpl.vocabulary.domain.validation.ParsedContent;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer;
import eu.simpl.vocabulary.domain.validation.SyntaxError;
import eu.simpl.vocabulary.domain.validation.TurtleValidator;
import eu.simpl.vocabulary.domain.validation.ValidationError;
import eu.simpl.vocabulary.domain.validation.ValidationWarning;
import eu.simpl.vocabulary.store.VocabularyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * The two validation steps of D9 on vocabulary content: syntax, then semantic (S1 to S7), with the warnings
 * of D10 to D12. Errors become one 400 validation report (D14); warnings are returned to the caller.
 */
public class ContentValidation {

    private final TurtleValidator turtle;
    private final ReferenceAnalyzer references;
    private final VocabularyStore store;
    private final String contentBaseUrl;

    public ContentValidation(TurtleValidator turtle, VocabularyStore store, String contentBaseUrl) {
        this.turtle = turtle;
        this.store = store;
        this.contentBaseUrl = contentBaseUrl;
        this.references = new ReferenceAnalyzer(contentBaseUrl);
    }

    /**
     * Validates internal vocabulary content.
     *
     * @param name vocabulary name
     * @param version number the new version will get (base IRI, D57)
     * @param bytes the content
     * @return the warnings
     * @throws eu.simpl.vocabulary.api.problem.ApiException 400 validation report when errors are found
     */
    public List<ValidationWarning> validateInternal(String name, long version, byte[] bytes) {
        ParsedContent parsed = parseOrReject(bytes, baseIri(name, version));
        List<ValidationError> errors = new ArrayList<>(turtle.semanticErrors(parsed));
        List<ReferenceAnalyzer.Reference> classified =
                references.classify(parsed.prefixes(), name, TurtleValidator.subjectIris(parsed.graph()));
        List<ValidationWarning> warnings = List.of();
        if (!classified.isEmpty()) {
            boolean needExternal = classified.stream().anyMatch(ReferenceAnalyzer.External.class::isInstance);
            ReferenceAnalyzer.ReferenceFacts facts =
                    store.referenceFacts(ReferenceAnalyzer.internalNames(classified), needExternal);
            ReferenceAnalyzer.Outcome outcome = references.check(classified, facts);
            errors.addAll(outcome.errors());
            warnings = outcome.warnings();
        }
        if (!errors.isEmpty()) {
            throw report(errors);
        }
        return warnings;
    }

    /**
     * Validates external vocabulary content: syntax only, and it must hold statements (D30, D70).
     *
     * @param name external vocabulary name
     * @param bytes the content
     */
    public void validateExternal(String name, byte[] bytes) {
        ParsedContent parsed = parseOrReject(bytes, contentBaseUrl + "/external-vocabularies/" + name);
        if (parsed.graph().isEmpty()) {
            throw report(List.of(new ValidationError(TurtleValidator.S6_DETAIL, "document")));
        }
    }

    private ParsedContent parseOrReject(byte[] bytes, String baseIri) {
        try {
            return turtle.parse(bytes, baseIri);
        } catch (SyntaxError e) {
            throw report(List.of(e.error()));
        }
    }

    private String baseIri(String name, long version) {
        return contentBaseUrl + "/content/" + name + "/versions/" + version;
    }

    private static eu.simpl.vocabulary.api.problem.ApiException report(List<ValidationError> errors) {
        return Problems.badRequest(errors.stream()
                .map(e -> Issue.invalidInput(MultipartForm.FILE_FIELD, e.detail(), e.location()))
                .toList());
    }
}
