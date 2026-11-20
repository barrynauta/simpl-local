package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.util.Map;
import org.apache.jena.graph.Node;

/**
 * One solution of a SELECT query.
 *
 * @param values bound variables
 */
public record Row(Map<String, Node> values) {

    /** Whether the variable is bound. */
    public boolean has(String variable) {
        return values.get(variable) != null;
    }

    /** Lexical form of a literal or the IRI, or null when unbound. */
    public String string(String variable) {
        Node node = values.get(variable);
        if (node == null) {
            return null;
        }
        return node.isLiteral() ? node.getLiteralLexicalForm() : node.toString();
    }

    /** Integer value of a literal. */
    public int integer(String variable) {
        return Integer.parseInt(string(variable));
    }

    /** Long value of a literal. */
    public long longValue(String variable) {
        return Long.parseLong(string(variable));
    }

    /** Instant of an xsd:dateTime literal. */
    public Instant instant(String variable) {
        String value = string(variable);
        return value == null ? null : Instant.parse(value);
    }
}
