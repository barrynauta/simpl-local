package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.ValidationIssue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.springframework.stereotype.Component;

/** Semantic validation rules S1 to S6 of D9 (S7 is in {@link ReferenceEvaluator}). All rules run. */
@Component
public class SemanticRules {

    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    private static final Node RDF_TYPE = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    private static final Node OBJECT_PROPERTY = NodeFactory.createURI("http://www.w3.org/2002/07/owl#ObjectProperty");
    private static final Node DATATYPE_PROPERTY =
            NodeFactory.createURI("http://www.w3.org/2002/07/owl#DatatypeProperty");
    private static final Node DISJOINT_WITH = NodeFactory.createURI("http://www.w3.org/2002/07/owl#disjointWith");
    private static final Node PREF_LABEL = NodeFactory.createURI("http://www.w3.org/2004/02/skos/core#prefLabel");

    /**
     * Runs S1 to S6.
     *
     * @param graph the parsed content
     * @return every error found, in rule order
     */
    public List<ValidationIssue> check(Graph graph) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (graph.isEmpty()) {
            issues.add(new ValidationIssue(S6, "document"));
            return issues;
        }
        Set<Node> objectProperties = subjectsTyped(graph, OBJECT_PROPERTY);
        Set<Node> datatypeProperties = subjectsTyped(graph, DATATYPE_PROPERTY);
        issues.addAll(literalForObjectProperty(graph, objectProperties));
        issues.addAll(resourceForDatatypeProperty(graph, datatypeProperties));
        issues.addAll(declaredAsBoth(objectProperties, datatypeProperties));
        issues.addAll(duplicatePreferredLabels(graph));
        issues.addAll(disjointInstances(graph));
        return issues;
    }

    private static Set<Node> subjectsTyped(Graph graph, Node type) {
        Set<Node> result = new HashSet<>();
        graph.find(Node.ANY, RDF_TYPE, type).forEachRemaining(t -> result.add(t.getSubject()));
        return result;
    }

    private static List<ValidationIssue> literalForObjectProperty(Graph graph, Set<Node> properties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : properties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(t -> {
                if (t.getObject().isLiteral()) {
                    locations.add(statement(t));
                }
            });
        }
        return toIssues(S1, locations);
    }

    private static List<ValidationIssue> resourceForDatatypeProperty(Graph graph, Set<Node> properties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : properties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(t -> {
                if (t.getObject().isURI() || t.getObject().isBlank()) {
                    locations.add(statement(t));
                }
            });
        }
        return toIssues(S2, locations);
    }

    private static List<ValidationIssue> declaredAsBoth(Set<Node> objectProperties, Set<Node> datatypeProperties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : objectProperties) {
            if (datatypeProperties.contains(property)) {
                locations.add(resource(property));
            }
        }
        return toIssues(S3, locations);
    }

    private static List<ValidationIssue> duplicatePreferredLabels(Graph graph) {
        Map<Node, Map<String, Integer>> counts = new LinkedHashMap<>();
        Map<String, String> writtenTags = new LinkedHashMap<>();
        graph.find(Node.ANY, PREF_LABEL, Node.ANY).forEachRemaining(t -> {
            if (!t.getObject().isLiteral()) {
                return;
            }
            String tag = t.getObject().getLiteralLanguage();
            String key = tag == null || tag.isEmpty() ? "" : tag.toLowerCase(Locale.ROOT);
            writtenTags.putIfAbsent(key, key.isEmpty() ? "none" : tag);
            counts.computeIfAbsent(t.getSubject(), s -> new LinkedHashMap<>()).merge(key, 1, Integer::sum);
        });
        TreeSet<String> locations = new TreeSet<>();
        counts.forEach((subject, perLanguage) -> perLanguage.forEach((language, count) -> {
            if (count > 1) {
                locations.add(resource(subject) + ", language '" + writtenTags.get(language) + "'");
            }
        }));
        return toIssues(S4, locations);
    }

    private static List<ValidationIssue> disjointInstances(Graph graph) {
        Set<List<Node>> disjointPairs = new HashSet<>();
        graph.find(Node.ANY, DISJOINT_WITH, Node.ANY).forEachRemaining(t -> {
            disjointPairs.add(List.of(t.getSubject(), t.getObject()));
            disjointPairs.add(List.of(t.getObject(), t.getSubject()));
        });
        if (disjointPairs.isEmpty()) {
            return List.of();
        }
        Map<Node, List<Node>> typesPerResource = new LinkedHashMap<>();
        graph.find(Node.ANY, RDF_TYPE, Node.ANY)
                .forEachRemaining(t -> typesPerResource
                        .computeIfAbsent(t.getSubject(), s -> new ArrayList<>())
                        .add(t.getObject()));
        TreeSet<String> locations = new TreeSet<>();
        typesPerResource.forEach((resource, types) -> {
            if (hasDisjointPair(types, disjointPairs)) {
                locations.add(resource(resource));
            }
        });
        return toIssues(S5, locations);
    }

    private static boolean hasDisjointPair(List<Node> types, Set<List<Node>> disjointPairs) {
        for (int i = 0; i < types.size(); i++) {
            for (int j = i + 1; j < types.size(); j++) {
                if (disjointPairs.contains(List.of(types.get(i), types.get(j)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<ValidationIssue> toIssues(String detail, TreeSet<String> locations) {
        return locations.stream()
                .sorted(Comparator.naturalOrder())
                .map(location -> new ValidationIssue(detail, location))
                .toList();
    }

    /** Location of S1 and S2 (D65): full IRIs, the literal as written. */
    static String statement(Triple triple) {
        return "statement '" + text(triple.getSubject()) + " " + text(triple.getPredicate()) + " "
                + text(triple.getObject()) + "'";
    }

    /** Location of S3, S4 and S5 (D65). */
    static String resource(Node node) {
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
