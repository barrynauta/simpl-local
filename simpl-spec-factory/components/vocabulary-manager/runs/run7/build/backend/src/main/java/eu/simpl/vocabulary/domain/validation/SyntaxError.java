package eu.simpl.vocabulary.domain.validation;

/** The content is not valid Turtle (D9 syntax step). Carries the first parse error. */
public class SyntaxError extends Exception {

    private static final long serialVersionUID = 1L;

    private final transient ValidationError error;

    public SyntaxError(ValidationError error) {
        super(error.detail(), null, false, false);
        this.error = error;
    }

    public ValidationError error() {
        return error;
    }
}
