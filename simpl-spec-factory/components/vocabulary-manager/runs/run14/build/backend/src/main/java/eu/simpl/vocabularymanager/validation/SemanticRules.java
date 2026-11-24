package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.Issue;
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

/**
 * Semantic validation rules S1 to S6 of D9 with the location texts of D65, D140, D142 to D144, D181 and D199. All
 * rules run; every error is returned. Issues carry the file position for the order of the report (D125).
 */
public final class SemanticRules {

    public static final String S1 = "Literal value for object property";
    public static final String S2 = "Resource value for datatype property";
    public static final String S3 = "Property declared as both object and datatype property";
    public static final String S4 = "Duplicate preferred label for the same language";
    public static final String S5 = "Resource is an instance of disjoint classes";
    public static final String S6 = "Vocabulary contains no statements";

    private static final Node PREF_LABEL =
            org.apache.jena.graph.NodeFactory.createURI("http://www.w3.org/2004/02/skos/core#prefLabel");

    private SemanticRules() {}

    /**
     * An issue with its position in the file.
     *
     * @param position number of triples before the offending statement
     * @param issue the issue
     */
    public record PositionedIssue(int position, Issue issue) {}

    /**
     * Runs S1 to S6.
     *
     * @param content the parsed content
     * @return the issues, unsorted
     */
    public static List<PositionedIssue> check(ParsedContent content) {
        List<PositionedIssue> issues = new ArrayList<>();
        List<Triple> triples = content.triples();
        if (triples.isEmpty()) {
            issues.add(new PositionedIssue(0, error(S6, "document")));
            return issues;
        }
        Map<Node, Integer> objectProperties = declarations(triples, OWL.ObjectProperty.asNode());
        Map<Node, Integer> datatypeProperties = declarations(triples, OWL.DatatypeProperty.asNode());
        Set<String> labels = content.blankNodeLabels();
        for (int i = 0; i < triples.size(); i++) {
            Triple triple = triples.get(i);
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();
            if (objectProperties.containsKey(predicate) && object.isLiteral()) {
                issues.add(new PositionedIssue(i, error(S1, statement(triple, labels))));
            }
            if (datatypeProperties.containsKey(predicate) && (object.isURI() || object.isBlank())) {
                issues.add(new PositionedIssue(i, error(S2, statement(triple, labels))));
            }
        }
        for (Map.Entry<Node, Integer> entry : objectProperties.entrySet()) {
            Integer other = datatypeProperties.get(entry.getKey());
            if (other != null) {
                int position = Math.min(entry.getValue(), other);
                issues.add(new PositionedIssue(
                        position, error(S3, "resource '" + plain(entry.getKey(), labels) + "'")));
            }
        }
        issues.addAll(duplicateLabels(triples, labels));
        issues.addAll(disjointInstances(triples, labels));
        return issues;
    }

    private static List<PositionedIssue> duplicateLabels(List<Triple> triples, Set<String> labels) {
        List<PositionedIssue> issues = new ArrayList<>();
        Map<Node, Map<String, Set<String>>> seen = new HashMap<>();
        Set<String> reported = new HashSet<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple triple = triples.get(i);
            if (!triple.getPredicate().equals(PREF_LABEL) || !triple.getObject().isLiteral()) {
                continue;
            }
            String language = triple.getObject().getLiteralLanguage();
            String tag = language == null || language.isEmpty() ? "none" : language.toLowerCase(Locale.ROOT);
            Set<String> values = seen.computeIfAbsent(triple.getSubject(), key -> new HashMap<>())
                    .computeIfAbsent(tag, key -> new HashSet<>());
            values.add(triple.getObject().getLiteralLexicalForm());
            String key = triple.getSubject().toString() + "\u0000" + tag;
            if (values.size() > 1 && reported.add(key)) {
                String location = "resource '" + plain(triple.getSubject(), labels) + "', language '" + tag + "'";
                issues.add(new PositionedIssue(i, error(S4, location)));
            }
        }
        return issues;
    }

    private static List<PositionedIssue> disjointInstances(List<Triple> triples, Set<String> labels) {
        Map<Node, Set<Node>> disjoint = new HashMap<>();
        for (Triple triple : triples) {
            if (triple.getPredicate().equals(OWL.disjointWith.asNode())) {
                disjoint.computeIfAbsent(triple.getSubject(), key -> new HashSet<>())
                        .add(triple.getObject());
                disjoint.computeIfAbsent(triple.getObject(), key -> new HashSet<>())
                        .add(triple.getSubject());
            }
        }
        List<PositionedIssue> issues = new ArrayList<>();
        if (disjoint.isEmpty()) {
            return issues;
        }
        Map<Node, Set<Node>> types = new LinkedHashMap<>();
        Set<Node> reported = new HashSet<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple triple = triples.get(i);
            if (!triple.getPredicate().equals(RDF.type.asNode())) {
                continue;
            }
            Set<Node> known = types.computeIfAbsent(triple.getSubject(), key -> new HashSet<>());
            Set<Node> disjointWithNew = disjoint.getOrDefault(triple.getObject(), Set.of());
            boolean clash = known.stream().anyMatch(disjointWithNew::contains);
            known.add(triple.getObject());
            if (clash && reported.add(triple.getSubject())) {
                issues.add(new PositionedIssue(i, error(S5, "resource '" + plain(triple.getSubject(), labels) + "'")));
            }
        }
        return issues;
    }

    private static Map<Node, Integer> declarations(List<Triple> triples, Node kind) {
        Map<Node, Integer> declared = new LinkedHashMap<>();
        for (int i = 0; i < triples.size(); i++) {
            Triple triple = triples.get(i);
            if (triple.getPredicate().equals(RDF.type.asNode())
                    && triple.getObject().equals(kind)
                    && triple.getSubject().isURI()) {
                declared.putIfAbsent(triple.getSubject(), i);
            }
        }
        return declared;
    }

    /** The statement location of S1 and S2 in N-Triples form (D181, D140, D199). */
    static String statement(Triple triple, Set<String> labels) {
        return "statement '" + nTriples(triple.getSubject(), labels) + " " + nTriples(triple.getPredicate(), labels)
                + " " + nTriples(triple.getObject(), labels) + "'";
    }

    private static String nTriples(Node node, Set<String> labels) {
        if (node.isBlank()) {
            return blank(node, labels);
        }
        return NodeFmtLib.strNT(node);
    }

    private static String plain(Node node, Set<String> labels) {
        if (node.isBlank()) {
            return blank(node, labels);
        }
        return node.isURI() ? node.getURI() : NodeFmtLib.strNT(node);
    }

    private static String blank(Node node, Set<String> labels) {
        String label = node.getBlankNodeLabel();
        return labels.contains(label) ? "_:" + label : "[]";
    }

    private static Issue error(String detail, String location) {
        return Issue.invalidInput("body", "vocabularyFile", detail, location);
    }
}
