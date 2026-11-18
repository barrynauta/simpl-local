package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.ValidationWarning;
import eu.simpl.vocabularymanager.error.Issue;
import java.util.List;

/** Errors block creation; warnings are returned with the created version (decision D10). */
public record ValidationOutcome(List<Issue> errors, List<ValidationWarning> warnings) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
