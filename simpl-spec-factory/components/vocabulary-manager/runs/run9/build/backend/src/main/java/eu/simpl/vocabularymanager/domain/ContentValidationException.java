package eu.simpl.vocabularymanager.domain;

import java.util.List;

/** Uploaded content failed syntax or semantic validation (D9, D10). */
public class ContentValidationException extends RuntimeException {

    private final transient List<ValidationIssue> issues;

    /** @param issues all validation errors, at least one */
    public ContentValidationException(List<ValidationIssue> issues) {
        super("Vocabulary content is not valid");
        this.issues = List.copyOf(issues);
    }

    /** @return the validation errors */
    public List<ValidationIssue> issues() {
        return issues;
    }
}
