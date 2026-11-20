package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ParameterizedSparqlString;

/**
 * The physical model in the RDF store: named graphs, IRIs and properties. docs/physical-model.ttl
 * holds the SHACL shapes of each named graph; an own test runs them against stored data (D87).
 */
public final class StoreSchema {

    public static final String NS = "https://w3id.org/simpl/vocabulary-manager#";
    public static final String GRAPH_INTERNAL = "urn:simpl:vm:graph:internal";
    public static final String GRAPH_INTERNAL_CONTENT = "urn:simpl:vm:graph:internal-content";
    public static final String GRAPH_EXTERNAL = "urn:simpl:vm:graph:external";
    public static final String GRAPH_EXTERNAL_CONTENT = "urn:simpl:vm:graph:external-content";

    public static final String PREFIXES = "PREFIX vm: <" + NS + ">\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";

    private StoreSchema() {}

    public static String vocabularyIri(String name) {
        return "urn:simpl:vm:vocabulary:" + name;
    }

    public static String versionIri(String name, int version) {
        return vocabularyIri(name) + ":version:" + version;
    }

    public static String externalIri(String name) {
        return "urn:simpl:vm:external:" + name;
    }

    /** A new query or update text with the prefixes and the graph IRIs bound. */
    public static ParameterizedSparqlString sparql(String body) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(PREFIXES + body);
        pss.setIri("gInternal", GRAPH_INTERNAL);
        pss.setIri("gContent", GRAPH_INTERNAL_CONTENT);
        pss.setIri("gExternal", GRAPH_EXTERNAL);
        pss.setIri("gExternalContent", GRAPH_EXTERNAL_CONTENT);
        return pss;
    }

    /** Times are stored to the second in UTC (D58). */
    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public static void setTime(ParameterizedSparqlString pss, String variable, Instant time) {
        pss.setLiteral(variable, format(time), XSDDatatype.XSDdateTime);
    }

    public static String format(Instant time) {
        return DateTimeFormatter.ISO_INSTANT.format(time.truncatedTo(ChronoUnit.SECONDS));
    }

    public static Instant instant(Node node) {
        return Instant.parse(node.getLiteralLexicalForm());
    }

    public static int integer(Node node) {
        return Integer.parseInt(node.getLiteralLexicalForm());
    }

    public static String text(Node node) {
        return node == null ? null : node.getLiteralLexicalForm();
    }
}
