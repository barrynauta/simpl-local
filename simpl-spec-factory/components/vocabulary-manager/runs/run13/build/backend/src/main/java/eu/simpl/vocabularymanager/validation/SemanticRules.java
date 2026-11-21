package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDF;

/**
 * The semantic rules S1 to S6 of D9, with the locations of D65, D140, D142, D143 and D144. All rules run
 * and every error is returned.
 */
public final class SemanticRules {

    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    private static final String OWL = "http://www.w3.org/2002/07/owl#";
    private static final Node OBJECT_PROPERTY = NodeFactory.createURI(OWL + "ObjectProperty");
    private static final Node DATATYPE_PROPERTY = NodeFactory.createURI(OWL + "DatatypeProperty");
    private static final Node DISJOINT_WITH = NodeFactory.createURI(OWL + "disjointWith");
    private static final Node PREF_LABEL = NodeFactory.createURI("http://www.w3.org/2004/02/skos/core#prefLabel");
    private static final Node TYPE = RDF.type.asNode();

    private SemanticRules() {}

    public static List<Finding> check(ParsedContent content) {
        List<Finding> findings = new ArrayList<>();
        List<Triple> triples = content.triples();
        if (triples.isEmpty()) {
            findings.add(new Finding(-1, S6, "document"));
            return findings;
        }
        Map<Node, Integer> objectProperties = declarations(content, triples, OBJECT_PROPERTY);
        Map<Node, Integer> datatypeProperties = declarations(content, triples, DATATYPE_PROPERTY);
        propertyValues(content, triples, objectProperties, datatypeProperties, findings);
        bothKinds(objectProperties, datatypeProperties, findings);
        duplicateLabels(content, triples, findings);
        disjointInstances(content, triples, findings);
        return findings;
    }

    /** IRIs typed with this property class, with the position of their first declaration. */
    private static Map<Node, Integer> declarations(ParsedContent content, List<Triple> triples, Node kind) {
        Map<Node, Integer> declared = new LinkedHashMap<>();
        for (Triple triple : triples) {
            if (triple.getPredicate().equals(TYPE)
                    && triple.getObject().equals(kind)
                    && triple.getSubject().isURI()) {
                declared.putIfAbsent(triple.getSubject(), content.position(triple));
            }
        }
        return declared;
    }

    /** S1 and S2. */
    private static void propertyValues(
            ParsedContent content,
            List<Triple> triples,
            Map<Node, Integer> objectProperties,
            Map<Node, Integer> datatypeProperties,
            List<Finding> findings) {
        for (Triple triple : triples) {
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();
            if (objectProperties.containsKey(predicate) && object.isLiteral()) {
                findings.add(new Finding(content.position(triple), S1, statement(triple)));
            }
            if (datatypeProperties.containsKey(predicate) && (object.isURI() || object.isBlank())) {
                findings.add(new Finding(content.position(triple), S2, statement(triple)));
            }
        }
    }

    /** S3: one issue per property (D144). */
    private static void bothKinds(
            Map<Node, Integer> objectProperties, Map<Node, Integer> datatypeProperties, List<Finding> findings) {
        for (Map.Entry<Node, Integer> entry : objectProperties.entrySet()) {
            Integer other = datatypeProperties.get(entry.getKey());
            if (other != null) {
                findings.add(new Finding(
                        Math.min(entry.getValue(), other),
                        S3,
                        "resource '" + entry.getKey().getURI() + "'"));
            }
        }
    }

    /** S4: tags compared case-insensitively, untagged counts as 'none' (D53, D142). */
    private static void duplicateLabels(ParsedContent content, List<Triple> triples, List<Finding> findings) {
        Map<String, Integer> seen = new HashMap<>();
        Set<String> reported = new HashSet<>();
        for (Triple triple : triples) {
            if (!triple.getPredicate().equals(PREF_LABEL) || !triple.getObject().isLiteral()) {
                continue;
            }
            String tag = triple.getObject().getLiteralLanguage();
            String language = tag == null || tag.isEmpty() ? "none" : tag.toLowerCase(Locale.ROOT);
            String key = format(triple.getSubject()) + "\u0000" + language;
            if (seen.putIfAbsent(key, content.position(triple)) != null && reported.add(key)) {
                findings.add(new Finding(
                        content.position(triple),
                        S4,
                        "resource '" + format(triple.getSubject()) + "', language '" + language + "'"));
            }
        }
    }

    /** S5: one issue per resource (D143). */
    private static void disjointInstances(ParsedContent content, List<Triple> triples, List<Finding> findings) {
        Map<Node, Set<Node>> disjoint = new HashMap<>();
        for (Triple triple : triples) {
            if (triple.getPredicate().equals(DISJOINT_WITH)) {
                disjoint.computeIfAbsent(triple.getSubject(), k -> new HashSet<>())
                        .add(triple.getObject());
                disjoint.computeIfAbsent(triple.getObject(), k -> new HashSet<>())
                        .add(triple.getSubject());
            }
        }
        if (disjoint.isEmpty()) {
            return;
        }
        Map<Node, List<Node>> types = new LinkedHashMap<>();
        Map<Node, Integer> reportedAt = new LinkedHashMap<>();
        for (Triple triple : triples) {
            if (!triple.getPredicate().equals(TYPE)) {
                continue;
            }
            Node resource = triple.getSubject();
            List<Node> known = types.computeIfAbsent(resource, k -> new ArrayList<>());
            for (Node earlier : known) {
                Set<Node> partners = disjoint.get(earlier);
                if (partners != null && partners.contains(triple.getObject())) {
                    reportedAt.putIfAbsent(resource, content.position(triple));
                }
            }
            known.add(triple.getObject());
        }
        reportedAt.forEach(
                (resource, position) -> findings.add(new Finding(position, S5, "resource '" + format(resource) + "'")));
    }

    /** statement '&lt;subject&gt; &lt;predicate&gt; &lt;object&gt;' with full IRIs and N-Triples literals (D140). */
    static String statement(Triple triple) {
        return "statement '" + format(triple.getSubject()) + " " + format(triple.getPredicate()) + " "
                + format(triple.getObject()) + "'";
    }

    static String format(Node node) {
        if (node.isURI()) {
            return node.getURI();
        }
        if (node.isBlank()) {
            return "_:" + node.getBlankNodeLabel();
        }
        if (node.isLiteral()) {
            StringBuilder text = new StringBuilder("\"")
                    .append(escape(node.getLiteralLexicalForm()))
                    .append('"');
            String language = node.getLiteralLanguage();
            if (language != null && !language.isEmpty()) {
                text.append('@').append(language);
            } else if (!XSDDatatype.XSDstring.getURI().equals(node.getLiteralDatatypeURI())) {
                text.append("^^<").append(node.getLiteralDatatypeURI()).append('>');
            }
            return text.toString();
        }
        return node.toString();
    }

    private static String escape(String lexical) {
        StringBuilder out = new StringBuilder(lexical.length());
        for (int i = 0; i < lexical.length(); i++) {
            char c = lexical.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
