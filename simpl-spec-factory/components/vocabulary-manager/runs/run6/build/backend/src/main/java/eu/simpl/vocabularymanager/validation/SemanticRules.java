package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Component;

/**
 * Semantic rules S1 to S6 of decision D9 (S7, the references, is in {@link ReferenceChecker}). Every rule runs
 * and every finding is returned, in rule order and then by location, so the report is stable.
 */
@Component
public class SemanticRules {

    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    /**
     * One semantic finding.
     *
     * @param detail the decided detail text
     * @param location where in the content
     */
    public record Finding(String detail, String location) {}

    /**
     * Runs rules S1 to S6.
     *
     * @param graph the parsed content
     * @return the findings, in rule order
     */
    public List<Finding> check(Graph graph) {
        List<Finding> findings = new ArrayList<>();
        if (graph.isEmpty()) {
            findings.add(new Finding(S6, "document"));
            return findings;
        }
        Set<Node> objectProperties = subjectsTypedAs(graph, OWL.ObjectProperty.asNode());
        Set<Node> datatypeProperties = subjectsTypedAs(graph, OWL.DatatypeProperty.asNode());
        findings.addAll(literalForObjectProperty(graph, objectProperties));
        findings.addAll(resourceForDatatypeProperty(graph, datatypeProperties));
        findings.addAll(bothKinds(objectProperties, datatypeProperties));
        findings.addAll(duplicatePreferredLabels(graph));
        findings.addAll(disjointClassInstances(graph));
        return findings;
    }

    private static Set<Node> subjectsTypedAs(Graph graph, Node type) {
        Set<Node> result = new HashSet<>();
        graph.find(Node.ANY, RDF.type.asNode(), type).forEachRemaining(t -> result.add(t.getSubject()));
        return result;
    }

    private static List<Finding> literalForObjectProperty(Graph graph, Set<Node> objectProperties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : objectProperties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(t -> {
                if (t.getObject().isLiteral()) {
                    locations.add(statement(t));
                }
            });
        }
        return locations.stream().map(l -> new Finding(S1, l)).toList();
    }

    private static List<Finding> resourceForDatatypeProperty(Graph graph, Set<Node> datatypeProperties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : datatypeProperties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(t -> {
                if (!t.getObject().isLiteral()) {
                    locations.add(statement(t));
                }
            });
        }
        return locations.stream().map(l -> new Finding(S2, l)).toList();
    }

    private static List<Finding> bothKinds(Set<Node> objectProperties, Set<Node> datatypeProperties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : objectProperties) {
            if (property.isURI() && datatypeProperties.contains(property)) {
                locations.add(resource(property));
            }
        }
        return locations.stream().map(l -> new Finding(S3, l)).toList();
    }

    private static List<Finding> duplicatePreferredLabels(Graph graph) {
        Map<Node, Map<String, Integer>> counts = new HashMap<>();
        graph.find(Node.ANY, SKOS.prefLabel.asNode(), Node.ANY).forEachRemaining(t -> {
            if (t.getObject().isLiteral()) {
                // Decision D53: an untagged literal counts as one language, the empty tag.
                String language = t.getObject().getLiteralLanguage();
                counts.computeIfAbsent(t.getSubject(), s -> new HashMap<>()).merge(language, 1, Integer::sum);
            }
        });
        TreeSet<String> locations = new TreeSet<>();
        counts.forEach((subject, perLanguage) -> perLanguage.forEach((language, count) -> {
            if (count > 1) {
                String suffix = language.isEmpty() ? " (no language)" : " (language '" + language + "')";
                locations.add(resource(subject) + suffix);
            }
        }));
        return locations.stream().map(l -> new Finding(S4, l)).toList();
    }

    private static List<Finding> disjointClassInstances(Graph graph) {
        List<Node[]> disjointPairs = new ArrayList<>();
        graph.find(Node.ANY, OWL.disjointWith.asNode(), Node.ANY)
                .forEachRemaining(t -> disjointPairs.add(new Node[] {t.getSubject(), t.getObject()}));
        TreeSet<String> locations = new TreeSet<>();
        for (Node[] pair : disjointPairs) {
            if (pair[0].equals(pair[1])) {
                continue;
            }
            graph.find(Node.ANY, RDF.type.asNode(), pair[0]).forEachRemaining(t -> {
                if (graph.contains(t.getSubject(), RDF.type.asNode(), pair[1])) {
                    locations.add(resource(t.getSubject()));
                }
            });
        }
        return locations.stream().map(l -> new Finding(S5, l)).toList();
    }

    private static String statement(Triple t) {
        return "statement '" + text(t.getSubject()) + " " + text(t.getPredicate()) + " " + text(t.getObject()) + "'";
    }

    private static String resource(Node node) {
        return "resource '" + text(node) + "'";
    }

    private static String text(Node node) {
        if (node.isURI()) {
            return node.getURI();
        }
        if (node.isBlank()) {
            return "_:" + node.getBlankNodeLabel();
        }
        if (node.isLiteral()) {
            return node.getLiteralLexicalForm();
        }
        return node.toString();
    }
}
