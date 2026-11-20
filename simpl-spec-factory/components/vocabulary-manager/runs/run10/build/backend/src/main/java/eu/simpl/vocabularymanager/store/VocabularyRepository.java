package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;

/**
 * Store access for vocabularies and vocabulary versions. Every query is built with Jena (ParameterizedSparqlString or
 * the update syntax objects, D94) and has a LIMIT (D85). Writes are conditional updates in one store transaction:
 * the first operation claims the change with a fresh change token only when the state read before is still current
 * (D82); later operations of the same request run only when the claim succeeded.
 */
public class VocabularyRepository {

    private static final String VOCABULARY_PATTERN = "GRAPH ?g { ?v a vm:InternalVocabulary ; vm:name ?name ;"
            + " vm:status ?status ; vm:latestVersion ?latest ; vm:creationDate ?created ;"
            + " vm:lastUpdateDate ?updated ; vm:createdBy ?createdBy .\n";
    private static final String VERSION_FIELDS = "?ver vm:vocabulary ?v ; vm:version ?version ;"
            + " vm:description ?description ; vm:changelog ?changelog ; vm:status ?vstatus ;"
            + " vm:createdBy ?vcreatedBy ; vm:creationDate ?vcreated ; vm:lastUpdateDate ?vupdated ;"
            + " vm:contentSha256 ?sha .\n";

    private final SparqlStore store;

    public VocabularyRepository(SparqlStore store) {
        this.store = store;
    }

    // ---- reads -------------------------------------------------------------------------------------------------

    public Optional<Vocabulary> find(String name) {
        ParameterizedSparqlString query = pss("SELECT * WHERE { " + VOCABULARY_PATTERN
                + " ?lv vm:vocabulary ?v ; vm:version ?latest ; vm:description ?description . } } LIMIT 1");
        query.setIri("v", Rdf.vocabularyIri(name));
        return store.select(query.toString()).stream().findFirst().map(VocabularyRepository::toVocabulary);
    }

    /** True when a vocabulary with this name exists, compared case-insensitively (D37). */
    public boolean existsIgnoreCase(String name) {
        ParameterizedSparqlString query = pss("SELECT ?v WHERE { GRAPH ?g { ?v vm:nameKey ?key } } LIMIT 1");
        query.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return !store.select(query.toString()).isEmpty();
    }

    public long count(ListQuery list) {
        ParameterizedSparqlString query =
                pss("SELECT (COUNT(?v) AS ?count) WHERE { " + VOCABULARY_PATTERN + " }\n" + filters(list) + "}");
        bindFilters(query, list);
        return Rdf.longValue(store.select(query.toString()).getFirst(), "count");
    }

    public List<Vocabulary> list(ListQuery list) {
        ParameterizedSparqlString query = pss("SELECT * WHERE { " + VOCABULARY_PATTERN
                + " ?lv vm:vocabulary ?v ; vm:version ?latest ; vm:description ?description . }\n"
                + filters(list) + "}\nORDER BY " + listOrder(list) + "\nLIMIT " + list.limit() + " OFFSET "
                + list.offset());
        bindFilters(query, list);
        return store.select(query.toString()).stream()
                .map(VocabularyRepository::toVocabulary)
                .toList();
    }

    public Optional<VocabularyVersion> findVersion(String name, int version) {
        ParameterizedSparqlString query = pss("SELECT * WHERE { GRAPH ?g { " + VERSION_FIELDS + " } } LIMIT 1");
        query.setIri("v", Rdf.vocabularyIri(name));
        query.setLiteral("version", Rdf.integer(version));
        return store.select(query.toString()).stream().findFirst().map(row -> toVersion(name, row, version));
    }

    public List<VocabularyVersion> listVersions(String name, String sort, long offset, int limit) {
        ParameterizedSparqlString query = pss("SELECT * WHERE { GRAPH ?g { " + VERSION_FIELDS + " } }\nORDER BY "
                + versionOrder(sort) + "\nLIMIT " + limit + " OFFSET " + offset);
        query.setIri("v", Rdf.vocabularyIri(name));
        return store.select(query.toString()).stream()
                .map(row -> toVersion(name, row, Rdf.integer(row, "version")))
                .toList();
    }

    public Optional<byte[]> content(String name, int version) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(ContentChunks.SELECT);
        query.setIri("c", Rdf.contentGraph(name, version));
        List<QuerySolution> rows = store.select(query.toString());
        return rows.isEmpty() ? Optional.empty() : Optional.of(ContentChunks.join(rows));
    }

    /** Status and latest version of the named vocabularies (search by name, D38). */
    public List<QuerySolution> searchLatest(Collection<String> names) {
        ParameterizedSparqlString query = pss("SELECT ?name ?status ?latest WHERE { VALUES (?name) { ?names }"
                + " GRAPH ?g { ?v vm:name ?name ; vm:status ?status ; vm:latestVersion ?latest } } LIMIT 101");
        query.setValues(
                "names", names.stream().map(Rdf::text).map(RDFNode.class::cast).toList());
        return store.select(query.toString());
    }

    /** Status of the named versions (search by name and version, D38). */
    public List<QuerySolution> searchVersions(Collection<List<? extends RDFNode>> pairs) {
        ParameterizedSparqlString query = pss("SELECT ?name ?version ?status WHERE {"
                + " VALUES (?name ?version) { ?pairs } GRAPH ?g { ?v vm:name ?name ."
                + " ?ver vm:vocabulary ?v ; vm:version ?version ; vm:status ?status } } LIMIT 101");
        query.setRowValues("pairs", pairs);
        return store.select(query.toString());
    }

    /**
     * The one store query of the reference check (Q5): every version of each referenced internal vocabulary, and
     * the registered external vocabularies whose namespace or alias key is among the given keys.
     */
    public List<QuerySolution> referenceData(Collection<String> names, Collection<String> externalKeys) {
        ParameterizedSparqlString query = pss("SELECT ?kind ?name ?vstatus ?latest ?version ?versionStatus ?key"
                + " WHERE {\n { VALUES (?name) { ?names } GRAPH ?g { ?v vm:name ?name ; vm:status ?vstatus ;"
                + " vm:latestVersion ?latest . ?ver vm:vocabulary ?v ; vm:version ?version ;"
                + " vm:status ?versionStatus } BIND(\"internal\" AS ?kind) }\n UNION\n"
                + " { VALUES (?key) { ?keys } GRAPH ?ge { { ?e vm:namespaceKey ?key } UNION"
                + " { ?e vm:alias ?a . ?a vm:key ?key } } BIND(\"external\" AS ?kind) }\n} LIMIT 200000");
        query.setIri("ge", Rdf.EXTERNAL_GRAPH);
        query.setValues(
                "names", names.stream().map(Rdf::text).map(RDFNode.class::cast).toList());
        query.setValues(
                "keys",
                externalKeys.stream().map(Rdf::text).map(RDFNode.class::cast).toList());
        return store.select(query.toString());
    }

    // ---- writes ------------------------------------------------------------------------------------------------

    /** Data of a new vocabulary version to store. */
    public record NewVersion(
            String name,
            int version,
            String description,
            String changelog,
            String createdBy,
            Instant now,
            byte[] content,
            String sha256) {}

    /**
     * Creates a vocabulary with version 1, only when no vocabulary has the same name ignoring case.
     *
     * @return true when this request created it
     */
    public boolean insertVocabulary(NewVersion data, String token) {
        ParameterizedSparqlString claim = pss("INSERT { GRAPH ?g {"
                + " ?v a vm:InternalVocabulary ; vm:name ?name ; vm:nameKey ?nameKey ; vm:status \"ACTIVE\" ;"
                + " vm:latestVersion ?one ; vm:creationDate ?now ; vm:lastUpdateDate ?now ;"
                + " vm:createdBy ?createdBy ; vm:changeToken ?token . } }\n"
                + "WHERE { FILTER NOT EXISTS { GRAPH ?g { ?other vm:nameKey ?nameKey } } } ;\n"
                + insertVersionTemplate());
        claim.setIri("v", Rdf.vocabularyIri(data.name()));
        claim.setLiteral("name", data.name());
        claim.setLiteral("nameKey", data.name().toLowerCase(Locale.ROOT));
        claim.setLiteral("one", Rdf.integer(1));
        claim.setLiteral("token", token);
        bindVersion(claim, data);
        store.update(withContent(claim, data, token).toString());
        return token.equals(changeToken(data.name()));
    }

    /**
     * Creates the next version, only when the latest version is still the expected one; deprecates every older
     * ACTIVE version in the same transaction (D4).
     *
     * @return true when this request created it
     */
    public boolean insertVersion(NewVersion data, int expectedLatest, String token) {
        ParameterizedSparqlString claim = pss("DELETE { GRAPH ?g { ?v vm:latestVersion ?latest ;"
                + " vm:lastUpdateDate ?updated ; vm:changeToken ?oldToken } }\n"
                + "INSERT { GRAPH ?g { ?v vm:latestVersion ?next ; vm:lastUpdateDate ?now ; vm:changeToken ?token } }\n"
                + "WHERE { GRAPH ?g { ?v vm:latestVersion ?latest ; vm:lastUpdateDate ?updated ;"
                + " vm:changeToken ?oldToken . FILTER(?latest = ?expected) } } ;\n"
                + insertVersionTemplate() + " ;\n"
                + "DELETE { GRAPH ?g { ?old vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldUpdated } }\n"
                + "INSERT { GRAPH ?g { ?old vm:status \"DEPRECATED\" ; vm:lastUpdateDate ?now } }\n"
                + "WHERE { GRAPH ?g { ?v vm:changeToken ?token . ?old vm:vocabulary ?v ; vm:version ?oldVersion ;"
                + " vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldUpdated . FILTER(?oldVersion < ?next) } }");
        claim.setIri("v", Rdf.vocabularyIri(data.name()));
        claim.setLiteral("expected", Rdf.integer(expectedLatest));
        claim.setLiteral("next", Rdf.integer(data.version()));
        claim.setLiteral("token", token);
        bindVersion(claim, data);
        store.update(withContent(claim, data, token).toString());
        return token.equals(changeToken(data.name()));
    }

    /**
     * Sets the vocabulary status when it is still the expected one.
     *
     * @return true when this request changed it
     */
    public boolean updateVocabularyStatus(
            String name, VocabularyStatus expected, VocabularyStatus target, Instant now, String token) {
        ParameterizedSparqlString update = pss("DELETE { GRAPH ?g { ?v vm:status ?status ;"
                + " vm:lastUpdateDate ?updated ; vm:changeToken ?oldToken } }\n"
                + "INSERT { GRAPH ?g { ?v vm:status ?target ; vm:lastUpdateDate ?now ; vm:changeToken ?token } }\n"
                + "WHERE { GRAPH ?g { ?v vm:status ?status ; vm:lastUpdateDate ?updated ;"
                + " vm:changeToken ?oldToken . FILTER(?status = ?expected) } }");
        update.setIri("v", Rdf.vocabularyIri(name));
        update.setLiteral("expected", expected.name());
        update.setLiteral("target", target.name());
        update.setLiteral("now", Rdf.dateTime(now));
        update.setLiteral("token", token);
        store.update(update.toString());
        return token.equals(changeToken(name));
    }

    /**
     * Sets a version status when it is still the expected one; the vocabulary's last update date follows (D25).
     *
     * @return true when this request changed it
     */
    public boolean updateVersionStatus(
            String name, int version, VocabularyStatus expected, VocabularyStatus target, Instant now, String token) {
        ParameterizedSparqlString update = pss("DELETE { GRAPH ?g { ?ver vm:status ?status ;"
                + " vm:lastUpdateDate ?vupdated . ?v vm:lastUpdateDate ?updated ; vm:changeToken ?oldToken } }\n"
                + "INSERT { GRAPH ?g { ?ver vm:status ?target ; vm:lastUpdateDate ?now ."
                + " ?v vm:lastUpdateDate ?now ; vm:changeToken ?token } }\n"
                + "WHERE { GRAPH ?g { ?ver vm:vocabulary ?v ; vm:status ?status ; vm:lastUpdateDate ?vupdated ."
                + " ?v vm:lastUpdateDate ?updated ; vm:changeToken ?oldToken . FILTER(?status = ?expected) } }");
        update.setIri("v", Rdf.vocabularyIri(name));
        update.setIri("ver", Rdf.versionIri(name, version));
        update.setLiteral("expected", expected.name());
        update.setLiteral("target", target.name());
        update.setLiteral("now", Rdf.dateTime(now));
        update.setLiteral("token", token);
        store.update(update.toString());
        return token.equals(changeToken(name));
    }

    String changeToken(String name) {
        ParameterizedSparqlString query = pss("SELECT ?token WHERE { GRAPH ?g { ?v vm:changeToken ?token } } LIMIT 1");
        query.setIri("v", Rdf.vocabularyIri(name));
        return store.select(query.toString()).stream()
                .findFirst()
                .map(row -> Rdf.string(row, "token"))
                .orElse(null);
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    private static String insertVersionTemplate() {
        return "INSERT { GRAPH ?g { ?ver a vm:InternalVocabularyVersion ; vm:vocabulary ?v ; vm:version ?next ;"
                + " vm:description ?description ; vm:changelog ?changelog ; vm:status \"ACTIVE\" ;"
                + " vm:creationDate ?now ; vm:lastUpdateDate ?now ; vm:createdBy ?createdBy ;"
                + " vm:contentGraph ?c ; vm:contentSha256 ?sha ; vm:contentSize ?size . } }\n"
                + "WHERE { GRAPH ?g { ?v vm:changeToken ?token } }";
    }

    private static void bindVersion(ParameterizedSparqlString pss, NewVersion data) {
        pss.setIri("ver", Rdf.versionIri(data.name(), data.version()));
        pss.setIri("c", Rdf.contentGraph(data.name(), data.version()));
        pss.setLiteral("next", Rdf.integer(data.version()));
        pss.setLiteral("description", data.description());
        pss.setLiteral("changelog", data.changelog());
        pss.setLiteral("createdBy", data.createdBy());
        pss.setLiteral("now", Rdf.dateTime(data.now()));
        pss.setLiteral("sha", data.sha256());
        pss.setLiteral("size", Rdf.integer(data.content().length));
    }

    /** Appends the conditional content insert (built with Jena's update syntax objects) to the request. */
    private static UpdateRequest withContent(ParameterizedSparqlString pss, NewVersion data, String token) {
        UpdateRequest request = pss.asUpdate();
        UpdateModify insert = new UpdateModify();
        insert.setHasInsertClause(true);
        insert.setHasDeleteClause(false);
        ContentChunks.addQuads(insert.getInsertAcc(), Rdf.contentGraph(data.name(), data.version()), data.content());
        ElementPathBlock condition = new ElementPathBlock();
        condition.addTriple(Triple.create(
                NodeFactory.createURI(Rdf.vocabularyIri(data.name())),
                NodeFactory.createURI(Rdf.NS + "changeToken"),
                NodeFactory.createLiteralString(token)));
        insert.setElement(new ElementNamedGraph(NodeFactory.createURI(Rdf.INTERNAL_GRAPH), condition));
        List<Update> operations = new ArrayList<>(request.getOperations());
        UpdateRequest result = new UpdateRequest();
        result.setPrefixMapping(request.getPrefixMapping());
        operations.forEach(result::add);
        result.add(insert);
        return result;
    }

    static ParameterizedSparqlString pss(String body) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(Rdf.PREFIXES + body);
        pss.setIri("g", Rdf.INTERNAL_GRAPH);
        return pss;
    }

    private static String filters(ListQuery list) {
        StringBuilder filters = new StringBuilder();
        if (list.nameContains() != null) {
            filters.append("FILTER(CONTAINS(LCASE(STR(?name)), ?nameFilter))\n");
        }
        if (list.status() != null) {
            filters.append("FILTER(?status = ?statusFilter)\n");
        }
        if (list.createdFrom() != null) {
            filters.append("FILTER(?created >= ?createdFrom && ?created < ?createdTo)\n");
        }
        if (list.updatedFrom() != null) {
            filters.append("FILTER(?updated >= ?updatedFrom && ?updated < ?updatedTo)\n");
        }
        return filters.toString();
    }

    private static void bindFilters(ParameterizedSparqlString pss, ListQuery list) {
        if (list.nameContains() != null) {
            pss.setLiteral("nameFilter", list.nameContains().toLowerCase(Locale.ROOT));
        }
        if (list.status() != null) {
            pss.setLiteral("statusFilter", list.status().name());
        }
        if (list.createdFrom() != null) {
            pss.setLiteral("createdFrom", Rdf.dateTime(list.createdFrom()));
            pss.setLiteral("createdTo", Rdf.dateTime(list.createdTo()));
        }
        if (list.updatedFrom() != null) {
            pss.setLiteral("updatedFrom", Rdf.dateTime(list.updatedFrom()));
            pss.setLiteral("updatedTo", Rdf.dateTime(list.updatedTo()));
        }
    }

    /** ORDER BY of the vocabulary list (D23, D58): ties broken by name ascending, by code point. */
    static String listOrder(ListQuery list) {
        String field = switch (list.sortField()) {
            case "status" -> "?status";
            case "creationDate" -> "?created";
            case "lastUpdated" -> "?updated";
            default -> "STR(?name)";
        };
        String primary = list.descending() ? "DESC(" + field + ")" : "ASC(" + field + ")";
        return field.equals("STR(?name)") ? primary : primary + " ASC(STR(?name))";
    }

    /** ORDER BY of the version list (D23): default version descending; ties broken by version descending. */
    static String versionOrder(String sort) {
        if (sort == null) {
            return "DESC(?version)";
        }
        boolean descending = sort.startsWith("-");
        String field = switch (descending ? sort.substring(1) : sort) {
            case "status" -> "?vstatus";
            case "creationDate" -> "?vcreated";
            default -> null;
        };
        if (field == null) {
            return "DESC(?version)";
        }
        return (descending ? "DESC(" + field + ")" : "ASC(" + field + ")") + " DESC(?version)";
    }

    private static Vocabulary toVocabulary(QuerySolution row) {
        return new Vocabulary(
                Rdf.string(row, "name"),
                VocabularyStatus.valueOf(Rdf.string(row, "status")),
                Rdf.integer(row, "latest"),
                Rdf.string(row, "description"),
                Rdf.string(row, "createdBy"),
                Rdf.instant(row, "created"),
                Rdf.instant(row, "updated"));
    }

    private static VocabularyVersion toVersion(String name, QuerySolution row, int version) {
        return new VocabularyVersion(
                name,
                version,
                Rdf.string(row, "description"),
                Rdf.string(row, "changelog"),
                VocabularyStatus.valueOf(Rdf.string(row, "vstatus")),
                Rdf.string(row, "vcreatedBy"),
                Rdf.instant(row, "vcreated"),
                Rdf.instant(row, "vupdated"),
                Rdf.string(row, "sha"));
    }
}
