package eu.simpl.vocabulary.validation;

import eu.simpl.vocabulary.domain.ExternalVocabulary;
import eu.simpl.vocabulary.domain.Status;
import eu.simpl.vocabulary.domain.Vocabulary;
import eu.simpl.vocabulary.domain.VocabularyVersion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;

/**
 * D17 semantic rules.
 * Errors: (a) a term used from an internal vocabulary version does not exist in it; (b) the referenced internal
 * version is DEPRECATED; (c) the referenced internal vocabulary or version does not exist; (d) one IRI declared
 * both as a class and as a property.
 * Warnings: a namespace that is used but is neither internal, nor a registered external vocabulary, nor a
 * built-in language namespace (rdf, rdfs, owl, xsd, skos), nor a namespace the file itself defines.
 */
public class SemanticValidator {

    private static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    private static final Set<String> BUILT_IN = Set.of(
            ExternalVocabulary.normalize(RDF.getURI()),
            ExternalVocabulary.normalize(RDFS.getURI()),
            ExternalVocabulary.normalize(OWL.getURI()),
            ExternalVocabulary.normalize(XSD.getURI()),
            ExternalVocabulary.normalize(SKOS.getURI()),
            ExternalVocabulary.normalize("http://www.w3.org/XML/1998/namespace"));
    private static final Set<Node> CLASS_TYPES = Set.of(RDFS.Class.asNode(), OWL.Class.asNode());
    private static final Set<Node> PROPERTY_TYPES = Set.of(RDF.Property.asNode(), OWL.ObjectProperty.asNode(),
            OWL.DatatypeProperty.asNode(), OWL.AnnotationProperty.asNode(), OWL.FunctionalProperty.asNode(),
            OWL.TransitiveProperty.asNode(), OWL.SymmetricProperty.asNode(), OWL.InverseFunctionalProperty.asNode(),
            OWL.OntologyProperty.asNode());

    private final String base;

    public SemanticValidator(String referenceBase) {
        this.base = referenceBase;
    }

    public record Result(List<ValidationIssue> errors, List<ValidationWarning> warnings) {}

    private record Key(String name, Integer version) {}

    public Result validate(String selfName, Graph graph, ReferenceResolver resolver) {
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        PrefixMapping prefixes = graph.getPrefixMapping();

        // Collect all IRIs that appear in the file, in a stable order.
        Set<String> iris = new LinkedHashSet<>();
        Set<String> subjectIris = new LinkedHashSet<>();
        graph.find().forEachRemaining(t -> collect(t, iris, subjectIris));

        // Internal references, grouped by (name, version), with the terms used.
        Map<Key, Set<String>> references = new LinkedHashMap<>();
        Map<Key, String> invalidNames = new LinkedHashMap<>();
        for (String ns : prefixes.getNsPrefixMap().values()) {
            InternalReference.parse(ns, base).ifPresent(r -> register(r, references, invalidNames, ns));
        }
        for (String iri : iris) {
            InternalReference.parse(iri, base).ifPresent(r -> register(r, references, invalidNames, iri));
        }
        invalidNames.forEach((key, iri) -> errors.add(new ValidationIssue(ValidationIssue.Category.SEMANTIC,
                "Reference '" + iri + "' does not identify an internal vocabulary. Use '" + base
                        + "/{Name}/{version}#Term' or '" + base + "/{Name}#Term'.", location(prefixes, iri))));

        for (Map.Entry<Key, Set<String>> e : references.entrySet()) {
            Key key = e.getKey();
            if (key.name().equals(selfName)) {
                continue; // the vocabulary's own namespace
            }
            String ns = new InternalReference(key.name(), key.version(), null).namespace(base);
            String where = location(prefixes, ns);
            Optional<Vocabulary> found = resolver.findVocabulary(key.name());
            if (found.isEmpty()) {
                errors.add(semantic("Referenced internal vocabulary '" + key.name() + "' does not exist.", where));
                continue;
            }
            Vocabulary vocabulary = found.get();
            VocabularyVersion target;
            if (key.version() != null) {
                Optional<VocabularyVersion> v = vocabulary.version(key.version());
                if (v.isEmpty()) {
                    errors.add(semantic("Referenced internal vocabulary '" + key.name() + "' version " + key.version()
                            + " does not exist." + recommendation(vocabulary), where));
                    continue;
                }
                target = v.get();
            } else {
                target = vocabulary.latest();
            }
            if (target.status() == Status.DEPRECATED) {
                errors.add(semantic("Referenced internal vocabulary '" + key.name() + "' version " + target.version()
                        + " is DEPRECATED." + recommendation(vocabulary), where));
            }
            if (!e.getValue().isEmpty()) {
                Set<String> known = resolver.localNamesOf(key.name(), target.version());
                for (String term : e.getValue()) {
                    if (!known.contains(term)) {
                        errors.add(semantic("Term '" + term + "' does not exist in internal vocabulary '"
                                + key.name() + "' version " + target.version() + ".", "resource '" + ns + term + "'"));
                    }
                }
            }
        }

        // (d) class and property at the same time.
        Set<String> classes = new HashSet<>();
        Set<String> properties = new HashSet<>();
        graph.find(Node.ANY, RDF.type.asNode(), Node.ANY).forEachRemaining(t -> {
            if (t.getSubject().isURI()) {
                if (CLASS_TYPES.contains(t.getObject())) {
                    classes.add(t.getSubject().getURI());
                }
                if (PROPERTY_TYPES.contains(t.getObject())) {
                    properties.add(t.getSubject().getURI());
                }
            }
        });
        classes.stream().filter(properties::contains).sorted().forEach(iri -> errors.add(semantic(
                "Resource '" + iri + "' is declared both as a class and as a property.", "resource '" + iri + "'")));

        // Warnings for unresolvable external namespaces.
        Set<String> own = new HashSet<>();
        for (String s : subjectIris) {
            own.add(ExternalVocabulary.normalize(namespaceOf(s)));
        }
        List<ExternalVocabulary> externals = resolver.externalVocabularies();
        Set<String> seen = new HashSet<>();
        for (String iri : iris) {
            if (iri.startsWith(base + "/")) {
                continue;
            }
            String ns = namespaceOf(iri);
            String normalized = ExternalVocabulary.normalize(ns);
            if (normalized.isEmpty() || BUILT_IN.contains(normalized) || own.contains(normalized)
                    || !seen.add(normalized)) {
                continue;
            }
            boolean registered = externals.stream().anyMatch(x -> x.matchesAny(ns));
            if (!registered) {
                warnings.add(new ValidationWarning(ValidationWarning.EXTERNAL_REFERENCE_SKIPPED,
                        "Cannot resolve external namespace '" + ns
                                + "' because no cached external vocabulary is registered.",
                        location(prefixes, ns)));
            }
        }
        return new Result(errors, warnings);
    }

    private void register(InternalReference r, Map<Key, Set<String>> references, Map<Key, String> invalid,
                          String iri) {
        Key key = new Key(r.name(), r.version());
        if (!NAME.matcher(r.name()).matches()) {
            invalid.putIfAbsent(key, iri);
            return;
        }
        Set<String> terms = references.computeIfAbsent(key, k -> new LinkedHashSet<>());
        if (r.term() != null) {
            terms.add(r.term());
        }
    }

    private String recommendation(Vocabulary vocabulary) {
        return vocabulary.latestActive()
                .map(v -> " Use the latest ACTIVE version " + v.version() + " ('" + base + "/" + vocabulary.name()
                        + "/" + v.version() + "#') instead.")
                .orElse(" No ACTIVE version of '" + vocabulary.name() + "' is available.");
    }

    private static ValidationIssue semantic(String detail, String location) {
        return new ValidationIssue(ValidationIssue.Category.SEMANTIC, detail, location);
    }

    private static void collect(Triple t, Set<String> iris, Set<String> subjects) {
        for (Node n : new Node[] {t.getSubject(), t.getPredicate(), t.getObject()}) {
            if (n.isURI()) {
                iris.add(n.getURI());
            } else if (n.isLiteral() && n.getLiteralDatatypeURI() != null) {
                iris.add(n.getLiteralDatatypeURI());
            }
        }
        if (t.getSubject().isURI()) {
            subjects.add(t.getSubject().getURI());
        }
    }

    /** Namespace of an IRI: up to and including the last '#', otherwise the last '/', otherwise the last ':'. */
    public static String namespaceOf(String iri) {
        int hash = iri.lastIndexOf('#');
        if (hash >= 0) {
            return iri.substring(0, hash + 1);
        }
        int slash = iri.lastIndexOf('/');
        int schemeEnd = iri.indexOf("://");
        if (slash >= 0 && (schemeEnd < 0 || slash > schemeEnd + 2)) {
            return iri.substring(0, slash + 1);
        }
        int colon = iri.lastIndexOf(':');
        return colon >= 0 ? iri.substring(0, colon + 1) : iri;
    }

    /** Local name of an IRI: the part after the namespace. */
    public static String localNameOf(String iri) {
        return iri.substring(namespaceOf(iri).length());
    }

    private static String location(PrefixMapping prefixes, String ns) {
        for (Map.Entry<String, String> p : prefixes.getNsPrefixMap().entrySet()) {
            if (p.getValue().equals(ns)) {
                return "prefix '" + p.getKey() + "' -> '" + ns + "'";
            }
        }
        for (Map.Entry<String, String> p : prefixes.getNsPrefixMap().entrySet()) {
            if (ExternalVocabulary.normalize(p.getValue()).equals(ExternalVocabulary.normalize(ns))) {
                return "prefix '" + p.getKey() + "' -> '" + p.getValue() + "'";
            }
        }
        return "namespace '" + ns + "'";
    }
}
