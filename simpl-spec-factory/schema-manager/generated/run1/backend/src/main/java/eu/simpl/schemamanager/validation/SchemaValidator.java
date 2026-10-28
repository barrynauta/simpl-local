package eu.simpl.schemamanager.validation;

import eu.simpl.schemamanager.config.Settings;
import eu.simpl.schemamanager.domain.SchemaType;
import eu.simpl.schemamanager.web.ApiException;
import eu.simpl.schemamanager.web.Issue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RiotException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The three validation stages: syntax (D30), default properties (D31, D32) and
 * semantic validation (D34 to D36). Stages run in that order; the first failing
 * stage stops processing (D33).
 */
@Component
public class SchemaValidator {

    public static final String SYNTAX_PREFIX = "Syntax Error: ";
    public static final String DEFAULT_PREFIX = "Default Properties Error: ";
    public static final String SEMANTIC_PREFIX = "Semantic Validation Error: ";
    public static final String VM_UNAVAILABLE = "Semantic validation unavailable: vocabulary manager unreachable";

    private static final List<String> WELL_KNOWN = List.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "http://www.w3.org/2000/01/rdf-schema#",
            "http://www.w3.org/2001/XMLSchema#",
            "http://www.w3.org/2002/07/owl#",
            "http://www.w3.org/ns/shacl#",
            "http://purl.org/dc/terms/",
            "http://www.w3.org/2004/02/skos/core#",
            "https://simpl-programme.ec.europa.eu/ns/default#");

    private final Settings settings;
    private final VocabularyClient vocabularies;

    public SchemaValidator(Settings settings, VocabularyClient vocabularies) {
        this.settings = settings;
        this.vocabularies = vocabularies;
    }

    /**
     * Validates content and returns the non-blocking warnings.
     *
     * @throws ApiException 400 on a validation failure, 500 when the Vocabulary Manager is unavailable
     */
    public List<Issue> validate(byte[] content, SchemaType type) {
        Model model = syntax(content);
        if (type == SchemaType.RESOURCE) {
            defaultProperties(model);
        }
        return semantic(model);
    }

    // ---- Stage 1: syntax and structure (D30)

    Model syntax(byte[] content) {
        Model model;
        try {
            model = Rdf.parseTurtle(content);
        } catch (RiotException e) {
            String msg = e.getMessage() == null ? "unparseable Turtle" : e.getMessage();
            throw new ApiException(HttpStatus.BAD_REQUEST, SYNTAX_PREFIX + msg,
                    List.of(new Issue("syntaxError", msg, position(msg))));
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST, SYNTAX_PREFIX + msg,
                    List.of(new Issue("syntaxError", msg, "document")));
        }
        if (!model.listSubjectsWithProperty(Rdf.RDF_TYPE, Rdf.NODE_SHAPE).hasNext()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, SYNTAX_PREFIX + "no sh:NodeShape found",
                    List.of(new Issue("noNodeShape", "The document contains no sh:NodeShape.", "document")));
        }
        return model;
    }

    private static String position(String msg) {
        int start = msg.indexOf("[line:");
        int end = msg.indexOf(']');
        if (start >= 0 && end > start) {
            return msg.substring(start + 1, end);
        }
        return "document";
    }

    // ---- Stage 2: default properties, presence check only (D32)

    void defaultProperties(Model model) {
        List<String> required = settings.defaultPropertyIris();
        if (required.isEmpty()) {
            return;
        }
        List<String> bestMissing = null;
        List<Resource> shapes = model.listSubjectsWithProperty(Rdf.RDF_TYPE, Rdf.NODE_SHAPE).toList();
        shapes.sort(Comparator.comparing(Resource::toString));
        for (Resource shape : shapes) {
            Set<String> paths = new LinkedHashSet<>();
            for (Statement st : shape.listProperties(Rdf.SH_PROPERTY).toList()) {
                if (st.getObject().isResource()) {
                    for (Statement p : st.getObject().asResource().listProperties(Rdf.SH_PATH).toList()) {
                        if (p.getObject().isURIResource()) {
                            paths.add(p.getObject().asResource().getURI());
                        }
                    }
                }
            }
            List<String> missing = required.stream().filter(iri -> !paths.contains(iri)).toList();
            if (missing.isEmpty()) {
                return;
            }
            if (bestMissing == null || missing.size() < bestMissing.size()) {
                bestMissing = missing;
            }
        }
        List<String> missing = bestMissing == null ? required : bestMissing;
        List<Issue> issues = missing.stream()
                .map(iri -> new Issue("missingDefaultProperty",
                        "Default property '" + iri + "' is missing: no sh:NodeShape has a sh:property with this sh:path.",
                        iri))
                .toList();
        throw new ApiException(HttpStatus.BAD_REQUEST,
                DEFAULT_PREFIX + "missing default properties: " + String.join(", ", missing), issues);
    }

    // ---- Stage 3: semantic validation (D34, D35)

    private boolean isLocal(String iri) {
        String base = settings.vocabularyNamespaceBase();
        return base != null && !base.isEmpty() && iri.startsWith(base);
    }

    private boolean isWellKnown(String ns) {
        if (WELL_KNOWN.contains(ns)) {
            return true;
        }
        for (String p : settings.defaultPropertyIris()) {
            if (ns.equals(namespaceOf(p))) {
                return true;
            }
        }
        return false;
    }

    static String namespaceOf(String iri) {
        int i = Math.max(iri.lastIndexOf('#'), iri.lastIndexOf('/'));
        return i < 0 ? iri : iri.substring(0, i + 1);
    }

    /** Vocabulary name: the path segment after the base, up to '#' or '/'. */
    String vocabularyName(String iri) {
        String rest = iri.substring(settings.vocabularyNamespaceBase().length());
        int cut = rest.length();
        int hash = rest.indexOf('#');
        int slash = rest.indexOf('/');
        if (hash >= 0) {
            cut = Math.min(cut, hash);
        }
        if (slash >= 0) {
            cut = Math.min(cut, slash);
        }
        return rest.substring(0, cut);
    }

    private record LocalRef(String iri, String location) {
    }

    List<Issue> semantic(Model model) {
        List<Issue> errors = new ArrayList<>();

        // Checks 1 and 2: local references in sh:path, sh:class, sh:targetClass, sh:datatype and sh:in.
        List<LocalRef> refs = new ArrayList<>();
        for (Statement st : sorted(model.listStatements().toList())) {
            String pred = st.getPredicate().getURI();
            RDFNode o = st.getObject();
            if (pred.equals(Rdf.SH_PATH.getURI()) || pred.equals(Rdf.SH_CLASS.getURI())
                    || pred.equals(Rdf.SH_TARGET_CLASS.getURI()) || pred.equals(Rdf.SH_DATATYPE.getURI())) {
                if (o.isURIResource() && isLocal(o.asResource().getURI())) {
                    refs.add(new LocalRef(o.asResource().getURI(), location(st.getSubject(), "sh:" + st.getPredicate().getLocalName(), o.asResource().getURI())));
                }
            } else if (pred.equals(Rdf.SH_IN.getURI()) && o.canAs(RDFList.class)) {
                try {
                    for (RDFNode member : o.as(RDFList.class).asJavaList()) {
                        if (member.isURIResource() && isLocal(member.asResource().getURI())) {
                            refs.add(new LocalRef(member.asResource().getURI(),
                                    location(st.getSubject(), "sh:in", member.asResource().getURI())));
                        }
                    }
                } catch (RuntimeException e) {
                    // A malformed list is not a local reference; nothing to check.
                }
            }
        }
        Map<String, Optional<Model>> cache = new HashMap<>();
        for (LocalRef ref : refs) {
            String vocab = vocabularyName(ref.iri());
            Optional<Model> vocabModel = vocab.isEmpty() ? Optional.empty()
                    : cache.computeIfAbsent(vocab, this::loadVocabulary);
            if (vocabModel.isEmpty()) {
                errors.add(new Issue("localReferenceUnavailable",
                        "Vocabulary '" + vocab + "' referenced by '" + ref.iri()
                                + "' is not available in the vocabulary manager.",
                        ref.location()));
            } else if (!vocabModel.get().listStatements(vocabModel.get().createResource(ref.iri()), null,
                    (RDFNode) null).hasNext()) {
                errors.add(new Issue("unknownVocabularyTerm",
                        "Term '" + ref.iri() + "' is not defined in vocabulary '" + vocab + "'.",
                        ref.location()));
            }
        }

        // Check 3: sh:node and sh:qualifiedValueShape must point to shapes defined in this document.
        for (Statement st : sorted(model.listStatements().toList())) {
            String pred = st.getPredicate().getURI();
            if (pred.equals(Rdf.SH_NODE.getURI()) || pred.equals(Rdf.SH_QUALIFIED_VALUE_SHAPE.getURI())) {
                RDFNode o = st.getObject();
                boolean defined = o.isResource() && o.asResource().listProperties().hasNext();
                if (!defined) {
                    String target = o.isURIResource() ? o.asResource().getURI() : o.toString();
                    errors.add(new Issue("unresolvedShapeReference",
                            "Shape '" + target + "' referenced by sh:" + st.getPredicate().getLocalName()
                                    + " is not defined in this document.",
                            location(st.getSubject(), "sh:" + st.getPredicate().getLocalName(), target)));
                }
            }
        }

        if (!errors.isEmpty()) {
            String summary = errors.stream().map(Issue::description).collect(Collectors.joining(" "));
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    SEMANTIC_PREFIX + errors.size() + " semantic issue(s) found. " + summary, errors);
        }

        // Check 4: warnings for every prefix bound to an external namespace.
        List<Issue> warnings = new ArrayList<>();
        Map<String, String> prefixes = new TreeMap<>(model.getNsPrefixMap());
        for (Map.Entry<String, String> e : prefixes.entrySet()) {
            String ns = e.getValue();
            if (!isLocal(ns) && !isWellKnown(ns)) {
                warnings.add(new Issue("externalReferenceSkipped",
                        "External vocabulary '" + ns + "' cannot be resolved; references to it were not validated.",
                        "prefix '" + e.getKey() + "' -> '" + ns + "'"));
            }
        }
        return warnings;
    }

    private Optional<Model> loadVocabulary(String name) {
        Optional<byte[]> body;
        try {
            body = vocabularies.fetch(name);
        } catch (VocabularyUnavailableException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, VM_UNAVAILABLE);
        }
        if (body.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Rdf.parseTurtle(body.get()));
        } catch (RuntimeException e) {
            // Content that cannot be parsed is treated as an unavailable vocabulary.
            return Optional.empty();
        }
    }

    /** Location names the property path (when there is one) and the IRI involved (D35). */
    private static String location(Resource subject, String predicate, String iri) {
        Statement path = subject.getProperty(Rdf.SH_PATH);
        if (path != null && path.getObject().isURIResource()) {
            return "property '" + path.getObject().asResource().getURI() + "' " + predicate + " '" + iri + "'";
        }
        String s = subject.isURIResource() ? subject.getURI() : "_:" + subject.getId();
        return "shape '" + s + "' " + predicate + " '" + iri + "'";
    }

    private static List<Statement> sorted(List<Statement> statements) {
        List<Statement> list = new ArrayList<>(statements);
        list.sort(Comparator.comparing((Statement s) -> s.getSubject().toString())
                .thenComparing(s -> s.getPredicate().getURI())
                .thenComparing(s -> s.getObject().toString()));
        return list;
    }
}
