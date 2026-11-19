package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.util.FmtUtils;
import org.springframework.stereotype.Repository;

/**
 * Store adapter on Apache Jena Fuseki through SPARQL 1.1 (decision E3). Every query is built with
 * {@link ParameterizedSparqlString}, so request values never reach the query text unescaped (OWASP-04). Every
 * write is one SPARQL update request, which Fuseki runs as one transaction (decisions D8, D43).
 */
@Repository
public class SparqlVocabularyStore implements VocabularyStore {

    static final String NS = "https://w3id.org/simpl/vocabulary-manager/store#";
    private static final String PREFIXES = "PREFIX vm: <" + NS + ">\n";
    private static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    private final SparqlClient client;

    /**
     * Creates the store adapter.
     *
     * @param client the SPARQL client
     */
    public SparqlVocabularyStore(SparqlClient client) {
        this.client = client;
    }

    // ----- vocabularies -----

    @Override
    public Optional<Vocabulary> findVocabulary(String name) {
        ParameterizedSparqlString q = pss(vocabularySelect() + "FILTER(?name = ?wanted)\n}");
        q.setLiteral("wanted", name);
        return client.select(q.toString()).stream().findFirst().map(SparqlVocabularyStore::toVocabulary);
    }

    @Override
    public boolean vocabularyNameTaken(String name) {
        ParameterizedSparqlString q = pss("SELECT ?v WHERE { ?v a vm:Vocabulary ; vm:nameKey ?key } LIMIT 1");
        q.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return !client.select(q.toString()).isEmpty();
    }

    @Override
    public Page<Vocabulary> listVocabularies(VocabularyQuery query) {
        String filters = vocabularyFilters(query);
        ParameterizedSparqlString count = pss("SELECT (COUNT(?v) AS ?total) WHERE {\n"
                + "?v a vm:Vocabulary ; vm:name ?name ; vm:status ?status ;"
                + " vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate .\n"
                + filters
                + "}");
        bindVocabularyFilters(count, query);
        long total = client.select(count.toString()).stream()
                .findFirst()
                .map(s -> s.getLiteral("total").getLong())
                .orElse(0L);
        if (query.offset() >= total) {
            return new Page<>(List.of(), total);
        }
        ParameterizedSparqlString page = pss(vocabularySelect()
                + filters
                + "}\nORDER BY "
                + vocabularyOrder(query.sort())
                + "\nLIMIT "
                + query.limit()
                + " OFFSET "
                + query.offset());
        bindVocabularyFilters(page, query);
        List<Vocabulary> items = client.select(page.toString()).stream()
                .map(SparqlVocabularyStore::toVocabulary)
                .toList();
        return new Page<>(items, total);
    }

    private static String vocabularySelect() {
        return "SELECT ?name ?status ?createdBy ?creationDate ?lastUpdateDate ?latestVersion ?description WHERE {\n"
                + "?v a vm:Vocabulary ; vm:name ?name ; vm:status ?status ; vm:createdBy ?createdBy ;"
                + " vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate ;"
                + " vm:latestVersion ?latestVersion .\n"
                + "?latest vm:vocabulary ?v ; vm:version ?latestVersion ; vm:description ?description .\n";
    }

    private static String vocabularyFilters(VocabularyQuery query) {
        StringBuilder filters = new StringBuilder();
        if (query.nameContains() != null) {
            filters.append("FILTER(CONTAINS(LCASE(?name), ?namePart))\n");
        }
        if (query.status() != null) {
            filters.append("FILTER(?status = ?wantedStatus)\n");
        }
        if (query.creationDate() != null) {
            filters.append("FILTER(?creationDate >= ?cdFrom && ?creationDate < ?cdTo)\n");
        }
        if (query.lastUpdated() != null) {
            filters.append("FILTER(?lastUpdateDate >= ?luFrom && ?lastUpdateDate < ?luTo)\n");
        }
        return filters.toString();
    }

    private static void bindVocabularyFilters(ParameterizedSparqlString q, VocabularyQuery query) {
        if (query.nameContains() != null) {
            q.setLiteral("namePart", query.nameContains().toLowerCase(Locale.ROOT));
        }
        if (query.status() != null) {
            q.setLiteral("wantedStatus", query.status().name());
        }
        if (query.creationDate() != null) {
            q.setParam("cdFrom", dateTime(query.creationDate().from()));
            q.setParam("cdTo", dateTime(query.creationDate().to()));
        }
        if (query.lastUpdated() != null) {
            q.setParam("luFrom", dateTime(query.lastUpdated().from()));
            q.setParam("luTo", dateTime(query.lastUpdated().to()));
        }
    }

    private static String vocabularyOrder(SortOrder sort) {
        String direction = sort.descending() ? "DESC" : "ASC";
        String key = switch (sort.field()) {
            case STATUS -> "?status";
            case CREATION_DATE -> "?creationDate";
            case LAST_UPDATED -> "?lastUpdateDate";
            default -> "?name";
        };
        if ("?name".equals(key)) {
            return direction + "(?name)";
        }
        return direction + "(" + key + ") ASC(?name)";
    }

    // ----- versions -----

    @Override
    public Optional<VocabularyVersion> findVersion(String name, long version) {
        ParameterizedSparqlString q = pss(versionSelect() + "FILTER(?name = ?wanted && ?version = ?wantedVersion)\n}");
        q.setLiteral("wanted", name);
        q.setParam("wantedVersion", integer(version));
        return client.select(q.toString()).stream().findFirst().map(SparqlVocabularyStore::toVersion);
    }

    @Override
    public Page<VocabularyVersion> listVersions(String name, SortOrder sort, long offset, int limit) {
        ParameterizedSparqlString count = pss("SELECT (COUNT(?ver) AS ?total) WHERE {\n"
                + "?v a vm:Vocabulary ; vm:name ?wanted . ?ver vm:vocabulary ?v .\n}");
        count.setLiteral("wanted", name);
        long total = client.select(count.toString()).stream()
                .findFirst()
                .map(s -> s.getLiteral("total").getLong())
                .orElse(0L);
        if (offset >= total) {
            return new Page<>(List.of(), total);
        }
        ParameterizedSparqlString page = pss(versionSelect()
                + "FILTER(?name = ?wanted)\n}\nORDER BY "
                + versionOrder(sort)
                + "\nLIMIT "
                + limit
                + " OFFSET "
                + offset);
        page.setLiteral("wanted", name);
        List<VocabularyVersion> items = client.select(page.toString()).stream()
                .map(SparqlVocabularyStore::toVersion)
                .toList();
        return new Page<>(items, total);
    }

    private static String versionSelect() {
        return "SELECT ?name ?version ?status ?description ?changelog ?createdBy ?creationDate ?lastUpdateDate"
                + " ?sha WHERE {\n"
                + "?v a vm:Vocabulary ; vm:name ?name .\n"
                + "?ver vm:vocabulary ?v ; vm:version ?version ; vm:status ?status ; vm:description ?description ;"
                + " vm:changelog ?changelog ; vm:createdBy ?createdBy ; vm:creationDate ?creationDate ;"
                + " vm:lastUpdateDate ?lastUpdateDate ; vm:contentSha256 ?sha .\n";
    }

    private static String versionOrder(SortOrder sort) {
        String direction = sort.descending() ? "DESC" : "ASC";
        return switch (sort.field()) {
            case STATUS -> direction + "(?status) DESC(?version)";
            case CREATION_DATE -> direction + "(?creationDate) DESC(?version)";
            case NAME -> direction + "(?name) DESC(?version)";
            default -> direction + "(?version)";
        };
    }

    @Override
    public Optional<Integer> latestActiveVersion(String name) {
        ParameterizedSparqlString q = pss("SELECT (MAX(?version) AS ?latest) WHERE {\n"
                + "?v a vm:Vocabulary ; vm:name ?wanted . ?ver vm:vocabulary ?v ; vm:version ?version ;"
                + " vm:status \"ACTIVE\" .\n}");
        q.setLiteral("wanted", name);
        return client.select(q.toString()).stream()
                .findFirst()
                .map(s -> s.getLiteral("latest"))
                .map(Literal::getInt);
    }

    @Override
    public Optional<byte[]> versionContent(String name, long version) {
        ParameterizedSparqlString q = pss("SELECT ?content WHERE {\n"
                + "?v a vm:Vocabulary ; vm:name ?wanted . ?ver vm:vocabulary ?v ; vm:version ?wantedVersion ;"
                + " vm:content ?content .\n}");
        q.setLiteral("wanted", name);
        q.setParam("wantedVersion", integer(version));
        return client.select(q.toString()).stream()
                .findFirst()
                .map(s -> Base64.getDecoder().decode(s.getLiteral("content").getLexicalForm()));
    }

    @Override
    public void createVocabulary(Vocabulary vocabulary, VocabularyVersion firstVersion, byte[] content) {
        ParameterizedSparqlString u = pss("INSERT DATA {\n"
                + "?v a vm:Vocabulary ; vm:name ?name ; vm:nameKey ?key ; vm:status ?status ;"
                + " vm:createdBy ?createdBy ; vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate ;"
                + " vm:latestVersion ?latestVersion .\n"
                + versionTriples()
                + "}");
        u.setIri("v", vocabularyIri(vocabulary.name()));
        u.setLiteral("name", vocabulary.name());
        u.setLiteral("key", vocabulary.name().toLowerCase(Locale.ROOT));
        u.setLiteral("status", vocabulary.status().name());
        u.setLiteral("createdBy", vocabulary.createdBy());
        u.setParam("creationDate", dateTime(vocabulary.creationDate()));
        u.setParam("lastUpdateDate", dateTime(vocabulary.lastUpdateDate()));
        u.setParam("latestVersion", integer(vocabulary.latestVersion()));
        bindVersion(u, firstVersion, content);
        client.update(u.toString());
    }

    @Override
    public void createVersion(VocabularyVersion version, byte[] content) {
        ParameterizedSparqlString u = pss("DELETE { ?old vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldUpdate }\n"
                + "INSERT { ?old vm:status \"DEPRECATED\" ; vm:lastUpdateDate ?now }\n"
                + "WHERE { ?old vm:vocabulary ?v ; vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldUpdate } ;\n"
                + "DELETE { ?v vm:latestVersion ?oldLatest ; vm:lastUpdateDate ?oldVocabularyUpdate }\n"
                + "INSERT { ?v vm:latestVersion ?versionNumber ; vm:lastUpdateDate ?now }\n"
                + "WHERE { ?v vm:latestVersion ?oldLatest ; vm:lastUpdateDate ?oldVocabularyUpdate } ;\n"
                + "INSERT DATA {\n"
                + versionTriples()
                + "}");
        u.setIri("v", vocabularyIri(version.name()));
        u.setParam("now", dateTime(version.creationDate()));
        bindVersion(u, version, content);
        client.update(u.toString());
    }

    private static String versionTriples() {
        return "?ver a vm:VocabularyVersion ; vm:vocabulary ?v ; vm:version ?versionNumber ;"
                + " vm:status ?versionStatus ; vm:description ?description ; vm:changelog ?changelog ;"
                + " vm:createdBy ?versionCreatedBy ; vm:creationDate ?versionCreationDate ;"
                + " vm:lastUpdateDate ?versionUpdate ; vm:contentSha256 ?sha ; vm:content ?content .\n";
    }

    private static void bindVersion(ParameterizedSparqlString u, VocabularyVersion version, byte[] content) {
        u.setIri("ver", versionIri(version.name(), version.version()));
        u.setParam("versionNumber", integer(version.version()));
        u.setLiteral("versionStatus", version.status().name());
        u.setLiteral("description", version.description());
        u.setLiteral("changelog", version.changelog());
        u.setLiteral("versionCreatedBy", version.createdBy());
        u.setParam("versionCreationDate", dateTime(version.creationDate()));
        u.setParam("versionUpdate", dateTime(version.lastUpdateDate()));
        u.setLiteral("sha", version.contentSha256());
        u.setLiteral("content", Base64.getEncoder().encodeToString(content));
    }

    @Override
    public void updateVocabularyStatus(String name, VocabularyStatus status, Instant now) {
        ParameterizedSparqlString u = pss("DELETE { ?v vm:status ?old ; vm:lastUpdateDate ?oldUpdate }\n"
                + "INSERT { ?v vm:status ?status ; vm:lastUpdateDate ?now }\n"
                + "WHERE { ?v vm:status ?old ; vm:lastUpdateDate ?oldUpdate }");
        u.setIri("v", vocabularyIri(name));
        u.setLiteral("status", status.name());
        u.setParam("now", dateTime(now));
        client.update(u.toString());
    }

    @Override
    public void updateVersionStatus(String name, int version, VocabularyStatus status, Instant now) {
        ParameterizedSparqlString u = pss("DELETE { ?ver vm:status ?old ; vm:lastUpdateDate ?oldUpdate }\n"
                + "INSERT { ?ver vm:status ?status ; vm:lastUpdateDate ?now }\n"
                + "WHERE { ?ver vm:status ?old ; vm:lastUpdateDate ?oldUpdate } ;\n"
                + "DELETE { ?v vm:lastUpdateDate ?oldVocabularyUpdate } INSERT { ?v vm:lastUpdateDate ?now }\n"
                + "WHERE { ?v vm:lastUpdateDate ?oldVocabularyUpdate }");
        u.setIri("v", vocabularyIri(name));
        u.setIri("ver", versionIri(name, version));
        u.setLiteral("status", status.name());
        u.setParam("now", dateTime(now));
        client.update(u.toString());
    }

    // ----- external vocabularies -----

    @Override
    public Optional<ExternalVocabulary> findExternal(String name) {
        ParameterizedSparqlString q = pss(externalSelect() + "FILTER(?name = ?wanted)\n}");
        q.setLiteral("wanted", name);
        return client.select(q.toString()).stream().findFirst().map(SparqlVocabularyStore::toExternal);
    }

    @Override
    public boolean externalNameTaken(String name) {
        ParameterizedSparqlString q = pss("SELECT ?e WHERE { ?e a vm:ExternalVocabulary ; vm:nameKey ?key } LIMIT 1");
        q.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return !client.select(q.toString()).isEmpty();
    }

    @Override
    public Optional<ExternalVocabulary> findExternalByUri(String normalisedUri) {
        return findExternalsByUris(List.of(normalisedUri)).stream().findFirst();
    }

    @Override
    public List<ExternalVocabulary> findExternalsByUris(List<String> normalisedUris) {
        if (normalisedUris.isEmpty()) {
            return List.of();
        }
        String values = normalisedUris.stream()
                .map(uri -> FmtUtils.stringForNode(NodeFactory.createLiteralString(uri)))
                .collect(Collectors.joining(" "));
        ParameterizedSparqlString q = pss(externalSelect()
                + "{ SELECT DISTINCT ?e WHERE { VALUES ?key { "
                + values
                + " } ?e vm:uriKey ?key } }\n}");
        return client.select(q.toString()).stream()
                .map(SparqlVocabularyStore::toExternal)
                .toList();
    }

    @Override
    public Page<ExternalVocabulary> listExternals(long offset, int limit) {
        ParameterizedSparqlString count = pss("SELECT (COUNT(?e) AS ?total) WHERE { ?e a vm:ExternalVocabulary }");
        long total = client.select(count.toString()).stream()
                .findFirst()
                .map(s -> s.getLiteral("total").getLong())
                .orElse(0L);
        if (offset >= total) {
            return new Page<>(List.of(), total);
        }
        ParameterizedSparqlString page =
                pss(externalSelect() + "}\nORDER BY ASC(?name)\nLIMIT " + limit + " OFFSET " + offset);
        List<ExternalVocabulary> items = client.select(page.toString()).stream()
                .map(SparqlVocabularyStore::toExternal)
                .toList();
        return new Page<>(items, total);
    }

    private static String externalSelect() {
        return "SELECT ?name ?namespaceUri ?aliases ?sourceUri ?description ?createdBy ?creationDate WHERE {\n"
                + "?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?namespaceUri ;"
                + " vm:createdBy ?createdBy ; vm:creationDate ?creationDate .\n"
                + "OPTIONAL { ?e vm:aliasList ?aliases }\n"
                + "OPTIONAL { ?e vm:sourceUri ?sourceUri }\n"
                + "OPTIONAL { ?e vm:description ?description }\n";
    }

    @Override
    public void createExternal(ExternalVocabulary external, byte[] content) {
        StringBuilder triples = new StringBuilder("?e a vm:ExternalVocabulary ; vm:name ?name ; vm:nameKey ?key ;"
                + " vm:namespaceUri ?namespaceUri ; vm:createdBy ?createdBy ; vm:creationDate ?creationDate ;"
                + " vm:content ?content .\n");
        List<String> keys = new ArrayList<>();
        keys.add(ExternalVocabulary.normalise(external.namespaceUri()));
        external.aliasUris().forEach(alias -> keys.add(ExternalVocabulary.normalise(alias)));
        for (int i = 0; i < keys.size(); i++) {
            triples.append("?e vm:uriKey ?uriKey").append(i).append(" .\n");
        }
        if (!external.aliasUris().isEmpty()) {
            triples.append("?e vm:aliasList ?aliases .\n");
        }
        if (external.sourceUri() != null) {
            triples.append("?e vm:sourceUri ?sourceUri .\n");
        }
        if (external.description() != null) {
            triples.append("?e vm:description ?description .\n");
        }
        ParameterizedSparqlString u = pss("INSERT DATA {\n" + triples + "}");
        u.setIri("e", externalIri(external.name()));
        u.setLiteral("name", external.name());
        u.setLiteral("key", external.name().toLowerCase(Locale.ROOT));
        u.setLiteral("namespaceUri", external.namespaceUri());
        u.setLiteral("createdBy", external.createdBy());
        u.setParam("creationDate", dateTime(external.creationDate()));
        u.setLiteral("content", Base64.getEncoder().encodeToString(content));
        for (int i = 0; i < keys.size(); i++) {
            u.setLiteral("uriKey" + i, keys.get(i));
        }
        if (!external.aliasUris().isEmpty()) {
            u.setLiteral("aliases", String.join("\n", external.aliasUris()));
        }
        if (external.sourceUri() != null) {
            u.setLiteral("sourceUri", external.sourceUri());
        }
        if (external.description() != null) {
            u.setLiteral("description", external.description());
        }
        client.update(u.toString());
    }

    @Override
    public void deleteExternal(String name) {
        ParameterizedSparqlString u = pss("DELETE WHERE { ?e ?p ?o }");
        u.setIri("e", externalIri(name));
        client.update(u.toString());
    }

    @Override
    public Optional<byte[]> externalContent(String name) {
        ParameterizedSparqlString q =
                pss("SELECT ?content WHERE { ?e a vm:ExternalVocabulary ; vm:name ?wanted ; vm:content ?content }");
        q.setLiteral("wanted", name);
        return client.select(q.toString()).stream()
                .findFirst()
                .map(s -> Base64.getDecoder().decode(s.getLiteral("content").getLexicalForm()));
    }

    @Override
    public boolean isAvailable() {
        return client.ping();
    }

    // ----- mapping -----

    private static ParameterizedSparqlString pss(String body) {
        return new ParameterizedSparqlString(PREFIXES + body);
    }

    private static String vocabularyIri(String name) {
        return "urn:simpl:vocabulary-manager:vocabulary:" + checkedName(name);
    }

    private static String versionIri(String name, int version) {
        return vocabularyIri(name) + ":version:" + version;
    }

    private static String externalIri(String name) {
        return "urn:simpl:vocabulary-manager:external:" + checkedName(name);
    }

    private static String checkedName(String name) {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Name breaks the name pattern");
        }
        return name;
    }

    private static Node dateTime(Instant instant) {
        return NodeFactory.createLiteralDT(instant.toString(), XSDDatatype.XSDdateTime);
    }

    private static Node integer(long value) {
        return NodeFactory.createLiteralDT(Long.toString(value), XSDDatatype.XSDinteger);
    }

    private static Vocabulary toVocabulary(QuerySolution s) {
        return new Vocabulary(
                text(s, "name"),
                VocabularyStatus.valueOf(text(s, "status")),
                text(s, "createdBy"),
                Instant.parse(text(s, "creationDate")),
                Instant.parse(text(s, "lastUpdateDate")),
                s.getLiteral("latestVersion").getInt(),
                text(s, "description"));
    }

    private static VocabularyVersion toVersion(QuerySolution s) {
        return new VocabularyVersion(
                text(s, "name"),
                s.getLiteral("version").getInt(),
                VocabularyStatus.valueOf(text(s, "status")),
                text(s, "description"),
                text(s, "changelog"),
                text(s, "createdBy"),
                Instant.parse(text(s, "creationDate")),
                Instant.parse(text(s, "lastUpdateDate")),
                text(s, "sha"));
    }

    private static ExternalVocabulary toExternal(QuerySolution s) {
        String aliases = optionalText(s, "aliases");
        List<String> aliasList = aliases == null ? List.of() : Arrays.asList(aliases.split("\n"));
        return new ExternalVocabulary(
                text(s, "name"),
                text(s, "namespaceUri"),
                List.copyOf(aliasList),
                optionalText(s, "sourceUri"),
                optionalText(s, "description"),
                text(s, "createdBy"),
                Instant.parse(text(s, "creationDate")));
    }

    private static String text(QuerySolution s, String variable) {
        RDFNode node = s.get(variable);
        if (node == null || !node.isLiteral()) {
            throw new StoreFailureException("Stored record lacks " + variable);
        }
        return node.asLiteral().getLexicalForm();
    }

    private static String optionalText(QuerySolution s, String variable) {
        RDFNode node = s.get(variable);
        return node == null ? null : node.asLiteral().getLexicalForm();
    }
}
