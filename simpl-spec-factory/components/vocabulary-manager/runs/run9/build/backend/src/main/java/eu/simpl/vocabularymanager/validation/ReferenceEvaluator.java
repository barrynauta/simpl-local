package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.ValidationIssue;
import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.validation.ReferenceAnalysis.ExternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceAnalysis.InternalReference;
import eu.simpl.vocabularymanager.validation.ReferenceData.ReferencedVocabulary;
import java.util.ArrayList;
import java.util.List;

/** Rule S7 and the reference warnings (D11, D12, D47, D69). Pure logic over data read beforehand. */
public final class ReferenceEvaluator {

    private ReferenceEvaluator() {}

    /**
     * Result of the evaluation.
     *
     * @param issues S7 errors
     * @param warnings reference warnings
     */
    public record Result(List<ValidationIssue> issues, List<ValidationWarning> warnings) {}

    /**
     * Evaluates the references of one upload.
     *
     * @param analysis classified references
     * @param data store data about them
     * @return errors and warnings in declaration order
     */
    public static Result evaluate(ReferenceAnalysis analysis, ReferenceData data) {
        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        for (InternalReference reference : analysis.internal()) {
            evaluateInternal(reference, data, issues, warnings);
        }
        for (ExternalReference reference : analysis.external()) {
            if (!data.registeredExternalKeys().contains(reference.key())) {
                warnings.add(new ValidationWarning(
                        ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '"
                                + reference.declaration().namespace()
                                + "' because no cached external vocabulary is registered.",
                        reference.declaration().location()));
            }
        }
        return new Result(List.copyOf(issues), List.copyOf(warnings));
    }

    private static void evaluateInternal(
            InternalReference reference,
            ReferenceData data,
            List<ValidationIssue> issues,
            List<ValidationWarning> warnings) {
        String location = reference.declaration().location();
        String name = reference.name();
        ReferencedVocabulary vocabulary = data.vocabularies().get(name);
        if (reference.versionText() == null) {
            if (vocabulary == null) {
                issues.add(
                        new ValidationIssue("Referenced internal vocabulary '" + name + "' does not exist.", location));
                return;
            }
            warnIfDeprecated(name, vocabulary.latestVersion(), vocabulary, location, warnings);
            return;
        }
        Integer number = reference.versionNumber();
        VocabularyStatus versionStatus = vocabulary == null || number == null
                ? null
                : vocabulary.versions().get(number);
        if (versionStatus == null) {
            issues.add(new ValidationIssue(
                    "Referenced internal vocabulary '" + name + "' version " + reference.versionText()
                            + " does not exist.",
                    location));
            return;
        }
        warnIfDeprecated(name, number, vocabulary, location, warnings);
    }

    private static void warnIfDeprecated(
            String name, int version, ReferencedVocabulary vocabulary, String location, List<ValidationWarning> out) {
        VocabularyStatus versionStatus = vocabulary.versions().get(version);
        boolean vocabularyDeprecated = vocabulary.status() == VocabularyStatus.DEPRECATED;
        if (!vocabularyDeprecated && versionStatus != VocabularyStatus.DEPRECATED) {
            return;
        }
        StringBuilder description = new StringBuilder("Referenced internal vocabulary '")
                .append(name)
                .append("' version ")
                .append(version)
                .append(" is deprecated.");
        if (vocabularyDeprecated) {
            description.append(" The vocabulary '").append(name).append("' is deprecated.");
        } else {
            Integer latestActive = vocabulary.latestActiveVersion();
            if (latestActive != null && latestActive != version) {
                description.append(" Use version ").append(latestActive).append(", the latest ACTIVE version.");
            }
        }
        out.add(new ValidationWarning(
                ValidationWarning.DEPRECATED_VOCABULARY_REFERENCE, description.toString(), location));
    }
}
