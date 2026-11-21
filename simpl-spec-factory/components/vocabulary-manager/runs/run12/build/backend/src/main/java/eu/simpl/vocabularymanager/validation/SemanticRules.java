package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

/**
 * Semantic rules S1 to S6 of decision D9 (S7 is the reference check). All rules run and every error
 * is returned, each with a position for file order (D125) and the location text of D65.
 */
public final class SemanticRules {

    public static final String S1 = "Literal value for object property";
    public static final String S2 = "Resource value for datatype property";
    public static final String S3 = "Property declared as both object and datatype property";
    public static final String S4 = "Duplicate preferred label for the same language";
    public static final String S5 = "Resource is an instance of disjoint classes";
    public static final String S6 = "Vocabulary contains no statements";

    /**
     * One validation error.
     *
     * @param detail rule text
     * @param location where (D65)
     * @param position position in the file, for ordering
     */
    public record Finding(String detail, String location, int position) {}

    private SemanticRules() {}

    public static List<Finding> check(ParsedTurtle parsed) {
        List<Finding> out = new ArrayList<>();
        List<Triple> triples = parsed.triples();
        List<Integer> pos = parsed.triplePositions();
        if (triples.isEmpty()) {
            out.add(new Finding(S6, "document", 0));
            return out;
        }
        Node type = RDF.type.asNode();
        Node objectProperty = OWL.ObjectProperty.asNode();
        Node datatypeProperty = OWL.DatatypeProperty.asNode();
        Node disjointWith = OWL.disjointWith.asNode();
        Node prefLabel = SKOS.prefLabel.asNode();

        Set<Node> objectProps = new HashSet<>();
        Set<Node> datatypeProps = new HashSet<>();
        Map<Node, Integer> s3Position = new LinkedHashMap<>();
        Map<Node, Set<Node>> disjoint = new HashMap<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple t = triples.get(i);
            Node p = t.getPredicate();
            if (p.equals(type)) {
                Node o = t.getObject();
                if (o.equals(objectProperty)) {
                    objectProps.add(t.getSubject());
                    if (datatypeProps.contains(t.getSubject())) {
                        s3Position.putIfAbsent(t.getSubject(), pos.get(i));
                    }
                } else if (o.equals(datatypeProperty)) {
                    datatypeProps.add(t.getSubject());
                    if (objectProps.contains(t.getSubject())) {
                        s3Position.putIfAbsent(t.getSubject(), pos.get(i));
                    }
                }
            } else if (p.equals(disjointWith)) {
                disjoint.computeIfAbsent(t.getSubject(), k -> new HashSet<>()).add(t.getObject());
                disjoint.computeIfAbsent(t.getObject(), k -> new HashSet<>()).add(t.getSubject());
            }
        }
        for (Map.Entry<Node, Integer> e : s3Position.entrySet()) {
            out.add(new Finding(S3, "resource '" + term(e.getKey()) + "'", e.getValue()));
        }

        Map<Node, Map<String, Integer>> labelCount = new HashMap<>();
        Map<String, Integer> s4Done = new LinkedHashMap<>();
        Map<Node, List<Node>> typesOf = new HashMap<>();
        Map<Node, Integer> s5Position = new LinkedHashMap<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple t = triples.get(i);
            Node s = t.getSubject();
            Node p = t.getPredicate();
            Node o = t.getObject();
            if (objectProps.contains(p) && o.isLiteral()) {
                out.add(new Finding(S1, statement(t), pos.get(i)));
            }
            if (datatypeProps.contains(p) && (o.isURI() || o.isBlank())) {
                out.add(new Finding(S2, statement(t), pos.get(i)));
            }
            if (p.equals(prefLabel) && o.isLiteral()) {
                String lang = o.getLiteralLanguage();
                String key = lang == null || lang.isEmpty() ? "none" : lang.toLowerCase(Locale.ROOT);
                Map<String, Integer> perLang = labelCount.computeIfAbsent(s, k -> new HashMap<>());
                int n = perLang.merge(key, 1, Integer::sum);
                String doneKey = term(s) + "\u0000" + key;
                if (n == 2 && !s4Done.containsKey(doneKey)) {
                    s4Done.put(doneKey, pos.get(i));
                    out.add(new Finding(S4, "resource '" + term(s) + "', language '" + key + "'", pos.get(i)));
                }
            }
            if (p.equals(type) && !disjoint.isEmpty()) {
                List<Node> types = typesOf.computeIfAbsent(s, k -> new ArrayList<>());
                if (!s5Position.containsKey(s)) {
                    Set<Node> dis = disjoint.get(o);
                    if (dis != null) {
                        for (Node other : types) {
                            if (dis.contains(other)) {
                                s5Position.put(s, pos.get(i));
                                out.add(new Finding(S5, "resource '" + term(s) + "'", pos.get(i)));
                                break;
                            }
                        }
                    }
                }
                types.add(o);
            }
        }
        return out;
    }

    /**
     * statement '&lt;s&gt; &lt;p&gt; &lt;o&gt;' with full IRIs and an N-Triples literal (D65, D140).
     */
    static String statement(Triple t) {
        return "statement '" + term(t.getSubject()) + " " + term(t.getPredicate()) + " " + term(t.getObject()) + "'";
    }

    static String term(Node n) {
        if (n.isURI()) {
            return n.getURI();
        }
        if (n.isBlank()) {
            return "_:" + n.getBlankNodeLabel();
        }
        if (n.isLiteral()) {
            StringBuilder sb = new StringBuilder("\"");
            for (char c : n.getLiteralLexicalForm().toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append(c);
                }
            }
            sb.append('"');
            String lang = n.getLiteralLanguage();
            if (lang != null && !lang.isEmpty()) {
                sb.append('@').append(lang);
            } else {
                String dt = n.getLiteralDatatypeURI();
                if (dt != null && !dt.equals("http://www.w3.org/2001/XMLSchema#string")) {
                    sb.append("^^<").append(dt).append('>');
                }
            }
            return sb.toString();
        }
        return n.toString();
    }
}
