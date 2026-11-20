package eu.simpl.vocabularymanager.validation;

/**
 * A namespace declared with @prefix or PREFIX in uploaded content (D12).
 *
 * @param prefix the prefix label, empty for the default prefix
 * @param namespace the namespace IRI as declared (resolved against the base)
 */
public record PrefixDeclaration(String prefix, String namespace) {

    /** Location text of D65: prefix 'p' -> 'namespace'. */
    public String location() {
        return "prefix '" + prefix + "' -> '" + namespace + "'";
    }
}
