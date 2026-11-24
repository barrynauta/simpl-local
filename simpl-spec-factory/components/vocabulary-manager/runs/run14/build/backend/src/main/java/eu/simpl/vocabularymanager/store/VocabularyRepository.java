package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.out.NodeFmtLib;
import org.springframework.stereotype.Repository;

/**
 * Store access for vocabularies and vocabulary versions. Every list query runs filter, order, LIMIT and OFFSET in
 * the store (D85). Every write is one SPARQL update request, which is one store transaction (D8, D43), and is
 * conditional (D82): its first operation writes a receipt only when the state read before is still current, and
 * every further operation runs only when that receipt exists. The caller asks for the receipt afterwards.
 */
@Repository
public class VocabularyRepository {

    /** Maximum rows of the bounded lookups that a decision fixes (D38, Q5). */
    private static final int LOOKUP_LIMIT = 20_000;

    private static final String VOCABULARY_PATTERN = """
            GRAPH <%s> {
              ?voc a vm:InternalVocabulary ; vm:vocabularyName ?name ; vm:nameKey ?key ; vm:status ?status ;
                   vm:latestVersion ?latest ; vm:latestDescription ?desc ; vm:createdBy ?createdBy ;
                   vm:creationDate ?created ; vm:lastUpdateDate ?updated .
            }
            """.formatted(Sparql.META_GRAPH);

    private static final String VERSION_PATTERN = """
            GRAPH <%s> {
              ?ver vm:ofVocabulary ?voc ; vm:version ?n ; vm:status ?vstatus ; vm:description ?vdesc ;
                   vm:changelog ?changelog ; vm:createdBy ?vcreatedBy ; vm:creationDate ?vcreated ;
                   vm:lastUpdateDate ?vupdated ; vm:contentSha256 ?hash .
            }
            """.formatted(Sparql.META_GRAPH);

    private final StoreClient store;

    public VocabularyRepository(StoreClient store) {
        this.store = store;
    }

    /**
     * Filters of the vocabulary list (D19, D20). Null fields do not filter.
     *
     * @param nameContains lower-case text the name must contain
     * @param status vocabulary status
     * @param createdFrom inclusive lower bound of the creation date
     * @param createdTo exclusive upper bound of the creation date
     * @param updatedFrom inclusive lower bound of the last update date
     * @param updatedTo exclusive upper bound of the last update date
     */
    public record ListFilter(
            String nameContains,
            VocabularyStatus status,
            Instant createdFrom,
            Instant createdTo,
            Instant updatedFrom,
            Instant updatedTo) {}

    /** Finds a vocabulary by its exact name (D54). */
    public Optional<Vocabulary> findVocabulary(String name) {
        SparqlTemplate pss = Sparql.command("SELECT ?name ?status ?latest ?desc ?createdBy ?created ?updated WHERE {\n"
                + VOCABULARY_PATTERN + "} LIMIT 1");
        pss.setIri("voc", Sparql.vocabularyIri(name));
        ResultSetRewindable rows = store.select(pss.toString());
        return rows.hasNext() ? Optional.of(toVocabulary(rows.next())) : Optional.empty();
    }

    /** True when a vocabulary with this name exists, compared case-insensitively (D37). */
    public boolean nameTaken(String nameKey) {
        SparqlTemplate pss = Sparql.command("ASK { GRAPH <" + Sparql.META_GRAPH + "> { ?voc vm:nameKey ?nameKey } }");
        pss.setLiteral("nameKey", nameKey);
        return store.ask(pss.toString());
    }

    /** Counts the vocabularies that match the filter. */
    public long count(ListFilter filter) {
        SparqlTemplate pss = Sparql.command(
                "SELECT (COUNT(?voc) AS ?total) WHERE {\n" + VOCABULARY_PATTERN + filters(filter) + "} LIMIT 1");
        bindFilters(pss, filter);
        ResultSetRewindable rows = store.select(pss.toString());
        return rows.hasNext() ? Sparql.number(rows.next(), "total") : 0;
    }

    /**
     * Lists one page of vocabularies.
     *
     * @param filter the filters
     * @param orderBy the ORDER BY clause, from a fixed set
     * @param offset rows to skip
     * @param limit rows to return, at most 100
     * @return the vocabularies
     */
    public List<Vocabulary> list(ListFilter filter, String orderBy, long offset, int limit) {
        SparqlTemplate pss = Sparql.command("SELECT ?name ?status ?latest ?desc ?createdBy ?created ?updated WHERE {\n"
                + VOCABULARY_PATTERN + filters(filter) + "} ORDER BY " + orderBy + " LIMIT " + limit
                + " OFFSET " + offset);
        bindFilters(pss, filter);
        List<Vocabulary> result = new ArrayList<>();
        ResultSetRewindable rows = store.select(pss.toString());
        while (rows.hasNext()) {
            result.add(toVocabulary(rows.next()));
        }
        return result;
    }

    /** Finds one version. */
    public Optional<VocabularyVersion> findVersion(String name, long version) {
        SparqlTemplate pss =
                Sparql.command("SELECT ?n ?vstatus ?vdesc ?changelog ?vcreatedBy ?vcreated ?vupdated ?hash WHERE {\n"
                        + VERSION_PATTERN + "} LIMIT 1");
        pss.setIri("ver", Sparql.versionIri(name, version));
        pss.setIri("voc", Sparql.vocabularyIri(name));
        ResultSetRewindable rows = store.select(pss.toString());
        return rows.hasNext() ? Optional.of(toVersion(name, rows.next())) : Optional.empty();
    }

    /**
     * Lists one page of versions of a vocabulary.
     *
     * @param name the vocabulary name
     * @param orderBy the ORDER BY clause, from a fixed set
     * @param offset rows to skip
     * @param limit rows to return, at most 100
     * @return the versions
     */
    public List<VocabularyVersion> listVersions(String name, String orderBy, long offset, int limit) {
        SparqlTemplate pss =
                Sparql.command("SELECT ?n ?vstatus ?vdesc ?changelog ?vcreatedBy ?vcreated ?vupdated ?hash WHERE {\n"
                        + VERSION_PATTERN + "} ORDER BY " + orderBy + " LIMIT " + limit + " OFFSET " + offset);
        pss.setIri("voc", Sparql.vocabularyIri(name));
        List<VocabularyVersion> result = new ArrayList<>();
        ResultSetRewindable rows = store.select(pss.toString());
        while (rows.hasNext()) {
            result.add(toVersion(name, rows.next()));
        }
        return result;
    }

    /** Reads the stored content bytes of one version (D29, D106). */
    public Optional<byte[]> content(String name, long version) {
        SparqlTemplate pss =
                Sparql.command("SELECT ?content WHERE { GRAPH ?graph { ?ver vm:rdfContent ?content } } LIMIT 1");
        pss.setIri("graph", Sparql.contentGraph(name, version));
        pss.setIri("ver", Sparql.versionIri(name, version));
        ResultSetRewindable rows = store.select(pss.toString());
        if (!rows.hasNext()) {
            return Optional.empty();
        }
        return Optional.of(Base64.getDecoder().decode(Sparql.string(rows.next(), "content")));
    }

    /**
     * A row of the bounded lookup used by the search (D38) and by the reference check (D12, Q5).
     *
     * @param name vocabulary name
     * @param vocabularyStatus vocabulary status
     * @param latestVersion latest version number
     * @param version a version number, or -1 when the vocabulary has no matching version
     * @param versionStatus that version's status, or null when there is no version row
     */
    public record VersionRow(
            String name,
            VocabularyStatus vocabularyStatus,
            long latestVersion,
            long version,
            VocabularyStatus versionStatus) {}

    /**
     * One query for many names: each vocabulary with its latest version, the requested versions and its ACTIVE
     * versions. External lookup keys that are registered come back in {@code registeredKeys}.
     *
     * @param names vocabulary names
     * @param versions version numbers of interest (any vocabulary)
     * @param externalKeys normalised external namespace keys to look up, may be empty
     * @return the rows and the registered keys
     */
    public Lookup lookup(Collection<String> names, Collection<Long> versions, Collection<String> externalKeys) {
        if (names.isEmpty() && externalKeys.isEmpty()) {
            return new Lookup(List.of(), List.of());
        }
        List<String> branches = new ArrayList<>();
        if (!names.isEmpty()) {
            String versionFilter = versions.isEmpty()
                    ? ""
                    : " || ?n IN (" + versions.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ")";
            branches.add("{ VALUES ?name { " + values(names) + " } GRAPH <" + Sparql.META_GRAPH + "> {"
                    + " ?voc vm:vocabularyName ?name ; vm:status ?status ; vm:latestVersion ?latest ."
                    + " OPTIONAL { ?ver vm:ofVocabulary ?voc ; vm:version ?n ; vm:status ?vstatus ."
                    + " FILTER(?n = ?latest || ?vstatus = \"ACTIVE\"" + versionFilter + ") } } }");
        }
        if (!externalKeys.isEmpty()) {
            branches.add("{ VALUES ?key { " + values(externalKeys) + " } GRAPH <" + Sparql.META_GRAPH + "> {"
                    + " ?ext vm:lookupKey ?key } }");
        }
        String query = Sparql.PREFIXES + "SELECT ?name ?status ?latest ?n ?vstatus ?key WHERE {\n"
                + String.join("\nUNION\n", branches) + "\n} LIMIT " + LOOKUP_LIMIT;
        List<VersionRow> rows = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        ResultSetRewindable results = store.select(query);
        while (results.hasNext()) {
            QuerySolution row = results.next();
            if (row.contains("key")) {
                keys.add(Sparql.string(row, "key"));
                continue;
            }
            RDFNode n = row.get("n");
            rows.add(new VersionRow(
                    Sparql.string(row, "name"),
                    VocabularyStatus.valueOf(Sparql.string(row, "status")),
                    Sparql.number(row, "latest"),
                    n == null ? -1 : n.asLiteral().getLong(),
                    n == null ? null : VocabularyStatus.valueOf(Sparql.string(row, "vstatus"))));
        }
        return new Lookup(rows, keys);
    }

    /**
     * Result of {@link #lookup}.
     *
     * @param rows version rows
     * @param registeredKeys external keys that are registered
     */
    public record Lookup(List<VersionRow> rows, List<String> registeredKeys) {}

    /**
     * Creates a vocabulary with version 1, only when no vocabulary has the same name case-insensitively.
     *
     * @return true when this request stored it
     */
    public boolean insertVocabulary(Vocabulary vocabulary, VocabularyVersion version, byte[] content) {
        String receipt = Sparql.receiptIri(UUID.randomUUID().toString());
        SparqlTemplate pss = Sparql.command("""
                INSERT { GRAPH <%1$s> { ?receipt vm:at ?now } }
                WHERE { FILTER NOT EXISTS { GRAPH <%2$s> { ?other vm:nameKey ?nameKey } } } ;
                INSERT {
                  GRAPH ?contentGraph { ?ver vm:rdfContent ?content }
                  GRAPH <%2$s> {
                    ?voc a vm:InternalVocabulary ; vm:vocabularyName ?name ; vm:nameKey ?nameKey ; vm:status ?status ;
                         vm:latestVersion ?n ; vm:latestDescription ?description ; vm:createdBy ?createdBy ;
                         vm:creationDate ?now ; vm:lastUpdateDate ?now .
                    ?ver a vm:InternalVocabularyVersion ; vm:ofVocabulary ?voc ; vm:version ?n ; vm:status ?status ;
                         vm:description ?description ; vm:changelog ?changelog ; vm:createdBy ?createdBy ;
                         vm:creationDate ?now ; vm:lastUpdateDate ?now ; vm:contentSha256 ?hash ;
                         vm:contentGraph ?contentGraph ; vm:contentLength ?length .
                  }
                }
                WHERE { GRAPH <%1$s> { ?receipt vm:at ?anyTime } } ;
                """.formatted(Sparql.RECEIPT_GRAPH, Sparql.META_GRAPH) + cleanupReceipts());
        pss.setIri("receipt", receipt);
        pss.setIri("voc", Sparql.vocabularyIri(vocabulary.name()));
        pss.setIri("ver", Sparql.versionIri(vocabulary.name(), 1));
        pss.setIri("contentGraph", Sparql.contentGraph(vocabulary.name(), 1));
        pss.setLiteral("name", vocabulary.name());
        pss.setLiteral("nameKey", vocabulary.name().toLowerCase(Locale.ROOT));
        pss.setLiteral("status", VocabularyStatus.ACTIVE.name());
        pss.setLiteral("n", Sparql.integer(1));
        pss.setLiteral("description", version.description());
        pss.setLiteral("changelog", version.changelog());
        pss.setLiteral("createdBy", version.createdBy());
        pss.setLiteral("hash", version.contentSha256());
        pss.setLiteral("length", Sparql.integer(content.length));
        bindTimes(pss, version.creationDate());
        pss.setLiteral("content", Base64.getEncoder().encodeToString(content));
        store.update(pss.toString());
        return receiptExists(receipt);
    }

    /**
     * Stores a new version when the latest version is still {@code expectedLatest}; deprecates every older ACTIVE
     * version in the same transaction (D4).
     *
     * @return true when this request stored it
     */
    public boolean insertVersion(String name, long expectedLatest, VocabularyVersion version, byte[] content) {
        String receipt = Sparql.receiptIri(UUID.randomUUID().toString());
        SparqlTemplate pss = Sparql.command("""
                INSERT { GRAPH <%1$s> { ?receipt vm:at ?now } }
                WHERE { GRAPH <%2$s> { ?voc vm:latestVersion ?current } FILTER(?current = ?expected) } ;
                INSERT {
                  GRAPH ?contentGraph { ?ver vm:rdfContent ?content }
                  GRAPH <%2$s> {
                    ?ver a vm:InternalVocabularyVersion ; vm:ofVocabulary ?voc ; vm:version ?n ; vm:status "ACTIVE" ;
                         vm:description ?description ; vm:changelog ?changelog ; vm:createdBy ?createdBy ;
                         vm:creationDate ?now ; vm:lastUpdateDate ?now ; vm:contentSha256 ?hash ;
                         vm:contentGraph ?contentGraph ; vm:contentLength ?length .
                  }
                }
                WHERE { GRAPH <%1$s> { ?receipt vm:at ?anyTime } } ;
                DELETE { GRAPH <%2$s> { ?older vm:status "ACTIVE" ; vm:lastUpdateDate ?olderUpdated } }
                INSERT { GRAPH <%2$s> { ?older vm:status "DEPRECATED" ; vm:lastUpdateDate ?now } }
                WHERE {
                  GRAPH <%1$s> { ?receipt vm:at ?anyTime }
                  GRAPH <%2$s> { ?older vm:ofVocabulary ?voc ; vm:status "ACTIVE" ; vm:version ?olderNumber ;
                                        vm:lastUpdateDate ?olderUpdated . FILTER(?olderNumber < ?n) }
                } ;
                DELETE { GRAPH <%2$s> { ?voc vm:latestVersion ?oldLatest ; vm:latestDescription ?oldDescription ;
                                             vm:lastUpdateDate ?oldUpdated } }
                INSERT { GRAPH <%2$s> { ?voc vm:latestVersion ?n ; vm:latestDescription ?description ;
                                             vm:lastUpdateDate ?now } }
                WHERE {
                  GRAPH <%1$s> { ?receipt vm:at ?anyTime }
                  GRAPH <%2$s> { ?voc vm:latestVersion ?oldLatest ; vm:latestDescription ?oldDescription ;
                                      vm:lastUpdateDate ?oldUpdated }
                } ;
                """.formatted(Sparql.RECEIPT_GRAPH, Sparql.META_GRAPH) + cleanupReceipts());
        pss.setIri("receipt", receipt);
        pss.setIri("voc", Sparql.vocabularyIri(name));
        pss.setIri("ver", Sparql.versionIri(name, version.version()));
        pss.setIri("contentGraph", Sparql.contentGraph(name, version.version()));
        pss.setLiteral("expected", Sparql.integer(expectedLatest));
        pss.setLiteral("n", Sparql.integer(version.version()));
        pss.setLiteral("description", version.description());
        pss.setLiteral("changelog", version.changelog());
        pss.setLiteral("createdBy", version.createdBy());
        pss.setLiteral("hash", version.contentSha256());
        pss.setLiteral("length", Sparql.integer(content.length));
        bindTimes(pss, version.creationDate());
        pss.setLiteral("content", Base64.getEncoder().encodeToString(content));
        store.update(pss.toString());
        return receiptExists(receipt);
    }

    /**
     * Sets the vocabulary status when it is still {@code from} (D4, D43).
     *
     * @return true when this request changed it
     */
    public boolean updateVocabularyStatus(String name, VocabularyStatus from, VocabularyStatus to, Instant now) {
        String receipt = Sparql.receiptIri(UUID.randomUUID().toString());
        SparqlTemplate pss = Sparql.command("""
                INSERT { GRAPH <%1$s> { ?receipt vm:at ?now } }
                WHERE { GRAPH <%2$s> { ?voc vm:status ?from } } ;
                DELETE { GRAPH <%2$s> { ?voc vm:status ?oldStatus ; vm:lastUpdateDate ?oldUpdated } }
                INSERT { GRAPH <%2$s> { ?voc vm:status ?to ; vm:lastUpdateDate ?now } }
                WHERE {
                  GRAPH <%1$s> { ?receipt vm:at ?anyTime }
                  GRAPH <%2$s> { ?voc vm:status ?oldStatus ; vm:lastUpdateDate ?oldUpdated }
                } ;
                """.formatted(Sparql.RECEIPT_GRAPH, Sparql.META_GRAPH) + cleanupReceipts());
        pss.setIri("receipt", receipt);
        pss.setIri("voc", Sparql.vocabularyIri(name));
        pss.setLiteral("from", from.name());
        pss.setLiteral("to", to.name());
        bindTimes(pss, now);
        store.update(pss.toString());
        return receiptExists(receipt);
    }

    /**
     * Sets a version status when it is still {@code from}; also sets the last update date of the version and of
     * the vocabulary (D160).
     *
     * @return true when this request changed it
     */
    public boolean updateVersionStatus(
            String name, long version, VocabularyStatus from, VocabularyStatus to, Instant now) {
        String receipt = Sparql.receiptIri(UUID.randomUUID().toString());
        SparqlTemplate pss = Sparql.command("""
                INSERT { GRAPH <%1$s> { ?receipt vm:at ?now } }
                WHERE { GRAPH <%2$s> { ?ver vm:status ?from } } ;
                DELETE { GRAPH <%2$s> { ?ver vm:status ?oldStatus ; vm:lastUpdateDate ?oldUpdated .
                                        ?voc vm:lastUpdateDate ?oldVocUpdated } }
                INSERT { GRAPH <%2$s> { ?ver vm:status ?to ; vm:lastUpdateDate ?now . ?voc vm:lastUpdateDate ?now } }
                WHERE {
                  GRAPH <%1$s> { ?receipt vm:at ?anyTime }
                  GRAPH <%2$s> { ?ver vm:status ?oldStatus ; vm:lastUpdateDate ?oldUpdated .
                                 ?voc vm:lastUpdateDate ?oldVocUpdated }
                } ;
                """.formatted(Sparql.RECEIPT_GRAPH, Sparql.META_GRAPH) + cleanupReceipts());
        pss.setIri("receipt", receipt);
        pss.setIri("voc", Sparql.vocabularyIri(name));
        pss.setIri("ver", Sparql.versionIri(name, version));
        pss.setLiteral("from", from.name());
        pss.setLiteral("to", to.name());
        bindTimes(pss, now);
        store.update(pss.toString());
        return receiptExists(receipt);
    }

    /** Asks whether the receipt of a conditional write exists. */
    boolean receiptExists(String receipt) {
        SparqlTemplate pss = Sparql.command("ASK { GRAPH <" + Sparql.RECEIPT_GRAPH + "> { ?receipt vm:at ?anyTime } }");
        pss.setIri("receipt", receipt);
        return store.ask(pss.toString());
    }

    /** Removes receipts older than one hour in the same update (D176). */
    static String cleanupReceipts() {
        return """
                DELETE { GRAPH <%1$s> { ?oldReceipt vm:at ?oldTime } }
                WHERE { GRAPH <%1$s> { ?oldReceipt vm:at ?oldTime } FILTER(?oldTime < ?cutoff) }
                """.formatted(Sparql.RECEIPT_GRAPH);
    }

    private static void bindTimes(SparqlTemplate pss, Instant now) {
        pss.setLiteral("now", Sparql.dateTime(now));
        pss.setLiteral("cutoff", Sparql.dateTime(now.minus(1, ChronoUnit.HOURS)));
    }

    private static String filters(ListFilter filter) {
        StringBuilder text = new StringBuilder();
        if (filter.nameContains() != null) {
            text.append("FILTER(CONTAINS(?key, ?nameFilter))\n");
        }
        if (filter.status() != null) {
            text.append("FILTER(?status = ?statusFilter)\n");
        }
        if (filter.createdFrom() != null) {
            text.append("FILTER(?created >= ?createdFrom && ?created < ?createdTo)\n");
        }
        if (filter.updatedFrom() != null) {
            text.append("FILTER(?updated >= ?updatedFrom && ?updated < ?updatedTo)\n");
        }
        return text.toString();
    }

    private static void bindFilters(SparqlTemplate pss, ListFilter filter) {
        if (filter.nameContains() != null) {
            pss.setLiteral("nameFilter", filter.nameContains());
        }
        if (filter.status() != null) {
            pss.setLiteral("statusFilter", filter.status().name());
        }
        if (filter.createdFrom() != null) {
            pss.setLiteral("createdFrom", Sparql.dateTime(filter.createdFrom()));
            pss.setLiteral("createdTo", Sparql.dateTime(filter.createdTo()));
        }
        if (filter.updatedFrom() != null) {
            pss.setLiteral("updatedFrom", Sparql.dateTime(filter.updatedFrom()));
            pss.setLiteral("updatedTo", Sparql.dateTime(filter.updatedTo()));
        }
    }

    private static String values(Collection<String> texts) {
        StringBuilder text = new StringBuilder();
        for (String value : texts) {
            text.append(NodeFmtLib.strNT(NodeFactory.createLiteralString(value)))
                    .append(' ');
        }
        return text.toString();
    }

    private static Vocabulary toVocabulary(QuerySolution row) {
        return new Vocabulary(
                Sparql.string(row, "name"),
                VocabularyStatus.valueOf(Sparql.string(row, "status")),
                Sparql.number(row, "latest"),
                Sparql.string(row, "desc"),
                Sparql.string(row, "createdBy"),
                Sparql.instant(row, "created"),
                Sparql.instant(row, "updated"));
    }

    private static VocabularyVersion toVersion(String name, QuerySolution row) {
        return new VocabularyVersion(
                name,
                Sparql.number(row, "n"),
                VocabularyStatus.valueOf(Sparql.string(row, "vstatus")),
                Sparql.string(row, "vdesc"),
                Sparql.string(row, "changelog"),
                Sparql.string(row, "vcreatedBy"),
                Sparql.instant(row, "vcreated"),
                Sparql.instant(row, "vupdated"),
                Sparql.string(row, "hash"));
    }
}
