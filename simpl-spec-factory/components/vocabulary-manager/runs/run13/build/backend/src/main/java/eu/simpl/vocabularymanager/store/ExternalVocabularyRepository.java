package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.SparqlSupport.instant;
import static eu.simpl.vocabularymanager.store.SparqlSupport.quad;
import static eu.simpl.vocabularymanager.store.SparqlSupport.rowText;
import static eu.simpl.vocabularymanager.store.StoreModel.*;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import eu.simpl.vocabularymanager.domain.UriKeys;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.UpdateRequest;

/** Store access for external vocabularies (D30, D41, D42, D56). */
public class ExternalVocabularyRepository {

    private static final String PATTERN = """
            GRAPH ?gExternal {
              ?ext a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?namespaceUri ;
                   vm:aliasList ?aliasList ; vm:createdBy ?createdBy ; vm:creationDate ?created .
              OPTIONAL { ?ext vm:sourceUri ?sourceUri }
              OPTIONAL { ?ext vm:description ?description }
            }
            """;

    /** Conflicts found for a registration, in the order of D147. */
    public record Conflicts(boolean nameTaken, Set<String> takenKeys) {}

    private final SparqlStoreClient store;

    public ExternalVocabularyRepository(SparqlStoreClient store) {
        this.store = store;
    }

    public Optional<ExternalVocabulary> find(String name) {
        ParameterizedSparqlString pss = SparqlSupport.query("SELECT * WHERE {\n" + PATTERN + "}");
        pss.setParam("ext", externalIri(name));
        Query query = pss.asQuery();
        query.setLimit(1);
        return store.select(query).stream().findFirst().map(ExternalVocabularyRepository::toExternal);
    }

    public Page<ExternalVocabulary> list(int page, int pageSize) {
        ParameterizedSparqlString pss =
                SparqlSupport.query("SELECT * WHERE {\n" + PATTERN + "} ORDER BY ASC(STR(?name))");
        Query query = pss.asQuery();
        query.setLimit(Math.min(pageSize, VocabularyRepository.MAX_PAGE_ROWS));
        query.setOffset((long) (page - 1) * pageSize);
        List<ExternalVocabulary> items = store.select(query).stream()
                .map(ExternalVocabularyRepository::toExternal)
                .toList();
        Query count = SparqlSupport.query(
                        "SELECT (COUNT(?ext) AS ?total) WHERE { GRAPH ?gExternal { ?ext a vm:ExternalVocabulary } }")
                .asQuery();
        count.setLimit(1);
        long total = store.select(count).stream()
                .findFirst()
                .map(row -> SparqlSupport.longValue(row, "total"))
                .orElse(0L);
        return new Page<>(items, total);
    }

    /**
     * External vocabularies whose namespace URI or an alias has one of these keys, with the keys each one
     * matched. Bounded by the 100 URIs of a search (D38).
     */
    public Map<String, ExternalMatch> findByKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        Query query = SparqlSupport.query("SELECT * WHERE {\n VALUES ?key { }\n"
                        + "GRAPH ?gExternal { { ?ext vm:namespaceKey ?key } UNION { ?ext vm:aliasKey ?key } }\n"
                        + PATTERN + "}")
                .asQuery();
        ValuesBinder.bind(query, "key", keys.stream().map(StoreModel::string).toList());
        query.setLimit(VocabularyRepository.MAX_LOOKUP_ROWS);
        Map<String, ExternalMatch> matches = new LinkedHashMap<>();
        for (QuerySolution row : store.select(query)) {
            ExternalVocabulary external = toExternal(row);
            matches.computeIfAbsent(external.name(), n -> new ExternalMatch(external, new HashSet<>()))
                    .keys()
                    .add(rowText(row, "key"));
        }
        return matches;
    }

    /** An external vocabulary and the lookup keys that matched it. */
    public record ExternalMatch(ExternalVocabulary vocabulary, Set<String> keys) {}

    public Optional<byte[]> content(String name) {
        ParameterizedSparqlString pss =
                SparqlSupport.query("SELECT ?content WHERE { GRAPH ?gContent { ?ext vm:content ?content } }");
        pss.setParam("ext", externalIri(name));
        Query query = pss.asQuery();
        query.setLimit(1);
        return store.select(query).stream()
                .findFirst()
                .map(row -> ContentCodec.decode(row.get("content").asNode()));
    }

    /** Which of the name and URI keys are already registered (D30, D56, D147). */
    public Conflicts conflicts(String name, Collection<String> keys) {
        Query query = SparqlSupport.query("SELECT ?hit ?key WHERE {\n"
                        + "{ GRAPH ?gExternal { ?e vm:nameKey ?nameKey } BIND(\"name\" AS ?hit) }\n"
                        + "UNION { VALUES ?key { }\n"
                        + "  GRAPH ?gExternal { { ?e2 vm:namespaceKey ?key } UNION { ?e2 vm:aliasKey ?key } }\n"
                        + "  BIND(\"key\" AS ?hit) } }")
                .asQuery();
        query = rebind(query, "nameKey", string(name.toLowerCase(Locale.ROOT)));
        ValuesBinder.bind(query, "key", keys.stream().map(StoreModel::string).toList());
        query.setLimit(1000);
        boolean nameTaken = false;
        Set<String> taken = new HashSet<>();
        for (QuerySolution row : store.select(query)) {
            if ("name".equals(rowText(row, "hit"))) {
                nameTaken = true;
            } else {
                taken.add(rowText(row, "key"));
            }
        }
        return new Conflicts(nameTaken, taken);
    }

    private static Query rebind(Query query, String variable, Node value) {
        return org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps.replaceVars(
                query, Map.of(Var.alloc(variable), value));
    }

    /** Registers unless the name or a key is taken in the meantime (D43, D82). */
    public boolean insert(ExternalVocabulary external, byte[] content, String token) {
        Node ext = externalIri(external.name());
        Node nameKey = string(external.name().toLowerCase(Locale.ROOT));
        List<Quad> inserts = new ArrayList<>(List.of(
                quad(GRAPH_EXTERNAL, ext, RDF_TYPE, EXTERNAL_VOCABULARY),
                quad(GRAPH_EXTERNAL, ext, NAME, string(external.name())),
                quad(GRAPH_EXTERNAL, ext, NAME_KEY, nameKey),
                quad(GRAPH_EXTERNAL, ext, NAMESPACE_URI, string(external.namespaceUri())),
                quad(GRAPH_EXTERNAL, ext, NAMESPACE_KEY, string(UriKeys.key(external.namespaceUri()))),
                quad(GRAPH_EXTERNAL, ext, ALIAS_LIST, string(String.join(" ", external.aliasUris()))),
                quad(GRAPH_EXTERNAL, ext, CREATED_BY, string(external.createdBy())),
                quad(GRAPH_EXTERNAL, ext, CREATION_DATE, dateTime(external.creationDate())),
                quad(GRAPH_EXTERNAL, ext, WRITE_TOKEN, string(token)),
                quad(GRAPH_CONTENT, ext, CONTENT, ContentCodec.encode(content))));
        Set<String> keys = new HashSet<>();
        keys.add(UriKeys.key(external.namespaceUri()));
        for (String alias : external.aliasUris()) {
            inserts.add(quad(GRAPH_EXTERNAL, ext, ALIAS_KEY, string(UriKeys.key(alias))));
            keys.add(UriKeys.key(alias));
        }
        if (external.sourceUri() != null) {
            inserts.add(quad(GRAPH_EXTERNAL, ext, SOURCE_URI, string(external.sourceUri())));
        }
        if (external.description() != null) {
            inserts.add(quad(GRAPH_EXTERNAL, ext, DESCRIPTION, string(external.description())));
        }
        Query condition = SparqlSupport.query("SELECT * WHERE {\n"
                        + "FILTER NOT EXISTS { GRAPH ?gExternal { ?o vm:nameKey ?nameKey } }\n"
                        + "FILTER NOT EXISTS { VALUES ?key { }\n"
                        + "  GRAPH ?gExternal { { ?o2 vm:namespaceKey ?key } UNION { ?o2 vm:aliasKey ?key } } } }")
                .asQuery();
        condition = rebind(condition, "nameKey", nameKey);
        ValuesBinder.bind(
                condition, "key", keys.stream().map(StoreModel::string).toList());
        UpdateRequest request = new UpdateRequest();
        request.add(SparqlSupport.modify(List.of(), inserts, condition.getQueryPattern()));
        store.update(request);

        ParameterizedSparqlString check =
                SparqlSupport.query("SELECT ?token WHERE { GRAPH ?gExternal { ?ext vm:writeToken ?token } }");
        check.setParam("ext", ext);
        Query checkQuery = check.asQuery();
        checkQuery.setLimit(2);
        return store.select(checkQuery).stream().anyMatch(row -> token.equals(rowText(row, "token")));
    }

    /** Removes metadata and content in one transaction (D41, D42). */
    public void delete(String name) {
        Node ext = externalIri(name);
        Var p = Var.alloc("p");
        Var o = Var.alloc("o");
        UpdateRequest request = new UpdateRequest();
        request.add(SparqlSupport.deleteWhere(List.of(quad(GRAPH_EXTERNAL, ext, p, o))));
        request.add(SparqlSupport.deleteWhere(List.of(quad(GRAPH_CONTENT, ext, p, o))));
        store.update(request);
    }

    private static ExternalVocabulary toExternal(QuerySolution row) {
        String aliasList = rowText(row, "aliasList");
        List<String> aliases =
                aliasList == null || aliasList.isEmpty() ? List.of() : Arrays.asList(aliasList.split(" "));
        return new ExternalVocabulary(
                rowText(row, "name"),
                rowText(row, "namespaceUri"),
                aliases,
                rowText(row, "sourceUri"),
                rowText(row, "description"),
                rowText(row, "createdBy"),
                instant(row, "created"));
    }
}
