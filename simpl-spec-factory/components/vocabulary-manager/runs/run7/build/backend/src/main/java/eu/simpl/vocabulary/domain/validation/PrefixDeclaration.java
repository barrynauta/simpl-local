package eu.simpl.vocabulary.domain.validation;

/**
 * A namespace declared with @prefix or PREFIX in uploaded content: a vocabulary reference (D12).
 *
 * @param prefix the prefix label
 * @param namespace the namespace IRI, resolved against the base IRI (D57)
 */
public record PrefixDeclaration(String prefix, String namespace) {

    /** Location text of a reference (D65). */
    public String location() {
        return "prefix '" + prefix + "' -> '" + namespace + "'";
    }
}
