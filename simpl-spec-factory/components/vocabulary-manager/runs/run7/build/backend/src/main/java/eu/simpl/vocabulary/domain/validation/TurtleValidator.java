package eu.simpl.vocabulary.domain.validation;

import java.io.ByteArrayInputStream;
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
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

/**
 * Syntax and semantic validation of uploaded Turtle (D9, D53, D57, D65). Parsing uses Apache Jena RIOT
 * (REUSE-02); the semantic rules S1 to S6 run over the parsed graph. Rule S7 needs the store and is in
 * {@link ReferenceAnalyzer}.
 */
public final class TurtleValidator {

    public static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax";
    public static final String S1_DETAIL = "Literal value for object property";
    public static final String S2_DETAIL = "Resource value for datatype property";
    public static final String S3_DETAIL = "Property declared as both object and datatype property";
    public static final String S4_DETAIL = "Duplicate preferred label for the same language";
    public static final String S5_DETAIL = "Resource is an instance of disjoint classes";
    public static final String S6_DETAIL = "Vocabulary contains no statements";

    /**
     * Syntax step: parses the bytes as Turtle (RDF 1.1).
     *
     * @param bytes the uploaded content
     * @param baseIri the base for relative IRIs (D57)
     * @return the parsed content
     * @throws SyntaxError with the first parse error
     */
    public ParsedContent parse(byte[] bytes, String baseIri) throws SyntaxError {
        Graph graph = GraphFactory.createDefaultGraph();
        List<PrefixDeclaration> prefixes = new ArrayList<>();
        StreamRDF sink = new StreamRDFWrapper(StreamRDFLib.graph(graph)) {
            @Override
            public void prefix(String prefix, String iri) {
                prefixes.add(new PrefixDeclaration(prefix, iri));
                super.prefix(prefix, iri);
            }
        };
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(bytes))
                    .lang(Lang.TURTLE)
                    .base(baseIri)
                    .strict(true)
                    .errorHandler(new FirstErrorHandler())
                    .parse(sink);
        } catch (RiotParseException e) {
            throw new SyntaxError(
                    new ValidationError(syntaxDetail(e.getOriginalMessage()), location(e.getLine(), e.getCol())));
        } catch (RiotException e) {
            throw new SyntaxError(new ValidationError(syntaxDetail(e.getMessage()), "document"));
        }
        return new ParsedContent(graph, List.copyOf(distinct(prefixes)));
    }

    private static List<PrefixDeclaration> distinct(List<PrefixDeclaration> prefixes) {
        return new ArrayList<>(new java.util.LinkedHashSet<>(prefixes));
    }

    private static String syntaxDetail(String message) {
        if (message == null || message.isBlank()) {
            return SYNTAX_DETAIL + ".";
        }
        String clean = message.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("[^\\x20-\\x7E]", "?")
                .strip();
        if (clean.length() > 300) {
            clean = clean.substring(0, 300);
        }
        return SYNTAX_DETAIL + ": " + clean;
    }

    /**
     * Location text of a parse error (D65): column only when the parser gives one (D60).
     *
     * @param line line number, negative when unknown
     * @param column column number, negative when unknown
     * @return the location
     */
    static String location(long line, long column) {
        if (line < 0) {
            return "document";
        }
        if (column < 0) {
            return "line " + line;
        }
        return "line " + line + ", column " + column;
    }

    /**
     * Semantic step: rules S1 to S6, all run, all errors returned (D9).
     *
     * @param content parsed content
     * @return the errors, in rule order
     */
    public List<ValidationError> semanticErrors(ParsedContent content) {
        Graph graph = content.graph();
        if (graph.isEmpty()) {
            return List.of(new ValidationError(S6_DETAIL, "document"));
        }
        Set<Node> objectProperties = declared(graph, OWL.ObjectProperty.asNode());
        Set<Node> datatypeProperties = declared(graph, OWL.DatatypeProperty.asNode());
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(statementErrors(graph, objectProperties, S1_DETAIL, true));
        errors.addAll(statementErrors(graph, datatypeProperties, S2_DETAIL, false));
        errors.addAll(doubleDeclarations(objectProperties, datatypeProperties));
        errors.addAll(duplicatePreferredLabels(graph));
        errors.addAll(disjointInstances(graph));
        return errors;
    }

    private static Set<Node> declared(Graph graph, Node type) {
        Set<Node> result = new HashSet<>();
        graph.find(Node.ANY, RDF.Nodes.type, type).forEachRemaining(t -> {
            if (t.getSubject().isURI()) {
                result.add(t.getSubject());
            }
        });
        return result;
    }

    /** S1 (literal objects of object properties) and S2 (resource objects of datatype properties). */
    private static List<ValidationError> statementErrors(
            Graph graph, Set<Node> properties, String detail, boolean literalIsError) {
        Set<String> locations = new TreeSet<>();
        for (Node property : properties) {
            graph.find(Node.ANY, property, Node.ANY).forEachRemaining(t -> {
                Node object = t.getObject();
                boolean offending = literalIsError ? object.isLiteral() : (object.isURI() || object.isBlank());
                if (offending) {
                    locations.add(statement(t));
                }
            });
        }
        return locations.stream().map(l -> new ValidationError(detail, l)).toList();
    }

    private static List<ValidationError> doubleDeclarations(Set<Node> objectProperties, Set<Node> datatypeProperties) {
        Set<String> both = new TreeSet<>();
        for (Node property : objectProperties) {
            if (datatypeProperties.contains(property)) {
                both.add(property.getURI());
            }
        }
        return both.stream()
                .map(iri -> new ValidationError(S3_DETAIL, "resource '" + iri + "'"))
                .toList();
    }

    /** S4 with D53: an untagged label counts as the language 'none'. */
    private static List<ValidationError> duplicatePreferredLabels(Graph graph) {
        Map<Node, Map<String, List<String>>> bySubject = new LinkedHashMap<>();
        graph.find(Node.ANY, SKOS.prefLabel.asNode(), Node.ANY).forEachRemaining(t -> {
            if (!t.getObject().isLiteral()) {
                return;
            }
            String tag = t.getObject().getLiteralLanguage();
            String key = tag == null || tag.isEmpty() ? "" : tag.toLowerCase(Locale.ROOT);
            bySubject
                    .computeIfAbsent(t.getSubject(), s -> new HashMap<>())
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(tag == null || tag.isEmpty() ? "none" : tag);
        });
        Set<String> locations = new TreeSet<>();
        bySubject.forEach((subject, byLanguage) -> byLanguage.forEach((key, tags) -> {
            if (tags.size() > 1) {
                locations.add("resource '" + text(subject) + "', language '" + tags.get(0) + "'");
            }
        }));
        return locations.stream().map(l -> new ValidationError(S4_DETAIL, l)).toList();
    }

    private static List<ValidationError> disjointInstances(Graph graph) {
        Map<Node, Set<Node>> disjoint = new HashMap<>();
        graph.find(Node.ANY, OWL.disjointWith.asNode(), Node.ANY).forEachRemaining(t -> {
            disjoint.computeIfAbsent(t.getSubject(), k -> new HashSet<>()).add(t.getObject());
            disjoint.computeIfAbsent(t.getObject(), k -> new HashSet<>()).add(t.getSubject());
        });
        if (disjoint.isEmpty()) {
            return List.of();
        }
        Map<Node, Set<Node>> typesBySubject = new HashMap<>();
        graph.find(Node.ANY, RDF.Nodes.type, Node.ANY).forEachRemaining(t -> {
            if (disjoint.containsKey(t.getObject())) {
                typesBySubject
                        .computeIfAbsent(t.getSubject(), k -> new HashSet<>())
                        .add(t.getObject());
            }
        });
        Set<String> locations = new TreeSet<>();
        typesBySubject.forEach((subject, types) -> {
            for (Node type : types) {
                Set<Node> others = disjoint.getOrDefault(type, Set.of());
                if (types.stream().anyMatch(others::contains)) {
                    locations.add("resource '" + text(subject) + "'");
                    return;
                }
            }
        });
        return locations.stream().map(l -> new ValidationError(S5_DETAIL, l)).toList();
    }

    /**
     * Subject IRIs of the graph, for the own-namespace rule of D12.
     *
     * @param graph the graph
     * @return the distinct subject IRIs
     */
    public static Set<String> subjectIris(Graph graph) {
        Set<String> subjects = new HashSet<>();
        graph.find().forEachRemaining(t -> {
            if (t.getSubject().isURI()) {
                subjects.add(t.getSubject().getURI());
            }
        });
        return subjects;
    }

    private static String statement(Triple t) {
        return "statement '" + text(t.getSubject()) + " " + text(t.getPredicate()) + " " + text(t.getObject()) + "'";
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

    /** Throws on the first error; ignores warnings (for example ill-formed literals). */
    private static final class FirstErrorHandler implements ErrorHandler {

        @Override
        public void warning(String message, long line, long col) {
            // Warnings never block (D9 checks syntax only).
        }

        @Override
        public void error(String message, long line, long col) {
            throw new RiotParseException(message, line, col);
        }

        @Override
        public void fatal(String message, long line, long col) {
            throw new RiotParseException(message, line, col);
        }
    }
}
