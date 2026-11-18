package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;

/** The RDF terms the component uses in its store, and conversions between Java values and literals. */
final class StoreModel {

    static final String NS = "https://simpl.example/ns/vocabulary-manager#";
    static final String INTERNAL_GRAPH = "urn:simpl:vocabulary-manager:graph:internal";
    static final String EXTERNAL_GRAPH = "urn:simpl:vocabulary-manager:graph:external";
    static final String PREFIXES = "PREFIX vm: <" + NS + ">\nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

    private StoreModel() {}

    static String vocabularyIri(String name) {
        return "urn:simpl:vocabulary-manager:vocabulary:" + name;
    }

    static String versionIri(String name, int version) {
        return vocabularyIri(name) + ":version:" + version;
    }

    static String externalIri(String name) {
        return "urn:simpl:vocabulary-manager:external:" + name;
    }

    static ParameterizedSparqlString command(String text) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(PREFIXES + text);
        pss.setIri("internalGraph", INTERNAL_GRAPH);
        pss.setIri("externalGraph", EXTERNAL_GRAPH);
        return pss;
    }

    static Literal dateTime(Instant instant) {
        return ResourceFactory.createTypedLiteral(instant.toString(), XSDDatatype.XSDdateTime);
    }

    /**
     * Marker bound in place of the content and replaced after the command is built (see {@link #withContent}). It is
     * random per process, so no client text can collide with it.
     */
    private static final String MARKER_TEXT = "vm-content-" + UUID.randomUUID();

    static final Literal CONTENT_MARKER = ResourceFactory.createPlainLiteral(MARKER_TEXT);

    /**
     * Puts the Base64 content into a built command. Base64 text needs no escaping in a quoted literal, and doing this
     * after ParameterizedSparqlString keeps large uploads fast.
     */
    static String withContent(ParameterizedSparqlString command, byte[] bytes) {
        return command.toString()
                .replace("\"" + MARKER_TEXT + "\"", "\"" + Base64.getEncoder().encodeToString(bytes) + "\"");
    }

    static Literal integer(int value) {
        return ResourceFactory.createTypedLiteral(Integer.toString(value), XSDDatatype.XSDinteger);
    }

    static String text(QuerySolution row, String variable) {
        RDFNode node = row.get(variable);
        return node == null ? null : node.asLiteral().getLexicalForm();
    }

    static int number(QuerySolution row, String variable) {
        return Integer.parseInt(text(row, variable));
    }

    static Instant instant(QuerySolution row, String variable) {
        String value = text(row, variable);
        return value == null ? null : Instant.parse(value);
    }

    static byte[] bytes(QuerySolution row, String variable) {
        return Base64.getDecoder().decode(text(row, variable));
    }
}
