package eu.simpl.vocabularymanager.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.out.NodeFmtLib;

/**
 * A SPARQL command with bound values (D94). Values are written as RDF terms by Jena's NodeFmtLib, which escapes
 * them, and substituted in one pass, so a value is never read again as query text. Jena's
 * ParameterizedSparqlString is not used because it refuses (with an exception) any literal value that contains a
 * text like '?now' that equals a variable name, which a description may legitimately contain (REUSE.md).
 */
public final class SparqlTemplate {

    private static final Pattern VARIABLE = Pattern.compile("\\?([A-Za-z_][A-Za-z0-9_]*)");

    private final String template;
    private final Map<String, String> values = new HashMap<>();

    SparqlTemplate(String template) {
        this.template = template;
    }

    /** Binds an IRI. */
    public void setIri(String variable, String iri) {
        values.put(variable, NodeFmtLib.strNT(NodeFactory.createURI(iri)));
    }

    /** Binds a plain string literal. */
    public void setLiteral(String variable, String value) {
        values.put(variable, NodeFmtLib.strNT(NodeFactory.createLiteralString(value)));
    }

    /** Binds a typed literal. */
    public void setLiteral(String variable, Literal value) {
        values.put(variable, NodeFmtLib.strNT(value.asNode()));
    }

    /** Binds a list of terms, for a VALUES block written as { ?variable }. */
    public void setValues(String variable, Collection<? extends RDFNode> nodes) {
        StringBuilder text = new StringBuilder();
        for (RDFNode node : nodes) {
            text.append(NodeFmtLib.strNT(node.asNode())).append(' ');
        }
        values.put(variable, text.toString());
    }

    /** Binds a list of plain string literals. */
    public void setStrings(String variable, Collection<String> strings) {
        StringBuilder text = new StringBuilder();
        for (String value : strings) {
            Node node = NodeFactory.createLiteralString(value);
            text.append(NodeFmtLib.strNT(node)).append(' ');
        }
        values.put(variable, text.toString());
    }

    /** The command text with all bound variables replaced. */
    @Override
    public String toString() {
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder(template.length() + 256);
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
