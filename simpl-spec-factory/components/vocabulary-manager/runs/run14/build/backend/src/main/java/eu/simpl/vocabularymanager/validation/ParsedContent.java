package eu.simpl.vocabularymanager.validation;

import java.util.List;
import java.util.Set;
import org.apache.jena.graph.Triple;

/**
 * Uploaded Turtle content after a successful syntax check.
 *
 * @param triples the triples in file order (duplicates kept)
 * @param prefixes the prefix declarations in file order
 * @param blankNodeLabels the blank node labels written in the file (D199)
 */
public record ParsedContent(List<Triple> triples, List<PrefixDeclaration> prefixes, Set<String> blankNodeLabels) {

    /**
     * One prefix declaration (D12, D145, D171).
     *
     * @param prefix the prefix name, empty for the default prefix
     * @param declared the namespace as written in the file
     * @param resolved the namespace resolved against the base
     * @param position number of triples before the declaration, for file order
     */
    public record PrefixDeclaration(String prefix, String declared, String resolved, int position) {}
}
