package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.Model.ContentFingerprint;
import eu.simpl.vocabularymanager.domain.Model.Page;
import eu.simpl.vocabularymanager.domain.Model.Status;
import eu.simpl.vocabularymanager.domain.Model.Vocabulary;
import eu.simpl.vocabularymanager.domain.Model.VocabularyVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.springframework.stereotype.Component;

/**
 * Store access for vocabularies and vocabulary versions. Filtering, sorting and paging run in the
 * store; every query has a LIMIT (D85). Writes are conditional updates (D82): the caller reads the
 * state, writes on the condition that it is unchanged, and checks a token to learn whether its
 * write was applied.
 */
@Component
public class VocabularyStore {

    private static final String VOC_PATTERN = """
      ?v a vm:InternalVocabulary ; vm:name ?name ; vm:status ?status ; vm:latestVersion ?latest ;
         vm:createdBy ?createdBy ; vm:creationDate ?created ; vm:lastUpdateDate ?updated .
      """;

    private final StoreClient store;

    public VocabularyStore(StoreClient store) {
        this.store = store;
    }

    /**
     * Filters of GET /vocabularies (D19, D20).
     *
     * @param nameContains lower-case text the name must contain, or null
     * @param status vocabulary status, or null
     * @param createdFrom inclusive lower bound of the creation date, or null
     * @param createdTo exclusive upper bound of the creation date
     * @param updatedFrom inclusive lower bound of the last update date, or null
     * @param updatedTo exclusive upper bound of the last update date
     */
    public record ListFilter(
            String nameContains,
            Status status,
            Instant createdFrom,
            Instant createdTo,
            Instant updatedFrom,
            Instant updatedTo) {}

    /**
     * The data of a new version.
     *
     * @param description trimmed description
     * @param changelog trimmed changelog
     * @param createdBy creator (D27)
     * @param at creation time
     * @param bytes the content as uploaded
     */
    public record NewVersion(String description, String changelog, String createdBy, Instant at, byte[] bytes) {}

    public Optional<Vocabulary> find(String name) {
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?name ?status ?latest ?desc ?createdBy ?created ?updated WHERE { GRAPH ?gInternal {"
                        + VOC_PATTERN
                        + " ?lv vm:ofVocabulary ?v ; vm:version ?latest ; vm:description ?desc . } } LIMIT 1");
        q.setIri("v", Rdf.vocabularyIri(name));
        List<QuerySolution> rows = store.select(q.toString());
        return rows.isEmpty() ? Optional.empty() : Optional.of(toVocabulary(rows.get(0)));
    }

    public boolean nameKeyExists(String name) {
        ParameterizedSparqlString q = Rdf.pss("ASK { GRAPH ?gInternal { ?x vm:nameKey ?key } }");
        q.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return store.ask(q.toString());
    }

    public Page<Vocabulary> list(ListFilter f, String sort, int page, int pageSize) {
        StringBuilder filters = new StringBuilder();
        if (f.nameContains() != null) {
            filters.append(" FILTER(CONTAINS(LCASE(STR(?name)), ?nameFilter))");
        }
        if (f.status() != null) {
            filters.append(" FILTER(STR(?status) = ?statusFilter)");
        }
        if (f.createdFrom() != null) {
            filters.append(" FILTER(?created >= ?cFrom && ?created < ?cTo)");
        }
        if (f.updatedFrom() != null) {
            filters.append(" FILTER(?updated >= ?uFrom && ?updated < ?uTo)");
        }
        ParameterizedSparqlString count = Rdf.pss(
                "SELECT (COUNT(?v) AS ?n) WHERE { GRAPH ?gInternal {" + VOC_PATTERN + "}" + filters + " } LIMIT 1");
        bindFilter(count, f);
        long total =
                Long.parseLong(Rdf.str(store.select(count.toString()).get(0).get("n")));
        List<Vocabulary> items = new ArrayList<>();
        long offset = (long) (page - 1) * pageSize;
        if (offset < total) {
            ParameterizedSparqlString q =
                    Rdf.pss("SELECT ?name ?status ?latest ?desc ?createdBy ?created ?updated WHERE { GRAPH"
                            + " ?gInternal {"
                            + VOC_PATTERN
                            + " ?lv vm:ofVocabulary ?v ; vm:version ?latest ; vm:description ?desc . }"
                            + filters
                            + " } ORDER BY "
                            + orderBy(sort)
                            + " LIMIT "
                            + pageSize
                            + " OFFSET "
                            + offset);
            bindFilter(q, f);
            for (QuerySolution row : store.select(q.toString())) {
                items.add(toVocabulary(row));
            }
        }
        return new Page<>(items, total);
    }

    private static void bindFilter(ParameterizedSparqlString q, ListFilter f) {
        if (f.nameContains() != null) {
            q.setLiteral("nameFilter", f.nameContains());
        }
        if (f.status() != null) {
            q.setLiteral("statusFilter", f.status().name());
        }
        if (f.createdFrom() != null) {
            q.setParam("cFrom", Rdf.dateTime(f.createdFrom()));
            q.setParam("cTo", Rdf.dateTime(f.createdTo()));
        }
        if (f.updatedFrom() != null) {
            q.setParam("uFrom", Rdf.dateTime(f.updatedFrom()));
            q.setParam("uTo", Rdf.dateTime(f.updatedTo()));
        }
    }

    /** ORDER BY of the list (D23, D58, D128): ties by name ascending. */
    static String orderBy(String sort) {
        String s = sort == null ? "name" : sort;
        boolean desc = s.startsWith("-");
        String field = desc ? s.substring(1) : s;
        String var =
                switch (field) {
                    case "status" -> "STR(?status)";
                    case "creationDate" -> "?created";
                    case "lastUpdated" -> "?updated";
                    default -> "STR(?name)";
                };
        String primary = desc ? "DESC(" + var + ")" : "ASC(" + var + ")";
        return "name".equals(field) ? primary : primary + " ASC(STR(?name))";
    }

    public Optional<VocabularyVersion> findVersion(String name, int version, int latestVersion) {
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?num ?desc ?log ?status ?createdBy ?created ?updated WHERE { GRAPH ?gInternal {"
                        + " ?ver vm:ofVocabulary ?v ; vm:version ?num ; vm:description ?desc ; vm:changelog"
                        + " ?log ; vm:status ?status ; vm:createdBy ?createdBy ; vm:creationDate ?created ;"
                        + " vm:lastUpdateDate ?updated . } } LIMIT 1");
        q.setIri("ver", Rdf.versionIri(name, version));
        q.setIri("v", Rdf.vocabularyIri(name));
        List<QuerySolution> rows = store.select(q.toString());
        return rows.isEmpty() ? Optional.empty() : Optional.of(toVersion(name, rows.get(0), latestVersion));
    }

    public Page<VocabularyVersion> listVersions(String name, int latestVersion, String sort, int page, int pageSize) {
        ParameterizedSparqlString count = Rdf.pss(
                "SELECT (COUNT(?ver) AS ?n) WHERE { GRAPH ?gInternal { ?ver vm:ofVocabulary ?v } }" + " LIMIT 1");
        count.setIri("v", Rdf.vocabularyIri(name));
        long total =
                Long.parseLong(Rdf.str(store.select(count.toString()).get(0).get("n")));
        List<VocabularyVersion> items = new ArrayList<>();
        long offset = (long) (page - 1) * pageSize;
        if (offset < total) {
            ParameterizedSparqlString q =
                    Rdf.pss("SELECT ?num ?desc ?log ?status ?createdBy ?created ?updated WHERE { GRAPH"
                            + " ?gInternal { ?ver vm:ofVocabulary ?v ; vm:version ?num ; vm:description ?desc"
                            + " ; vm:changelog ?log ; vm:status ?status ; vm:createdBy ?createdBy ;"
                            + " vm:creationDate ?created ; vm:lastUpdateDate ?updated . } } ORDER BY "
                            + versionOrderBy(sort)
                            + " LIMIT "
                            + pageSize
                            + " OFFSET "
                            + offset);
            q.setIri("v", Rdf.vocabularyIri(name));
            for (QuerySolution row : store.select(q.toString())) {
                items.add(toVersion(name, row, latestVersion));
            }
        }
        return new Page<>(items, total);
    }

    /** ORDER BY of the version list (D23, D154): default and ties version descending. */
    static String versionOrderBy(String sort) {
        if (sort == null) {
            return "DESC(?num)";
        }
        boolean desc = sort.startsWith("-");
        String field = desc ? sort.substring(1) : sort;
        String var =
                switch (field) {
                    case "status" -> "STR(?status)";
                    case "creationDate" -> "?created";
                    default -> null;
                };
        if (var == null) {
            return "DESC(?num)";
        }
        return (desc ? "DESC(" + var + ")" : "ASC(" + var + ")") + " DESC(?num)";
    }

    public Optional<byte[]> content(String name, int version) {
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?i ?data WHERE { GRAPH ?gContent { ?c vm:chunkOf ?ver ; vm:index ?i ; vm:data"
                        + " ?data } } ORDER BY ?i LIMIT 101");
        q.setIri("ver", Rdf.versionIri(name, version));
        List<QuerySolution> rows = store.select(q.toString());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<String> chunks = new ArrayList<>();
        for (QuerySolution r : rows) {
            chunks.add(Rdf.str(r.get("data")));
        }
        return Optional.of(Content.fromChunks(chunks));
    }

    public Optional<ContentFingerprint> fingerprint(String name, int version) {
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?sha ?size ?desc WHERE { GRAPH ?gInternal { ?ver vm:contentSha256 ?sha ;"
                        + " vm:contentSize ?size ; vm:description ?desc } } LIMIT 1");
        q.setIri("ver", Rdf.versionIri(name, version));
        List<QuerySolution> rows = store.select(q.toString());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        QuerySolution r = rows.get(0);
        return Optional.of(new ContentFingerprint(
                Rdf.str(r.get("sha")), Long.parseLong(Rdf.str(r.get("size"))), Rdf.str(r.get("desc"))));
    }

    /**
     * Creates a vocabulary with version 1 when no vocabulary has the same name, compared
     * case-insensitively (D37, D43, D82). One store transaction.
     *
     * @return true when this request's write was applied
     */
    public boolean insertVocabulary(String name, NewVersion nv, String token) {
        ParameterizedSparqlString op = Rdf.pss("""
            INSERT { GRAPH ?gInternal {
              ?v a vm:InternalVocabulary ; vm:name ?name ; vm:nameKey ?key ; vm:status "ACTIVE" ;
                 vm:latestVersion 1 ; vm:createdBy ?by ; vm:creationDate ?at ; vm:lastUpdateDate ?at .
              ?ver a vm:InternalVocabularyVersion ; vm:ofVocabulary ?v ; vm:version 1 ;
                 vm:description ?desc ; vm:changelog ?log ; vm:status "ACTIVE" ; vm:createdBy ?by ;
                 vm:creationDate ?at ; vm:lastUpdateDate ?at ; vm:contentSha256 ?sha ;
                 vm:contentSize ?size ; vm:createToken ?token .
            } } WHERE { FILTER NOT EXISTS { GRAPH ?gInternal { ?other vm:nameKey ?key } } }
            """);
        op.setIri("v", Rdf.vocabularyIri(name));
        op.setIri("ver", Rdf.versionIri(name, 1));
        op.setLiteral("name", name);
        op.setLiteral("key", name.toLowerCase(Locale.ROOT));
        bindNewVersion(op, nv, token);
        String update = op.toString() + " ;\n" + chunkInsert(name, 1, nv.bytes(), token);
        store.update(update);
        return tokenApplied(name, 1, token);
    }

    /**
     * Creates version expectedLatest+1 when the latest version is still expectedLatest, and
     * deprecates every other ACTIVE version (D4, D43, D82). One store transaction.
     *
     * @return true when this request's write was applied
     */
    public boolean insertVersion(String name, int expectedLatest, NewVersion nv, String token) {
        int next = expectedLatest + 1;
        ParameterizedSparqlString op1 = Rdf.pss("""
            DELETE { GRAPH ?gInternal { ?v vm:latestVersion ?old ; vm:lastUpdateDate ?lu } }
            INSERT { GRAPH ?gInternal {
              ?v vm:latestVersion ?next ; vm:lastUpdateDate ?at .
              ?ver a vm:InternalVocabularyVersion ; vm:ofVocabulary ?v ; vm:version ?next ;
                 vm:description ?desc ; vm:changelog ?log ; vm:status "ACTIVE" ; vm:createdBy ?by ;
                 vm:creationDate ?at ; vm:lastUpdateDate ?at ; vm:contentSha256 ?sha ;
                 vm:contentSize ?size ; vm:createToken ?token .
            } } WHERE { GRAPH ?gInternal { ?v vm:latestVersion ?old ; vm:lastUpdateDate ?lu .
              FILTER(?old = ?expected)
              FILTER NOT EXISTS { ?ver vm:version ?anyNum } } }
            """);
        op1.setIri("v", Rdf.vocabularyIri(name));
        op1.setIri("ver", Rdf.versionIri(name, next));
        op1.setParam("next", Rdf.integer(next));
        op1.setParam("expected", Rdf.integer(expectedLatest));
        bindNewVersion(op1, nv, token);
        ParameterizedSparqlString op2 = Rdf.pss("""
            DELETE { GRAPH ?gInternal { ?o vm:status "ACTIVE" ; vm:lastUpdateDate ?olu } }
            INSERT { GRAPH ?gInternal { ?o vm:status "DEPRECATED" ; vm:lastUpdateDate ?at } }
            WHERE { GRAPH ?gInternal { ?ver vm:createToken ?token .
              ?o vm:ofVocabulary ?v ; vm:status "ACTIVE" ; vm:lastUpdateDate ?olu .
              FILTER(?o != ?ver) } }
            """);
        op2.setIri("v", Rdf.vocabularyIri(name));
        op2.setIri("ver", Rdf.versionIri(name, next));
        op2.setLiteral("token", token);
        op2.setParam("at", Rdf.dateTime(nv.at()));
        String update = op1.toString() + " ;\n" + op2.toString() + " ;\n" + chunkInsert(name, next, nv.bytes(), token);
        store.update(update);
        return tokenApplied(name, next, token);
    }

    private static void bindNewVersion(ParameterizedSparqlString op, NewVersion nv, String token) {
        op.setLiteral("desc", nv.description());
        op.setLiteral("log", nv.changelog());
        op.setLiteral("by", nv.createdBy());
        op.setParam("at", Rdf.dateTime(nv.at()));
        op.setLiteral("sha", Content.sha256(nv.bytes()));
        op.setParam("size", Rdf.integer(nv.bytes().length));
        op.setLiteral("token", token);
    }

    private static String chunkInsert(String name, int version, byte[] bytes, String token) {
        List<String> chunks = Content.toChunks(bytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            ParameterizedSparqlString op =
                    Rdf.pss("INSERT { GRAPH ?gContent { ?c vm:chunkOf ?ver ; vm:index ?i ; vm:data ?data } }"
                            + " WHERE { GRAPH ?gInternal { ?ver vm:createToken ?token } }");
            op.setIri("c", Rdf.versionIri(name, version) + ":chunk:" + i);
            op.setIri("ver", Rdf.versionIri(name, version));
            op.setParam("i", Rdf.integer(i));
            op.setLiteral("token", token);
            op.setParam("data", Rdf.base64(chunks.get(i)));
            if (i > 0) {
                sb.append(" ;\n");
            }
            sb.append(op.toString());
        }
        return sb.toString();
    }

    private boolean tokenApplied(String name, int version, String token) {
        ParameterizedSparqlString q = Rdf.pss("ASK { GRAPH ?gInternal { ?ver vm:createToken ?token } }");
        q.setIri("ver", Rdf.versionIri(name, version));
        q.setLiteral("token", token);
        return store.ask(q.toString());
    }

    /**
     * Sets the vocabulary status when it is still {@code from} (D3, D43, D82).
     *
     * @return true when this request's change was applied
     */
    public boolean changeVocabularyStatus(String name, Status from, Status to, Instant at, String token) {
        ParameterizedSparqlString op = Rdf.pss("""
            DELETE { GRAPH ?gInternal { ?v vm:status ?from ; vm:lastUpdateDate ?lu } }
            INSERT { GRAPH ?gInternal { ?v vm:status ?to ; vm:lastUpdateDate ?at ;
                     vm:statusChange ?token } }
            WHERE { GRAPH ?gInternal { ?v vm:status ?from ; vm:lastUpdateDate ?lu } }
            """);
        op.setIri("v", Rdf.vocabularyIri(name));
        op.setLiteral("from", from.name());
        op.setLiteral("to", to.name());
        op.setParam("at", Rdf.dateTime(at));
        op.setLiteral("token", token);
        store.update(op.toString());
        return statusChangeApplied(Rdf.vocabularyIri(name), token);
    }

    /**
     * Sets a version status when it is still {@code from}; also sets the vocabulary's last update
     * date (D4, D160).
     *
     * @return true when this request's change was applied
     */
    public boolean changeVersionStatus(String name, int version, Status from, Status to, Instant at, String token) {
        ParameterizedSparqlString op1 = Rdf.pss("""
            DELETE { GRAPH ?gInternal { ?ver vm:status ?from ; vm:lastUpdateDate ?lu } }
            INSERT { GRAPH ?gInternal { ?ver vm:status ?to ; vm:lastUpdateDate ?at ;
                     vm:statusChange ?token } }
            WHERE { GRAPH ?gInternal { ?ver vm:status ?from ; vm:lastUpdateDate ?lu } }
            """);
        op1.setIri("ver", Rdf.versionIri(name, version));
        op1.setLiteral("from", from.name());
        op1.setLiteral("to", to.name());
        op1.setParam("at", Rdf.dateTime(at));
        op1.setLiteral("token", token);
        ParameterizedSparqlString op2 = Rdf.pss("""
            DELETE { GRAPH ?gInternal { ?v vm:lastUpdateDate ?lu } }
            INSERT { GRAPH ?gInternal { ?v vm:lastUpdateDate ?at } }
            WHERE { GRAPH ?gInternal { ?ver vm:statusChange ?token . ?v vm:lastUpdateDate ?lu } }
            """);
        op2.setIri("ver", Rdf.versionIri(name, version));
        op2.setIri("v", Rdf.vocabularyIri(name));
        op2.setParam("at", Rdf.dateTime(at));
        op2.setLiteral("token", token);
        store.update(op1.toString() + " ;\n" + op2.toString());
        return statusChangeApplied(Rdf.versionIri(name, version), token);
    }

    private boolean statusChangeApplied(String iri, String token) {
        ParameterizedSparqlString q = Rdf.pss("ASK { GRAPH ?gInternal { ?n vm:statusChange ?token } }");
        q.setIri("n", iri);
        q.setLiteral("token", token);
        return store.ask(q.toString());
    }

    private static Vocabulary toVocabulary(QuerySolution r) {
        return new Vocabulary(
                Rdf.str(r.get("name")),
                Status.parse(Rdf.str(r.get("status"))),
                Rdf.integerValue(r.get("latest")),
                Rdf.str(r.get("desc")),
                Rdf.str(r.get("createdBy")),
                Rdf.instant(r.get("created")),
                Rdf.instant(r.get("updated")));
    }

    private static VocabularyVersion toVersion(String name, QuerySolution r, int latest) {
        return new VocabularyVersion(
                name,
                Rdf.integerValue(r.get("num")),
                Rdf.str(r.get("desc")),
                Rdf.str(r.get("log")),
                Status.parse(Rdf.str(r.get("status"))),
                Rdf.str(r.get("createdBy")),
                Rdf.instant(r.get("created")),
                Rdf.instant(r.get("updated")),
                latest);
    }
}
