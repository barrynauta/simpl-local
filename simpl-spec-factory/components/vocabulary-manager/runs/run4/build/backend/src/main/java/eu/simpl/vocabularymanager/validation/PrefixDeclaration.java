package eu.simpl.vocabularymanager.validation;

/** A namespace declared with @prefix or PREFIX in uploaded content: a vocabulary reference (decision D12). */
public record PrefixDeclaration(String prefix, String namespace) {

    /** The location text of warnings and errors about this reference. */
    public String location() {
        return "prefix '" + prefix + "' -> '" + namespace + "'";
    }
}
