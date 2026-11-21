package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.ReferenceState;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.ExternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.InternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalyzer.Reference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The semantic step of D9 with the reference check (S7, D11, D12, D47): all rules run, all errors are
 * reported in file order, at most 1000 (D125). Without errors the warnings are returned.
 */
public final class ContentValidator {

    public static final int MAX_ISSUES = 1000;

    private ContentValidator() {}

    /**
     * Checks parsed content against the references the store knows.
     *
     * @return the warnings when there is no error
     * @throws ApiException 400 VocabularyValidationProblem with every error
     */
    public static List<ValidationWarning> validate(
            ParsedContent content, List<Reference> references, ReferenceState state) {
        List<Finding> errors = new ArrayList<>(SemanticRules.check(content));
        List<ValidationWarning> warnings = new ArrayList<>();
        for (Reference reference : references) {
            if (reference instanceof InternalReference internal) {
                checkInternal(internal, state.internal().get(internal.name()), errors, warnings);
            } else if (reference instanceof ExternalReference external
                    && !state.registeredExternalKeys().contains(external.key())) {
                warnings.add(new ValidationWarning(
                        ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '"
                                + external.declaration().namespace()
                                + "' because no cached external vocabulary is registered.",
                        external.location()));
            }
        }
        if (!errors.isEmpty()) {
            throw report(errors);
        }
        return warnings;
    }

    private static void checkInternal(
            InternalReference reference,
            ReferenceState.InternalState vocabulary,
            List<Finding> errors,
            List<ValidationWarning> warnings) {
        int position = reference.declaration().position();
        String name = reference.name();
        if (reference.versionText() != null && reference.version() == null) {
            errors.add(new Finding(position, missingVersion(name, reference.versionText()), reference.location()));
            return;
        }
        if (vocabulary == null) {
            String detail = reference.versionText() == null
                    ? "Referenced internal vocabulary '" + name + "' does not exist."
                    : missingVersion(name, reference.versionText());
            errors.add(new Finding(position, detail, reference.location()));
            return;
        }
        int version = reference.version() == null ? vocabulary.latestVersion() : reference.version();
        VocabularyStatus versionStatus = vocabulary.versions().get(version);
        if (versionStatus == null) {
            errors.add(new Finding(position, missingVersion(name, reference.versionText()), reference.location()));
            return;
        }
        boolean vocabularyDeprecated = vocabulary.status() == VocabularyStatus.DEPRECATED;
        if (versionStatus == VocabularyStatus.DEPRECATED || vocabularyDeprecated) {
            warnings.add(new ValidationWarning(
                    ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE,
                    deprecatedText(vocabulary, version, vocabularyDeprecated),
                    reference.location()));
        }
    }

    /** The texts of D11 and D47. */
    static String deprecatedText(ReferenceState.InternalState vocabulary, int version, boolean vocabularyDeprecated) {
        String text =
                "Referenced internal vocabulary '" + vocabulary.name() + "' version " + version + " is deprecated.";
        if (vocabularyDeprecated) {
            return text + " The vocabulary '" + vocabulary.name() + "' is deprecated.";
        }
        Integer latestActive = vocabulary.latestActiveVersion();
        if (latestActive != null && latestActive != version) {
            return text + " Use version " + latestActive + ", the latest ACTIVE version.";
        }
        return text;
    }

    private static String missingVersion(String name, String versionText) {
        return "Referenced internal vocabulary '" + name + "' version " + versionText + " does not exist.";
    }

    private static ApiException report(List<Finding> errors) {
        List<Finding> ordered = new ArrayList<>(errors);
        ordered.sort(Comparator.comparingInt(Finding::position));
        boolean truncated = ordered.size() > MAX_ISSUES;
        List<ProblemIssue> issues = ordered.stream()
                .limit(MAX_ISSUES)
                .map(finding ->
                        ProblemIssue.invalidInput("body", "vocabularyFile", finding.detail(), finding.location()))
                .toList();
        return new ApiException(ProblemType.BAD_REQUEST, Problems.BAD_REQUEST_DETAIL, issues, Map.of(), truncated);
    }
}
