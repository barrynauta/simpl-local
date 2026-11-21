package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.Model.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Model.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.stereotype.Component;

/** Store access for registered external vocabularies (D30, D41, D56, D73). */
@Component
public class ExternalStore {

    private final StoreClient store;

    public ExternalStore(StoreClient store) {
        this.store = store;
    }

    /**
     * A new registration.
     *
     * @param name name
     * @param namespaceUri as sent
     * @param namespaceKey normalised
     * @param aliasUris as sent, in order
     * @param aliasKeys normalised, same order
     * @param sourceUri optional
     * @param description optional
     * @param createdBy creator
     * @param at registration time
     * @param bytes content
     */
    public record NewExternal(
            String name,
            String namespaceUri,
            String namespaceKey,
            List<String> aliasUris,
            List<String> aliasKeys,
            String sourceUri,
            String description,
            String createdBy,
            Instant at,
            byte[] bytes) {}

    public Optional<ExternalVocabulary> find(String name) {
        List<ExternalVocabulary> found = load(List.of(name));
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    public boolean nameKeyExists(String name) {
        ParameterizedSparqlString q = Rdf.pss("ASK { GRAPH ?gExternal { ?e vm:nameKey ?key } }");
        q.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return store.ask(q.toString());
    }

    /** The normalised keys among {@code keys} that a namespace URI or alias already uses. */
    public Set<String> usedKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Set.of();
        }
        ParameterizedSparqlString q =
                Rdf.pss("SELECT DISTINCT ?key WHERE { VALUES (?key) { ?keys } GRAPH ?gExternal { { ?e"
                        + " vm:namespaceKey ?key } UNION { ?e vm:alias ?a . ?a vm:key ?key } } } LIMIT 101");
        q.setValues("keys", literals(keys));
        Set<String> out = new java.util.HashSet<>();
        for (QuerySolution r : store.select(q.toString())) {
            out.add(Rdf.str(r.get("key")));
        }
        return out;
    }

    /** Names of the external vocabularies that own each key (namespace or alias), per key. */
    public Map<String, String> ownersOfKeys(Collection<String> keys) {
        Map<String, String> out = new LinkedHashMap<>();
        if (keys.isEmpty()) {
            return out;
        }
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?key ?name WHERE { VALUES (?key) { ?keys } GRAPH ?gExternal { ?e vm:name ?name ."
                        + " { ?e vm:namespaceKey ?key } UNION { ?e vm:alias ?a . ?a vm:key ?key } } }"
                        + " LIMIT 1000");
        q.setValues("keys", literals(keys));
        for (QuerySolution r : store.select(q.toString())) {
            out.putIfAbsent(Rdf.str(r.get("key")), Rdf.str(r.get("name")));
        }
        return out;
    }

    public Page<ExternalVocabulary> list(int page, int pageSize) {
        ParameterizedSparqlString count = Rdf.pss(
                "SELECT (COUNT(?e) AS ?n) WHERE { GRAPH ?gExternal { ?e a vm:ExternalVocabulary } }" + " LIMIT 1");
        long total =
                Long.parseLong(Rdf.str(store.select(count.toString()).get(0).get("n")));
        long offset = (long) (page - 1) * pageSize;
        if (offset >= total) {
            return new Page<>(List.of(), total);
        }
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?name WHERE { GRAPH ?gExternal { ?e a vm:ExternalVocabulary ; vm:name ?name } }"
                        + " ORDER BY ASC(STR(?name)) LIMIT "
                        + pageSize
                        + " OFFSET "
                        + offset);
        List<String> names = new ArrayList<>();
        for (QuerySolution r : store.select(q.toString())) {
            names.add(Rdf.str(r.get("name")));
        }
        return new Page<>(load(names), total);
    }

    /** Full records for the names, in the order of {@code names}; unknown names are skipped. */
    public List<ExternalVocabulary> load(List<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?name ?ns ?src ?desc ?by ?at ?ai ?auri WHERE { VALUES (?name) { ?names } GRAPH"
                        + " ?gExternal { ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?ns ;"
                        + " vm:createdBy ?by ; vm:creationDate ?at . OPTIONAL { ?e vm:sourceUri ?src }"
                        + " OPTIONAL { ?e vm:description ?desc } OPTIONAL { ?e vm:alias ?a . ?a vm:index ?ai"
                        + " ; vm:uri ?auri } } } LIMIT 5000");
        q.setValues("names", literals(names));
        Map<String, QuerySolution> first = new LinkedHashMap<>();
        Map<String, TreeMap<Integer, String>> aliases = new LinkedHashMap<>();
        for (QuerySolution r : store.select(q.toString())) {
            String name = Rdf.str(r.get("name"));
            first.putIfAbsent(name, r);
            TreeMap<Integer, String> al = aliases.computeIfAbsent(name, k -> new TreeMap<>());
            if (r.contains("ai")) {
                al.put(Rdf.integerValue(r.get("ai")), Rdf.str(r.get("auri")));
            }
        }
        List<ExternalVocabulary> out = new ArrayList<>();
        for (String n : names) {
            QuerySolution r = first.get(n);
            if (r == null) {
                continue;
            }
            out.add(new ExternalVocabulary(
                    n,
                    Rdf.str(r.get("ns")),
                    List.copyOf(aliases.get(n).values()),
                    Rdf.str(r.get("src")),
                    Rdf.str(r.get("desc")),
                    Rdf.str(r.get("by")),
                    Rdf.instant(r.get("at"))));
        }
        return out;
    }

    public Optional<byte[]> content(String name) {
        ParameterizedSparqlString q =
                Rdf.pss("SELECT ?i ?data WHERE { GRAPH ?gExternalContent { ?c vm:chunkOf ?e ; vm:index ?i ;"
                        + " vm:data ?data } } ORDER BY ?i LIMIT 101");
        q.setIri("e", Rdf.externalIri(name));
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

    /**
     * Registers when name and keys are still free (one store transaction).
     *
     * @return true when this request's write was applied
     */
    public boolean insert(NewExternal ne, String token) {
        List<String> keys = new ArrayList<>();
        keys.add(ne.namespaceKey());
        keys.addAll(ne.aliasKeys());
        StringBuilder tpl = new StringBuilder();
        tpl.append("?e a vm:ExternalVocabulary ; vm:name ?name ; vm:nameKey ?nameKey ; vm:namespaceUri ?ns ;"
                + " vm:namespaceKey ?nsKey ; vm:createdBy ?by ; vm:creationDate ?at ; vm:createToken"
                + " ?token .");
        if (ne.sourceUri() != null) {
            tpl.append(" ?e vm:sourceUri ?src .");
        }
        if (ne.description() != null) {
            tpl.append(" ?e vm:description ?desc .");
        }
        for (int i = 0; i < ne.aliasUris().size(); i++) {
            tpl.append(" ?e vm:alias ?a")
                    .append(i)
                    .append(" . ?a")
                    .append(i)
                    .append(" vm:index ?ai")
                    .append(i)
                    .append(" ; vm:uri ?au")
                    .append(i)
                    .append(" ; vm:key ?ak")
                    .append(i)
                    .append(" .");
        }
        ParameterizedSparqlString op = Rdf.pss("INSERT { GRAPH ?gExternal { "
                + tpl
                + " } } WHERE { FILTER NOT EXISTS { GRAPH ?gExternal { ?o vm:nameKey ?nameKey } }"
                + " FILTER NOT EXISTS { VALUES (?k) { ?keys } GRAPH ?gExternal { { ?o2"
                + " vm:namespaceKey ?k } UNION { ?o3 vm:key ?k } } } }");
        String iri = Rdf.externalIri(ne.name());
        op.setIri("e", iri);
        op.setLiteral("name", ne.name());
        op.setLiteral("nameKey", ne.name().toLowerCase(Locale.ROOT));
        op.setLiteral("ns", ne.namespaceUri());
        op.setLiteral("nsKey", ne.namespaceKey());
        op.setLiteral("by", ne.createdBy());
        op.setParam("at", Rdf.dateTime(ne.at()));
        op.setLiteral("token", token);
        if (ne.sourceUri() != null) {
            op.setLiteral("src", ne.sourceUri());
        }
        if (ne.description() != null) {
            op.setLiteral("desc", ne.description());
        }
        for (int i = 0; i < ne.aliasUris().size(); i++) {
            op.setIri("a" + i, iri + ":alias:" + i);
            op.setParam("ai" + i, Rdf.integer(i));
            op.setLiteral("au" + i, ne.aliasUris().get(i));
            op.setLiteral("ak" + i, ne.aliasKeys().get(i));
        }
        op.setValues("keys", literals(keys));
        StringBuilder update = new StringBuilder(op.toString());
        List<String> chunks = Content.toChunks(ne.bytes());
        for (int i = 0; i < chunks.size(); i++) {
            ParameterizedSparqlString c =
                    Rdf.pss("INSERT { GRAPH ?gExternalContent { ?c vm:chunkOf ?e ; vm:index ?i ; vm:data ?data }"
                            + " } WHERE { GRAPH ?gExternal { ?e vm:createToken ?token } }");
            c.setIri("c", iri + ":chunk:" + i);
            c.setIri("e", iri);
            c.setParam("i", Rdf.integer(i));
            c.setLiteral("token", token);
            c.setParam("data", Rdf.base64(chunks.get(i)));
            update.append(" ;\n").append(c.toString());
        }
        store.update(update.toString());
        ParameterizedSparqlString check = Rdf.pss("ASK { GRAPH ?gExternal { ?e vm:createToken ?token } }");
        check.setIri("e", iri);
        check.setLiteral("token", token);
        return store.ask(check.toString());
    }

    /** Removes metadata, aliases and content (D41). One store transaction. */
    public void delete(String name) {
        String iri = Rdf.externalIri(name);
        ParameterizedSparqlString a = Rdf.pss("DELETE WHERE { GRAPH ?gExternal { ?e vm:alias ?al . ?al ?p ?o } }");
        a.setIri("e", iri);
        ParameterizedSparqlString m = Rdf.pss("DELETE WHERE { GRAPH ?gExternal { ?e ?p ?o } }");
        m.setIri("e", iri);
        ParameterizedSparqlString c = Rdf.pss("DELETE WHERE { GRAPH ?gExternalContent { ?c vm:chunkOf ?e ; ?p ?o } }");
        c.setIri("e", iri);
        store.update(a.toString() + " ;\n" + m.toString() + " ;\n" + c.toString());
    }

    private static List<RDFNode> literals(Collection<String> values) {
        List<RDFNode> out = new ArrayList<>();
        for (String s : values) {
            out.add(ResourceFactory.createStringLiteral(s));
        }
        return out;
    }
}
