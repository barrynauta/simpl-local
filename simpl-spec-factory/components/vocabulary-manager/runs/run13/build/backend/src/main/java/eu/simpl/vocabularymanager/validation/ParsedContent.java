package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Triple;

/**
 * Turtle content after the syntax step: the triples and the prefix declarations in file order.
 *
 * @param positions every distinct triple with the event position of its first occurrence, in file order
 * @param prefixes every prefix declaration in file order
 */
public record ParsedContent(Map<Triple, Integer> positions, List<PrefixDeclaration> prefixes) {

    public ParsedContent {
        positions = Collections.unmodifiableMap(new LinkedHashMap<>(positions));
        prefixes = List.copyOf(prefixes);
    }

    /** The distinct triples in file order. */
    public List<Triple> triples() {
        return new ArrayList<>(positions.keySet());
    }

    /** Event position of a triple, for the file order of issues (D125). */
    public int position(Triple triple) {
        return positions.getOrDefault(triple, Integer.MAX_VALUE);
    }

    /**
     * One prefix declaration.
     *
     * @param position event position in the file, shared with triples, for the file order of issues
     */
    public record PrefixDeclaration(String prefix, String namespace, int position) {}
}
