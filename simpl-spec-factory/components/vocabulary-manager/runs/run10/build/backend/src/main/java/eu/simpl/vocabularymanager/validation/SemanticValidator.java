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
import java.util.TreeSet;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

/**
 * Semantic validation rules S1 to S6 of D9 (S7 is the reference check). All rules run and every error is returned;
 * locations follow D65. The number of returned issues is capped at {@link #MAX_ISSUES} so a response stays bounded.
 */
public class SemanticValidator {

    public static final int MAX_ISSUES = 1000;
    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    private static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
    private static final Node TYPE = uri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    private static final Node OBJECT_PROPERTY = uri(OWL_NS + "ObjectProperty");
    private static final Node DATATYPE_PROPERTY = uri(OWL_NS + "DatatypeProperty");
    private static final Node DISJOINT_WITH = uri(OWL_NS + "disjointWith");
    private static final Node PREF_LABEL = uri("http://www.w3.org/2004/02/skos/core#prefLabel");

    /** Returns the issues of S1 to S6, in rule order and, within a rule, sorted by location. */
    public List<Issue> validate(Graph graph) {
        List<Issue> issues = new ArrayList<>();
        if (graph.isEmpty()) {
            issues.add(issue(S6, "document"));
            return issues;
        }
        Set<Node> objectProperties = subjectsOfType(graph, OBJECT_PROPERTY);
        Set<Node> datatypeProperties = subjectsOfType(graph, DATATYPE_PROPERTY);
        issues.addAll(literalForObjectProperty(graph, objectProperties));
        issues.addAll(resourceForDatatypeProperty(graph, datatypeProperties));
        issues.addAll(declaredBoth(objectProperties, datatypeProperties));
        issues.addAll(duplicatePrefLabels(graph));
        issues.addAll(disjointInstances(graph));
        return issues.size() > MAX_ISSUES ? issues.subList(0, MAX_ISSUES) : issues;
    }

    private static Set<Node> subjectsOfType(Graph graph, Node type) {
        Set<Node> subjects = new HashSet<>();
        graph.find(Node.ANY, TYPE, type).forEachRemaining(triple -> subjects.add(triple.getSubject()));
        return subjects;
    }

    private static List<Issue> literalForObjectProperty(Graph graph, Set<Node> properties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : properties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(triple -> {
                if (triple.getObject().isLiteral()) {
                    locations.add(statement(triple));
                }
            });
        }
        return issues(S1, locations);
    }

    private static List<Issue> resourceForDatatypeProperty(Graph graph, Set<Node> properties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : properties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(triple -> {
                Node object = triple.getObject();
                if (object.isURI() || object.isBlank()) {
                    locations.add(statement(triple));
                }
            });
        }
        return issues(S2, locations);
    }

    private static List<Issue> declaredBoth(Set<Node> objectProperties, Set<Node> datatypeProperties) {
        TreeSet<String> locations = new TreeSet<>();
        for (Node property : objectProperties) {
            if (property.isURI() && datatypeProperties.contains(property)) {
                locations.add(resource(property));
            }
        }
        return issues(S3, locations);
    }

    private static List<Issue> duplicatePrefLabels(Graph graph) {
        Map<Node, Map<String, Integer>> counts = new LinkedHashMap<>();
        graph.find(Node.ANY, PREF_LABEL, Node.ANY).forEachRemaining(triple -> {
            Node object = triple.getObject();
            if (object.isLiteral()) {
                String language = object.getLiteralLanguage().toLowerCase(Locale.ROOT);
                counts.computeIfAbsent(triple.getSubject(), key -> new HashMap<>())
                        .merge(language, 1, Integer::sum);
            }
        });
        TreeSet<String> locations = new TreeSet<>();
        counts.forEach((subject, byLanguage) -> byLanguage.forEach((language, count) -> {
            if (count > 1) {
                locations.add(resource(subject) + ", language '" + (language.isEmpty() ? "none" : language) + "'");
            }
        }));
        return issues(S4, locations);
    }

    private static List<Issue> disjointInstances(Graph graph) {
        Map<Node, Set<Node>> disjoint = new HashMap<>();
        graph.find(Node.ANY, DISJOINT_WITH, Node.ANY).forEachRemaining(triple -> {
            disjoint.computeIfAbsent(triple.getSubject(), key -> new HashSet<>())
                    .add(triple.getObject());
            disjoint.computeIfAbsent(triple.getObject(), key -> new HashSet<>()).add(triple.getSubject());
        });
        TreeSet<String> locations = new TreeSet<>();
        if (disjoint.isEmpty()) {
            return List.of();
        }
        Map<Node, Set<Node>> typesOf = new HashMap<>();
        graph.find(Node.ANY, TYPE, Node.ANY).forEachRemaining(triple -> {
            if (disjoint.containsKey(triple.getObject())) {
                typesOf.computeIfAbsent(triple.getSubject(), key -> new HashSet<>())
                        .add(triple.getObject());
            }
        });
        typesOf.forEach((instance, types) -> {
            boolean clash =
                    types.stream().anyMatch(type -> disjoint.get(type).stream().anyMatch(types::contains));
            if (clash) {
                locations.add(resource(instance));
            }
        });
        return issues(S5, locations);
    }

    private static List<Issue> issues(String detail, Set<String> locations) {
        return locations.stream().map(location -> issue(detail, location)).toList();
    }

    private static Issue issue(String detail, String location) {
        return Issue.invalidInput("vocabularyFile", detail, location);
    }

    static String statement(Triple triple) {
        return "statement '" + text(triple.getSubject()) + " " + text(triple.getPredicate()) + " "
                + text(triple.getObject()) + "'";
    }

    static String resource(Node node) {
        return "resource '" + text(node) + "'";
    }

    static String text(Node node) {
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

    private static Node uri(String iri) {
        return NodeFactory.createURI(iri);
    }
}
