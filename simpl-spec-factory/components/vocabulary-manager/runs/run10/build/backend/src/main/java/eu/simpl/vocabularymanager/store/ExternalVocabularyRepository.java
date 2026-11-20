package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.UpdateRequest;

/** Store access for external vocabularies (D30, D41, D56). Aliases are kept in the order sent (D73). */
public class ExternalVocabularyRepository {

    static final String ALIAS_SEPARATOR = "\n";
    private static final String FIELDS = "?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?ns ;"
            + " vm:aliasList ?aliases ; vm:createdBy ?createdBy ; vm:creationDate ?created ."
            + " OPTIONAL { ?e vm:sourceUri ?src } OPTIONAL { ?e vm:description ?desc }\n";

    private final SparqlStore store;

    public ExternalVocabularyRepository(SparqlStore store) {
        this.store = store;
    }

    public long count() {
        ParameterizedSparqlString query =
                pss("SELECT (COUNT(?e) AS ?count) WHERE { GRAPH ?g { ?e a vm:ExternalVocabulary } }");
        return Rdf.longValue(store.select(query.toString()).getFirst(), "count");
    }

    public List<ExternalVocabulary> list(long offset, int limit) {
        ParameterizedSparqlString query = pss("SELECT * WHERE { GRAPH ?g { " + FIELDS
                + " } } ORDER BY ASC(STR(?name)) LIMIT " + limit + " OFFSET " + offset);
        return store.select(query.toString()).stream()
                .map(ExternalVocabularyRepository::toExternal)
                .toList();
    }

    public Optional<ExternalVocabulary> find(String name) {
        ParameterizedSparqlString query = pss("SELECT * WHERE { GRAPH ?g { " + FIELDS + " } } LIMIT 1");
        query.setIri("e", Rdf.externalIri(name));
        return store.select(query.toString()).stream().findFirst().map(ExternalVocabularyRepository::toExternal);
    }

    /** External vocabularies by name, in no particular order (search results, at most 100 names). */
    public List<ExternalVocabulary> findAll(Collection<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        ParameterizedSparqlString query =
                pss("SELECT * WHERE { VALUES (?name) { ?names } GRAPH ?g { " + FIELDS + " } } LIMIT 101");
        query.setValues(
                "names", names.stream().map(Rdf::text).map(RDFNode.class::cast).toList());
        return store.select(query.toString()).stream()
                .map(ExternalVocabularyRepository::toExternal)
                .toList();
    }

    public boolean existsIgnoreCase(String name) {
        ParameterizedSparqlString query = pss("SELECT ?e WHERE { GRAPH ?g { ?e vm:nameKey ?key } } LIMIT 1");
        query.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return !store.select(query.toString()).isEmpty();
    }

    /** Maps each given lookup key to the name of the external vocabulary that has it as namespace or alias key. */
    public Map<String, String> namesByKey(Collection<String> keys) {
        Map<String, String> result = new LinkedHashMap<>();
        if (keys.isEmpty()) {
            return result;
        }
        ParameterizedSparqlString query = pss("SELECT DISTINCT ?key ?name WHERE { VALUES (?key) { ?keys }"
                + " GRAPH ?g { ?e vm:name ?name . { ?e vm:namespaceKey ?key } UNION"
                + " { ?e vm:alias ?a . ?a vm:key ?key } } } LIMIT 101");
        query.setValues(
                "keys", keys.stream().map(Rdf::text).map(RDFNode.class::cast).toList());
        for (QuerySolution row : store.select(query.toString())) {
            result.putIfAbsent(Rdf.string(row, "key"), Rdf.string(row, "name"));
        }
        return result;
    }

    public Optional<byte[]> content(String name) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(ContentChunks.SELECT);
        query.setIri("c", Rdf.externalContentGraph(name));
        List<QuerySolution> rows = store.select(query.toString());
        return rows.isEmpty() ? Optional.empty() : Optional.of(ContentChunks.join(rows));
    }

    /** Data of an external vocabulary to register. */
    public record Registration(
            String name,
            String namespaceUri,
            List<String> aliasUris,
            String sourceUri,
            String description,
            String createdBy,
            Instant now,
            byte[] content) {}

    /**
     * Registers the external vocabulary, only when neither its name (ignoring case) nor any of its lookup keys is
     * registered yet.
     *
     * @return true when this request registered it
     */
    public boolean insert(Registration data, String token) {
        StringBuilder template = new StringBuilder("INSERT { GRAPH ?g { ?e a vm:ExternalVocabulary ; vm:name ?name ;"
                + " vm:nameKey ?nameKey ; vm:namespaceUri ?ns ; vm:namespaceKey ?nsKey ; vm:aliasList ?aliases ;"
                + " vm:createdBy ?createdBy ; vm:creationDate ?now ; vm:contentGraph ?c ; vm:changeToken ?token");
        if (data.sourceUri() != null) {
            template.append(" ; vm:sourceUri ?src");
        }
        if (data.description() != null) {
            template.append(" ; vm:description ?desc");
        }
        template.append(" .\n");
        for (int i = 0; i < data.aliasUris().size(); i++) {
            String n = String.format(Locale.ROOT, "%02d", i);
            template.append(" ?e vm:alias ?alias")
                    .append(n)
                    .append(" . ?alias")
                    .append(n)
                    .append(" a vm:AliasUri ; vm:index ?aliasIndex")
                    .append(n)
                    .append(" ; vm:uri ?aliasUri")
                    .append(n)
                    .append(" ; vm:key ?aliasKey")
                    .append(n)
                    .append(" .\n");
        }
        template.append("} }\nWHERE { FILTER NOT EXISTS { GRAPH ?g { ?x vm:nameKey ?nameKey } }"
                + " FILTER NOT EXISTS { GRAPH ?g { VALUES (?k) { ?keys } { ?y vm:namespaceKey ?k } UNION"
                + " { ?z vm:key ?k } } } }");
        ParameterizedSparqlString insert = pss(template.toString());
        insert.setIri("e", Rdf.externalIri(data.name()));
        insert.setIri("c", Rdf.externalContentGraph(data.name()));
        insert.setLiteral("name", data.name());
        insert.setLiteral("nameKey", data.name().toLowerCase(Locale.ROOT));
        insert.setLiteral("ns", data.namespaceUri());
        insert.setLiteral("nsKey", ExternalVocabulary.key(data.namespaceUri()));
        insert.setLiteral("aliases", String.join(ALIAS_SEPARATOR, data.aliasUris()));
        insert.setLiteral("createdBy", data.createdBy());
        insert.setLiteral("now", Rdf.dateTime(data.now()));
        insert.setLiteral("token", token);
        if (data.sourceUri() != null) {
            insert.setLiteral("src", data.sourceUri());
        }
        if (data.description() != null) {
            insert.setLiteral("desc", data.description());
        }
        List<RDFNode> keys = new ArrayList<>();
        keys.add(Rdf.text(ExternalVocabulary.key(data.namespaceUri())));
        for (int i = 0; i < data.aliasUris().size(); i++) {
            String alias = data.aliasUris().get(i);
            String n = String.format(Locale.ROOT, "%02d", i);
            insert.setIri("alias" + n, Rdf.externalAliasIri(data.name(), i));
            insert.setLiteral("aliasIndex" + n, Rdf.integer(i));
            insert.setLiteral("aliasUri" + n, alias);
            insert.setLiteral("aliasKey" + n, ExternalVocabulary.key(alias));
            keys.add(Rdf.text(ExternalVocabulary.key(alias)));
        }
        insert.setValues("keys", keys);
        UpdateRequest request = insert.asUpdate();
        UpdateModify content = new UpdateModify();
        content.setHasInsertClause(true);
        content.setHasDeleteClause(false);
        ContentChunks.addQuads(content.getInsertAcc(), Rdf.externalContentGraph(data.name()), data.content());
        ElementPathBlock condition = new ElementPathBlock();
        condition.addTriple(Triple.create(
                NodeFactory.createURI(Rdf.externalIri(data.name())),
                NodeFactory.createURI(Rdf.NS + "changeToken"),
                NodeFactory.createLiteralString(token)));
        content.setElement(new ElementNamedGraph(NodeFactory.createURI(Rdf.EXTERNAL_GRAPH), condition));
        request.add(content);
        store.update(request.toString());
        ParameterizedSparqlString check = pss("SELECT ?token WHERE { GRAPH ?g { ?e vm:changeToken ?token } } LIMIT 1");
        check.setIri("e", Rdf.externalIri(data.name()));
        return store.select(check.toString()).stream().anyMatch(row -> token.equals(Rdf.string(row, "token")));
    }

    /** Removes metadata and content in one store transaction (D41). */
    public void delete(String name) {
        ParameterizedSparqlString delete = pss("DELETE WHERE { GRAPH ?g { ?e vm:alias ?a . ?a ?ap ?ao } } ;\n"
                + "DELETE WHERE { GRAPH ?g { ?e ?p ?o } } ;\nDROP SILENT GRAPH ?c");
        delete.setIri("e", Rdf.externalIri(name));
        delete.setIri("c", Rdf.externalContentGraph(name));
        store.update(delete.toString());
    }

    static ParameterizedSparqlString pss(String body) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(Rdf.PREFIXES + body);
        pss.setIri("g", Rdf.EXTERNAL_GRAPH);
        return pss;
    }

    private static ExternalVocabulary toExternal(QuerySolution row) {
        String aliases = Rdf.string(row, "aliases");
        List<String> aliasUris =
                aliases == null || aliases.isEmpty() ? List.of() : Arrays.asList(aliases.split(ALIAS_SEPARATOR));
        return new ExternalVocabulary(
                Rdf.string(row, "name"),
                Rdf.string(row, "ns"),
                aliasUris,
                Rdf.string(row, "src"),
                Rdf.string(row, "desc"),
                Rdf.string(row, "createdBy"),
                Rdf.instant(row, "created"));
    }
}
