package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.SparqlSupport.instant;
import static eu.simpl.vocabularymanager.store.SparqlSupport.quad;
import static eu.simpl.vocabularymanager.store.SparqlSupport.rowInt;
import static eu.simpl.vocabularymanager.store.SparqlSupport.rowText;
import static eu.simpl.vocabularymanager.store.StoreModel.*;

import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.update.UpdateRequest;

/**
 * Store access for vocabularies and vocabulary versions (the DAO of CODE-47). Every list query is paged in
 * the store with ORDER BY, LIMIT and OFFSET and a COUNT query (D85). Every write is one update request,
 * so one store transaction, with a condition on the state the request read (D8, D82).
 */
public class VocabularyRepository {

    /** Highest LIMIT of a list query: 100 rows plus one (D85). */
    static final int MAX_PAGE_ROWS = 101;
    /** Bound of the reference and search lookups (D38, Q5). */
    static final int MAX_LOOKUP_ROWS = 100_000;

    private static final String VOCABULARY_PATTERN = """
            GRAPH ?gInternal {
              ?voc a vm:InternalVocabulary ; vm:name ?name ; vm:status ?status ; vm:latestVersion ?latest ;
                   vm:createdBy ?createdBy ; vm:creationDate ?created ; vm:lastUpdateDate ?updated .
              ?latestVer vm:vocabulary ?voc ; vm:version ?latest ; vm:description ?description .
            }
            """;
    private static final String LIST_PATTERN = """
            GRAPH ?gInternal {
              ?voc a vm:InternalVocabulary ; vm:name ?name ; vm:status ?status ; vm:latestVersion ?latest ;
                   vm:createdBy ?createdBy ; vm:creationDate ?created ; vm:lastUpdateDate ?updated .
            }
            """;
    private static final String VERSION_PATTERN = """
            GRAPH ?gInternal {
              ?ver vm:vocabulary ?voc ; vm:name ?name ; vm:version ?version ; vm:description ?description ;
                   vm:changelog ?changelog ; vm:status ?status ; vm:createdBy ?createdBy ;
                   vm:creationDate ?created ; vm:lastUpdateDate ?updated ; vm:contentSha256 ?sha .
            }
            """;

    /** Data of a new vocabulary version. */
    public record NewVersion(
            String name,
            int version,
            String description,
            String changelog,
            String createdBy,
            Instant now,
            String sha256,
            byte[] content) {}

    /** Sortable fields of the version list (D23, D154). */
    public enum VersionSort {
        VERSION,
        STATUS,
        CREATION_DATE
    }

    private final SparqlStoreClient store;

    public VocabularyRepository(SparqlStoreClient store) {
        this.store = store;
    }

    public Optional<Vocabulary> findVocabulary(String name) {
        ParameterizedSparqlString pss = SparqlSupport.query("SELECT * WHERE {\n" + VOCABULARY_PATTERN + "}");
        pss.setParam("voc", vocabularyIri(name));
        Query query = pss.asQuery();
        query.setLimit(1);
        return store.select(query).stream().findFirst().map(VocabularyRepository::toVocabulary);
    }

    /** True when a vocabulary with this name exists, compared case-insensitively (D37). */
    public boolean nameTaken(String name) {
        ParameterizedSparqlString pss =
                SparqlSupport.query("SELECT ?voc WHERE { GRAPH ?gInternal { ?voc vm:nameKey ?key } }");
        pss.setLiteral("key", name.toLowerCase(java.util.Locale.ROOT));
        Query query = pss.asQuery();
        query.setLimit(1);
        return !store.select(query).isEmpty();
    }

    public Page<Vocabulary> list(VocabularyQuery criteria) {
        String filters = filters(criteria);
        String order = orderBy(criteria);
        // Sort and page first in a sub-query, then join the description of the latest version of the
        // page's rows only (D85: ORDER BY, LIMIT and OFFSET in the store).
        ParameterizedSparqlString pss = SparqlSupport.query("SELECT * WHERE {\n{ SELECT * WHERE {\n"
                + LIST_PATTERN + filters + "} ORDER BY " + order + " }\n"
                + "GRAPH ?gInternal { ?latestVer vm:vocabulary ?voc ; vm:version ?latest ;"
                + " vm:description ?description . }\n} ORDER BY " + order);
        bindFilters(pss, criteria);
        Query query = pss.asQuery();
        ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
            @Override
            public void visit(ElementSubQuery subQuery) {
                subQuery.getQuery().setLimit(Math.min(criteria.pageSize(), MAX_PAGE_ROWS));
                subQuery.getQuery().setOffset(criteria.offset());
            }
        });
        query.setLimit(MAX_PAGE_ROWS);
        List<Vocabulary> items = store.select(query).stream()
                .map(VocabularyRepository::toVocabulary)
                .toList();

        ParameterizedSparqlString count =
                SparqlSupport.query("SELECT (COUNT(?voc) AS ?total) WHERE {\n" + LIST_PATTERN + filters + "}");
        bindFilters(count, criteria);
        Query countQuery = count.asQuery();
        countQuery.setLimit(1);
        long total = store.select(countQuery).stream()
                .findFirst()
                .map(row -> SparqlSupport.longValue(row, "total"))
                .orElse(0L);
        return new Page<>(items, total);
    }

    private static String filters(VocabularyQuery criteria) {
        StringBuilder text = new StringBuilder();
        if (criteria.nameContains() != null) {
            text.append("FILTER(CONTAINS(LCASE(STR(?name)), ?nameFilter))\n");
        }
        if (criteria.status() != null) {
            text.append("FILTER(STR(?status) = ?statusFilter)\n");
        }
        if (criteria.creation() != null) {
            text.append("FILTER(?created >= ?createdFrom && ?created < ?createdTo)\n");
        }
        if (criteria.lastUpdate() != null) {
            text.append("FILTER(?updated >= ?updatedFrom && ?updated < ?updatedTo)\n");
        }
        return text.toString();
    }

    private static void bindFilters(ParameterizedSparqlString pss, VocabularyQuery criteria) {
        if (criteria.nameContains() != null) {
            pss.setLiteral("nameFilter", criteria.nameContains());
        }
        if (criteria.status() != null) {
            pss.setLiteral("statusFilter", criteria.status().name());
        }
        if (criteria.creation() != null) {
            pss.setParam("createdFrom", dateTime(criteria.creation().from()));
            pss.setParam("createdTo", dateTime(criteria.creation().toExclusive()));
        }
        if (criteria.lastUpdate() != null) {
            pss.setParam("updatedFrom", dateTime(criteria.lastUpdate().from()));
            pss.setParam("updatedTo", dateTime(criteria.lastUpdate().toExclusive()));
        }
    }

    /** Sort with ties broken by name ascending (D23); names compare by code point (D58). */
    private static String orderBy(VocabularyQuery criteria) {
        String direction = criteria.descending() ? "DESC" : "ASC";
        return switch (criteria.sort()) {
            case NAME -> direction + "(STR(?name))";
            case STATUS -> direction + "(STR(?status)) ASC(STR(?name))";
            case CREATION_DATE -> direction + "(?created) ASC(STR(?name))";
            case LAST_UPDATED -> direction + "(?updated) ASC(STR(?name))";
        };
    }

    public Optional<VocabularyVersion> findVersion(String name, int version) {
        ParameterizedSparqlString pss = SparqlSupport.query("SELECT * WHERE {\n" + VERSION_PATTERN + "}");
        pss.setParam("ver", versionIri(name, version));
        Query query = pss.asQuery();
        query.setLimit(1);
        return store.select(query).stream().findFirst().map(VocabularyRepository::toVersion);
    }

    public Page<VocabularyVersion> versions(
            String name, VersionSort sort, boolean descending, int page, int pageSize, long total) {
        String direction = descending ? "DESC" : "ASC";
        String order = switch (sort) {
            case VERSION -> "DESC(?version)";
            case STATUS -> direction + "(STR(?status)) DESC(?version)";
            case CREATION_DATE -> direction + "(?created) DESC(?version)";
        };
        ParameterizedSparqlString pss =
                SparqlSupport.query("SELECT * WHERE {\n" + VERSION_PATTERN + "} ORDER BY " + order);
        pss.setParam("voc", vocabularyIri(name));
        Query query = pss.asQuery();
        query.setLimit(Math.min(pageSize, MAX_PAGE_ROWS));
        query.setOffset((long) (page - 1) * pageSize);
        List<VocabularyVersion> items = store.select(query).stream()
                .map(VocabularyRepository::toVersion)
                .toList();
        return new Page<>(items, total);
    }

    /** The stored content of a version, as uploaded. */
    public Optional<byte[]> content(String name, int version) {
        ParameterizedSparqlString pss =
                SparqlSupport.query("SELECT ?content WHERE { GRAPH ?gContent { ?ver vm:content ?content } }");
        pss.setParam("ver", versionIri(name, version));
        Query query = pss.asQuery();
        query.setLimit(1);
        return store.select(query).stream()
                .findFirst()
                .map(row -> ContentCodec.decode(row.get("content").asNode()));
    }

    /**
     * One store query for the references of an upload or a search (Q5): the named vocabularies with their
     * version statuses, and which of the given URI keys belong to a registered external vocabulary.
     */
    public ReferenceState referenceState(Collection<String> names, Collection<String> externalKeys) {
        if (names.isEmpty() && externalKeys.isEmpty()) {
            return new ReferenceState(Map.of(), Set.of());
        }
        List<String> branches = new ArrayList<>();
        if (!names.isEmpty()) {
            branches.add("""
                    { VALUES ?name { }
                      GRAPH ?gInternal { ?voc vm:name ?name ; vm:status ?vstatus ; vm:latestVersion ?latest .
                        ?ver vm:vocabulary ?voc ; vm:version ?version ; vm:status ?status . }
                      BIND("internal" AS ?kind) }
                    """);
        }
        if (!externalKeys.isEmpty()) {
            branches.add("""
                    { VALUES ?extKey { }
                      GRAPH ?gExternal { { ?ext vm:namespaceKey ?extKey } UNION { ?ext vm:aliasKey ?extKey } }
                      BIND("external" AS ?kind) }
                    """);
        }
        Query query = SparqlSupport.query("SELECT ?kind ?name ?vstatus ?latest ?version ?status ?extKey WHERE {\n"
                        + String.join(" UNION ", branches) + "}")
                .asQuery();
        ValuesBinder.bind(query, "name", names.stream().map(StoreModel::string).toList());
        ValuesBinder.bind(
                query, "extKey", externalKeys.stream().map(StoreModel::string).toList());
        query.setLimit(MAX_LOOKUP_ROWS);
        Map<String, ReferenceState.InternalState> internal = new HashMap<>();
        Set<String> external = new HashSet<>();
        for (QuerySolution row : store.select(query)) {
            if ("external".equals(rowText(row, "kind"))) {
                external.add(rowText(row, "extKey"));
                continue;
            }
            String name = rowText(row, "name");
            ReferenceState.InternalState state = internal.computeIfAbsent(
                    name,
                    key -> new ReferenceState.InternalState(
                            key,
                            VocabularyStatus.parse(rowText(row, "vstatus")),
                            rowInt(row, "latest"),
                            new TreeMap<>()));
            state.versions().put(rowInt(row, "version"), VocabularyStatus.parse(rowText(row, "status")));
        }
        return new ReferenceState(internal, external);
    }

    /** Creates a vocabulary with version 1 unless the name is taken case-insensitively (D37, D43, D82). */
    public boolean insertVocabulary(NewVersion data, String token) {
        Node voc = vocabularyIri(data.name());
        Node ver = versionIri(data.name(), 1);
        Node key = string(data.name().toLowerCase(java.util.Locale.ROOT));
        List<Quad> inserts = new ArrayList<>(List.of(
                quad(GRAPH_INTERNAL, voc, RDF_TYPE, INTERNAL_VOCABULARY),
                quad(GRAPH_INTERNAL, voc, NAME, string(data.name())),
                quad(GRAPH_INTERNAL, voc, NAME_KEY, key),
                quad(GRAPH_INTERNAL, voc, STATUS, string(VocabularyStatus.ACTIVE.name())),
                quad(GRAPH_INTERNAL, voc, LATEST_VERSION, integer(1)),
                quad(GRAPH_INTERNAL, voc, CREATED_BY, string(data.createdBy())),
                quad(GRAPH_INTERNAL, voc, CREATION_DATE, dateTime(data.now())),
                quad(GRAPH_INTERNAL, voc, LAST_UPDATE_DATE, dateTime(data.now())),
                quad(GRAPH_INTERNAL, voc, WRITE_TOKEN, string(token))));
        inserts.addAll(versionQuads(ver, voc, data, token));
        inserts.add(quad(GRAPH_CONTENT, ver, CONTENT, ContentCodec.encode(data.content())));
        ParameterizedSparqlString where = SparqlSupport.query(
                "SELECT * WHERE { FILTER NOT EXISTS { GRAPH ?gInternal { ?other vm:nameKey ?key } } }");
        where.setParam("key", key);
        UpdateRequest request = new UpdateRequest();
        request.add(SparqlSupport.modify(List.of(), inserts, SparqlSupport.where(where)));
        store.update(request);
        return tokenIs(voc, WRITE_TOKEN, token);
    }

    /**
     * Creates version {@code data.version()} if the latest version is still {@code expectedLatest}, and
     * deprecates every older ACTIVE version in the same transaction (D4, D82).
     */
    public boolean insertVersion(NewVersion data, int expectedLatest, String token) {
        Node voc = vocabularyIri(data.name());
        Node ver = versionIri(data.name(), data.version());
        Node now = dateTime(data.now());
        Var oldUpdated = Var.alloc("oldUpdated");
        Var oldToken = Var.alloc("oldToken");
        List<Quad> inserts = new ArrayList<>(List.of(
                quad(GRAPH_INTERNAL, voc, LATEST_VERSION, integer(data.version())),
                quad(GRAPH_INTERNAL, voc, LAST_UPDATE_DATE, now),
                quad(GRAPH_INTERNAL, voc, WRITE_TOKEN, string(token))));
        inserts.addAll(versionQuads(ver, voc, data, token));
        List<Quad> deletes = List.of(
                quad(GRAPH_INTERNAL, voc, LATEST_VERSION, integer(expectedLatest)),
                quad(GRAPH_INTERNAL, voc, LAST_UPDATE_DATE, oldUpdated),
                quad(GRAPH_INTERNAL, voc, WRITE_TOKEN, oldToken));
        ParameterizedSparqlString condition = SparqlSupport.query("""
                SELECT * WHERE { GRAPH ?gInternal {
                  ?voc vm:latestVersion ?expected ; vm:lastUpdateDate ?oldUpdated .
                  OPTIONAL { ?voc vm:writeToken ?oldToken }
                } }
                """);
        condition.setParam("voc", voc);
        condition.setParam("expected", integer(expectedLatest));

        Var old = Var.alloc("old");
        Var oldDate = Var.alloc("oldDate");
        ParameterizedSparqlString olderActive = SparqlSupport.query("""
                SELECT * WHERE { GRAPH ?gInternal {
                  ?ver vm:writeToken ?token .
                  ?old vm:vocabulary ?voc ; vm:status "ACTIVE" ; vm:version ?v ; vm:lastUpdateDate ?oldDate .
                  FILTER(?v < ?next)
                } }
                """);
        olderActive.setParam("ver", ver);
        olderActive.setParam("voc", voc);
        olderActive.setLiteral("token", token);
        olderActive.setParam("next", integer(data.version()));

        ParameterizedSparqlString mine =
                SparqlSupport.query("SELECT * WHERE { GRAPH ?gInternal { ?ver vm:writeToken ?token } }");
        mine.setParam("ver", ver);
        mine.setLiteral("token", token);

        UpdateRequest request = new UpdateRequest();
        request.add(SparqlSupport.modify(deletes, inserts, SparqlSupport.where(condition)));
        request.add(SparqlSupport.modify(
                List.of(
                        quad(GRAPH_INTERNAL, old, STATUS, string(VocabularyStatus.ACTIVE.name())),
                        quad(GRAPH_INTERNAL, old, LAST_UPDATE_DATE, oldDate)),
                List.of(
                        quad(GRAPH_INTERNAL, old, STATUS, string(VocabularyStatus.DEPRECATED.name())),
                        quad(GRAPH_INTERNAL, old, LAST_UPDATE_DATE, now)),
                SparqlSupport.where(olderActive)));
        request.add(SparqlSupport.modify(
                List.of(),
                List.of(quad(GRAPH_CONTENT, ver, CONTENT, ContentCodec.encode(data.content()))),
                SparqlSupport.where(mine)));
        store.update(request);
        return tokenIs(ver, WRITE_TOKEN, token);
    }

    private static List<Quad> versionQuads(Node ver, Node voc, NewVersion data, String token) {
        Node now = dateTime(data.now());
        return List.of(
                quad(GRAPH_INTERNAL, ver, RDF_TYPE, INTERNAL_VOCABULARY_VERSION),
                quad(GRAPH_INTERNAL, ver, VOCABULARY, voc),
                quad(GRAPH_INTERNAL, ver, NAME, string(data.name())),
                quad(GRAPH_INTERNAL, ver, VERSION, integer(data.version())),
                quad(GRAPH_INTERNAL, ver, DESCRIPTION, string(data.description())),
                quad(GRAPH_INTERNAL, ver, CHANGELOG, string(data.changelog())),
                quad(GRAPH_INTERNAL, ver, STATUS, string(VocabularyStatus.ACTIVE.name())),
                quad(GRAPH_INTERNAL, ver, CREATED_BY, string(data.createdBy())),
                quad(GRAPH_INTERNAL, ver, CREATION_DATE, now),
                quad(GRAPH_INTERNAL, ver, LAST_UPDATE_DATE, now),
                quad(GRAPH_INTERNAL, ver, CONTENT_SHA256, string(data.sha256())),
                quad(GRAPH_INTERNAL, ver, CONTENT_LENGTH, integer(data.content().length)),
                quad(GRAPH_INTERNAL, ver, WRITE_TOKEN, string(token)));
    }

    /** Sets the vocabulary status if it is still {@code current} (D3, D82). */
    public boolean setVocabularyStatus(
            String name, VocabularyStatus current, VocabularyStatus target, Instant now, String token) {
        Node voc = vocabularyIri(name);
        return setStatus(voc, null, current, target, now, token);
    }

    /** Sets a version status if it is still {@code current}; updates both dates (D4, D160). */
    public boolean setVersionStatus(
            String name, int version, VocabularyStatus current, VocabularyStatus target, Instant now, String token) {
        return setStatus(versionIri(name, version), vocabularyIri(name), current, target, now, token);
    }

    private boolean setStatus(
            Node subject, Node parent, VocabularyStatus current, VocabularyStatus target, Instant now, String token) {
        Var oldUpdated = Var.alloc("oldUpdated");
        Var oldToken = Var.alloc("oldToken");
        Var parentUpdated = Var.alloc("parentUpdated");
        List<Quad> deletes = new ArrayList<>(List.of(
                quad(GRAPH_INTERNAL, subject, STATUS, string(current.name())),
                quad(GRAPH_INTERNAL, subject, LAST_UPDATE_DATE, oldUpdated),
                quad(GRAPH_INTERNAL, subject, STATUS_TOKEN, oldToken)));
        List<Quad> inserts = new ArrayList<>(List.of(
                quad(GRAPH_INTERNAL, subject, STATUS, string(target.name())),
                quad(GRAPH_INTERNAL, subject, LAST_UPDATE_DATE, dateTime(now)),
                quad(GRAPH_INTERNAL, subject, STATUS_TOKEN, string(token))));
        String parentPattern = "";
        if (parent != null) {
            deletes.add(quad(GRAPH_INTERNAL, parent, LAST_UPDATE_DATE, parentUpdated));
            inserts.add(quad(GRAPH_INTERNAL, parent, LAST_UPDATE_DATE, dateTime(now)));
            parentPattern = "?parent vm:lastUpdateDate ?parentUpdated .";
        }
        ParameterizedSparqlString condition = SparqlSupport.query("SELECT * WHERE { GRAPH ?gInternal {\n"
                + "?subject vm:status ?current ; vm:lastUpdateDate ?oldUpdated .\n"
                + "OPTIONAL { ?subject vm:statusToken ?oldToken }\n" + parentPattern + "} }");
        condition.setParam("subject", subject);
        condition.setLiteral("current", current.name());
        if (parent != null) {
            condition.setParam("parent", parent);
        }
        UpdateRequest request = new UpdateRequest();
        request.add(SparqlSupport.modify(deletes, inserts, SparqlSupport.where(condition)));
        store.update(request);
        return tokenIs(subject, STATUS_TOKEN, token);
    }

    private boolean tokenIs(Node subject, Node property, String token) {
        ParameterizedSparqlString pss =
                SparqlSupport.query("SELECT ?token WHERE { GRAPH ?gInternal { ?subject ?property ?token } }");
        pss.setParam("subject", subject);
        pss.setParam("property", property);
        Query query = pss.asQuery();
        query.setLimit(2);
        return store.select(query).stream().anyMatch(row -> token.equals(rowText(row, "token")));
    }

    private static Vocabulary toVocabulary(QuerySolution row) {
        return new Vocabulary(
                rowText(row, "name"),
                VocabularyStatus.parse(rowText(row, "status")),
                rowInt(row, "latest"),
                rowText(row, "description"),
                rowText(row, "createdBy"),
                instant(row, "created"),
                instant(row, "updated"));
    }

    private static VocabularyVersion toVersion(QuerySolution row) {
        return new VocabularyVersion(
                rowText(row, "name"),
                rowInt(row, "version"),
                rowText(row, "description"),
                rowText(row, "changelog"),
                VocabularyStatus.parse(rowText(row, "status")),
                rowText(row, "createdBy"),
                instant(row, "created"),
                instant(row, "updated"),
                rowText(row, "sha"));
    }
}
