package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.Issue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

/** Semantic rules S1 to S6 of decision D9. All rules run and every error is returned. */
@Component
public class SemanticRules {

    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    private static final Node RDF_TYPE = RDF.type.asNode();
    private static final Node PREF_LABEL = SKOS.prefLabel.asNode();
    private static final Node DISJOINT_WITH = OWL.disjointWith.asNode();

    /** Runs S1 to S6 on the parsed graph. */
    public List<Issue> check(Graph graph) {
        List<Issue> issues = new ArrayList<>();
        if (graph.isEmpty()) {
            issues.add(issue(S6, "document"));
            return issues;
        }
        Set<Node> objectProperties = typedAs(graph, OWL.ObjectProperty.asNode());
        Set<Node> datatypeProperties = typedAs(graph, OWL.DatatypeProperty.asNode());
        issues.addAll(literalForObjectProperty(graph, objectProperties));
        issues.addAll(resourceForDatatypeProperty(graph, datatypeProperties));
        issues.addAll(declaredBoth(objectProperties, datatypeProperties));
        issues.addAll(duplicatePreferredLabels(graph));
        issues.addAll(disjointInstances(graph));
        return issues;
    }

    private static List<Issue> literalForObjectProperty(Graph graph, Set<Node> objectProperties) {
        Set<String> locations = new TreeSet<>();
        for (Node property : objectProperties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(triple -> {
                if (triple.getObject().isLiteral()) {
                    locations.add(statement(triple));
                }
            });
        }
        return locations.stream().map(location -> issue(S1, location)).toList();
    }

    private static List<Issue> resourceForDatatypeProperty(Graph graph, Set<Node> datatypeProperties) {
        Set<String> locations = new TreeSet<>();
        for (Node property : datatypeProperties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(triple -> {
                Node object = triple.getObject();
                if (object.isURI() || object.isBlank()) {
                    locations.add(statement(triple));
                }
            });
        }
        return locations.stream().map(location -> issue(S2, location)).toList();
    }

    private static List<Issue> declaredBoth(Set<Node> objectProperties, Set<Node> datatypeProperties) {
        return objectProperties.stream()
                .filter(datatypeProperties::contains)
                .map(SemanticRules::resource)
                .sorted()
                .map(location -> issue(S3, location))
                .toList();
    }

    private static List<Issue> duplicatePreferredLabels(Graph graph) {
        Map<Node, Map<String, Integer>> counts = new HashMap<>();
        graph.find(Node.ANY, PREF_LABEL, Node.ANY).forEachRemaining(triple -> {
            Node label = triple.getObject();
            if (label.isLiteral()) {
                String language = label.getLiteralLanguage().toLowerCase(Locale.ROOT);
                counts.computeIfAbsent(triple.getSubject(), s -> new HashMap<>())
                        .merge(language, 1, Integer::sum);
            }
        });
        List<String> locations = new ArrayList<>();
        counts.forEach((subject, perLanguage) -> perLanguage.values().stream()
                .filter(count -> count > 1)
                .forEach(count -> locations.add(resource(subject))));
        locations.sort(Comparator.naturalOrder());
        return locations.stream().map(location -> issue(S4, location)).toList();
    }

    private static List<Issue> disjointInstances(Graph graph) {
        Set<List<Node>> disjointPairs = new LinkedHashSet<>();
        graph.find(Node.ANY, DISJOINT_WITH, Node.ANY)
                .forEachRemaining(t -> disjointPairs.add(List.of(t.getSubject(), t.getObject())));
        if (disjointPairs.isEmpty()) {
            return List.of();
        }
        Map<Node, Set<Node>> typesBySubject = new HashMap<>();
        graph.find(Node.ANY, RDF_TYPE, Node.ANY)
                .forEachRemaining(t -> typesBySubject
                        .computeIfAbsent(t.getSubject(), s -> new HashSet<>())
                        .add(t.getObject()));
        List<String> locations = new ArrayList<>();
        typesBySubject.forEach((subject, types) -> {
            Set<Set<Node>> reported = new HashSet<>();
            for (List<Node> pair : disjointPairs) {
                Node first = pair.get(0);
                Node second = pair.get(1);
                if (!first.equals(second)
                        && types.contains(first)
                        && types.contains(second)
                        && reported.add(Set.of(first, second))) {
                    locations.add(resource(subject));
                }
            }
        });
        locations.sort(Comparator.naturalOrder());
        return locations.stream().map(location -> issue(S5, location)).toList();
    }

    private static Set<Node> typedAs(Graph graph, Node type) {
        Set<Node> nodes = new HashSet<>();
        graph.find(Node.ANY, RDF_TYPE, type).forEachRemaining(t -> {
            if (t.getSubject().isURI()) {
                nodes.add(t.getSubject());
            }
        });
        return nodes;
    }

    private static Issue issue(String detail, String location) {
        return Issue.invalidInput(TurtleSyntaxValidator.FIELD, detail, location);
    }

    static String statement(Triple triple) {
        return "statement '" + term(triple.getSubject()) + " " + term(triple.getPredicate()) + " "
                + term(triple.getObject()) + "'";
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
        if (node.isLiteral()) {
            return node.getLiteralLexicalForm();
        }
        return node.toString();
    }
}
