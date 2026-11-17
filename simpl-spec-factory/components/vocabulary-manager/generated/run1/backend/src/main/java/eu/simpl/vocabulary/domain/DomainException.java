package eu.simpl.vocabulary.domain;

/** A business rule violation. The web layer maps the kind to an HTTP status. */
public class DomainException extends RuntimeException {

    public enum Kind { NOT_FOUND, CONFLICT }

    private final Kind kind;

    public DomainException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static DomainException conflict(String message) {
        return new DomainException(Kind.CONFLICT, message);
    }

    public static DomainException notFound(String message) {
        return new DomainException(Kind.NOT_FOUND, message);
    }
}
