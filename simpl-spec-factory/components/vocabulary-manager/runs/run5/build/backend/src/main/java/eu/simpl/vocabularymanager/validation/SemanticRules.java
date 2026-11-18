package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Component;

/**
 * Semantic rules S1 to S6 of D9. All rules run and every error is returned. Within a rule, errors are ordered by
 * location so the report is stable.
 */
@Component
public class SemanticRules {

    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    private static final Node OBJECT_PROPERTY = OWL.ObjectProperty.asNode();
    private static final Node DATATYPE_PROPERTY = OWL.DatatypeProperty.asNode();
    private static final Node DISJOINT_WITH = OWL.disjointWith.asNode();
    private static final Node PREF_LABEL = SKOS.prefLabel.asNode();
    private static final Node TYPE = RDF.type.asNode();

    /** Runs S1 to S6 on the parsed triples. */
    public List<ValidationError> check(Graph graph) {
        List<ValidationError> errors = new ArrayList<>();
        if (graph.isEmpty()) {
            errors.add(new ValidationError(S6, "document"));
            return errors;
        }
        Set<Node> objectProperties = subjectsTyped(graph, OBJECT_PROPERTY);
        Set<Node> datatypeProperties = subjectsTyped(graph, DATATYPE_PROPERTY);
        errors.addAll(literalForObjectProperty(graph, objectProperties));
        errors.addAll(resourceForDatatypeProperty(graph, datatypeProperties));
        errors.addAll(declaredBoth(objectProperties, datatypeProperties));
        errors.addAll(duplicatePrefLabels(graph));
        errors.addAll(disjointInstances(graph));
        return errors;
    }

    private static Set<Node> subjectsTyped(Graph graph, Node type) {
        Set<Node> result = new HashSet<>();
        graph.find(Node.ANY, TYPE, type).forEachRemaining(t -> result.add(t.getSubject()));
        return result;
    }

    private static List<ValidationError> literalForObjectProperty(Graph graph, Set<Node> objectProperties) {
        List<ValidationError> errors = new ArrayList<>();
        for (Node property : objectProperties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(t -> {
                if (t.getObject().isLiteral()) {
                    errors.add(new ValidationError(S1, statement(t)));
                }
            });
        }
        return sorted(errors);
    }

    private static List<ValidationError> resourceForDatatypeProperty(Graph graph, Set<Node> datatypeProperties) {
        List<ValidationError> errors = new ArrayList<>();
        for (Node property : datatypeProperties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(t -> {
                Node o = t.getObject();
                if (o.isURI() || o.isBlank()) {
                    errors.add(new ValidationError(S2, statement(t)));
                }
            });
        }
        return sorted(errors);
    }

    private static List<ValidationError> declaredBoth(Set<Node> objectProperties, Set<Node> datatypeProperties) {
        List<ValidationError> errors = new ArrayList<>();
        for (Node property : objectProperties) {
            if (property.isURI() && datatypeProperties.contains(property)) {
                errors.add(new ValidationError(S3, resource(property)));
            }
        }
        return sorted(errors);
    }

    private static List<ValidationError> duplicatePrefLabels(Graph graph) {
        Map<Node, Map<String, Integer>> counts = new HashMap<>();
        graph.find(Node.ANY, PREF_LABEL, Node.ANY).forEachRemaining(t -> {
            Node o = t.getObject();
            if (o.isLiteral()) {
                String language = o.getLiteralLanguage() == null
                        ? ""
                        : o.getLiteralLanguage().toLowerCase(Locale.ROOT);
                counts.computeIfAbsent(t.getSubject(), k -> new HashMap<>()).merge(language, 1, Integer::sum);
            }
        });
        List<ValidationError> errors = new ArrayList<>();
        counts.forEach((subject, byLanguage) -> byLanguage.forEach((language, count) -> {
            if (count > 1) {
                errors.add(new ValidationError(S4, resource(subject)));
            }
        }));
        return sorted(errors);
    }

    private static List<ValidationError> disjointInstances(Graph graph) {
        Set<List<Node>> disjointPairs = new HashSet<>();
        graph.find(Node.ANY, DISJOINT_WITH, Node.ANY).forEachRemaining(t -> {
            disjointPairs.add(List.of(t.getSubject(), t.getObject()));
            disjointPairs.add(List.of(t.getObject(), t.getSubject()));
        });
        if (disjointPairs.isEmpty()) {
            return List.of();
        }
        Map<Node, Set<Node>> types = new HashMap<>();
        graph.find(Node.ANY, TYPE, Node.ANY)
                .forEachRemaining(t -> types.computeIfAbsent(t.getSubject(), k -> new HashSet<>())
                        .add(t.getObject()));
        List<ValidationError> errors = new ArrayList<>();
        types.forEach((resource, classes) -> {
            if (hasDisjointPair(classes, disjointPairs)) {
                errors.add(new ValidationError(S5, resource(resource)));
            }
        });
        return sorted(errors);
    }

    private static boolean hasDisjointPair(Set<Node> classes, Set<List<Node>> disjointPairs) {
        for (Node a : classes) {
            for (Node b : classes) {
                if (!a.equals(b) && disjointPairs.contains(List.of(a, b))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<ValidationError> sorted(List<ValidationError> errors) {
        errors.sort(Comparator.comparing(ValidationError::location));
        return errors;
    }

    static String statement(Triple t) {
        return "statement '" + text(t.getSubject()) + " " + text(t.getPredicate()) + " " + text(t.getObject()) + "'";
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
}
