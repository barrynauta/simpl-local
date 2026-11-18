package eu.simpl.vocabularymanager.validation;

import java.util.List;

/** Content failed syntax or semantic validation; carries every error found (D9, D10). */
public class ContentValidationException extends RuntimeException {

    private final transient List<ValidationError> errors;

    public ContentValidationException(List<ValidationError> errors) {
        super("Vocabulary content is not valid: " + errors.size() + " error(s)");
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> errors() {
        return errors;
    }
}
