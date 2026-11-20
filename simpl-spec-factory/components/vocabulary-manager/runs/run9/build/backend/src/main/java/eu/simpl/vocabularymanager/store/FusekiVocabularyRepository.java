package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreModel.EXTERNAL_GRAPH;
import static eu.simpl.vocabularymanager.store.StoreModel.PREFIXES;
import static eu.simpl.vocabularymanager.store.StoreModel.VOCABULARY_GRAPH;

import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.domain.SearchReference;
import eu.simpl.vocabularymanager.domain.SearchResult;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyFilter;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.validation.ReferenceData;
import eu.simpl.vocabularymanager.validation.ReferenceData.ReferencedVocabulary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * Vocabularies in Fuseki. Every query is built with Jena's ParameterizedSparqlString (D94); every list query has
 * ORDER BY, LIMIT and OFFSET (D85); every write is one conditional update request, checked through a write token
 * (D82).
 */
@Repository
public class FusekiVocabularyRepository implements VocabularyRepository {

    private static final Logger LOG = LoggerFactory.getLogger(FusekiVocabularyRepository.class);
    private static final int SEARCH_ROW_LIMIT = 10_000;
    private static final int REFERENCE_ROW_LIMIT = 20_000;

    private static final String VOCABULARY_PATTERN = """
            ?v a vm:Vocabulary ; vm:name ?name ; vm:status ?status ; vm:creationDate ?created ;
               vm:lastUpdateDate ?updated ; vm:latestVersion ?latest ; vm:createdBy ?createdBy .
            ?lv vm:ofVocabulary ?v ; vm:version ?latest ; vm:description ?desc ; vm:contentSha256 ?sha .
            """;

    private static final String VERSION_TRIPLES = """
            ?pVersion a vm:VocabularyVersion ; vm:ofVocabulary ?pVocab ; vm:version ?pNum ;
               vm:description ?pDesc ; vm:changelog ?pLog ; vm:status "ACTIVE" ; vm:createdBy ?pCreator ;
               vm:creationDate ?pNow ; vm:lastUpdateDate ?pNow ; vm:contentSha256 ?pSha ;
               vm:contentLength ?pLen ; vm:contentGraph ?pContentGraph .
            """;

    private final SparqlStore store;

    /** @param store the SPARQL endpoints */
    public FusekiVocabularyRepository(SparqlStore store) {
        this.store = store;
    }

    @Override
    public Optional<Vocabulary> find(String name) {
        ParameterizedSparqlString query =
                query("SELECT * WHERE { GRAPH ?pGraph { " + VOCABULARY_PATTERN + " FILTER(?name = ?pName) } } LIMIT 1");
        query.setLiteral("pName", name);
        return store.select(query.toString()).stream().findFirst().map(FusekiVocabularyRepository::vocabulary);
    }

    @Override
    public boolean existsIgnoreCase(String name) {
        ParameterizedSparqlString query = query("ASK { GRAPH ?pGraph { ?v vm:nameKey ?pKey } }");
        query.setLiteral("pKey", name.toLowerCase(Locale.ROOT));
        return store.ask(query.toString());
    }

    @Override
    public Page<Vocabulary> list(VocabularyFilter filter, PageRequest page) {
        String filters = filters(filter);
        ParameterizedSparqlString count = query("SELECT (COUNT(?v) AS ?count) WHERE { GRAPH ?pGraph { "
                + VOCABULARY_PATTERN + " } " + filters + " } LIMIT 1");
        bindFilters(count, filter);
        long total = store.select(count.toString()).get(0).longValue("count");
        ParameterizedSparqlString select =
                query("SELECT * WHERE { GRAPH ?pGraph { " + VOCABULARY_PATTERN + " } " + filters + " } ORDER BY "
                        + vocabularyOrder(filter) + " LIMIT " + page.pageSize() + " OFFSET " + page.offset());
        bindFilters(select, filter);
        List<Vocabulary> items = store.select(select.toString()).stream()
                .map(FusekiVocabularyRepository::vocabulary)
                .toList();
        return new Page<>(items, total);
    }

    private static String filters(VocabularyFilter filter) {
        StringBuilder text = new StringBuilder();
        if (filter.nameContains() != null) {
            text.append(" FILTER(CONTAINS(LCASE(?name), ?pNameLower))");
        }
        if (filter.status() != null) {
            text.append(" FILTER(?status = ?pStatus)");
        }
        if (filter.creationFrom() != null) {
            text.append(" FILTER(?created >= ?pCreatedFrom && ?created < ?pCreatedTo)");
        }
        if (filter.updatedFrom() != null) {
            text.append(" FILTER(?updated >= ?pUpdatedFrom && ?updated < ?pUpdatedTo)");
        }
        return text.toString();
    }

    private static void bindFilters(ParameterizedSparqlString query, VocabularyFilter filter) {
        if (filter.nameContains() != null) {
            query.setLiteral("pNameLower", filter.nameContains().toLowerCase(Locale.ROOT));
        }
        if (filter.status() != null) {
            query.setLiteral("pStatus", filter.status().name());
        }
        if (filter.creationFrom() != null) {
            query.setLiteral("pCreatedFrom", filter.creationFrom().toString(), XSDDatatype.XSDdateTime);
            query.setLiteral("pCreatedTo", filter.creationTo().toString(), XSDDatatype.XSDdateTime);
        }
        if (filter.updatedFrom() != null) {
            query.setLiteral("pUpdatedFrom", filter.updatedFrom().toString(), XSDDatatype.XSDdateTime);
            query.setLiteral("pUpdatedTo", filter.updatedTo().toString(), XSDDatatype.XSDdateTime);
        }
    }

    private static String vocabularyOrder(VocabularyFilter filter) {
        String direction = filter.descending() ? "DESC" : "ASC";
        return switch (filter.sort()) {
            case STATUS -> direction + "(?status) ASC(STR(?name))";
            case CREATION_DATE -> direction + "(?created) ASC(STR(?name))";
            case LAST_UPDATED -> direction + "(?updated) ASC(STR(?name))";
            default -> direction + "(STR(?name))";
        };
    }

    @Override
    public Page<VocabularyVersion> listVersions(
            String name, int latestVersion, VocabularyFilter.SortField sort, boolean descending, PageRequest page) {
        String pattern = """
                ?ver vm:ofVocabulary ?pVocab ; vm:version ?num ; vm:description ?desc ; vm:changelog ?changelog ;
                   vm:status ?status ; vm:createdBy ?createdBy ; vm:creationDate ?created ; vm:lastUpdateDate ?updated .
                """;
        ParameterizedSparqlString count =
                query("SELECT (COUNT(?ver) AS ?count) WHERE { GRAPH ?pGraph { " + pattern + " } } LIMIT 1");
        count.setIri("pVocab", StoreModel.vocabularyIri(name));
        long total = store.select(count.toString()).get(0).longValue("count");
        ParameterizedSparqlString select = query("SELECT * WHERE { GRAPH ?pGraph { " + pattern + " } } ORDER BY "
                + versionOrder(sort, descending) + " LIMIT " + page.pageSize() + " OFFSET " + page.offset());
        select.setIri("pVocab", StoreModel.vocabularyIri(name));
        List<VocabularyVersion> items = store.select(select.toString()).stream()
                .map(row -> version(name, row, latestVersion))
                .toList();
        return new Page<>(items, total);
    }

    private static String versionOrder(VocabularyFilter.SortField sort, boolean descending) {
        String direction = descending ? "DESC" : "ASC";
        return switch (sort) {
            case STATUS -> direction + "(?status) DESC(?num)";
            case CREATION_DATE -> direction + "(?created) DESC(?num)";
            case NAME -> "DESC(?num)";
            default -> direction + "(?num)";
        };
    }

    @Override
    public Optional<VocabularyVersion> findVersion(String name, int version) {
        ParameterizedSparqlString query = query("""
                SELECT * WHERE { GRAPH ?pGraph {
                  ?pVocab vm:latestVersion ?latest .
                  ?ver vm:ofVocabulary ?pVocab ; vm:version ?num ; vm:description ?desc ; vm:changelog ?changelog ;
                     vm:status ?status ; vm:createdBy ?createdBy ; vm:creationDate ?created ; vm:lastUpdateDate ?updated .
                  FILTER(?num = ?pNum) } } LIMIT 1
                """);
        query.setIri("pVocab", StoreModel.vocabularyIri(name));
        query.setLiteral("pNum", Integer.toString(version), XSDDatatype.XSDinteger);
        return store.select(query.toString()).stream()
                .findFirst()
                .map(row -> version(name, row, row.integer("latest")));
    }

    @Override
    public Optional<byte[]> content(String name, Integer version) {
        String selector = version == null ? "FILTER(?num = ?latest)" : "FILTER(?num = ?pNum)";
        ParameterizedSparqlString query = query("SELECT ?i ?data WHERE { GRAPH ?pGraph { "
                + "?pVocab vm:latestVersion ?latest . ?ver vm:ofVocabulary ?pVocab ; vm:version ?num ; "
                + "vm:contentGraph ?cg . " + selector + " } GRAPH ?cg { ?chunk vm:chunkOf ?ver ; vm:index ?i ; "
                + "vm:data ?data } } ORDER BY ?i LIMIT " + ContentChunks.MAX_CHUNKS);
        query.setIri("pVocab", StoreModel.vocabularyIri(name));
        if (version != null) {
            query.setLiteral("pNum", Integer.toString(version), XSDDatatype.XSDinteger);
        }
        List<Row> rows = store.select(query.toString());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                ContentChunks.join(rows.stream().map(r -> r.string("data")).toList()));
    }

    @Override
    public ReferenceData referenceData(Set<String> names, Set<Integer> versions, Set<String> externalKeys) {
        if (names.isEmpty() && externalKeys.isEmpty()) {
            return ReferenceData.empty();
        }
        List<String> branches = new ArrayList<>();
        if (!names.isEmpty()) {
            String requested = versions.isEmpty()
                    ? ""
                    : "{ ?ver vm:ofVocabulary ?v ; vm:version ?num ; vm:status ?nstatus . VALUES (?num) { ?pNums } }"
                            + " UNION ";
            branches.add("{ GRAPH ?pGraph { VALUES (?name) { ?pNames } "
                    + "?v vm:name ?name ; vm:status ?vstatus ; vm:latestVersion ?latest . OPTIONAL { " + requested
                    + "{ ?ver vm:ofVocabulary ?v ; vm:status \"ACTIVE\" ; vm:version ?num . BIND(\"ACTIVE\" AS ?nstatus) }"
                    + " UNION { ?ver vm:ofVocabulary ?v ; vm:version ?latest ; vm:status ?nstatus . BIND(?latest AS ?num) }"
                    + " } } }");
        }
        if (!externalKeys.isEmpty()) {
            branches.add(
                    "{ GRAPH ?pExternalGraph { { ?e vm:namespaceKey ?key } UNION { ?e vm:alias ?a . ?a vm:key ?key }"
                            + " VALUES (?key) { ?pKeys } } }");
        }
        ParameterizedSparqlString query = query("SELECT ?name ?vstatus ?latest ?num ?nstatus ?key WHERE { "
                + String.join(" UNION ", branches) + " } LIMIT " + REFERENCE_ROW_LIMIT);
        query.setIri("pExternalGraph", EXTERNAL_GRAPH);
        if (!names.isEmpty()) {
            query.setValues("pNames", literals(names));
        }
        if (!versions.isEmpty()) {
            query.setValues("pNums", integers(versions));
        }
        if (!externalKeys.isEmpty()) {
            query.setValues("pKeys", literals(externalKeys));
        }
        Map<String, ReferencedVocabulary> vocabularies = new HashMap<>();
        Set<String> keys = new LinkedHashSet<>();
        for (Row row : store.select(query.toString())) {
            if (row.has("key")) {
                keys.add(row.string("key"));
                continue;
            }
            ReferencedVocabulary vocabulary = vocabularies.computeIfAbsent(
                    row.string("name"),
                    n -> new ReferencedVocabulary(
                            VocabularyStatus.valueOf(row.string("vstatus")), row.integer("latest"), new HashMap<>()));
            if (row.has("num")) {
                vocabulary.versions().put(row.integer("num"), VocabularyStatus.valueOf(row.string("nstatus")));
            }
        }
        return new ReferenceData(vocabularies, keys);
    }

    @Override
    public List<SearchResult> search(List<SearchReference> references) {
        Set<String> names = new LinkedHashSet<>();
        Set<Integer> versions = new LinkedHashSet<>();
        for (SearchReference reference : references) {
            names.add(reference.name());
            if (reference.version() != null) {
                versions.add(reference.version());
            }
        }
        String versionPart = versions.isEmpty()
                ? ""
                : " OPTIONAL { ?ver vm:ofVocabulary ?v ; vm:version ?num ; vm:status ?nstatus . VALUES (?num) { ?pNums } }";
        ParameterizedSparqlString query = query("SELECT ?name ?vstatus ?latest ?num ?nstatus WHERE { GRAPH ?pGraph { "
                + "VALUES (?name) { ?pNames } ?v vm:name ?name ; vm:status ?vstatus ; vm:latestVersion ?latest ."
                + versionPart + " } } LIMIT " + SEARCH_ROW_LIMIT);
        query.setValues("pNames", literals(names));
        if (!versions.isEmpty()) {
            query.setValues("pNums", integers(versions));
        }
        Map<String, Row> vocabularies = new HashMap<>();
        Map<String, Map<Integer, VocabularyStatus>> versionStatus = new HashMap<>();
        for (Row row : store.select(query.toString())) {
            String name = row.string("name");
            vocabularies.putIfAbsent(name, row);
            if (row.has("num")) {
                versionStatus
                        .computeIfAbsent(name, n -> new HashMap<>())
                        .put(row.integer("num"), VocabularyStatus.valueOf(row.string("nstatus")));
            }
        }
        List<SearchResult> results = new ArrayList<>();
        for (SearchReference reference : references) {
            Row vocabulary = vocabularies.get(reference.name());
            if (vocabulary == null) {
                continue;
            }
            if (reference.version() == null) {
                results.add(new SearchResult(
                        reference.name(),
                        vocabulary.integer("latest"),
                        VocabularyStatus.valueOf(vocabulary.string("vstatus")),
                        true));
                continue;
            }
            VocabularyStatus status =
                    versionStatus.getOrDefault(reference.name(), Map.of()).get(reference.version());
            if (status != null) {
                results.add(new SearchResult(reference.name(), reference.version(), status, false));
            }
        }
        return results;
    }

    @Override
    public boolean insertVocabulary(NewVersion first) {
        String token = UUID.randomUUID().toString();
        ParameterizedSparqlString update = query("INSERT { GRAPH ?pGraph { "
                + "?pVocab a vm:Vocabulary ; vm:name ?pVocabName ; vm:nameKey ?pKey ; vm:status \"ACTIVE\" ; "
                + "vm:createdBy ?pCreator ; vm:creationDate ?pNow ; vm:lastUpdateDate ?pNow ; vm:latestVersion ?pNum ; "
                + "vm:writeToken ?pToken . " + VERSION_TRIPLES
                + " } }"
                + " WHERE { FILTER NOT EXISTS { GRAPH ?pGraph { ?existing vm:nameKey ?pKey } } }");
        bindVersion(update, first);
        update.setLiteral("pVocabName", first.name());
        update.setLiteral("pKey", first.name().toLowerCase(Locale.ROOT));
        update.setLiteral("pToken", token);
        return writeAndConfirm(update.toString() + " ;\n" + contentInsert(first, token), first.name(), token);
    }

    @Override
    public boolean insertVersion(NewVersion version, int expectedLatest) {
        String token = UUID.randomUUID().toString();
        ParameterizedSparqlString update = query("DELETE { GRAPH ?pGraph { ?pVocab vm:latestVersion ?l ; "
                + "vm:lastUpdateDate ?u } } INSERT { GRAPH ?pGraph { ?pVocab vm:latestVersion ?pNum ; "
                + "vm:lastUpdateDate ?pNow ; vm:writeToken ?pToken . " + VERSION_TRIPLES + " } "
                + "} "
                + "WHERE { GRAPH ?pGraph { ?pVocab vm:latestVersion ?l ; vm:lastUpdateDate ?u . FILTER(?l = ?pExpected) } } ;\n"
                + "DELETE { GRAPH ?pGraph { ?old vm:status \"ACTIVE\" ; vm:lastUpdateDate ?od } } "
                + "INSERT { GRAPH ?pGraph { ?old vm:status \"DEPRECATED\" ; vm:lastUpdateDate ?pNow } } "
                + "WHERE { GRAPH ?pGraph { ?pVocab vm:writeToken ?pToken . ?old vm:ofVocabulary ?pVocab ; "
                + "vm:version ?k ; vm:status \"ACTIVE\" ; vm:lastUpdateDate ?od . FILTER(?k < ?pNum) } }");
        bindVersion(update, version);
        update.setLiteral("pExpected", Integer.toString(expectedLatest), XSDDatatype.XSDinteger);
        update.setLiteral("pToken", token);
        return writeAndConfirm(update.toString() + " ;\n" + contentInsert(version, token), version.name(), token);
    }

    private static String contentInsert(NewVersion version, String token) {
        ParameterizedSparqlString condition =
                query("SELECT * WHERE { GRAPH ?pGraph { ?pVocab vm:writeToken ?pToken } }");
        condition.setIri("pVocab", StoreModel.vocabularyIri(version.name()));
        condition.setLiteral("pToken", token);
        return ContentChunks.insert(
                StoreModel.versionContentGraph(version.name(), version.version()),
                StoreModel.versionIri(version.name(), version.version()),
                version.content(),
                condition.toString());
    }

    private static void bindVersion(ParameterizedSparqlString update, NewVersion version) {
        update.setIri("pVocab", StoreModel.vocabularyIri(version.name()));
        update.setIri("pVersion", StoreModel.versionIri(version.name(), version.version()));
        update.setIri("pContentGraph", StoreModel.versionContentGraph(version.name(), version.version()));
        update.setLiteral("pNum", Integer.toString(version.version()), XSDDatatype.XSDinteger);
        update.setLiteral("pDesc", version.description());
        update.setLiteral("pLog", version.changelog());
        update.setLiteral("pCreator", version.createdBy());
        update.setLiteral("pNow", version.now().toString(), XSDDatatype.XSDdateTime);
        update.setLiteral("pSha", version.contentSha256());
        update.setLiteral("pLen", Integer.toString(version.content().length), XSDDatatype.XSDinteger);
    }

    @Override
    public boolean updateVocabularyStatus(String name, VocabularyStatus expected, VocabularyStatus next, Instant now) {
        String token = UUID.randomUUID().toString();
        ParameterizedSparqlString update = query(
                "DELETE { GRAPH ?pGraph { ?pVocab vm:status ?s ; vm:lastUpdateDate ?u } }"
                        + " INSERT { GRAPH ?pGraph { ?pVocab vm:status ?pNext ; vm:lastUpdateDate ?pNow ; vm:writeToken ?pToken } }"
                        + " WHERE { GRAPH ?pGraph { ?pVocab vm:status ?s ; vm:lastUpdateDate ?u . FILTER(?s = ?pExpected) } }");
        update.setIri("pVocab", StoreModel.vocabularyIri(name));
        update.setLiteral("pNext", next.name());
        update.setLiteral("pExpected", expected.name());
        update.setLiteral("pNow", now.toString(), XSDDatatype.XSDdateTime);
        update.setLiteral("pToken", token);
        return writeAndConfirm(update.toString(), name, token);
    }

    @Override
    public boolean updateVersionStatus(
            String name, int version, VocabularyStatus expected, VocabularyStatus next, Instant now) {
        String token = UUID.randomUUID().toString();
        ParameterizedSparqlString update = query("DELETE { GRAPH ?pGraph { ?pVersion vm:status ?s ; "
                + "vm:lastUpdateDate ?vu . ?pVocab vm:lastUpdateDate ?u } } INSERT { GRAPH ?pGraph { "
                + "?pVersion vm:status ?pNext ; vm:lastUpdateDate ?pNow . ?pVocab vm:lastUpdateDate ?pNow ; "
                + "vm:writeToken ?pToken } } WHERE { GRAPH ?pGraph { ?pVersion vm:status ?s ; vm:lastUpdateDate ?vu . "
                + "?pVocab vm:lastUpdateDate ?u . FILTER(?s = ?pExpected) } }");
        update.setIri("pVocab", StoreModel.vocabularyIri(name));
        update.setIri("pVersion", StoreModel.versionIri(name, version));
        update.setLiteral("pNext", next.name());
        update.setLiteral("pExpected", expected.name());
        update.setLiteral("pNow", now.toString(), XSDDatatype.XSDdateTime);
        update.setLiteral("pToken", token);
        return writeAndConfirm(update.toString(), name, token);
    }

    /** Sends one update request and checks through the write token whether its condition held (D82). */
    private boolean writeAndConfirm(String update, String name, String token) {
        store.update(update);
        ParameterizedSparqlString ask = query("ASK { GRAPH ?pGraph { ?pVocab vm:writeToken ?pToken } }");
        ask.setIri("pVocab", StoreModel.vocabularyIri(name));
        ask.setLiteral("pToken", token);
        boolean applied = store.ask(ask.toString());
        if (applied) {
            removeToken(name, token);
        }
        return applied;
    }

    private void removeToken(String name, String token) {
        ParameterizedSparqlString cleanup = query("DELETE DATA { GRAPH ?pGraph { ?pVocab vm:writeToken ?pToken } }");
        cleanup.setIri("pVocab", StoreModel.vocabularyIri(name));
        cleanup.setLiteral("pToken", token);
        try {
            store.update(cleanup.toString());
        } catch (StoreUnavailableException | StoreFailureException ex) {
            LOG.warn("Write token cleanup failed; a stale token stays, which is harmless: {}", ex.getMessage());
        }
    }

    private static ParameterizedSparqlString query(String body) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(PREFIXES + body);
        query.setIri("pGraph", VOCABULARY_GRAPH);
        return query;
    }

    private static List<RDFNode> literals(Set<String> values) {
        return values.stream()
                .map(v -> (RDFNode) ResourceFactory.createPlainLiteral(v))
                .toList();
    }

    private static List<RDFNode> integers(Set<Integer> values) {
        return values.stream()
                .map(v -> (RDFNode) ResourceFactory.createTypedLiteral(Integer.toString(v), XSDDatatype.XSDinteger))
                .toList();
    }

    private static Vocabulary vocabulary(Row row) {
        return new Vocabulary(
                row.string("name"),
                VocabularyStatus.valueOf(row.string("status")),
                row.integer("latest"),
                row.string("desc"),
                row.string("sha"),
                row.string("createdBy"),
                row.instant("created"),
                row.instant("updated"));
    }

    private static VocabularyVersion version(String name, Row row, int latestVersion) {
        return new VocabularyVersion(
                name,
                row.integer("num"),
                row.string("desc"),
                row.string("changelog"),
                VocabularyStatus.valueOf(row.string("status")),
                row.string("createdBy"),
                row.instant("created"),
                row.instant("updated"),
                latestVersion);
    }
}
