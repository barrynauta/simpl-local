package eu.simpl.vocabulary.store;

import eu.simpl.vocabulary.domain.ExternalVocabulary;
import eu.simpl.vocabulary.domain.Status;
import eu.simpl.vocabulary.domain.Vocabulary;
import eu.simpl.vocabulary.domain.VocabularyVersion;
import eu.simpl.vocabulary.validation.ReferenceResolver;
import eu.simpl.vocabulary.validation.SemanticValidator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDrop;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;

/**
 * Persistence in an RDF dataset (Apache Jena Fuseki in production, D27), accessed through RDFConnection.
 * <p>
 * Layout: one named graph per vocabulary with the metadata of the vocabulary and all its versions; one named
 * graph per version with the parsed triples (used by semantic validation); one named graph per version with the
 * uploaded Turtle text as a literal (so downloads return the file as uploaded). Every change is one SPARQL Update
 * request, which Fuseki runs in one transaction.
 */
public class JenaStore implements ReferenceResolver {

    public static final String NS = "https://vocabulary.simpl.local/ns#";
    private static final String META = "urn:simpl:vm:meta:";
    private static final String CONTENT = "urn:simpl:vm:content:";
    private static final String RAW = "urn:simpl:vm:raw:";
    private static final String EXT_META = "urn:simpl:vm:ext-meta:";
    private static final String EXT_CONTENT = "urn:simpl:vm:ext-content:";
    private static final String EXT_RAW = "urn:simpl:vm:ext-raw:";

    private static final Property NAME = p("name");
    private static final Property STATUS = p("status");
    private static final Property CREATED_BY = p("createdBy");
    private static final Property CREATION_DATE = p("creationDate");
    private static final Property LAST_UPDATE = p("lastUpdateDate");
    private static final Property HAS_VERSION = p("hasVersion");
    private static final Property VERSION = p("version");
    private static final Property DESCRIPTION = p("description");
    private static final Property CHANGELOG = p("changelog");
    private static final Property TTL = p("ttl");
    private static final Property NAMESPACE = p("namespaceUri");
    private static final Property ALIAS = p("aliasUri");
    private static final Property SOURCE = p("sourceUri");
    private static final Resource VOCABULARY_TYPE = ResourceFactory.createResource(NS + "Vocabulary");
    private static final Resource VERSION_TYPE = ResourceFactory.createResource(NS + "Version");
    private static final Resource EXTERNAL_TYPE = ResourceFactory.createResource(NS + "ExternalVocabulary");

    private final Supplier<RDFConnection> connections;

    public JenaStore(Supplier<RDFConnection> connections) {
        this.connections = connections;
    }

    /** New content for a version, stored together with the metadata. */
    public record NewContent(int version, Graph graph, String text) {}

    // ---------------------------------------------------------------- internal vocabularies

    public List<Vocabulary> findAll() {
        Model model = construct("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } "
                + "FILTER(STRSTARTS(STR(?g), \"" + META + "\")) }");
        List<Vocabulary> result = new ArrayList<>();
        model.listSubjectsWithProperty(RDF.type, VOCABULARY_TYPE).forEachRemaining(r -> result.add(readVocabulary(r)));
        result.sort(Comparator.comparing(Vocabulary::name));
        return result;
    }

    @Override
    public Optional<Vocabulary> findVocabulary(String name) {
        Model model = construct("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <" + META + name + "> { ?s ?p ?o } }");
        var subjects = model.listSubjectsWithProperty(RDF.type, VOCABULARY_TYPE).toList();
        return subjects.isEmpty() ? Optional.empty() : Optional.of(readVocabulary(subjects.get(0)));
    }

    /** Replaces the metadata of a vocabulary and, if given, adds the content of a new version, atomically. */
    public void save(Vocabulary vocabulary, NewContent content) {
        UpdateRequest request = new UpdateRequest();
        Node metaGraph = NodeFactory.createURI(META + vocabulary.name());
        request.add(new UpdateDrop(metaGraph, true));
        QuadDataAcc quads = new QuadDataAcc();
        Node v = NodeFactory.createURI("urn:simpl:vm:vocabulary:" + vocabulary.name());
        add(quads, metaGraph, v, RDF.type.asNode(), VOCABULARY_TYPE.asNode());
        add(quads, metaGraph, v, NAME.asNode(), lit(vocabulary.name()));
        add(quads, metaGraph, v, STATUS.asNode(), lit(vocabulary.status().name()));
        add(quads, metaGraph, v, CREATED_BY.asNode(), lit(vocabulary.createdBy()));
        add(quads, metaGraph, v, CREATION_DATE.asNode(), date(vocabulary.creationDate()));
        add(quads, metaGraph, v, LAST_UPDATE.asNode(), date(vocabulary.lastUpdateDate()));
        for (VocabularyVersion ver : vocabulary.versions()) {
            Node vn = NodeFactory.createURI("urn:simpl:vm:version:" + vocabulary.name() + ":" + ver.version());
            add(quads, metaGraph, v, HAS_VERSION.asNode(), vn);
            add(quads, metaGraph, vn, RDF.type.asNode(), VERSION_TYPE.asNode());
            add(quads, metaGraph, vn, VERSION.asNode(),
                    NodeFactory.createLiteralDT(Integer.toString(ver.version()), XSDDatatype.XSDinteger));
            add(quads, metaGraph, vn, DESCRIPTION.asNode(), lit(ver.description()));
            add(quads, metaGraph, vn, CHANGELOG.asNode(), lit(ver.changelog()));
            add(quads, metaGraph, vn, STATUS.asNode(), lit(ver.status().name()));
            add(quads, metaGraph, vn, CREATED_BY.asNode(), lit(ver.createdBy()));
            add(quads, metaGraph, vn, CREATION_DATE.asNode(), date(ver.creationDate()));
            add(quads, metaGraph, vn, LAST_UPDATE.asNode(), date(ver.lastUpdateDate()));
        }
        if (content != null) {
            String suffix = vocabulary.name() + ":" + content.version();
            Node contentGraph = NodeFactory.createURI(CONTENT + suffix);
            Node rawGraph = NodeFactory.createURI(RAW + suffix);
            request.add(new UpdateDrop(contentGraph, true));
            request.add(new UpdateDrop(rawGraph, true));
            content.graph().find().forEachRemaining(t -> quads.addQuad(new Quad(contentGraph, t)));
            add(quads, rawGraph, rawGraph, TTL.asNode(), lit(content.text()));
        }
        request.add(new UpdateDataInsert(quads));
        try (RDFConnection conn = connections.get()) {
            conn.update(request);
        }
    }

    public Optional<String> rawContent(String name, int version) {
        return selectString("SELECT ?t WHERE { GRAPH <" + RAW + name + ":" + version + "> { ?s <" + TTL.getURI()
                + "> ?t } }");
    }

    @Override
    public Set<String> localNamesOf(String name, int version) {
        Set<String> names = new HashSet<>();
        try (RDFConnection conn = connections.get()) {
            conn.querySelect("SELECT DISTINCT ?s WHERE { GRAPH <" + CONTENT + name + ":" + version
                    + "> { ?s ?p ?o FILTER(isIRI(?s)) } }", (QuerySolution qs) -> {
                        names.add(SemanticValidator.localNameOf(qs.getResource("s").getURI()));
                    });
        }
        return names;
    }

    // ---------------------------------------------------------------- external vocabularies

    @Override
    public List<ExternalVocabulary> externalVocabularies() {
        Model model = construct("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } "
                + "FILTER(STRSTARTS(STR(?g), \"" + EXT_META + "\")) }");
        List<ExternalVocabulary> result = new ArrayList<>();
        model.listSubjectsWithProperty(RDF.type, EXTERNAL_TYPE).forEachRemaining(r -> result.add(readExternal(r)));
        result.sort(Comparator.comparing(ExternalVocabulary::name));
        return result;
    }

    public Optional<ExternalVocabulary> findExternal(String name) {
        Model model = construct("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <" + EXT_META + name + "> { ?s ?p ?o } }");
        var subjects = model.listSubjectsWithProperty(RDF.type, EXTERNAL_TYPE).toList();
        return subjects.isEmpty() ? Optional.empty() : Optional.of(readExternal(subjects.get(0)));
    }

    public void saveExternal(ExternalVocabulary x, Graph graph, String text) {
        UpdateRequest request = new UpdateRequest();
        Node meta = NodeFactory.createURI(EXT_META + x.name());
        Node content = NodeFactory.createURI(EXT_CONTENT + x.name());
        Node raw = NodeFactory.createURI(EXT_RAW + x.name());
        request.add(new UpdateDrop(meta, true));
        request.add(new UpdateDrop(content, true));
        request.add(new UpdateDrop(raw, true));
        QuadDataAcc quads = new QuadDataAcc();
        Node s = NodeFactory.createURI("urn:simpl:vm:external:" + x.name());
        add(quads, meta, s, RDF.type.asNode(), EXTERNAL_TYPE.asNode());
        add(quads, meta, s, NAME.asNode(), lit(x.name()));
        add(quads, meta, s, NAMESPACE.asNode(), lit(x.namespaceUri()));
        for (String alias : x.aliasUris()) {
            add(quads, meta, s, ALIAS.asNode(), lit(alias));
        }
        if (x.sourceUri() != null) {
            add(quads, meta, s, SOURCE.asNode(), lit(x.sourceUri()));
        }
        if (x.description() != null) {
            add(quads, meta, s, DESCRIPTION.asNode(), lit(x.description()));
        }
        add(quads, meta, s, CREATED_BY.asNode(), lit(x.createdBy()));
        add(quads, meta, s, CREATION_DATE.asNode(), date(x.creationDate()));
        graph.find().forEachRemaining(t -> quads.addQuad(new Quad(content, t)));
        add(quads, raw, raw, TTL.asNode(), lit(text));
        request.add(new UpdateDataInsert(quads));
        try (RDFConnection conn = connections.get()) {
            conn.update(request);
        }
    }

    public void deleteExternal(String name) {
        UpdateRequest request = new UpdateRequest();
        request.add(new UpdateDrop(NodeFactory.createURI(EXT_META + name), true));
        request.add(new UpdateDrop(NodeFactory.createURI(EXT_CONTENT + name), true));
        request.add(new UpdateDrop(NodeFactory.createURI(EXT_RAW + name), true));
        try (RDFConnection conn = connections.get()) {
            conn.update(request);
        }
    }

    public Optional<String> externalRaw(String name) {
        return selectString("SELECT ?t WHERE { GRAPH <" + EXT_RAW + name + "> { ?s <" + TTL.getURI() + "> ?t } }");
    }

    // ---------------------------------------------------------------- health

    public boolean ping() {
        try (RDFConnection conn = connections.get()) {
            return conn.queryAsk("ASK { }");
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- helpers

    private Model construct(String query) {
        try (RDFConnection conn = connections.get()) {
            return conn.queryConstruct(query);
        }
    }

    private Optional<String> selectString(String query) {
        List<String> values = new ArrayList<>();
        try (RDFConnection conn = connections.get()) {
            conn.querySelect(query, qs -> values.add(qs.getLiteral("t").getLexicalForm()));
        }
        return values.stream().findFirst();
    }

    private Vocabulary readVocabulary(Resource r) {
        Map<Integer, VocabularyVersion> versions = new LinkedHashMap<>();
        r.listProperties(HAS_VERSION).forEachRemaining(st -> {
            Resource vr = st.getResource();
            int number = vr.getProperty(VERSION).getInt();
            versions.put(number, new VocabularyVersion(number, str(vr, DESCRIPTION), str(vr, CHANGELOG),
                    Status.valueOf(str(vr, STATUS)), str(vr, CREATED_BY), instant(vr, CREATION_DATE),
                    instant(vr, LAST_UPDATE)));
        });
        return new Vocabulary(str(r, NAME), Status.valueOf(str(r, STATUS)), str(r, CREATED_BY),
                instant(r, CREATION_DATE), instant(r, LAST_UPDATE), new ArrayList<>(versions.values()));
    }

    private ExternalVocabulary readExternal(Resource r) {
        List<String> aliases = new ArrayList<>();
        r.listProperties(ALIAS).forEachRemaining(st -> aliases.add(st.getString()));
        aliases.sort(String::compareTo);
        return new ExternalVocabulary(str(r, NAME), str(r, NAMESPACE), aliases, str(r, SOURCE), str(r, DESCRIPTION),
                str(r, CREATED_BY), instant(r, CREATION_DATE));
    }

    private static String str(Resource r, Property p) {
        Statement st = r.getProperty(p);
        if (st == null) {
            return null;
        }
        RDFNode o = st.getObject();
        return o.isLiteral() ? ((Literal) o).getLexicalForm() : o.toString();
    }

    private static Instant instant(Resource r, Property p) {
        String s = str(r, p);
        return s == null ? null : Instant.parse(s);
    }

    private static void add(QuadDataAcc acc, Node g, Node s, Node p, Node o) {
        acc.addQuad(new Quad(g, s, p, o));
    }

    private static Node lit(String s) {
        return NodeFactory.createLiteralString(s == null ? "" : s);
    }

    private static Node date(Instant i) {
        return NodeFactory.createLiteralDT(i.toString(), XSDDatatype.XSDdateTime);
    }

    private static Property p(String local) {
        return ResourceFactory.createProperty(NS + local);
    }
}
