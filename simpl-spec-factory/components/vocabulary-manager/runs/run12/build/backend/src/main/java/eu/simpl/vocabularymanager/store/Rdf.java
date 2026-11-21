package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

/**
 * The physical RDF model (docs/physical-model-shapes.ttl): four named graphs and the vm: terms.
 * Identifiers are IRIs built from the vocabulary name, which is restricted to [A-Za-z0-9].
 */
public final class Rdf {

    public static final String NS = "https://w3id.org/simpl/vocabulary-manager#";
    public static final String G_INTERNAL = "urn:simpl:vm:graph:internal";
    public static final String G_CONTENT = "urn:simpl:vm:graph:content";
    public static final String G_EXTERNAL = "urn:simpl:vm:graph:external";
    public static final String G_EXTERNAL_CONTENT = "urn:simpl:vm:graph:external-content";

    /** Bytes per stored content chunk (D106: content stays in the store, in chunks). */
    public static final int CHUNK_BYTES = 1024 * 1024;

    private Rdf() {}

    public static String vocabularyIri(String name) {
        return "urn:simpl:vm:vocabulary:" + name;
    }

    public static String versionIri(String name, int version) {
        return vocabularyIri(name) + ":version:" + version;
    }

    public static String externalIri(String name) {
        return "urn:simpl:vm:external:" + name;
    }

    /** A query or update with the vm:, xsd: and graph prefixes set. */
    public static ParameterizedSparqlString pss(String body) {
        ParameterizedSparqlString p = new ParameterizedSparqlString();
        p.setNsPrefix("vm", NS);
        p.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        p.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        p.setCommandText(body);
        p.setIri("gInternal", G_INTERNAL);
        p.setIri("gContent", G_CONTENT);
        p.setIri("gExternal", G_EXTERNAL);
        p.setIri("gExternalContent", G_EXTERNAL_CONTENT);
        return p;
    }

    public static Node dateTime(Instant t) {
        String lex = DateTimeFormatter.ISO_INSTANT.format(t.truncatedTo(ChronoUnit.SECONDS));
        return NodeFactory.createLiteralDT(lex, XSDDatatype.XSDdateTime);
    }

    public static Node integer(long n) {
        return NodeFactory.createLiteralDT(Long.toString(n), XSDDatatype.XSDinteger);
    }

    public static Node string(String s) {
        return NodeFactory.createLiteralString(s);
    }

    public static Node base64(String s) {
        return NodeFactory.createLiteralDT(s, XSDDatatype.XSDbase64Binary);
    }

    public static String str(RDFNode n) {
        if (n == null) {
            return null;
        }
        return n.isLiteral() ? ((Literal) n).getLexicalForm() : n.toString();
    }

    public static Instant instant(RDFNode n) {
        return n == null ? null : Instant.parse(((Literal) n).getLexicalForm());
    }

    public static int integerValue(RDFNode n) {
        return Integer.parseInt(((Literal) n).getLexicalForm());
    }
}
