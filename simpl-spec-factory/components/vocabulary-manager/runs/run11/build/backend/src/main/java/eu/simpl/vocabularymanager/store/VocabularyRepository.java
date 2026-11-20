package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreSchema.instant;
import static eu.simpl.vocabularymanager.store.StoreSchema.integer;
import static eu.simpl.vocabularymanager.store.StoreSchema.sparql;
import static eu.simpl.vocabularymanager.store.StoreSchema.text;

import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.sparql.engine.binding.Binding;
import org.springframework.stereotype.Repository;

/**
 * Data access for vocabularies and vocabulary versions (CODE-47). Every query is built with
 * ParameterizedSparqlString (D94); every list query has ORDER BY, LIMIT and OFFSET and a COUNT
 * query (D85). Writes are conditional updates that carry a write id; the caller checks with
 * {@link #writeApplied(String, String)} whether the condition held (D82).
 */
@Repository
public class VocabularyRepository {

    private static final String VOCABULARY_PATTERN = """
            ?v a vm:InternalVocabulary ; vm:name ?name ; vm:status ?status ; vm:latestVersion ?latest ;
               vm:createdBy ?createdBy ; vm:creationDate ?created ; vm:lastUpdateDate ?updated .
            """;

    private final SparqlClient client;

    public VocabularyRepository(SparqlClient client) {
        this.client = client;
    }

    /**
     * The vocabulary with this exact name (D54).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Optional<Vocabulary> find(String name) {
        ParameterizedSparqlString pss = sparql("SELECT ?name ?status ?latest ?createdBy ?created ?updated ?desc"
                + " WHERE { GRAPH ?gInternal { " + VOCABULARY_PATTERN
                + " ?lv vm:vocabulary ?v ; vm:version ?latest ; vm:description ?desc . } } LIMIT 1");
        pss.setIri("v", StoreSchema.vocabularyIri(name));
        return client.select(pss.toString()).stream().findFirst().map(VocabularyRepository::vocabulary);
    }

    /**
     * True when a vocabulary name exists, compared case-insensitively (D37).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public boolean nameTaken(String name) {
        ParameterizedSparqlString pss = sparql("ASK { GRAPH ?gInternal { ?v vm:nameKey ?key } }");
        pss.setLiteral("key", name.toLowerCase(java.util.Locale.ROOT));
        return client.ask(pss.toString());
    }

    /**
     * One page of vocabularies (D19 to D23, D85).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Page<Vocabulary> list(ListQuery query) {
        StringBuilder filters = new StringBuilder();
        if (query.nameContains() != null) {
            filters.append(" FILTER(CONTAINS(LCASE(STR(?name)), ?nameFilter))");
        }
        if (query.status() != null) {
            filters.append(" FILTER(?status = ?statusFilter)");
        }
        if (query.creationDate() != null) {
            filters.append(" FILTER(?created >= ?createdFrom && ?created < ?createdTo)");
        }
        if (query.lastUpdated() != null) {
            filters.append(" FILTER(?updated >= ?updatedFrom && ?updated < ?updatedTo)");
        }
        String where = " WHERE { GRAPH ?gInternal { " + VOCABULARY_PATTERN + filters + " } }";

        ParameterizedSparqlString count = sparql("SELECT (COUNT(?v) AS ?n)" + where);
        bindFilters(count, query);
        long total = Long.parseLong(text(client.select(count.toString()).get(0).get("n")));
        if (total == 0 || query.offset() >= total) {
            return new Page<>(List.of(), total);
        }
        String order = listOrder(query.sortField(), query.ascending());
        ParameterizedSparqlString select = sparql("SELECT ?name ?status ?latest ?createdBy ?created ?updated ?desc"
                + " WHERE { GRAPH ?gInternal { " + VOCABULARY_PATTERN + filters
                + " ?lv vm:vocabulary ?v ; vm:version ?latest ; vm:description ?desc . } }"
                + " ORDER BY " + order + " LIMIT " + query.pageSize() + " OFFSET " + query.offset());
        bindFilters(select, query);
        List<Vocabulary> items = client.select(select.toString()).stream()
                .map(VocabularyRepository::vocabulary)
                .toList();
        return new Page<>(items, total);
    }

    /**
     * One vocabulary version.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Optional<VocabularyVersion> findVersion(String name, int version) {
        ParameterizedSparqlString pss = sparql(
                "SELECT ?name ?ver ?desc ?changelog ?status ?createdBy ?created ?updated ?latest WHERE { GRAPH ?gInternal {"
                        + " ?v vm:name ?name ; vm:latestVersion ?latest ."
                        + " ?x vm:vocabulary ?v ; vm:version ?ver ; vm:description ?desc ; vm:changelog ?changelog ;"
                        + " vm:status ?status ; vm:createdBy ?createdBy ; vm:creationDate ?created ;"
                        + " vm:lastUpdateDate ?updated . } } LIMIT 1");
        pss.setIri("v", StoreSchema.vocabularyIri(name));
        pss.setIri("x", StoreSchema.versionIri(name, version));
        return client.select(pss.toString()).stream().findFirst().map(VocabularyRepository::version);
    }

    /**
     * One page of the versions of a vocabulary (D23, D35, D85).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Page<VocabularyVersion> listVersions(String name, String sortField, boolean ascending, long page, int size) {
        String pattern = " ?v vm:name ?name ; vm:latestVersion ?latest ."
                + " ?x vm:vocabulary ?v ; vm:version ?ver ; vm:description ?desc ; vm:changelog ?changelog ;"
                + " vm:status ?status ; vm:createdBy ?createdBy ; vm:creationDate ?created ;"
                + " vm:lastUpdateDate ?updated . ";
        ParameterizedSparqlString count =
                sparql("SELECT (COUNT(?x) AS ?n) WHERE { GRAPH ?gInternal { ?x vm:vocabulary ?v } }");
        count.setIri("v", StoreSchema.vocabularyIri(name));
        long total = Long.parseLong(text(client.select(count.toString()).get(0).get("n")));
        long offset = (page - 1) * size;
        if (total == 0 || offset >= total) {
            return new Page<>(List.of(), total);
        }
        ParameterizedSparqlString select = sparql(
                "SELECT ?name ?ver ?desc ?changelog ?status ?createdBy ?created ?updated ?latest WHERE { GRAPH ?gInternal {"
                        + pattern + "} } ORDER BY " + versionOrder(sortField, ascending) + " LIMIT " + size
                        + " OFFSET " + offset);
        select.setIri("v", StoreSchema.vocabularyIri(name));
        List<VocabularyVersion> items = client.select(select.toString()).stream()
                .map(VocabularyRepository::version)
                .toList();
        return new Page<>(items, total);
    }

    /**
     * The stored content bytes of a version.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Optional<byte[]> content(String name, int version) {
        ParameterizedSparqlString pss =
                sparql("SELECT ?content WHERE { GRAPH ?gContent { ?x vm:content ?content } } LIMIT 1");
        pss.setIri("x", StoreSchema.versionIri(name, version));
        return client.select(pss.toString()).stream()
                .findFirst()
                .map(b -> Base64.getDecoder().decode(text(b.get("content"))));
    }

    /**
     * Stores a new vocabulary with version 1, only when no vocabulary has the same name
     * case-insensitively (D43, D82). One store transaction.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public void create(NewVersion data, String writeId) {
        ParameterizedSparqlString pss = sparql("""
                INSERT { GRAPH ?gInternal {
                    ?v a vm:InternalVocabulary ; vm:name ?name ; vm:nameKey ?key ; vm:status "ACTIVE" ;
                       vm:latestVersion 1 ; vm:createdBy ?createdBy ; vm:creationDate ?now ;
                       vm:lastUpdateDate ?now ; vm:writeId ?writeId ; vm:creationWriteId ?writeId .
                    ?x a vm:InternalVocabularyVersion ; vm:vocabulary ?v ; vm:version 1 ;
                       vm:description ?desc ; vm:changelog ?changelog ; vm:status "ACTIVE" ;
                       vm:createdBy ?createdBy ; vm:creationDate ?now ; vm:lastUpdateDate ?now ;
                       vm:creationWriteId ?writeId . }
                  GRAPH ?gContent { ?x vm:content ?content ; vm:contentLength ?length . } }
                WHERE { FILTER NOT EXISTS { GRAPH ?gInternal { ?any vm:nameKey ?key } } }
                """);
        bindNewVersion(pss, data, 1, writeId);
        pss.setLiteral("key", data.name().toLowerCase(java.util.Locale.ROOT));
        client.update(pss.toString());
    }

    /**
     * Adds version expectedLatest + 1 when the latest version number is still expectedLatest
     * (D82). In the same transaction every older ACTIVE version becomes DEPRECATED (D4).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public void addVersion(NewVersion data, int expectedLatest, String writeId) {
        int next = expectedLatest + 1;
        ParameterizedSparqlString pss = sparql("""
                DELETE { GRAPH ?gInternal { ?v vm:latestVersion ?l ; vm:lastUpdateDate ?u ; vm:writeId ?w . } }
                INSERT { GRAPH ?gInternal { ?v vm:latestVersion ?next ; vm:lastUpdateDate ?now ; vm:writeId ?writeId . } }
                WHERE { GRAPH ?gInternal { ?v vm:latestVersion ?expected ; vm:latestVersion ?l ;
                        vm:lastUpdateDate ?u ; vm:writeId ?w . } } ;
                DELETE { GRAPH ?gInternal { ?old vm:status "ACTIVE" ; vm:lastUpdateDate ?ou . } }
                INSERT { GRAPH ?gInternal { ?old vm:status "DEPRECATED" ; vm:lastUpdateDate ?now . } }
                WHERE { GRAPH ?gInternal { ?v vm:writeId ?writeId . ?old vm:vocabulary ?v ;
                        vm:status "ACTIVE" ; vm:lastUpdateDate ?ou . } } ;
                INSERT { GRAPH ?gInternal {
                    ?x a vm:InternalVocabularyVersion ; vm:vocabulary ?v ; vm:version ?next ;
                       vm:description ?desc ; vm:changelog ?changelog ; vm:status "ACTIVE" ;
                       vm:createdBy ?createdBy ; vm:creationDate ?now ; vm:lastUpdateDate ?now ;
                       vm:creationWriteId ?writeId . }
                  GRAPH ?gContent { ?x vm:content ?content ; vm:contentLength ?length . } }
                WHERE { GRAPH ?gInternal { ?v vm:writeId ?writeId } }
                """);
        bindNewVersion(pss, data, next, writeId);
        pss.setLiteral("expected", expectedLatest);
        pss.setLiteral("next", next);
        client.update(pss.toString());
    }

    /**
     * Sets the vocabulary status when it is still the expected one (D3, D82).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public void setStatus(
            String name, VocabularyStatus expected, VocabularyStatus target, Instant now, String writeId) {
        ParameterizedSparqlString pss = sparql("""
                DELETE { GRAPH ?gInternal { ?v vm:status ?s ; vm:lastUpdateDate ?u ; vm:statusWriteId ?w . } }
                INSERT { GRAPH ?gInternal { ?v vm:status ?target ; vm:lastUpdateDate ?now ; vm:statusWriteId ?writeId . } }
                WHERE { GRAPH ?gInternal { ?v vm:status ?expected ; vm:status ?s ; vm:lastUpdateDate ?u .
                        OPTIONAL { ?v vm:statusWriteId ?w } } }
                """);
        pss.setIri("v", StoreSchema.vocabularyIri(name));
        pss.setLiteral("expected", expected.name());
        pss.setLiteral("target", target.name());
        pss.setLiteral("writeId", writeId);
        StoreSchema.setTime(pss, "now", now);
        client.update(pss.toString());
    }

    /**
     * Sets a version status when it is still the expected one; the vocabulary's last update date
     * follows in the same transaction (D4, D25, D82).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public void setVersionStatus(
            String name, int version, VocabularyStatus expected, VocabularyStatus target, Instant now, String writeId) {
        ParameterizedSparqlString pss = sparql("""
                DELETE { GRAPH ?gInternal { ?x vm:status ?s ; vm:lastUpdateDate ?u ; vm:statusWriteId ?w . } }
                INSERT { GRAPH ?gInternal { ?x vm:status ?target ; vm:lastUpdateDate ?now ; vm:statusWriteId ?writeId . } }
                WHERE { GRAPH ?gInternal { ?x vm:status ?expected ; vm:status ?s ; vm:lastUpdateDate ?u .
                        OPTIONAL { ?x vm:statusWriteId ?w } } } ;
                DELETE { GRAPH ?gInternal { ?v vm:lastUpdateDate ?vu . } }
                INSERT { GRAPH ?gInternal { ?v vm:lastUpdateDate ?now . } }
                WHERE { GRAPH ?gInternal { ?x vm:statusWriteId ?writeId . ?v vm:lastUpdateDate ?vu . } }
                """);
        pss.setIri("v", StoreSchema.vocabularyIri(name));
        pss.setIri("x", StoreSchema.versionIri(name, version));
        pss.setLiteral("expected", expected.name());
        pss.setLiteral("target", target.name());
        pss.setLiteral("writeId", writeId);
        StoreSchema.setTime(pss, "now", now);
        client.update(pss.toString());
    }

    /** Which write id property a check reads. */
    public enum WriteKind {
        CREATION("creationWriteId"),
        STATUS("statusWriteId");

        private final String property;

        WriteKind(String property) {
            this.property = property;
        }
    }

    /**
     * True when the node carries the write id, so the conditional update held (D82).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public boolean writeApplied(String nodeIri, WriteKind kind, String writeId) {
        ParameterizedSparqlString pss = sparql("ASK { GRAPH ?gInternal { ?x vm:" + kind.property + " ?writeId } }");
        pss.setIri("x", nodeIri);
        pss.setLiteral("writeId", writeId);
        return client.ask(pss.toString());
    }

    /**
     * The state of the referenced internal vocabularies and the registered external keys, in one
     * store query (D12, Q5).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public ReferenceState references(List<String> names, List<String> externalKeys) {
        List<String> branches = new ArrayList<>();
        if (!names.isEmpty()) {
            branches.add("""
                    { VALUES ?name { %s } GRAPH ?gInternal { ?v vm:name ?name ; vm:status ?vstatus ;
                      vm:latestVersion ?latest . ?x vm:vocabulary ?v ; vm:version ?ver ; vm:status ?verStatus . }
                      BIND("i" AS ?kind) }
                    """.formatted(literals(names)));
        }
        if (!externalKeys.isEmpty()) {
            branches.add("""
                    { VALUES ?key { %s } GRAPH ?gExternal { ?e vm:lookupKey ?key } BIND("e" AS ?kind) }
                    """.formatted(literals(externalKeys)));
        }
        if (branches.isEmpty()) {
            return new ReferenceState(List.of(), List.of());
        }
        String query = "SELECT ?kind ?name ?vstatus ?latest ?ver ?verStatus ?key WHERE { "
                + String.join(" UNION ", branches) + " }";
        List<ReferenceState.VersionRow> versions = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (Binding row : client.select(sparql(query).toString())) {
            if ("i".equals(text(row.get("kind")))) {
                versions.add(new ReferenceState.VersionRow(
                        text(row.get("name")),
                        VocabularyStatus.valueOf(text(row.get("vstatus"))),
                        integer(row.get("latest")),
                        integer(row.get("ver")),
                        VocabularyStatus.valueOf(text(row.get("verStatus")))));
            } else {
                keys.add(text(row.get("key")));
            }
        }
        return new ReferenceState(versions, keys);
    }

    private static String literals(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            ParameterizedSparqlString one = new ParameterizedSparqlString("?x");
            one.setLiteral("x", value);
            out.append(one.toString()).append(' ');
        }
        return out.toString();
    }

    private static void bindNewVersion(ParameterizedSparqlString pss, NewVersion data, int version, String writeId) {
        pss.setIri("v", StoreSchema.vocabularyIri(data.name()));
        pss.setIri("x", StoreSchema.versionIri(data.name(), version));
        pss.setLiteral("name", data.name());
        pss.setLiteral("desc", data.description());
        pss.setLiteral("changelog", data.changelog());
        pss.setLiteral("createdBy", data.createdBy());
        pss.setLiteral("writeId", writeId);
        pss.setLiteral("content", Base64.getEncoder().encodeToString(data.content()), XSDDatatype.XSDbase64Binary);
        pss.setLiteral("length", data.content().length);
        StoreSchema.setTime(pss, "now", data.now());
    }

    private static void bindFilters(ParameterizedSparqlString pss, ListQuery query) {
        if (query.nameContains() != null) {
            pss.setLiteral("nameFilter", query.nameContains().toLowerCase(java.util.Locale.ROOT));
        }
        if (query.status() != null) {
            pss.setLiteral("statusFilter", query.status().name());
        }
        if (query.creationDate() != null) {
            StoreSchema.setTime(pss, "createdFrom", query.creationDate().from());
            StoreSchema.setTime(pss, "createdTo", query.creationDate().to());
        }
        if (query.lastUpdated() != null) {
            StoreSchema.setTime(pss, "updatedFrom", query.lastUpdated().from());
            StoreSchema.setTime(pss, "updatedTo", query.lastUpdated().to());
        }
    }

    static String listOrder(String field, boolean ascending) {
        String direction = ascending ? "ASC" : "DESC";
        String variable = switch (field) {
            case "status" -> "?status";
            case "creationDate" -> "?created";
            case "lastUpdated" -> "?updated";
            default -> "?name";
        };
        String primary = direction + "(" + variable + ")";
        return "?name".equals(variable) ? primary : primary + " ASC(?name)";
    }

    static String versionOrder(String field, boolean ascending) {
        if (field == null) {
            return "DESC(?ver)";
        }
        String direction = ascending ? "ASC" : "DESC";
        String variable = switch (field) {
            case "status" -> "?status";
            case "creationDate" -> "?created";
            default -> "?name";
        };
        return direction + "(" + variable + ") DESC(?ver)";
    }

    private static Vocabulary vocabulary(Binding row) {
        return new Vocabulary(
                text(row.get("name")),
                integer(row.get("latest")),
                text(row.get("desc")),
                VocabularyStatus.valueOf(text(row.get("status"))),
                text(row.get("createdBy")),
                instant(row.get("created")),
                instant(row.get("updated")));
    }

    private static VocabularyVersion version(Binding row) {
        return new VocabularyVersion(
                text(row.get("name")),
                integer(row.get("ver")),
                text(row.get("desc")),
                text(row.get("changelog")),
                VocabularyStatus.valueOf(text(row.get("status"))),
                text(row.get("createdBy")),
                instant(row.get("created")),
                instant(row.get("updated")),
                integer(row.get("latest")));
    }
}
