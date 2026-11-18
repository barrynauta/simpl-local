package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.Issue;
import java.util.List;
import org.apache.jena.graph.Graph;

/**
 * Result of the syntax step.
 *
 * @param graph the parsed triples (empty when the syntax is invalid)
 * @param prefixes the prefix declarations in file order
 * @param syntaxError the first syntax error, or null when the content parses
 */
public record ParsedTurtle(Graph graph, List<PrefixDeclaration> prefixes, Issue syntaxError) {

    public boolean isValid() {
        return syntaxError == null;
    }
}
