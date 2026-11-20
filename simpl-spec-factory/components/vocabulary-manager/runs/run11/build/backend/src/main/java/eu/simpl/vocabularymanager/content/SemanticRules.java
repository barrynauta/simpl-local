package eu.simpl.vocabularymanager.content;

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
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Component;

/**
 * Semantic validation rules S1 to S6 of D9 (refined by D53, D65, D140). All rules run and every
 * error is returned. S7 (internal references) needs the store and runs in {@link ReferenceChecker}.
 */
@Component
public class SemanticRules {

    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    /** Runs S1 to S6 and returns the errors in file order. */
    public List<ContentIssue> check(TurtleDocument document) {
        List<Triple> triples = document.triples();
        List<ContentIssue> issues = new ArrayList<>();
        if (triples.isEmpty()) {
            issues.add(new ContentIssue(S6, "document", 0));
            return issues;
        }
        Map<Node, Integer> objectProperties = declared(triples, OWL.ObjectProperty.asNode());
        Map<Node, Integer> datatypeProperties = declared(triples, OWL.DatatypeProperty.asNode());
        Map<Integer, Integer> sequenceOfTriple = sequences(document);

        checkPropertyValues(triples, objectProperties, datatypeProperties, sequenceOfTriple, issues);
        checkDoubleDeclarations(objectProperties, datatypeProperties, sequenceOfTriple, issues);
        checkPreferredLabels(triples, sequenceOfTriple, issues);
        checkDisjointTypes(triples, sequenceOfTriple, issues);
        issues.sort((a, b) -> Integer.compare(a.sequence(), b.sequence()));
        return issues;
    }

    private static void checkPropertyValues(
            List<Triple> triples,
            Map<Node, Integer> objectProperties,
            Map<Node, Integer> datatypeProperties,
            Map<Integer, Integer> sequenceOfTriple,
            List<ContentIssue> issues) {
        for (int i = 0; i < triples.size(); i++) {
            Triple t = triples.get(i);
            Node p = t.getPredicate();
            Node o = t.getObject();
            if (objectProperties.containsKey(p) && o.isLiteral()) {
                issues.add(new ContentIssue(S1, statement(t), sequenceOfTriple.get(i)));
            }
            if (datatypeProperties.containsKey(p) && (o.isURI() || o.isBlank())) {
                issues.add(new ContentIssue(S2, statement(t), sequenceOfTriple.get(i)));
            }
        }
    }

    private static void checkDoubleDeclarations(
            Map<Node, Integer> objectProperties,
            Map<Node, Integer> datatypeProperties,
            Map<Integer, Integer> sequenceOfTriple,
            List<ContentIssue> issues) {
        for (Map.Entry<Node, Integer> entry : objectProperties.entrySet()) {
            Integer other = datatypeProperties.get(entry.getKey());
            if (other != null) {
                int at = Math.max(entry.getValue(), other);
                issues.add(new ContentIssue(S3, resource(entry.getKey()), sequenceOfTriple.get(at)));
            }
        }
    }

    private static void checkPreferredLabels(
            List<Triple> triples, Map<Integer, Integer> sequenceOfTriple, List<ContentIssue> issues) {
        Map<List<Object>, Integer> seen = new HashMap<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple t = triples.get(i);
            if (!t.getPredicate().equals(SKOS.prefLabel.asNode())
                    || !t.getObject().isLiteral()) {
                continue;
            }
            String language = t.getObject().getLiteralLanguage();
            String key = language == null || language.isEmpty() ? "" : language.toLowerCase(Locale.ROOT);
            List<Object> slot = List.of(t.getSubject(), key);
            int count = seen.merge(slot, 1, Integer::sum);
            if (count == 2) {
                String shown = key.isEmpty() ? "none" : language;
                issues.add(new ContentIssue(
                        S4, resource(t.getSubject()) + ", language '" + shown + "'", sequenceOfTriple.get(i)));
            }
        }
    }

    private static void checkDisjointTypes(
            List<Triple> triples, Map<Integer, Integer> sequenceOfTriple, List<ContentIssue> issues) {
        Set<List<Node>> disjoint = new HashSet<>();
        for (Triple t : triples) {
            if (t.getPredicate().equals(OWL.disjointWith.asNode())) {
                disjoint.add(List.of(t.getSubject(), t.getObject()));
                disjoint.add(List.of(t.getObject(), t.getSubject()));
            }
        }
        if (disjoint.isEmpty()) {
            return;
        }
        Map<Node, List<Node>> types = new LinkedHashMap<>();
        Set<List<Node>> reported = new HashSet<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple t = triples.get(i);
            if (!t.getPredicate().equals(RDF.type.asNode())) {
                continue;
            }
            List<Node> known = types.computeIfAbsent(t.getSubject(), k -> new ArrayList<>());
            for (Node earlier : known) {
                if (disjoint.contains(List.of(earlier, t.getObject()))
                        && reported.add(List.of(t.getSubject(), earlier, t.getObject()))) {
                    issues.add(new ContentIssue(S5, resource(t.getSubject()), sequenceOfTriple.get(i)));
                }
            }
            known.add(t.getObject());
        }
    }

    /** Properties declared with the given owl type, with the index of the first declaration. */
    private static Map<Node, Integer> declared(List<Triple> triples, Node type) {
        Map<Node, Integer> found = new LinkedHashMap<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple t = triples.get(i);
            if (t.getPredicate().equals(RDF.type.asNode())
                    && t.getObject().equals(type)
                    && (t.getSubject().isURI())) {
                found.putIfAbsent(t.getSubject(), i);
            }
        }
        return found;
    }

    /** Maps the index of each triple to its sequence number in file order (prefixes interleaved). */
    private static Map<Integer, Integer> sequences(TurtleDocument document) {
        Map<Integer, Integer> map = new HashMap<>();
        int prefixIndex = 0;
        int sequence = 0;
        List<TurtleDocument.PrefixDeclaration> prefixes = document.prefixes();
        for (int i = 0; i < document.triples().size(); i++) {
            while (prefixIndex < prefixes.size() && prefixes.get(prefixIndex).sequence() == sequence) {
                prefixIndex++;
                sequence++;
            }
            map.put(i, sequence);
            sequence++;
        }
        return map;
    }

    /** Location of S1 and S2: full IRIs, the literal in N-Triples form (D65, D140). */
    static String statement(Triple t) {
        return "statement '" + term(t.getSubject()) + " " + term(t.getPredicate()) + " " + term(t.getObject()) + "'";
    }

    static String resource(Node node) {
        return "resource '" + term(node) + "'";
    }

    static String term(Node node) {
        if (node.isURI()) {
            return node.getURI();
        }
        if (node.isBlank()) {
            return "_:" + node.getBlankNodeLabel();
        }
        return NodeFmtLib.strNT(node);
    }
}
