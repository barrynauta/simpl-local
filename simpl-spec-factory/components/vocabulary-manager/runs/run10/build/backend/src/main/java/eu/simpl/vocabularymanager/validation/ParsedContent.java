package eu.simpl.vocabularymanager.validation;

import java.util.List;
import org.apache.jena.graph.Graph;

/** Uploaded content that passed syntax validation: its triples and its prefix declarations in document order. */
public record ParsedContent(Graph graph, List<PrefixDeclaration> prefixes) {

    /** One @prefix or PREFIX declaration (a vocabulary reference, D12). */
    public record PrefixDeclaration(String prefix, String namespace) {

        /** The location text of D65: prefix 'p' -> 'namespace'. */
        public String location() {
            return "prefix '" + prefix + "' -> '" + namespace + "'";
        }
    }
}
