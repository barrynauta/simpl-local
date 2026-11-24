package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Names of the physical RDF model (docs/data-model.md, D87) and helpers to build SPARQL with
 * {@link SparqlTemplate} (D94): values are never joined into query text unescaped.
 */
public final class Sparql {

    public static final String VM = "https://w3id.org/simpl/vocabulary-manager#";
    public static final String META_GRAPH = "urn:simpl:vm:graph:meta";
    public static final String RECEIPT_GRAPH = "urn:simpl:vm:graph:receipts";
    public static final String PREFIXES = "PREFIX vm: <" + VM + ">\nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

    private Sparql() {}

    /** A new parameterised command with the prefixes. */
    public static SparqlTemplate command(String text) {
        return new SparqlTemplate(PREFIXES + text);
    }

    public static String vocabularyIri(String name) {
        return "urn:simpl:vm:vocabulary:" + name;
    }

    public static String versionIri(String name, long version) {
        return "urn:simpl:vm:vocabulary:" + name + ":v" + version;
    }

    public static String contentGraph(String name, long version) {
        return "urn:simpl:vm:graph:content:" + name + ":v" + version;
    }

    public static String externalIri(String name) {
        return "urn:simpl:vm:external:" + name;
    }

    public static String externalContentGraph(String name) {
        return "urn:simpl:vm:graph:external-content:" + name;
    }

    public static String receiptIri(String id) {
        return "urn:simpl:vm:receipt:" + id;
    }

    /** An xsd:integer literal. */
    public static Literal integer(long value) {
        return ResourceFactory.createTypedLiteral(String.valueOf(value), XSDDatatype.XSDinteger);
    }

    /** An xsd:dateTime literal in UTC to the second (D58). */
    public static Literal dateTime(Instant instant) {
        return ResourceFactory.createTypedLiteral(instant.toString(), XSDDatatype.XSDdateTime);
    }

    /** A plain string literal. */
    public static Literal text(String value) {
        return ResourceFactory.createStringLiteral(value);
    }

    /** Reads a string value, or null. */
    public static String string(QuerySolution row, String variable) {
        RDFNode node = row.get(variable);
        return node == null ? null : node.asLiteral().getLexicalForm();
    }

    /** Reads an integer value. */
    public static long number(QuerySolution row, String variable) {
        return row.getLiteral(variable).getLong();
    }

    /** Reads an xsd:dateTime value. */
    public static Instant instant(QuerySolution row, String variable) {
        return Instant.parse(row.getLiteral(variable).getLexicalForm());
    }
}
