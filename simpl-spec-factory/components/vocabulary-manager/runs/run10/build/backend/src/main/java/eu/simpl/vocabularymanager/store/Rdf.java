package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Names of the physical RDF model (docs/physical-model-shapes.ttl): the vocabulary of the store, the named graphs and
 * the IRI scheme of stored resources.
 */
public final class Rdf {

    public static final String NS = "https://simpl-open.eu/ns/vocabulary-manager#";
    public static final String PREFIXES = "PREFIX vm: <" + NS + ">\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";
    public static final String INTERNAL_GRAPH = "urn:simpl:vocabulary-manager:graph:internal";
    public static final String EXTERNAL_GRAPH = "urn:simpl:vocabulary-manager:graph:external";
    private static final String BASE = "urn:simpl:vocabulary-manager:";

    public static final Node CHUNK_INDEX = NodeFactory.createURI(NS + "chunkIndex");
    public static final Node CHUNK_DATA = NodeFactory.createURI(NS + "chunkData");
    public static final Node CHUNK_CLASS = NodeFactory.createURI(NS + "ContentChunk");
    public static final Node RDF_TYPE = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    private Rdf() {}

    public static String vocabularyIri(String name) {
        return BASE + "vocabulary:" + name;
    }

    public static String versionIri(String name, int version) {
        return BASE + "vocabulary:" + name + ":version:" + version;
    }

    public static String contentGraph(String name, int version) {
        return BASE + "content:" + name + ":" + version;
    }

    public static String externalIri(String name) {
        return BASE + "external:" + name;
    }

    public static String externalAliasIri(String name, int index) {
        return BASE + "external:" + name + ":alias:" + index;
    }

    public static String externalContentGraph(String name) {
        return BASE + "external-content:" + name;
    }

    public static Literal integer(long value) {
        return ResourceFactory.createTypedLiteral(Long.toString(value), XSDDatatype.XSDinteger);
    }

    public static Literal dateTime(Instant instant) {
        return ResourceFactory.createTypedLiteral(
                DateTimeFormatter.ISO_INSTANT.format(instant), XSDDatatype.XSDdateTime);
    }

    public static Literal text(String value) {
        return ResourceFactory.createStringLiteral(value);
    }

    public static String string(QuerySolution row, String variable) {
        RDFNode node = row.get(variable);
        if (node == null) {
            return null;
        }
        return node.isLiteral() ? node.asLiteral().getLexicalForm() : node.toString();
    }

    public static int integer(QuerySolution row, String variable) {
        return Integer.parseInt(string(row, variable));
    }

    public static long longValue(QuerySolution row, String variable) {
        return Long.parseLong(string(row, variable));
    }

    public static Instant instant(QuerySolution row, String variable) {
        String value = string(row, variable);
        return value == null ? null : Instant.parse(value);
    }
}
