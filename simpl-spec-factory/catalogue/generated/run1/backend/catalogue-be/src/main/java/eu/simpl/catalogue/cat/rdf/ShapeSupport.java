package eu.simpl.catalogue.cat.rdf;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.RDF;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** SHACL shape handling: parsing, top-level shape detection (D58) and validation (D14 step 6, D63). */
public final class ShapeSupport {
    public static final String SIMPL = "http://w3id.org/gaia-x/simpl#";
    public static final Set<String> OFFERING_CLASSES = Set.of(
            SIMPL + "DataOffering", SIMPL + "ApplicationOffering", SIMPL + "InfrastructureOffering");

    private ShapeSupport() {}

    public static Graph parseTurtle(String ttl) {
        Graph g = GraphFactory.createDefaultGraph();
        RDFParser.fromString(ttl, Lang.TURTLE).parse(g);
        return g;
    }

    public static Graph parseJsonLd(String json) {
        Graph g = GraphFactory.createDefaultGraph();
        RDFParser.fromString(json, Lang.JSONLD11).parse(g);
        return g;
    }

    public static String toTurtle(Graph g) {
        Model m = ModelFactory.createModelForGraph(g);
        m.setNsPrefix("sh", SHACL.getURI());
        m.setNsPrefix("simpl", SIMPL);
        m.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        StringWriter w = new StringWriter();
        RDFDataMgr.write(w, m, Lang.TURTLE);
        return w.toString();
    }

    /** Local name of the top-level NodeShape targeting an offering class, or null. */
    public static String offeringShapeId(Graph g) {
        for (Triple t : g.find(Node.ANY, RDF.type.asNode(), SHACL.NodeShape).toList()) {
            for (Triple tc : g.find(t.getSubject(), SHACL.targetClass, Node.ANY).toList()) {
                if (tc.getObject().isURI() && OFFERING_CLASSES.contains(tc.getObject().getURI()) && t.getSubject().isURI()) {
                    return localName(t.getSubject().getURI());
                }
            }
        }
        return null;
    }

    /** NodeShapes that no other shape references with sh:node. */
    public static List<Node> topLevelShapes(Graph g) {
        List<Node> out = new ArrayList<>();
        for (Triple t : g.find(Node.ANY, RDF.type.asNode(), SHACL.NodeShape).toList()) {
            Node s = t.getSubject();
            if (!g.find(Node.ANY, SHACL.node, s).hasNext()) out.add(s);
        }
        return out;
    }

    public static Node shapeNamed(Graph g, String localName) {
        for (Triple t : g.find(Node.ANY, RDF.type.asNode(), SHACL.NodeShape).toList()) {
            if (t.getSubject().isURI() && localName(t.getSubject().getURI()).equals(localName)) return t.getSubject();
        }
        return null;
    }

    public static String localName(String iri) {
        int i = Math.max(Math.max(iri.lastIndexOf('#'), iri.lastIndexOf('/')), iri.lastIndexOf(':'));
        return i < 0 ? iri : iri.substring(i + 1);
    }

    /**
     * Validates the data graph against one shape graph. The subject node is additionally declared a target
     * of the top-level shape, so the RD is checked even if its rdf:type differs from the target class.
     * Returns the list of violated paths (empty when the graph conforms).
     */
    public static List<String> validate(Graph shapesGraph, String topShapeLocalName, Graph data, String subjectIri) {
        Graph sg = GraphFactory.createDefaultGraph();
        shapesGraph.find().forEachRemaining(sg::add);
        Node top = shapeNamed(sg, topShapeLocalName);
        if (top != null && subjectIri != null) {
            sg.add(Triple.create(top, SHACL.targetNode, NodeFactory.createURI(subjectIri)));
        }
        Shapes shapes = Shapes.parse(sg);
        ValidationReport report = ShaclValidator.get().validate(shapes, data);
        Set<String> paths = new LinkedHashSet<>();
        for (ReportEntry e : report.getEntries()) {
            String path = e.resultPath() == null ? "(node)" : compact(e.resultPath().toString());
            String parent = parentPath(data, e.focusNode());
            paths.add((parent == null ? "" : parent + "/") + path + ": " + e.message());
        }
        return new ArrayList<>(paths);
    }

    private static String parentPath(Graph data, Node focus) {
        if (focus == null || !focus.isBlank()) return null;
        List<Triple> up = data.find(Node.ANY, Node.ANY, focus).toList();
        if (up.isEmpty()) return null;
        String p = compact("<" + up.get(0).getPredicate().getURI() + ">");
        String grand = parentPath(data, up.get(0).getSubject());
        return grand == null ? p : grand + "/" + p;
    }

    private static String compact(String path) {
        String s = path;
        if (s.startsWith("<") && s.endsWith(">")) s = s.substring(1, s.length() - 1);
        if (s.startsWith(SIMPL)) return "simpl:" + s.substring(SIMPL.length());
        return s;
    }
}
