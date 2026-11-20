package eu.simpl.vocabularymanager.store;

/**
 * Physical model in the RDF store: named graphs and IRIs (docs/physical-model.md, SHACL shapes in docs/shapes).
 * Names are checked against the D37 pattern before they reach an IRI.
 */
public final class StoreModel {

    /** Namespace of the store vocabulary. */
    public static final String VM = "https://simpl-programme.ec.europa.eu/ns/vocabulary-manager#";

    /** Graph with the metadata of all vocabularies and vocabulary versions. */
    public static final String VOCABULARY_GRAPH = "urn:simpl:vocabulary-manager:graph:vocabularies";

    /** Graph with the metadata of all external vocabularies. */
    public static final String EXTERNAL_GRAPH = "urn:simpl:vocabulary-manager:graph:external-vocabularies";

    /** Prologue of every query and update. */
    public static final String PREFIXES = "PREFIX vm: <" + VM + ">\nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

    private static final String BASE = "urn:simpl:vocabulary-manager:";

    private StoreModel() {}

    /** IRI of a vocabulary. */
    public static String vocabularyIri(String name) {
        return BASE + "vocabulary:" + name;
    }

    /** IRI of a vocabulary version. */
    public static String versionIri(String name, int version) {
        return vocabularyIri(name) + ":version:" + version;
    }

    /** Named graph holding the content of one version. */
    public static String versionContentGraph(String name, int version) {
        return BASE + "content:" + name + ":" + version;
    }

    /** IRI of an external vocabulary. */
    public static String externalIri(String name) {
        return BASE + "external-vocabulary:" + name;
    }

    /** IRI of the n-th alias of an external vocabulary. */
    public static String aliasIri(String name, int index) {
        return externalIri(name) + ":alias:" + index;
    }

    /** Named graph holding the content of an external vocabulary. */
    public static String externalContentGraph(String name) {
        return BASE + "external-content:" + name;
    }
}
