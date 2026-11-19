package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * The physical model in the RDF store (docs/physical-model.shacl.ttl) and safe SPARQL term formatting. Every value
 * that goes into a SPARQL string goes through these helpers, so no input reaches the store unescaped (OWASP-04).
 */
public final class StoreModel {

    public static final String NS = "urn:simpl:vocabulary-manager:ns#";
    public static final String PREFIXES = "PREFIX vm: <" + NS + ">\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";
    /** Vocabularies and their versions (metadata only). */
    public static final String VOCABULARY_GRAPH = "<urn:simpl:vocabulary-manager:graph:vocabularies>";
    /** External vocabularies (metadata only). */
    public static final String EXTERNAL_GRAPH = "<urn:simpl:vocabulary-manager:graph:external-vocabularies>";
    /** Change records: one per applied write, used to confirm conditional updates (D82) and as an audit trail. */
    public static final String CHANGE_GRAPH = "<urn:simpl:vocabulary-manager:graph:changes>";

    private StoreModel() {}

    /**
     * @param name a validated vocabulary name
     * @return the IRI of the vocabulary resource
     */
    public static String vocabularyIri(String name) {
        return FmtUtils.stringForURI("urn:simpl:vocabulary-manager:vocabulary:" + name);
    }

    /**
     * @param name a validated vocabulary name
     * @param version the version number
     * @return the IRI of the version resource
     */
    public static String versionIri(String name, long version) {
        return FmtUtils.stringForURI("urn:simpl:vocabulary-manager:vocabulary:" + name + ":version:" + version);
    }

    /**
     * @param name a validated external vocabulary name
     * @return the IRI of the external vocabulary resource
     */
    public static String externalIri(String name) {
        return FmtUtils.stringForURI("urn:simpl:vocabulary-manager:external-vocabulary:" + name);
    }

    /**
     * @param id a random id
     * @return the IRI of a content graph
     */
    public static String contentGraphIri(String id) {
        return FmtUtils.stringForURI("urn:simpl:vocabulary-manager:content:" + id);
    }

    /**
     * @param token the change token
     * @return the IRI of a change record
     */
    public static String changeIri(String token) {
        return FmtUtils.stringForURI("urn:simpl:vocabulary-manager:change:" + token);
    }

    /**
     * @param value any string
     * @return an escaped SPARQL string literal
     */
    public static String str(String value) {
        return FmtUtils.stringForString(value);
    }

    /**
     * @param values strings
     * @return escaped literals separated by spaces, for VALUES
     */
    public static String strings(Collection<String> values) {
        return values.stream().map(StoreModel::str).collect(Collectors.joining(" "));
    }

    /**
     * @param value a number
     * @return an xsd:integer literal
     */
    public static String integer(long value) {
        return "\"" + value + "\"^^xsd:integer";
    }

    /**
     * @param instant a time, stored in UTC to the second (D58)
     * @return an xsd:dateTime literal
     */
    public static String dateTime(Instant instant) {
        return "\"" + instant.truncatedTo(ChronoUnit.SECONDS) + "\"^^xsd:dateTime";
    }

    /**
     * @param solution a query solution
     * @param variable a variable name
     * @return the lexical form, or null when unbound
     */
    public static String text(QuerySolution solution, String variable) {
        RDFNode node = solution.get(variable);
        if (node == null) {
            return null;
        }
        return node.isLiteral() ? node.asLiteral().getLexicalForm() : node.toString();
    }

    /**
     * @param solution a query solution
     * @param variable a variable name
     * @return the number, or -1 when unbound
     */
    public static long number(QuerySolution solution, String variable) {
        String value = text(solution, variable);
        return value == null ? -1 : Long.parseLong(value);
    }

    /**
     * @param solution a query solution
     * @param variable a variable name
     * @return the time, or null when unbound
     */
    public static Instant instant(QuerySolution solution, String variable) {
        String value = text(solution, variable);
        return value == null ? null : Instant.parse(value);
    }
}
