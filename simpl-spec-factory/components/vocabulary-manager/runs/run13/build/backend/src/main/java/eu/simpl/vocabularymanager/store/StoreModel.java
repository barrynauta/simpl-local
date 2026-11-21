package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 * The physical model in the RDF store (D87, docs/physical-model.md): three named graphs and the
 * vocabulary of the stored resources.
 */
public final class StoreModel {

    public static final String NS = "https://w3id.org/simpl/vocabulary-manager#";
    public static final Node GRAPH_INTERNAL = NodeFactory.createURI("urn:simpl:vocabulary-manager:graph:internal");
    public static final Node GRAPH_EXTERNAL = NodeFactory.createURI("urn:simpl:vocabulary-manager:graph:external");
    public static final Node GRAPH_CONTENT = NodeFactory.createURI("urn:simpl:vocabulary-manager:graph:content");

    public static final Node RDF_TYPE = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    public static final Node INTERNAL_VOCABULARY = term("InternalVocabulary");
    public static final Node INTERNAL_VOCABULARY_VERSION = term("InternalVocabularyVersion");
    public static final Node EXTERNAL_VOCABULARY = term("ExternalVocabulary");

    public static final Node NAME = term("name");
    public static final Node NAME_KEY = term("nameKey");
    public static final Node STATUS = term("status");
    public static final Node LATEST_VERSION = term("latestVersion");
    public static final Node VOCABULARY = term("vocabulary");
    public static final Node VERSION = term("version");
    public static final Node DESCRIPTION = term("description");
    public static final Node CHANGELOG = term("changelog");
    public static final Node CREATED_BY = term("createdBy");
    public static final Node CREATION_DATE = term("creationDate");
    public static final Node LAST_UPDATE_DATE = term("lastUpdateDate");
    public static final Node CONTENT = term("content");
    public static final Node CONTENT_SHA256 = term("contentSha256");
    public static final Node CONTENT_LENGTH = term("contentLength");
    public static final Node WRITE_TOKEN = term("writeToken");
    public static final Node STATUS_TOKEN = term("statusToken");
    public static final Node NAMESPACE_URI = term("namespaceUri");
    public static final Node NAMESPACE_KEY = term("namespaceKey");
    public static final Node ALIAS_LIST = term("aliasList");
    public static final Node ALIAS_KEY = term("aliasKey");
    public static final Node SOURCE_URI = term("sourceUri");

    private static final String VOCABULARY_IRI = "urn:simpl:vocabulary-manager:vocabulary:";
    private static final String EXTERNAL_IRI = "urn:simpl:vocabulary-manager:external:";

    private StoreModel() {}

    private static Node term(String local) {
        return NodeFactory.createURI(NS + local);
    }

    /** IRI of a vocabulary. Names are checked against the name pattern before they get here. */
    public static Node vocabularyIri(String name) {
        return NodeFactory.createURI(VOCABULARY_IRI + name);
    }

    public static Node versionIri(String name, int version) {
        return NodeFactory.createURI(VOCABULARY_IRI + name + ":version:" + version);
    }

    public static Node externalIri(String name) {
        return NodeFactory.createURI(EXTERNAL_IRI + name);
    }

    public static Node string(String value) {
        return NodeFactory.createLiteralString(value);
    }

    public static Node integer(long value) {
        return NodeFactory.createLiteralDT(Long.toString(value), XSDDatatype.XSDinteger);
    }

    /** Times are stored in UTC to the second (D58). */
    public static Node dateTime(Instant value) {
        return NodeFactory.createLiteralDT(value.truncatedTo(ChronoUnit.SECONDS).toString(), XSDDatatype.XSDdateTime);
    }
}
