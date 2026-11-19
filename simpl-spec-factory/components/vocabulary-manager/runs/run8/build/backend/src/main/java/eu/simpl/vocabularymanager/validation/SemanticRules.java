package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * Semantic rules S1 to S6 of D9 (S7 is in {@link ReferenceRules}). All rules run; all errors are returned (SIMPL-18183,
 * SIMPL-18180 AC7). Locations follow D65.
 */
public final class SemanticRules {

    static final String S1 = "Literal value for object property";
    static final String S2 = "Resource value for datatype property";
    static final String S3 = "Property declared as both object and datatype property";
    static final String S4 = "Duplicate preferred label for the same language";
    static final String S5 = "Resource is an instance of disjoint classes";
    static final String S6 = "Vocabulary contains no statements";

    private SemanticRules() {}

    /**
     * @param content the parsed content
     * @return the errors of S1 to S6, in rule order and then file order
     */
    public static List<ContentIssue> check(ParsedContent content) {
        List<Triple> triples = content.triples();
        List<ContentIssue> issues = new ArrayList<>();
        if (triples.isEmpty()) {
            issues.add(new ContentIssue(S6, "document"));
            return issues;
        }
        Set<Node> objectProperties = declared(triples, OWL.ObjectProperty.asNode());
        Set<Node> datatypeProperties = declared(triples, OWL.DatatypeProperty.asNode());
        for (Triple triple : triples) {
            if (objectProperties.contains(triple.getPredicate()) && triple.getObject().isLiteral()) {
                issues.add(new ContentIssue(S1, statement(triple)));
            }
        }
        for (Triple triple : triples) {
            Node object = triple.getObject();
            if (datatypeProperties.contains(triple.getPredicate()) && (object.isURI() || object.isBlank())) {
                issues.add(new ContentIssue(S2, statement(triple)));
            }
        }
        for (Node property : objectProperties) {
            if (datatypeProperties.contains(property)) {
                issues.add(new ContentIssue(S3, "resource '" + term(property) + "'"));
            }
        }
        issues.addAll(duplicatePreferredLabels(triples));
        issues.addAll(disjointInstances(triples));
        return issues;
    }

    private static Set<Node> declared(List<Triple> triples, Node type) {
        Set<Node> result = new LinkedHashSet<>();
        Node rdfType = RDF.type.asNode();
        for (Triple triple : triples) {
            if (triple.getPredicate().equals(rdfType)
                    && triple.getObject().equals(type)
                    && triple.getSubject().isURI()) {
                result.add(triple.getSubject());
            }
        }
        return result;
    }

    private static List<ContentIssue> duplicatePreferredLabels(List<Triple> triples) {
        Node prefLabel = SKOS.prefLabel.asNode();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> locations = new HashMap<>();
        for (Triple triple : triples) {
            Node object = triple.getObject();
            if (!triple.getPredicate().equals(prefLabel) || !object.isLiteral()) {
                continue;
            }
            String language = object.getLiteralLanguage();
            String shown = language == null || language.isEmpty() ? "none" : language;
            String key = term(triple.getSubject()) + "\u0000" + shown.toLowerCase(Locale.ROOT);
            counts.merge(key, 1, Integer::sum);
            locations.putIfAbsent(key, "resource '" + term(triple.getSubject()) + "', language '" + shown + "'");
        }
        List<ContentIssue> issues = new ArrayList<>();
        counts.forEach((key, count) -> {
            if (count > 1) {
                issues.add(new ContentIssue(S4, locations.get(key)));
            }
        });
        return issues;
    }

    private static List<ContentIssue> disjointInstances(List<Triple> triples) {
        Node disjointWith = OWL.disjointWith.asNode();
        Node rdfType = RDF.type.asNode();
        Set<List<Node>> disjointPairs = new HashSet<>();
        for (Triple triple : triples) {
            if (triple.getPredicate().equals(disjointWith)) {
                disjointPairs.add(List.of(triple.getSubject(), triple.getObject()));
                disjointPairs.add(List.of(triple.getObject(), triple.getSubject()));
            }
        }
        if (disjointPairs.isEmpty()) {
            return List.of();
        }
        Map<Node, List<Node>> types = new LinkedHashMap<>();
        for (Triple triple : triples) {
            if (triple.getPredicate().equals(rdfType)) {
                types.computeIfAbsent(triple.getSubject(), key -> new ArrayList<>())
                        .add(triple.getObject());
            }
        }
        List<ContentIssue> issues = new ArrayList<>();
        types.forEach((resource, classes) -> {
            if (hasDisjointPair(classes, disjointPairs)) {
                issues.add(new ContentIssue(S5, "resource '" + term(resource) + "'"));
            }
        });
        return issues;
    }

    private static boolean hasDisjointPair(List<Node> classes, Set<List<Node>> disjointPairs) {
        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                if (disjointPairs.contains(List.of(classes.get(i), classes.get(j)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String statement(Triple triple) {
        return "statement '" + term(triple.getSubject()) + " " + term(triple.getPredicate()) + " "
                + term(triple.getObject()) + "'";
    }

    /**
     * @param node an RDF term
     * @return full IRI, literal as written (lexical form), or _:label for a blank node
     */
    static String term(Node node) {
        if (node.isURI()) {
            return node.getURI();
        }
        if (node.isLiteral()) {
            return node.getLiteralLexicalForm();
        }
        if (node.isBlank()) {
            return "_:" + node.getBlankNodeLabel();
        }
        return node.toString();
    }
}
