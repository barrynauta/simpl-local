package eu.simpl.vocabularymanager.validation;

/**
 * A namespace declared with @prefix or PREFIX: a vocabulary reference (D12).
 *
 * @param prefix the prefix label, empty for the default prefix
 * @param namespace the namespace IRI, resolved against the base (D57)
 */
public record PrefixDeclaration(String prefix, String namespace) {

    /** Location text used by issues and warnings: prefix 'p' -> 'namespace'. */
    public String location() {
        return "prefix '" + prefix + "' -> '" + namespace + "'";
    }
}
