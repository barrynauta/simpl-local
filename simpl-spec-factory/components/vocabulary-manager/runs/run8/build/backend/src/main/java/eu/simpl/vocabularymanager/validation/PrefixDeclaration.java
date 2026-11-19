package eu.simpl.vocabularymanager.validation;

/**
 * A namespace declared with @prefix or PREFIX: a vocabulary reference (D12).
 *
 * @param prefix the prefix label, possibly empty
 * @param namespace the namespace IRI, resolved against the base
 */
public record PrefixDeclaration(String prefix, String namespace) {

    /**
     * @return the location text of D65: prefix '&lt;prefix&gt;' -&gt; '&lt;namespace&gt;'
     */
    public String location() {
        return "prefix '" + prefix + "' -> '" + namespace + "'";
    }
}
