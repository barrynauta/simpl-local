package eu.simpl.vocabularymanager.content;

import java.util.List;
import org.apache.jena.graph.Triple;

/**
 * The parsed form of an uploaded Turtle file: the triples and the prefix declarations in file
 * order. The sequence numbers place prefixes and triples in one order, so issues can be sorted in
 * file order (D125).
 */
public record TurtleDocument(List<Triple> triples, List<PrefixDeclaration> prefixes) {

    /** One @prefix or PREFIX declaration. */
    public record PrefixDeclaration(String prefix, String namespace, int sequence) {}

    public TurtleDocument {
        triples = List.copyOf(triples);
        prefixes = List.copyOf(prefixes);
    }
}
