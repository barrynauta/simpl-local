package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreModel.CHANGE_GRAPH;
import static eu.simpl.vocabularymanager.store.StoreModel.EXTERNAL_GRAPH;
import static eu.simpl.vocabularymanager.store.StoreModel.PREFIXES;
import static eu.simpl.vocabularymanager.store.StoreModel.VOCABULARY_GRAPH;
import static eu.simpl.vocabularymanager.store.StoreModel.changeIri;
import static eu.simpl.vocabularymanager.store.StoreModel.contentGraphIri;
import static eu.simpl.vocabularymanager.store.StoreModel.dateTime;
import static eu.simpl.vocabularymanager.store.StoreModel.instant;
import static eu.simpl.vocabularymanager.store.StoreModel.integer;
import static eu.simpl.vocabularymanager.store.StoreModel.number;
import static eu.simpl.vocabularymanager.store.StoreModel.str;
import static eu.simpl.vocabularymanager.store.StoreModel.strings;
import static eu.simpl.vocabularymanager.store.StoreModel.text;
import static eu.simpl.vocabularymanager.store.StoreModel.versionIri;
import static eu.simpl.vocabularymanager.store.StoreModel.vocabularyIri;

import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.validation.ReferenceLookup;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.query.QuerySolution;
import org.springframework.stereotype.Repository;

/**
 * Store access for vocabularies and versions. Every list query runs filter, sort and paging in the store with ORDER
 * BY, LIMIT and OFFSET, and every query has a LIMIT (D85). Every write is one conditional SPARQL update, confirmed by
 * its change record (D82).
 */
@Repository
public class VocabularyRepository implements ReferenceLookup {

    /** Highest LIMIT of a reference or search lookup: 100 references plus their ACTIVE versions (D38, Q5). */
    static final int LOOKUP_LIMIT = 20_000;

    private static final String VOCABULARY_PATTERN = """
            ?voc a vm:Vocabulary ; vm:name ?name ; vm:status ?status ; vm:latestVersion ?latest ;
                 vm:createdBy ?createdBy ; vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate .
            """;
    private static final String VERSION_FIELDS = """
            ?ver vm:ofVocabulary ?voc ; vm:versionNumber ?num ; vm:status ?verStatus ; vm:description ?description ;
                 vm:changelog ?changelog ; vm:createdBy ?verCreatedBy ; vm:creationDate ?verCreationDate ;
                 vm:lastUpdateDate ?verLastUpdateDate .
            """;

    private final SparqlStore store;

    /**
     * @param store the SPARQL store
     */
    public VocabularyRepository(SparqlStore store) {
        this.store = store;
    }

    /**
     * @param name exact vocabulary name (D54)
     * @return the vocabulary with its latest version values
     */
    public Optional<Vocabulary> findVocabulary(String name) {
        String query = PREFIXES + "SELECT * WHERE { GRAPH " + VOCABULARY_GRAPH + " {\n"
                + "  VALUES ?name { " + str(name) + " }\n" + VOCABULARY_PATTERN
                + "  ?lv vm:ofVocabulary ?voc ; vm:versionNumber ?latest ; vm:description ?latestDescription ;"
                + " vm:contentSha256 ?sha .\n"
                + "} } LIMIT 1";
        return store.select(query).stream().findFirst().map(VocabularyRepository::toVocabulary);
    }

    /**
     * @param name a name
     * @return true when a vocabulary with this name exists, compared case-insensitively (D37)
     */
    public boolean nameTaken(String name) {
        return store.ask(PREFIXES + "ASK { GRAPH " + VOCABULARY_GRAPH + " { ?voc vm:nameKey "
                + str(FieldRules.nameKey(name)) + " } }");
    }

    /**
     * @param filter the list filters (D19, D20)
     * @param sort the ORDER BY clause
     * @param offset rows to skip
     * @param limit rows to return, at most 100
     * @return the page and the total
     */
    public Page<Vocabulary> listVocabularies(VocabularyFilter filter, String sort, long offset, int limit) {
        String filters = filter.toSparql();
        String countQuery = PREFIXES + "SELECT (COUNT(*) AS ?total) WHERE { GRAPH " + VOCABULARY_GRAPH + " {\n"
                + VOCABULARY_PATTERN + "}\n" + filters + "} LIMIT 1";
        long total = store.select(countQuery).stream()
                .findFirst()
                .map(solution -> number(solution, "total"))
                .orElse(0L);
        if (total == 0 || offset >= total) {
            return new Page<>(List.of(), total);
        }
        String query = PREFIXES + "SELECT * WHERE { GRAPH " + VOCABULARY_GRAPH + " {\n" + VOCABULARY_PATTERN
                + "  ?lv vm:ofVocabulary ?voc ; vm:versionNumber ?latest ; vm:description ?latestDescription ;"
                + " vm:contentSha256 ?sha .\n"
                + "}\n" + filters + "} ORDER BY " + sort + " LIMIT " + limit + " OFFSET " + offset;
        List<Vocabulary> items = store.select(query).stream()
                .map(VocabularyRepository::toVocabulary)
                .toList();
        return new Page<>(items, total);
    }

    /**
     * @param name exact vocabulary name
     * @param version version number
     * @return the version
     */
    public Optional<VocabularyVersion> findVersion(String name, long version) {
        String query = PREFIXES + "SELECT * WHERE { GRAPH " + VOCABULARY_GRAPH + " {\n"
                + "  ?voc vm:name " + str(name) + " ; vm:latestVersion ?latest .\n"
                + "  VALUES ?num { " + integer(version) + " }\n" + VERSION_FIELDS
                + "} } LIMIT 1";
        return store.select(query).stream().findFirst().map(solution -> toVersion(name, solution));
    }

    /**
     * @param name exact vocabulary name
     * @param sort the ORDER BY clause
     * @param offset rows to skip
     * @param limit rows to return, at most 100
     * @return the versions of the page
     */
    public List<VocabularyVersion> listVersions(String name, String sort, long offset, int limit) {
        String query = PREFIXES + "SELECT * WHERE { GRAPH " + VOCABULARY_GRAPH + " {\n"
                + "  ?voc vm:name " + str(name) + " ; vm:latestVersion ?latest .\n" + VERSION_FIELDS
                + "} } ORDER BY " + sort + " LIMIT " + limit + " OFFSET " + offset;
        return store.select(query).stream()
                .map(solution -> toVersion(name, solution))
                .toList();
    }

    /**
     * @param name exact vocabulary name
     * @param version version number, or null for the latest version
     * @return the stored content bytes
     */
    public Optional<byte[]> content(String name, Long version) {
        String number = version == null ? "?latest" : integer(version);
        String query = PREFIXES + "SELECT ?content WHERE {\n"
                + "  GRAPH " + VOCABULARY_GRAPH + " { ?voc vm:name " + str(name) + " ; vm:latestVersion ?latest .\n"
                + "    ?ver vm:ofVocabulary ?voc ; vm:versionNumber " + number + " ; vm:contentGraph ?g . }\n"
                + "  GRAPH ?g { ?ver vm:content ?content }\n"
                + "} LIMIT 1";
        return store.select(query).stream()
                .findFirst()
                .map(solution -> Base64.getDecoder().decode(text(solution, "content")));
    }

    @Override
    public State lookup(Set<String> internalNames, Set<Long> versions, Set<String> externalKeys) {
        if (internalNames.isEmpty() && externalKeys.isEmpty()) {
            return new State(Map.of(), Set.of());
        }
        List<String> branches = new ArrayList<>();
        if (!internalNames.isEmpty()) {
            String versionFilter = versions.isEmpty()
                    ? ""
                    : " || ?num IN (" + versions.stream().map(StoreModel::integer).collect(Collectors.joining(", "))
                            + ")";
            branches.add("{ VALUES ?name { " + strings(internalNames) + " }\n"
                    + "  GRAPH " + VOCABULARY_GRAPH + " { ?voc vm:name ?name ; vm:status ?vstatus ;"
                    + " vm:latestVersion ?latest .\n"
                    + "    OPTIONAL { ?ver vm:ofVocabulary ?voc ; vm:versionNumber ?num ; vm:status ?verStatus .\n"
                    + "      FILTER(?verStatus = \"ACTIVE\" || ?num = ?latest" + versionFilter + ") } }\n"
                    + "  BIND(\"internal\" AS ?kind) }");
        }
        if (!externalKeys.isEmpty()) {
            branches.add("{ VALUES ?key { " + strings(externalKeys) + " }\n"
                    + "  GRAPH " + EXTERNAL_GRAPH + " { ?ext vm:lookupKey ?key }\n"
                    + "  BIND(\"external\" AS ?kind) }");
        }
        String query = PREFIXES + "SELECT ?kind ?name ?vstatus ?latest ?num ?verStatus ?key WHERE {\n"
                + String.join("\nUNION\n", branches) + "\n} LIMIT " + LOOKUP_LIMIT;
        Map<String, VocabularyStatus> statuses = new HashMap<>();
        Map<String, Long> latest = new HashMap<>();
        Map<String, Map<Long, VocabularyStatus>> versionStatuses = new LinkedHashMap<>();
        Set<String> registered = new HashSet<>();
        for (QuerySolution solution : store.select(query)) {
            if ("external".equals(text(solution, "kind"))) {
                registered.add(text(solution, "key"));
                continue;
            }
            String name = text(solution, "name");
            statuses.put(name, VocabularyStatus.valueOf(text(solution, "vstatus")));
            latest.put(name, number(solution, "latest"));
            Map<Long, VocabularyStatus> byVersion = versionStatuses.computeIfAbsent(name, key -> new HashMap<>());
            if (solution.contains("num")) {
                byVersion.put(number(solution, "num"), VocabularyStatus.valueOf(text(solution, "verStatus")));
            }
        }
        Map<String, InternalVocabularyState> internal = new LinkedHashMap<>();
        versionStatuses.forEach((name, byVersion) -> internal.put(
                name, new InternalVocabularyState(name, statuses.get(name), latest.get(name), Map.copyOf(byVersion))));
        return new State(internal, registered);
    }

    /**
     * Creates a vocabulary with version 1, only when no vocabulary has the same name case-insensitively (D37, D43).
     *
     * @param write the new version 1
     * @return true when applied, false when the name was taken in the meantime
     */
    public boolean insertVocabulary(VersionWrite write) {
        String voc = vocabularyIri(write.name());
        String update = PREFIXES + "INSERT {\n  GRAPH " + VOCABULARY_GRAPH + " {\n"
                + "    " + voc + " a vm:Vocabulary ; vm:name " + str(write.name()) + " ; vm:nameKey "
                + str(FieldRules.nameKey(write.name())) + " ;\n"
                + "      vm:status \"ACTIVE\" ; vm:latestVersion " + integer(1) + " ; vm:createdBy "
                + str(write.createdBy()) + " ;\n"
                + "      vm:creationDate " + dateTime(write.time()) + " ; vm:lastUpdateDate " + dateTime(write.time())
                + " .\n"
                + versionTriples(write, 1) + "  }\n"
                + contentAndChange(write, 1, "createVocabulary", voc)
                + "}\nWHERE { FILTER NOT EXISTS { GRAPH " + VOCABULARY_GRAPH + " { ?existing vm:nameKey "
                + str(FieldRules.nameKey(write.name())) + " } } }";
        return applyAndConfirm(update, write.changeToken());
    }

    /**
     * Creates the next version, only when the latest version is still the one read (D82). Every older ACTIVE version
     * becomes DEPRECATED in the same transaction (D4).
     *
     * @param write the new version
     * @param expectedLatest the latest version number the request read
     * @return true when applied, false when another write changed the latest version
     */
    public boolean insertVersion(VersionWrite write, long expectedLatest) {
        long next = expectedLatest + 1;
        String voc = vocabularyIri(write.name());
        String now = dateTime(write.time());
        String update = PREFIXES + "DELETE {\n  GRAPH " + VOCABULARY_GRAPH + " {\n"
                + "    " + voc + " vm:latestVersion ?latest ; vm:lastUpdateDate ?vocUpdated .\n"
                + "    ?old vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldUpdated .\n  }\n}\n"
                + "INSERT {\n  GRAPH " + VOCABULARY_GRAPH + " {\n"
                + "    " + voc + " vm:latestVersion " + integer(next) + " ; vm:lastUpdateDate " + now + " .\n"
                + "    ?old vm:status \"DEPRECATED\" ; vm:lastUpdateDate " + now + " .\n"
                + versionTriples(write, next) + "  }\n"
                + contentAndChange(write, next, "createVocabularyVersion", voc)
                + "}\nWHERE {\n  GRAPH " + VOCABULARY_GRAPH + " {\n"
                + "    " + voc + " vm:latestVersion ?latest ; vm:lastUpdateDate ?vocUpdated .\n"
                + "    FILTER(?latest = " + integer(expectedLatest) + ")\n"
                + "    OPTIONAL { ?old vm:ofVocabulary " + voc
                + " ; vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldUpdated . }\n"
                + "  }\n}";
        return applyAndConfirm(update, write.changeToken());
    }

    /**
     * Changes the vocabulary status, only when it is still the status read (D82).
     *
     * @param name exact vocabulary name
     * @param from the status read
     * @param to the requested status
     * @param change the change record values
     * @return true when applied
     */
    public boolean updateVocabularyStatus(String name, VocabularyStatus from, VocabularyStatus to, ChangeRecord change) {
        String voc = vocabularyIri(name);
        String update = PREFIXES + "DELETE { GRAPH " + VOCABULARY_GRAPH + " { " + voc + " vm:status "
                + str(from.name()) + " ; vm:lastUpdateDate ?updated } }\n"
                + "INSERT { GRAPH " + VOCABULARY_GRAPH + " { " + voc + " vm:status " + str(to.name())
                + " ; vm:lastUpdateDate " + dateTime(change.time()) + " }\n"
                + changeTriples(change, "updateVocabularyStatus", voc) + "}\n"
                + "WHERE { GRAPH " + VOCABULARY_GRAPH + " { " + voc + " vm:status " + str(from.name())
                + " ; vm:lastUpdateDate ?updated } }";
        return applyAndConfirm(update, change.token());
    }

    /**
     * Changes a version status, only when it is still the status read (D82). The vocabulary's last update date
     * follows (D25).
     *
     * @param name exact vocabulary name
     * @param version the version number
     * @param from the status read
     * @param to the requested status
     * @param change the change record values
     * @return true when applied
     */
    public boolean updateVersionStatus(
            String name, long version, VocabularyStatus from, VocabularyStatus to, ChangeRecord change) {
        String voc = vocabularyIri(name);
        String ver = versionIri(name, version);
        String now = dateTime(change.time());
        String update = PREFIXES + "DELETE { GRAPH " + VOCABULARY_GRAPH + " { " + ver + " vm:status "
                + str(from.name()) + " ; vm:lastUpdateDate ?verUpdated . " + voc
                + " vm:lastUpdateDate ?vocUpdated } }\n"
                + "INSERT { GRAPH " + VOCABULARY_GRAPH + " { " + ver + " vm:status " + str(to.name())
                + " ; vm:lastUpdateDate " + now + " . " + voc + " vm:lastUpdateDate " + now + " }\n"
                + changeTriples(change, "updateVocabularyVersionStatus", ver) + "}\n"
                + "WHERE { GRAPH " + VOCABULARY_GRAPH + " { " + ver + " vm:status " + str(from.name())
                + " ; vm:lastUpdateDate ?verUpdated . " + voc + " vm:lastUpdateDate ?vocUpdated } }";
        return applyAndConfirm(update, change.token());
    }

    private String versionTriples(VersionWrite write, long number) {
        String ver = versionIri(write.name(), number);
        String now = dateTime(write.time());
        return "    " + ver + " a vm:VocabularyVersion ; vm:ofVocabulary " + vocabularyIri(write.name()) + " ;\n"
                + "      vm:versionNumber " + integer(number) + " ; vm:status \"ACTIVE\" ;\n"
                + "      vm:description " + str(write.description()) + " ; vm:changelog " + str(write.changelog())
                + " ;\n"
                + "      vm:createdBy " + str(write.createdBy()) + " ; vm:creationDate " + now
                + " ; vm:lastUpdateDate " + now + " ;\n"
                + "      vm:contentGraph " + contentGraphIri(write.changeToken()) + " ; vm:contentSha256 "
                + str(write.contentSha256()) + " ;\n"
                + "      vm:contentLength " + integer(write.content().length) + " .\n";
    }

    private String contentAndChange(VersionWrite write, long number, String operation, String target) {
        String encoded = Base64.getEncoder().encodeToString(write.content());
        return "  GRAPH " + contentGraphIri(write.changeToken()) + " { " + versionIri(write.name(), number)
                + " vm:content \"" + encoded + "\"^^xsd:base64Binary }\n"
                + changeTriples(new ChangeRecord(write.changeToken(), write.actor(), write.time()), operation, target);
    }

    static String changeTriples(ChangeRecord change, String operation, String target) {
        return "  GRAPH " + CHANGE_GRAPH + " { " + changeIri(change.token()) + " a vm:Change ; vm:operation "
                + str(operation) + " ; vm:target " + target + " ; vm:actor " + str(change.actor()) + " ; vm:at "
                + dateTime(change.time()) + " }\n";
    }

    private boolean applyAndConfirm(String update, String token) {
        store.update(update);
        return confirmed(store, token);
    }

    static boolean confirmed(SparqlStore store, String token) {
        return store.ask(PREFIXES + "ASK { GRAPH " + CHANGE_GRAPH + " { " + changeIri(token) + " a vm:Change } }");
    }

    private static Vocabulary toVocabulary(QuerySolution solution) {
        return new Vocabulary(
                text(solution, "name"),
                VocabularyStatus.valueOf(text(solution, "status")),
                number(solution, "latest"),
                text(solution, "latestDescription"),
                text(solution, "sha"),
                text(solution, "createdBy"),
                instant(solution, "creationDate"),
                instant(solution, "lastUpdateDate"));
    }

    private static VocabularyVersion toVersion(String name, QuerySolution solution) {
        return new VocabularyVersion(
                name,
                number(solution, "num"),
                VocabularyStatus.valueOf(text(solution, "verStatus")),
                text(solution, "description"),
                text(solution, "changelog"),
                text(solution, "verCreatedBy"),
                instant(solution, "verCreationDate"),
                instant(solution, "verLastUpdateDate"),
                number(solution, "latest"));
    }

    /**
     * Values of a new vocabulary version.
     *
     * @param name vocabulary name
     * @param description trimmed description
     * @param changelog trimmed changelog
     * @param createdBy creator (D27)
     * @param actor token subject for the change record (D80)
     * @param content uploaded bytes
     * @param contentSha256 hash of the bytes (D46)
     * @param time creation time, to the second
     * @param changeToken unique id of this write
     */
    public record VersionWrite(
            String name,
            String description,
            String changelog,
            String createdBy,
            String actor,
            byte[] content,
            String contentSha256,
            Instant time,
            String changeToken) {}

    /**
     * Values of a change record.
     *
     * @param token unique id of the write
     * @param actor token subject (D80)
     * @param time time of the change
     */
    public record ChangeRecord(String token, String actor, Instant time) {}
}
