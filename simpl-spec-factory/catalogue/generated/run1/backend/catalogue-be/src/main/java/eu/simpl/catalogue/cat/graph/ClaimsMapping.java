package eu.simpl.catalogue.cat.graph;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * D85 (to validate): how the claims of one resource description become a property graph.
 * <ul>
 *   <li>Every IRI or blank node of the claims graph is one node with label {@code Resource}, scoped to the RD:
 *       the same IRI in two RDs gives two nodes. Property {@code uri} holds the IRI (absent for blank nodes),
 *       {@code claimsGraphUri} a one-item list with the RD id.</li>
 *   <li>Each {@code rdf:type} adds a label: the local name of the type IRI.</li>
 *   <li>A literal object becomes a node property named after the local name of the predicate. Integers become
 *       numbers, decimals and doubles become floats, booleans stay booleans, anything else is its lexical form.
 *       Several values give a list.</li>
 *   <li>An IRI or blank node object becomes a relationship from subject to object, typed with the local name
 *       of the predicate, with property {@code claimsGraphUri}.</li>
 * </ul>
 * The local name is the part after the last '#', or else after the last '/' or ':'.
 */
public final class ClaimsMapping {
    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String BASE_LABEL = "Resource";

    public record GraphNode(String key, Set<String> labels, Map<String, Object> props) {}

    public record GraphRel(String from, String type, String to) {}

    public record Mapped(List<GraphNode> nodes, List<GraphRel> rels) {}

    private ClaimsMapping() {}

    public static Mapped map(Graph claims, String graphUri) {
        Map<String, GraphNode> nodes = new LinkedHashMap<>();
        Map<String, Map<String, List<Object>>> literals = new LinkedHashMap<>();
        List<GraphRel> rels = new ArrayList<>();
        for (Triple t : claims.find().toList()) {
            GraphNode s = node(nodes, t.getSubject(), graphUri);
            String p = t.getPredicate().getURI();
            Node o = t.getObject();
            if (RDF_TYPE.equals(p) && o.isURI()) {
                s.labels().add(localName(o.getURI()));
            } else if (o.isLiteral()) {
                literals.computeIfAbsent(s.key(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(localName(p), k -> new ArrayList<>()).add(value(o));
            } else {
                GraphNode target = node(nodes, o, graphUri);
                rels.add(new GraphRel(s.key(), localName(p), target.key()));
            }
        }
        literals.forEach((key, props) -> {
            Map<String, Object> target = nodes.get(key).props();
            props.forEach((name, values) -> {
                if (target.containsKey(name)) return; // "uri" and "claimsGraphUri" are reserved
                target.put(name, values.size() == 1 ? values.get(0) : homogeneous(values));
            });
        });
        return new Mapped(new ArrayList<>(nodes.values()), rels);
    }

    private static GraphNode node(Map<String, GraphNode> nodes, Node n, String graphUri) {
        String key = n.isURI() ? n.getURI() : "_:" + n.getBlankNodeLabel();
        return nodes.computeIfAbsent(key, k -> {
            Map<String, Object> props = new LinkedHashMap<>();
            if (n.isURI()) props.put("uri", n.getURI());
            props.put("claimsGraphUri", List.of(graphUri));
            Set<String> labels = new LinkedHashSet<>();
            labels.add(BASE_LABEL);
            return new GraphNode(k, labels, props);
        });
    }

    static String localName(String iri) {
        int i = iri.lastIndexOf('#');
        if (i < 0) i = Math.max(iri.lastIndexOf('/'), iri.lastIndexOf(':'));
        String name = i >= 0 && i < iri.length() - 1 ? iri.substring(i + 1) : iri;
        return name.isBlank() ? iri : name;
    }

    private static Object value(Node lit) {
        String lex = lit.getLiteralLexicalForm();
        String dt = lit.getLiteralDatatypeURI();
        if (dt == null) return lex;
        try {
            if (dt.equals(XSDDatatype.XSDboolean.getURI())) return Boolean.parseBoolean(lex.trim());
            if (dt.equals(XSDDatatype.XSDinteger.getURI()) || dt.equals(XSDDatatype.XSDint.getURI())
                    || dt.equals(XSDDatatype.XSDlong.getURI()) || dt.equals(XSDDatatype.XSDshort.getURI())
                    || dt.equals(XSDDatatype.XSDnonNegativeInteger.getURI())
                    || dt.equals(XSDDatatype.XSDpositiveInteger.getURI())) {
                return Long.parseLong(lex.trim());
            }
            if (dt.equals(XSDDatatype.XSDdecimal.getURI()) || dt.equals(XSDDatatype.XSDdouble.getURI())
                    || dt.equals(XSDDatatype.XSDfloat.getURI())) {
                return Double.parseDouble(lex.trim());
            }
        } catch (NumberFormatException e) {
            return lex;
        }
        return lex;
    }

    /** Neo4j lists must hold one type: mixed values become strings. */
    private static List<Object> homogeneous(List<Object> values) {
        Class<?> first = values.get(0).getClass();
        if (values.stream().allMatch(v -> v.getClass() == first)) return values;
        return values.stream().map(v -> (Object) String.valueOf(v)).toList();
    }
}
