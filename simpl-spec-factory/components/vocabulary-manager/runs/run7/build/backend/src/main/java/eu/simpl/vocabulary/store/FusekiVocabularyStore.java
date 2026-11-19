package eu.simpl.vocabulary.store;

import eu.simpl.vocabulary.domain.model.ExternalVocabulary;
import eu.simpl.vocabulary.domain.model.StoredContent;
import eu.simpl.vocabulary.domain.model.Vocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.domain.model.VocabularyVersion;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.InternalFacts;
import eu.simpl.vocabulary.domain.validation.ReferenceAnalyzer.ReferenceFacts;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.out.NodeFmtLib;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link VocabularyStore} on Apache Jena Fuseki (E3), dataset 'vocabulary'. Metadata lives in one named
 * graph; the bytes of each version and external vocabulary live base64-encoded in a named graph of their
 * own, so they are returned exactly as uploaded (D29). Every value is written as an RDF term formatted by
 * Jena (no string concatenation of input into SPARQL, OWT-A05).
 */
public class FusekiVocabularyStore implements VocabularyStore {

    static final String MODEL = "urn:simpl:vocabulary-manager:model#";
    static final String META = "<urn:simpl:vocabulary-manager:graph:metadata>";
    private static final String PREFIX = "PREFIX vm: <" + MODEL + ">\n";
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9]{1,64}$");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    static final int CHUNK_CHARS = 65536;

    private final SparqlClient client;

    public FusekiVocabularyStore(SparqlClient client) {
        this.client = client;
    }

    // ---------------------------------------------------------------- internal vocabularies

    @Override
    public Optional<Vocabulary> findVocabulary(String name) {
        String query =
                PREFIX + "SELECT ?name ?status ?createdBy ?creationDate ?lastUpdateDate ?latest ?description WHERE {"
                        + " VALUES ?v { " + vocabularyIri(name) + " }"
                        + vocabularyPattern() + " }";
        return client.select(query).stream().findFirst().map(FusekiVocabularyStore::toVocabulary);
    }

    @Override
    public boolean vocabularyNameTaken(String name) {
        String query = PREFIX + "SELECT ?v WHERE { GRAPH " + META + " { ?v a vm:Vocabulary ; vm:nameKey "
                + literal(name.toLowerCase(Locale.ROOT)) + " } } LIMIT 1";
        return !client.select(query).isEmpty();
    }

    @Override
    public List<Vocabulary> listVocabularies() {
        String query =
                PREFIX + "SELECT ?name ?status ?createdBy ?creationDate ?lastUpdateDate ?latest ?description WHERE {"
                        + vocabularyPattern() + " }";
        return client.select(query).stream()
                .map(FusekiVocabularyStore::toVocabulary)
                .toList();
    }

    private static String vocabularyPattern() {
        return " GRAPH " + META + " { ?v a vm:Vocabulary ; vm:name ?name ; vm:status ?status ;"
                + " vm:createdBy ?createdBy ; vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate ;"
                + " vm:latestVersion ?latest . ?ver vm:vocabulary ?v ; vm:number ?latest ; vm:description ?description . }";
    }

    private static Vocabulary toVocabulary(QuerySolution row) {
        return new Vocabulary(
                text(row, "name"),
                VocabularyStatus.valueOf(text(row, "status")),
                text(row, "createdBy"),
                instant(row, "creationDate"),
                instant(row, "lastUpdateDate"),
                number(row, "latest"),
                text(row, "description"));
    }

    @Override
    public List<VocabularyVersion> listVersions(String name) {
        String query = PREFIX
                + "SELECT ?number ?description ?changelog ?status ?createdBy ?creationDate ?lastUpdateDate ?sha WHERE {"
                + " VALUES ?v { " + vocabularyIri(name) + " }" + versionPattern() + " }";
        return client.select(query).stream().map(row -> toVersion(name, row)).toList();
    }

    @Override
    public Optional<VocabularyVersion> findVersion(String name, long version) {
        String query = PREFIX
                + "SELECT ?number ?description ?changelog ?status ?createdBy ?creationDate ?lastUpdateDate ?sha WHERE {"
                + " VALUES ?v { " + vocabularyIri(name) + " } VALUES ?number { " + integer(version) + " }"
                + versionPattern() + " }";
        return client.select(query).stream().findFirst().map(row -> toVersion(name, row));
    }

    private static String versionPattern() {
        return " GRAPH " + META + " { ?ver vm:vocabulary ?v ; vm:number ?number ; vm:description ?description ;"
                + " vm:changelog ?changelog ; vm:status ?status ; vm:createdBy ?createdBy ;"
                + " vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate ; vm:contentSha256 ?sha . }";
    }

    private static VocabularyVersion toVersion(String name, QuerySolution row) {
        return new VocabularyVersion(
                name,
                number(row, "number"),
                text(row, "description"),
                text(row, "changelog"),
                VocabularyStatus.valueOf(text(row, "status")),
                text(row, "createdBy"),
                instant(row, "creationDate"),
                instant(row, "lastUpdateDate"),
                text(row, "sha"));
    }

    @Override
    public Optional<StoredContent> findContent(String name, long version) {
        return readContent(contentIri(name, version)).map(bytes -> new StoredContent(name, version, bytes));
    }

    @Override
    public Optional<StoredContent> findLatestContent(String name) {
        Optional<Vocabulary> vocabulary = findVocabulary(name);
        if (vocabulary.isEmpty()) {
            return Optional.empty();
        }
        long latest = vocabulary.get().latestVersion();
        return findContent(name, latest);
    }

    @Override
    public void createVocabulary(Vocabulary vocabulary, VocabularyVersion first, byte[] content) {
        String v = vocabularyIri(vocabulary.name());
        StringBuilder update = new StringBuilder(PREFIX)
                .append("INSERT DATA { GRAPH ")
                .append(META)
                .append(" { ");
        update.append(v)
                .append(" a vm:Vocabulary ; vm:name ")
                .append(literal(vocabulary.name()))
                .append(" ; vm:nameKey ")
                .append(literal(vocabulary.name().toLowerCase(Locale.ROOT)))
                .append(" ; vm:status ")
                .append(literal(vocabulary.status().name()))
                .append(" ; vm:createdBy ")
                .append(literal(vocabulary.createdBy()))
                .append(" ; vm:creationDate ")
                .append(dateTime(vocabulary.creationDate()))
                .append(" ; vm:lastUpdateDate ")
                .append(dateTime(vocabulary.lastUpdateDate()))
                .append(" ; vm:latestVersion ")
                .append(integer(first.version()))
                .append(" . ");
        appendVersionTriples(update, first);
        update.append(" } ");
        appendContentGraph(update, contentIri(first.name(), first.version()), content);
        update.append(" }");
        client.update(update.toString());
    }

    @Override
    public void createVersion(VocabularyVersion version, byte[] content) {
        String v = vocabularyIri(version.name());
        String now = dateTime(version.creationDate());
        StringBuilder update = new StringBuilder(PREFIX);
        update.append("DELETE { GRAPH ")
                .append(META)
                .append(" { ?old vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldDate } }")
                .append(" INSERT { GRAPH ")
                .append(META)
                .append(" { ?old vm:status \"DEPRECATED\" ; vm:lastUpdateDate ")
                .append(now)
                .append(" } } WHERE { GRAPH ")
                .append(META)
                .append(" { ?old vm:vocabulary ")
                .append(v)
                .append(" ; vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldDate } } ;\n");
        update.append("DELETE { GRAPH ")
                .append(META)
                .append(" { ")
                .append(v)
                .append(" vm:latestVersion ?latest ; vm:lastUpdateDate ?updated } } INSERT { GRAPH ")
                .append(META)
                .append(" { ")
                .append(v)
                .append(" vm:latestVersion ")
                .append(integer(version.version()))
                .append(" ; vm:lastUpdateDate ")
                .append(now)
                .append(" } } WHERE { GRAPH ")
                .append(META)
                .append(" { ")
                .append(v)
                .append(" vm:latestVersion ?latest ; vm:lastUpdateDate ?updated } } ;\n");
        update.append("INSERT DATA { GRAPH ").append(META).append(" { ");
        appendVersionTriples(update, version);
        update.append(" } ");
        appendContentGraph(update, contentIri(version.name(), version.version()), content);
        update.append(" }");
        client.update(update.toString());
    }

    private static void appendVersionTriples(StringBuilder update, VocabularyVersion version) {
        update.append(versionIri(version.name(), version.version()))
                .append(" a vm:VocabularyVersion ; vm:vocabulary ")
                .append(vocabularyIri(version.name()))
                .append(" ; vm:number ")
                .append(integer(version.version()))
                .append(" ; vm:description ")
                .append(literal(version.description()))
                .append(" ; vm:changelog ")
                .append(literal(version.changelog()))
                .append(" ; vm:status ")
                .append(literal(version.status().name()))
                .append(" ; vm:createdBy ")
                .append(literal(version.createdBy()))
                .append(" ; vm:creationDate ")
                .append(dateTime(version.creationDate()))
                .append(" ; vm:lastUpdateDate ")
                .append(dateTime(version.lastUpdateDate()))
                .append(" ; vm:contentSha256 ")
                .append(literal(version.contentSha256()))
                .append(" ; vm:content ")
                .append(contentIri(version.name(), version.version()))
                .append(" . ");
    }

    /**
     * Writes content as base64 in chunks of {@link #CHUNK_CHARS} characters: SPARQL parsers handle many
     * short literals much faster than one literal of several megabytes (D71, Q1).
     */
    private static void appendContentGraph(StringBuilder update, String graph, byte[] content) {
        String encoded = Base64.getEncoder().encodeToString(content);
        String graphIri = graph.substring(1, graph.length() - 1);
        update.append("GRAPH ")
                .append(graph)
                .append(" { ")
                .append(graph)
                .append(" vm:chunkCount ")
                .append(integer((encoded.length() + CHUNK_CHARS - 1) / CHUNK_CHARS))
                .append(" . ");
        for (int start = 0, index = 0; start < encoded.length(); start += CHUNK_CHARS, index++) {
            String chunk = encoded.substring(start, Math.min(encoded.length(), start + CHUNK_CHARS));
            update.append(iri(graphIri + ":chunk:" + index))
                    .append(" vm:index ")
                    .append(integer(index))
                    .append(" ; vm:data ")
                    .append(literal(chunk))
                    .append(" . ");
        }
        update.append("} ");
    }

    /**
     * Reads chunked content of a graph.
     *
     * @param graph the content graph, formatted
     * @return the bytes, empty when the graph holds no content
     */
    private Optional<byte[]> readContent(String graph) {
        String query = PREFIX + "SELECT ?count ?index ?data WHERE { GRAPH " + graph + " { " + graph
                + " vm:chunkCount ?count . OPTIONAL { ?chunk vm:index ?index ; vm:data ?data } } } ORDER BY ?index";
        List<QuerySolution> rows = client.select(query);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder encoded = new StringBuilder();
        long expected = number(rows.get(0), "count");
        long found = 0;
        for (QuerySolution row : rows) {
            if (row.contains("data")) {
                encoded.append(text(row, "data"));
                found++;
            }
        }
        if (found != expected) {
            throw new StoreException("Stored content is incomplete");
        }
        return Optional.of(decode(encoded.toString()));
    }

    @Override
    public void updateVocabularyStatus(String name, VocabularyStatus status, Instant now) {
        String v = vocabularyIri(name);
        String update =
                PREFIX + "DELETE { GRAPH " + META + " { " + v + " vm:status ?status ; vm:lastUpdateDate ?updated } }"
                        + " INSERT { GRAPH " + META + " { " + v + " vm:status " + literal(status.name())
                        + " ; vm:lastUpdateDate "
                        + dateTime(now) + " } } WHERE { GRAPH " + META + " { " + v
                        + " vm:status ?status ; vm:lastUpdateDate ?updated } }";
        client.update(update);
    }

    @Override
    public void updateVersionStatus(String name, long version, VocabularyStatus status, Instant now) {
        String v = vocabularyIri(name);
        String ver = versionIri(name, version);
        String update =
                PREFIX + "DELETE { GRAPH " + META + " { " + ver + " vm:status ?status ; vm:lastUpdateDate ?updated } }"
                        + " INSERT { GRAPH " + META + " { " + ver + " vm:status " + literal(status.name())
                        + " ; vm:lastUpdateDate "
                        + dateTime(now) + " } } WHERE { GRAPH " + META + " { " + ver
                        + " vm:status ?status ; vm:lastUpdateDate ?updated } } ;\n"
                        + "DELETE { GRAPH " + META + " { " + v + " vm:lastUpdateDate ?updated } } INSERT { GRAPH "
                        + META + " { " + v
                        + " vm:lastUpdateDate " + dateTime(now) + " } } WHERE { GRAPH " + META + " { " + v
                        + " vm:lastUpdateDate ?updated } }";
        client.update(update);
    }

    @Override
    public ReferenceFacts referenceFacts(Set<String> internalNames, boolean includeExternal) {
        List<String> parts = new ArrayList<>();
        if (!internalNames.isEmpty()) {
            StringBuilder values = new StringBuilder();
            internalNames.stream()
                    .filter(n -> SAFE_NAME.matcher(n).matches())
                    .forEach(n -> values.append(literal(n)).append(' '));
            parts.add("{ VALUES ?name { " + values + "} GRAPH " + META + " { ?v a vm:Vocabulary ; vm:name ?name ;"
                    + " vm:status ?vstatus . ?ver vm:vocabulary ?v ; vm:number ?number ; vm:status ?nstatus . } }");
        }
        if (includeExternal) {
            parts.add("{ GRAPH " + META + " { ?e a vm:ExternalVocabulary ; vm:lookupKey ?key } }");
        }
        Map<String, InternalFacts> vocabularies = new HashMap<>();
        Set<String> keys = new HashSet<>();
        if (parts.isEmpty()) {
            return new ReferenceFacts(vocabularies, keys);
        }
        String query =
                PREFIX + "SELECT ?name ?vstatus ?number ?nstatus ?key WHERE { " + String.join(" UNION ", parts) + " }";
        for (QuerySolution row : client.select(query)) {
            if (row.contains("key")) {
                keys.add(text(row, "key"));
            } else if (row.contains("name")) {
                String name = text(row, "name");
                InternalFacts facts = vocabularies.computeIfAbsent(
                        name,
                        n -> new InternalFacts(n, VocabularyStatus.valueOf(text(row, "vstatus")), new TreeMap<>()));
                facts.versions().put(number(row, "number"), VocabularyStatus.valueOf(text(row, "nstatus")));
            }
        }
        return new ReferenceFacts(vocabularies, keys);
    }

    // ---------------------------------------------------------------- external vocabularies

    @Override
    public boolean externalNameTaken(String name) {
        String query = PREFIX + "SELECT ?e WHERE { GRAPH " + META + " { ?e a vm:ExternalVocabulary ; vm:nameKey "
                + literal(name.toLowerCase(Locale.ROOT)) + " } } LIMIT 1";
        return !client.select(query).isEmpty();
    }

    @Override
    public List<ExternalVocabulary> listExternal() {
        String query = PREFIX + "SELECT ?name ?namespace ?aliases ?source ?description ?createdBy ?creationDate WHERE {"
                + externalPattern() + " }";
        return client.select(query).stream()
                .map(FusekiVocabularyStore::toExternal)
                .toList();
    }

    @Override
    public Optional<ExternalVocabulary> findExternal(String name) {
        String query = PREFIX + "SELECT ?name ?namespace ?aliases ?source ?description ?createdBy ?creationDate WHERE {"
                + " VALUES ?e { " + externalIri(name) + " }" + externalPattern() + " }";
        return client.select(query).stream().findFirst().map(FusekiVocabularyStore::toExternal);
    }

    private static String externalPattern() {
        return " GRAPH " + META + " { ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?namespace ;"
                + " vm:aliasUris ?aliases ; vm:createdBy ?createdBy ; vm:creationDate ?creationDate ."
                + " OPTIONAL { ?e vm:sourceUri ?source } OPTIONAL { ?e vm:description ?description } }";
    }

    private static ExternalVocabulary toExternal(QuerySolution row) {
        List<String> aliases = JSON.readValue(text(row, "aliases"), new TypeReference<List<String>>() {});
        return new ExternalVocabulary(
                text(row, "name"),
                text(row, "namespace"),
                aliases,
                row.contains("source") ? text(row, "source") : null,
                row.contains("description") ? text(row, "description") : null,
                text(row, "createdBy"),
                instant(row, "creationDate"));
    }

    @Override
    public Optional<StoredContent> findExternalContent(String name) {
        return readContent(externalContentIri(name)).map(bytes -> new StoredContent(name, 0, bytes));
    }

    @Override
    public void createExternal(ExternalVocabulary external, byte[] content) {
        String e = externalIri(external.name());
        StringBuilder update = new StringBuilder(PREFIX)
                .append("INSERT DATA { GRAPH ")
                .append(META)
                .append(" { ");
        update.append(e)
                .append(" a vm:ExternalVocabulary ; vm:name ")
                .append(literal(external.name()))
                .append(" ; vm:nameKey ")
                .append(literal(external.name().toLowerCase(Locale.ROOT)))
                .append(" ; vm:namespaceUri ")
                .append(literal(external.namespaceUri()))
                .append(" ; vm:aliasUris ")
                .append(literal(JSON.writeValueAsString(external.aliasUris())))
                .append(" ; vm:createdBy ")
                .append(literal(external.createdBy()))
                .append(" ; vm:creationDate ")
                .append(dateTime(external.creationDate()))
                .append(" ; vm:content ")
                .append(externalContentIri(external.name()));
        if (external.sourceUri() != null) {
            update.append(" ; vm:sourceUri ").append(literal(external.sourceUri()));
        }
        if (external.description() != null) {
            update.append(" ; vm:description ").append(literal(external.description()));
        }
        Set<String> keys = new HashSet<>();
        keys.add(eu.simpl.vocabulary.api.request.FieldRules.normaliseNamespace(external.namespaceUri()));
        external.aliasUris().forEach(a -> keys.add(eu.simpl.vocabulary.api.request.FieldRules.normaliseNamespace(a)));
        for (String key : keys) {
            update.append(" ; vm:lookupKey ").append(literal(key));
        }
        update.append(" . } ");
        appendContentGraph(update, externalContentIri(external.name()), content);
        update.append(" }");
        client.update(update.toString());
    }

    @Override
    public void deleteExternal(String name) {
        String e = externalIri(name);
        String update = PREFIX + "DELETE WHERE { GRAPH " + META + " { " + e + " ?p ?o } } ;\n" + "DROP SILENT GRAPH "
                + externalContentIri(name);
        client.update(update);
    }

    @Override
    public boolean ping() {
        return client.ping();
    }

    // ---------------------------------------------------------------- formatting helpers

    private static String safe(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Unsafe resource name");
        }
        return name;
    }

    static String vocabularyIri(String name) {
        return iri("urn:simpl:vocabulary-manager:vocabulary:" + safe(name));
    }

    static String versionIri(String name, long version) {
        return iri("urn:simpl:vocabulary-manager:vocabulary:" + safe(name) + ":version:" + version);
    }

    static String contentIri(String name, long version) {
        return iri("urn:simpl:vocabulary-manager:content:" + safe(name) + ":" + version);
    }

    static String externalIri(String name) {
        return iri("urn:simpl:vocabulary-manager:external:" + safe(name));
    }

    static String externalContentIri(String name) {
        return iri("urn:simpl:vocabulary-manager:external-content:" + safe(name));
    }

    private static String iri(String value) {
        return NodeFmtLib.strNT(NodeFactory.createURI(value));
    }

    static String literal(String value) {
        return NodeFmtLib.strNT(NodeFactory.createLiteralString(value));
    }

    private static String integer(long value) {
        return NodeFmtLib.strNT(NodeFactory.createLiteralDT(Long.toString(value), XSDDatatype.XSDinteger));
    }

    private static String dateTime(Instant value) {
        return NodeFmtLib.strNT(NodeFactory.createLiteralDT(value.toString(), XSDDatatype.XSDdateTime));
    }

    private static String text(QuerySolution row, String variable) {
        RDFNode node = row.get(variable);
        if (node == null) {
            throw new StoreException("Store row misses " + variable);
        }
        if (node.isLiteral()) {
            return node.asLiteral().getLexicalForm();
        }
        Node graphNode = node.asNode();
        return graphNode.isURI() ? graphNode.getURI() : graphNode.toString();
    }

    private static long number(QuerySolution row, String variable) {
        Literal literal = row.getLiteral(variable);
        try {
            return Long.parseLong(literal.getLexicalForm());
        } catch (NumberFormatException e) {
            throw new StoreException("Store value " + variable + " is not a number", e);
        }
    }

    private static Instant instant(QuerySolution row, String variable) {
        return Instant.parse(text(row, variable));
    }

    private static byte[] decode(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new StoreException("Stored content is damaged", e);
        }
    }
}
