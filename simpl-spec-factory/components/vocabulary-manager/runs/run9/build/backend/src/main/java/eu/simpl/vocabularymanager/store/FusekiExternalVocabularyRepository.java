package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreModel.EXTERNAL_GRAPH;
import static eu.simpl.vocabularymanager.store.StoreModel.PREFIXES;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.validation.UriKeys;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/** External vocabularies in Fuseki, with the same query rules as {@link FusekiVocabularyRepository}. */
@Repository
public class FusekiExternalVocabularyRepository implements ExternalVocabularyRepository {

    private static final Logger LOG = LoggerFactory.getLogger(FusekiExternalVocabularyRepository.class);
    private static final int MAX_ALIASES_PER_ITEM = 20;
    private static final int ROW_LIMIT_PER_ITEM = MAX_ALIASES_PER_ITEM + 1;

    private static final String METADATA = """
            ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?ns ; vm:createdBy ?createdBy ;
               vm:creationDate ?created .
            OPTIONAL { ?e vm:sourceUri ?source }
            OPTIONAL { ?e vm:description ?desc }
            OPTIONAL { ?e vm:alias ?a . ?a vm:uri ?aliasUri ; vm:index ?aliasIndex }
            """;

    private final SparqlStore store;

    /** @param store the SPARQL endpoints */
    public FusekiExternalVocabularyRepository(SparqlStore store) {
        this.store = store;
    }

    @Override
    public Optional<ExternalVocabulary> find(String name) {
        ParameterizedSparqlString query = query("SELECT * WHERE { GRAPH ?pGraph { " + METADATA
                + " FILTER(?name = ?pName) } } LIMIT " + ROW_LIMIT_PER_ITEM);
        query.setLiteral("pName", name);
        return collect(store.select(query.toString())).stream().findFirst();
    }

    @Override
    public boolean existsIgnoreCase(String name) {
        ParameterizedSparqlString query = query("ASK { GRAPH ?pGraph { ?e vm:nameKey ?pKey } }");
        query.setLiteral("pKey", name.toLowerCase(Locale.ROOT));
        return store.ask(query.toString());
    }

    @Override
    public Page<ExternalVocabulary> list(PageRequest page) {
        ParameterizedSparqlString count =
                query("SELECT (COUNT(?e) AS ?count) WHERE { GRAPH ?pGraph { ?e a vm:ExternalVocabulary } } LIMIT 1");
        long total = store.select(count.toString()).get(0).longValue("count");
        ParameterizedSparqlString select = query("SELECT * WHERE { { SELECT ?e ?name WHERE { GRAPH ?pGraph { "
                + "?e a vm:ExternalVocabulary ; vm:name ?name } } ORDER BY ASC(STR(?name)) LIMIT " + page.pageSize()
                + " OFFSET " + page.offset() + " } GRAPH ?pGraph { " + METADATA + " } } ORDER BY ASC(STR(?name)) "
                + "LIMIT " + ((long) page.pageSize() * ROW_LIMIT_PER_ITEM));
        return new Page<>(collect(store.select(select.toString())), total);
    }

    @Override
    public Set<String> existingKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Set.of();
        }
        ParameterizedSparqlString query = query("SELECT DISTINCT ?key WHERE { GRAPH ?pGraph { "
                + "{ ?e vm:namespaceKey ?key } UNION { ?e vm:alias ?a . ?a vm:key ?key } VALUES (?key) { ?pKeys } } }"
                + " LIMIT " + (keys.size() + 1));
        query.setValues("pKeys", literals(keys));
        Set<String> found = new LinkedHashSet<>();
        store.select(query.toString()).forEach(row -> found.add(row.string("key")));
        return found;
    }

    @Override
    public Map<String, ExternalVocabulary> findByKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        ParameterizedSparqlString query = query("SELECT * WHERE { { SELECT DISTINCT ?e ?key WHERE { GRAPH ?pGraph { "
                + "{ ?e vm:namespaceKey ?key } UNION { ?e vm:alias ?ka . ?ka vm:key ?key } VALUES (?key) { ?pKeys } } } }"
                + " GRAPH ?pGraph { " + METADATA + " } } LIMIT " + ((long) keys.size() * ROW_LIMIT_PER_ITEM));
        query.setValues("pKeys", literals(keys));
        List<Row> rows = store.select(query.toString());
        Map<String, ExternalVocabulary> byName = new LinkedHashMap<>();
        collect(rows).forEach(v -> byName.put(v.name(), v));
        Map<String, ExternalVocabulary> byKey = new LinkedHashMap<>();
        for (Row row : rows) {
            byKey.putIfAbsent(row.string("key"), byName.get(row.string("name")));
        }
        return byKey;
    }

    @Override
    public Optional<byte[]> content(String name) {
        ParameterizedSparqlString query = query("SELECT ?i ?data WHERE { GRAPH ?pGraph { ?pExt vm:contentGraph ?cg } "
                + "GRAPH ?cg { ?chunk vm:chunkOf ?pExt ; vm:index ?i ; vm:data ?data } } ORDER BY ?i LIMIT "
                + ContentChunks.MAX_CHUNKS);
        query.setIri("pExt", StoreModel.externalIri(name));
        List<Row> rows = store.select(query.toString());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                ContentChunks.join(rows.stream().map(r -> r.string("data")).toList()));
    }

    @Override
    public boolean insert(ExternalVocabulary vocabulary, byte[] content) {
        String token = UUID.randomUUID().toString();
        String name = vocabulary.name();
        List<String> keys = new ArrayList<>();
        keys.add(UriKeys.key(vocabulary.namespaceUri()));
        StringBuilder triples = new StringBuilder("?pExt a vm:ExternalVocabulary ; vm:name ?pExtName ; "
                + "vm:nameKey ?pNameKey ; vm:namespaceUri ?pNs ; vm:namespaceKey ?pNsKey ; vm:createdBy ?pCreator ; "
                + "vm:creationDate ?pNow ; vm:contentGraph ?pContentGraph ; vm:writeToken ?pToken .\n");
        if (vocabulary.sourceUri() != null) {
            triples.append("?pExt vm:sourceUri ?pSource .\n");
        }
        if (vocabulary.description() != null) {
            triples.append("?pExt vm:description ?pDesc .\n");
        }
        for (int i = 0; i < vocabulary.aliasUris().size(); i++) {
            String suffix = String.format(Locale.ROOT, "%02d", i);
            triples.append("?pExt vm:alias ?pAliasNode")
                    .append(suffix)
                    .append(" . ?pAliasNode")
                    .append(suffix)
                    .append(" vm:uri ?pAliasUri")
                    .append(suffix)
                    .append(" ; vm:key ?pAliasKey")
                    .append(suffix)
                    .append(" ; vm:index ?pAliasIndex")
                    .append(suffix)
                    .append(" .\n");
        }
        ParameterizedSparqlString update = query("INSERT { GRAPH ?pGraph { " + triples
                + " } } WHERE { "
                + "FILTER NOT EXISTS { GRAPH ?pGraph { ?x vm:nameKey ?pNameKey } } "
                + "FILTER NOT EXISTS { GRAPH ?pGraph { { ?y vm:namespaceKey ?k } UNION { ?y vm:alias ?ya . ?ya vm:key ?k }"
                + " VALUES (?k) { ?pKeys } } } }");
        update.setIri("pExt", StoreModel.externalIri(name));
        update.setIri("pContentGraph", StoreModel.externalContentGraph(name));
        update.setLiteral("pExtName", name);
        update.setLiteral("pNameKey", name.toLowerCase(Locale.ROOT));
        update.setLiteral("pNs", vocabulary.namespaceUri());
        update.setLiteral("pNsKey", UriKeys.key(vocabulary.namespaceUri()));
        update.setLiteral("pCreator", vocabulary.createdBy());
        update.setLiteral("pNow", vocabulary.creationDate().toString(), XSDDatatype.XSDdateTime);
        update.setLiteral("pToken", token);
        if (vocabulary.sourceUri() != null) {
            update.setLiteral("pSource", vocabulary.sourceUri());
        }
        if (vocabulary.description() != null) {
            update.setLiteral("pDesc", vocabulary.description());
        }
        for (int i = 0; i < vocabulary.aliasUris().size(); i++) {
            String suffix = String.format(Locale.ROOT, "%02d", i);
            String alias = vocabulary.aliasUris().get(i);
            keys.add(UriKeys.key(alias));
            update.setIri("pAliasNode" + suffix, StoreModel.aliasIri(name, i));
            update.setLiteral("pAliasUri" + suffix, alias);
            update.setLiteral("pAliasKey" + suffix, UriKeys.key(alias));
            update.setLiteral("pAliasIndex" + suffix, Integer.toString(i), XSDDatatype.XSDinteger);
        }
        update.setValues("pKeys", literals(keys));
        ParameterizedSparqlString condition = query("SELECT * WHERE { GRAPH ?pGraph { ?pExt vm:writeToken ?pToken } }");
        condition.setIri("pExt", StoreModel.externalIri(name));
        condition.setLiteral("pToken", token);
        store.update(update.toString() + " ;\n"
                + ContentChunks.insert(
                        StoreModel.externalContentGraph(name),
                        StoreModel.externalIri(name),
                        content,
                        condition.toString()));
        ParameterizedSparqlString ask = query("ASK { GRAPH ?pGraph { ?pExt vm:writeToken ?pToken } }");
        ask.setIri("pExt", StoreModel.externalIri(name));
        ask.setLiteral("pToken", token);
        boolean applied = store.ask(ask.toString());
        if (applied) {
            ParameterizedSparqlString cleanup = query("DELETE DATA { GRAPH ?pGraph { ?pExt vm:writeToken ?pToken } }");
            cleanup.setIri("pExt", StoreModel.externalIri(name));
            cleanup.setLiteral("pToken", token);
            try {
                store.update(cleanup.toString());
            } catch (StoreUnavailableException | StoreFailureException ex) {
                LOG.warn("Write token cleanup failed; a stale token stays, which is harmless: {}", ex.getMessage());
            }
        }
        return applied;
    }

    @Override
    public void delete(String name) {
        ParameterizedSparqlString update = query(
                "DELETE { GRAPH ?pGraph { ?a ?ap ?ao } } WHERE { GRAPH ?pGraph { ?pExt vm:alias ?a . ?a ?ap ?ao } } ;\n"
                        + "DELETE WHERE { GRAPH ?pGraph { ?pExt ?p ?o } } ;\n" + "DROP SILENT GRAPH ?pContentGraph");
        update.setIri("pExt", StoreModel.externalIri(name));
        update.setIri("pContentGraph", StoreModel.externalContentGraph(name));
        store.update(update.toString());
    }

    private static ParameterizedSparqlString query(String body) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(PREFIXES + body);
        query.setIri("pGraph", EXTERNAL_GRAPH);
        return query;
    }

    private static List<RDFNode> literals(Collection<String> values) {
        return new LinkedHashSet<>(values)
                .stream()
                        .map(v -> (RDFNode) ResourceFactory.createPlainLiteral(v))
                        .toList();
    }

    /** Folds rows (one per alias) into external vocabularies, keeping the row order of names. */
    private static List<ExternalVocabulary> collect(List<Row> rows) {
        Map<String, Row> first = new LinkedHashMap<>();
        Map<String, TreeMap<Integer, String>> aliases = new LinkedHashMap<>();
        for (Row row : rows) {
            String name = row.string("name");
            first.putIfAbsent(name, row);
            TreeMap<Integer, String> perName = aliases.computeIfAbsent(name, n -> new TreeMap<>());
            if (row.has("aliasUri")) {
                perName.put(row.integer("aliasIndex"), row.string("aliasUri"));
            }
        }
        List<ExternalVocabulary> result = new ArrayList<>();
        first.forEach((name, row) -> result.add(new ExternalVocabulary(
                name,
                row.string("ns"),
                List.copyOf(aliases.get(name).values()),
                row.string("source"),
                row.string("desc"),
                row.string("createdBy"),
                row.instant("created"))));
        return result;
    }
}
