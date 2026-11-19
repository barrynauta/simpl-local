package eu.simpl.vocabularymanager.validation;

import java.util.List;

/**
 * Result of validating uploaded content.
 *
 * @param errors blocking validation errors, empty when valid
 * @param warnings non-blocking warnings
 */
public record ValidationOutcome(List<ContentIssue> errors, List<ValidationWarning> warnings) {

    /**
     * @return true when there are no errors
     */
    public boolean valid() {
        return errors.isEmpty();
    }
}
