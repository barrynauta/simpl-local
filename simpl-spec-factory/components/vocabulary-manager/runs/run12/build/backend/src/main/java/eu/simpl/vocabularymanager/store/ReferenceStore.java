package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.Model.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.stereotype.Component;

/**
 * Batch lookups by name and version (internal) and by normalised namespace key (external): the
 * reference check of an upload in one store query (Q5, D85) and the internal search (D38).
 */
@Component
public class ReferenceStore {

    private final StoreClient store;

    public ReferenceStore(StoreClient store) {
        this.store = store;
    }

    /**
     * What the store knows about one internal vocabulary.
     *
     * @param status vocabulary status
     * @param latest latest version number
     * @param versions status of each looked-up version (and, when asked, every ACTIVE version)
     */
    public record InternalInfo(Status status, int latest, Map<Integer, Status> versions) {

        /** Highest version number with status ACTIVE among the known versions, or 0. */
        public int latestActive() {
            int best = 0;
            for (Map.Entry<Integer, Status> e : versions.entrySet()) {
                if (e.getValue() == Status.ACTIVE && e.getKey() > best) {
                    best = e.getKey();
                }
            }
            return best;
        }
    }

    /**
     * Result of a combined lookup.
     *
     * @param internal per vocabulary name
     * @param externalKeys normalised namespace keys that a registered external vocabulary owns
     */
    public record Lookup(Map<String, InternalInfo> internal, Set<String> externalKeys) {}

    /**
     * One store query for internal names (with the given version numbers, the latest version and,
     * when {@code withActive}, every ACTIVE version) and for external namespace keys.
     */
    public Lookup lookup(
            Collection<String> names,
            Collection<Integer> versions,
            boolean withActive,
            Collection<String> externalKeys) {
        Map<String, InternalInfo> internal = new LinkedHashMap<>();
        Set<String> ext = new HashSet<>();
        if (names.isEmpty() && externalKeys.isEmpty()) {
            return new Lookup(internal, ext);
        }
        List<String> branches = new ArrayList<>();
        if (!names.isEmpty()) {
            String versionFilter =
                    versions.isEmpty() ? "(?num = ?latest)" : "(?num = ?latest || ?num IN (?versionList))";
            if (withActive) {
                versionFilter = "(" + versionFilter + " || STR(?ns) = \"ACTIVE\")";
            }
            branches.add("{ VALUES (?name) { ?names } GRAPH ?gInternal { ?v a vm:InternalVocabulary ; vm:name ?name"
                    + " ; vm:status ?vs ; vm:latestVersion ?latest . OPTIONAL { ?ver vm:ofVocabulary ?v ;"
                    + " vm:version ?num ; vm:status ?ns . FILTER"
                    + versionFilter
                    + " } } }");
        }
        if (!externalKeys.isEmpty()) {
            branches.add("{ VALUES (?key) { ?keys } GRAPH ?gExternal { { ?e vm:namespaceKey ?key } UNION { ?e"
                    + " vm:alias ?a . ?a vm:key ?key } } }");
        }
        ParameterizedSparqlString q = Rdf.pss("SELECT ?name ?vs ?latest ?num ?ns ?key WHERE { "
                + String.join(" UNION ", branches)
                + " } LIMIT 20000");
        if (!names.isEmpty()) {
            q.setValues("names", literals(names));
            if (!versions.isEmpty()) {
                StringBuilder in = new StringBuilder();
                for (Integer v : versions) {
                    if (!in.isEmpty()) {
                        in.append(", ");
                    }
                    in.append(v.intValue());
                }
                q.setCommandText(q.getCommandText().replace("?versionList", in.toString()));
            }
        }
        if (!externalKeys.isEmpty()) {
            q.setValues("keys", literals(externalKeys));
        }
        Map<String, Map<Integer, Status>> versionMaps = new HashMap<>();
        for (QuerySolution r : store.select(q.toString())) {
            if (r.contains("key")) {
                ext.add(Rdf.str(r.get("key")));
                continue;
            }
            String name = Rdf.str(r.get("name"));
            Map<Integer, Status> vm = versionMaps.computeIfAbsent(name, k -> new HashMap<>());
            if (!internal.containsKey(name)) {
                internal.put(
                        name,
                        new InternalInfo(Status.parse(Rdf.str(r.get("vs"))), Rdf.integerValue(r.get("latest")), vm));
            }
            if (r.contains("num")) {
                vm.put(Rdf.integerValue(r.get("num")), Status.parse(Rdf.str(r.get("ns"))));
            }
        }
        return new Lookup(internal, ext);
    }

    private static List<RDFNode> literals(Collection<String> values) {
        List<RDFNode> out = new ArrayList<>();
        for (String s : values) {
            out.add(ResourceFactory.createStringLiteral(s));
        }
        return out;
    }
}
