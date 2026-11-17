package eu.simpl.vocabulary.validation;

/** A blocking validation error with its location (line and column, "document", a prefix or a resource). */
public record ValidationIssue(Category category, String detail, String location) {

    public enum Category { SYNTAX, STRUCTURE, SEMANTIC }
}
