package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.out.NodeFmtLib;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Store access for external vocabularies (D30, D41, D56). The namespace URI and the alias URIs are stored as sent
 * (D73); their normalised forms are stored as lookup keys.
 */
@Repository
public class ExternalVocabularyRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int SEARCH_LIMIT = 101;

    private static final String PATTERN = """
            GRAPH <%s> {
              ?ext a vm:ExternalVocabulary ; vm:externalName ?name ; vm:namespaceUri ?namespace ;
                   vm:aliasUris ?aliases ; vm:createdBy ?createdBy ; vm:creationDate ?created .
              OPTIONAL { ?ext vm:sourceUri ?source }
              OPTIONAL { ?ext vm:description ?description }
            }
            """.formatted(Sparql.META_GRAPH);

    private final StoreClient store;

    public ExternalVocabularyRepository(StoreClient store) {
        this.store = store;
    }

    /** Finds an external vocabulary by its exact name. */
    public Optional<ExternalVocabulary> find(String name) {
        SparqlTemplate pss =
                Sparql.command("SELECT ?name ?namespace ?aliases ?createdBy ?created ?source ?description WHERE {\n"
                        + PATTERN + "} LIMIT 1");
        pss.setIri("ext", Sparql.externalIri(name));
        ResultSetRewindable rows = store.select(pss.toString());
        return rows.hasNext() ? Optional.of(toExternal(rows.next())) : Optional.empty();
    }

    /** True when an external vocabulary has this name, compared case-insensitively (D56). */
    public boolean nameTaken(String name) {
        SparqlTemplate pss =
                Sparql.command("ASK { GRAPH <" + Sparql.META_GRAPH + "> { ?ext vm:externalNameKey ?key } }");
        pss.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return store.ask(pss.toString());
    }

    /** The lookup keys among the given ones that are already registered. */
    public List<String> registeredKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        SparqlTemplate pss = Sparql.command("SELECT ?key WHERE { VALUES ?key { "
                + literals(keys) + " } GRAPH <" + Sparql.META_GRAPH + "> { ?ext vm:lookupKey ?key } } LIMIT "
                + SEARCH_LIMIT);
        List<String> found = new ArrayList<>();
        ResultSetRewindable rows = store.select(pss.toString());
        while (rows.hasNext()) {
            found.add(Sparql.string(rows.next(), "key"));
        }
        return found;
    }

    /** Counts the external vocabularies. */
    public long count() {
        SparqlTemplate pss = Sparql.command("SELECT (COUNT(?ext) AS ?total) WHERE { GRAPH <" + Sparql.META_GRAPH
                + "> { ?ext a vm:ExternalVocabulary } } LIMIT 1");
        ResultSetRewindable rows = store.select(pss.toString());
        return rows.hasNext() ? Sparql.number(rows.next(), "total") : 0;
    }

    /** One page in name order (D41). */
    public List<ExternalVocabulary> list(long offset, int limit) {
        SparqlTemplate pss =
                Sparql.command("SELECT ?name ?namespace ?aliases ?createdBy ?created ?source ?description WHERE {\n"
                        + PATTERN + "} ORDER BY ASC(?name) LIMIT " + limit + " OFFSET " + offset);
        List<ExternalVocabulary> result = new ArrayList<>();
        ResultSetRewindable rows = store.select(pss.toString());
        while (rows.hasNext()) {
            result.add(toExternal(rows.next()));
        }
        return result;
    }

    /**
     * The external vocabularies matching normalised lookup keys, with the key that matched.
     *
     * @param keys normalised URIs, at most 100 (D38)
     * @return key to external vocabulary
     */
    public Map<String, ExternalVocabulary> findByKeys(Collection<String> keys) {
        Map<String, ExternalVocabulary> result = new LinkedHashMap<>();
        if (keys.isEmpty()) {
            return result;
        }
        SparqlTemplate pss = Sparql.command(
                "SELECT ?key ?name ?namespace ?aliases ?createdBy ?created ?source ?description WHERE {\n VALUES ?key { "
                        + literals(keys) + " }\n GRAPH <" + Sparql.META_GRAPH + "> { ?ext vm:lookupKey ?key }\n"
                        + PATTERN + "} LIMIT " + SEARCH_LIMIT);
        ResultSetRewindable rows = store.select(pss.toString());
        while (rows.hasNext()) {
            QuerySolution row = rows.next();
            result.put(Sparql.string(row, "key"), toExternal(row));
        }
        return result;
    }

    /** Reads the stored content bytes (D41). */
    public Optional<byte[]> content(String name) {
        SparqlTemplate pss =
                Sparql.command("SELECT ?content WHERE { GRAPH ?graph { ?ext vm:rdfContent ?content } } LIMIT 1");
        pss.setIri("graph", Sparql.externalContentGraph(name));
        pss.setIri("ext", Sparql.externalIri(name));
        ResultSetRewindable rows = store.select(pss.toString());
        if (!rows.hasNext()) {
            return Optional.empty();
        }
        return Optional.of(Base64.getDecoder().decode(Sparql.string(rows.next(), "content")));
    }

    /**
     * Registers an external vocabulary when no name or lookup key is taken (D30, D56, D82).
     *
     * @param vocabulary the metadata
     * @param keys the normalised lookup keys of the namespace and the aliases
     * @param content the content bytes
     * @return true when this request stored it
     */
    public boolean insert(ExternalVocabulary vocabulary, Collection<String> keys, byte[] content) {
        String receipt = Sparql.receiptIri(UUID.randomUUID().toString());
        StringBuilder optional = new StringBuilder();
        if (vocabulary.sourceUri() != null) {
            optional.append(" ?ext vm:sourceUri ?source .");
        }
        if (vocabulary.description() != null) {
            optional.append(" ?ext vm:description ?description .");
        }
        SparqlTemplate pss = Sparql.command("""
                INSERT { GRAPH <%1$s> { ?receipt vm:at ?now } }
                WHERE {
                  FILTER NOT EXISTS { GRAPH <%2$s> { ?other vm:externalNameKey ?nameKey } }
                  FILTER NOT EXISTS { VALUES ?anyKey { ?keyList } GRAPH <%2$s> { ?taken vm:lookupKey ?anyKey } }
                } ;
                INSERT {
                  GRAPH ?contentGraph { ?ext vm:rdfContent ?content }
                  GRAPH <%2$s> {
                    ?ext a vm:ExternalVocabulary ; vm:externalName ?name ; vm:externalNameKey ?nameKey ;
                         vm:namespaceUri ?namespace ; vm:aliasUris ?aliases ; vm:createdBy ?createdBy ;
                         vm:creationDate ?now .%3$s
                  }
                }
                WHERE { GRAPH <%1$s> { ?receipt vm:at ?anyTime } } ;
                INSERT { GRAPH <%2$s> { ?ext vm:lookupKey ?newKey } }
                WHERE { GRAPH <%1$s> { ?receipt vm:at ?anyTime } VALUES ?newKey { ?newKeyList } } ;
                """.formatted(Sparql.RECEIPT_GRAPH, Sparql.META_GRAPH, optional)
                + VocabularyRepository.cleanupReceipts());
        List<RDFNode> keyNodes =
                keys.stream().map(key -> (RDFNode) Sparql.text(key)).toList();
        pss.setValues("keyList", keyNodes);
        pss.setValues("newKeyList", keyNodes);
        pss.setIri("receipt", receipt);
        pss.setIri("ext", Sparql.externalIri(vocabulary.name()));
        pss.setIri("contentGraph", Sparql.externalContentGraph(vocabulary.name()));
        pss.setLiteral("name", vocabulary.name());
        pss.setLiteral("nameKey", vocabulary.name().toLowerCase(Locale.ROOT));
        pss.setLiteral("namespace", vocabulary.namespaceUri());
        pss.setLiteral("aliases", JSON.writeValueAsString(vocabulary.aliasUris()));
        pss.setLiteral("createdBy", vocabulary.createdBy());
        if (vocabulary.sourceUri() != null) {
            pss.setLiteral("source", vocabulary.sourceUri());
        }
        if (vocabulary.description() != null) {
            pss.setLiteral("description", vocabulary.description());
        }
        pss.setLiteral("now", Sparql.dateTime(vocabulary.creationDate()));
        pss.setLiteral("cutoff", Sparql.dateTime(vocabulary.creationDate().minusSeconds(3600)));
        pss.setLiteral("content", Base64.getEncoder().encodeToString(content));
        store.update(pss.toString());
        SparqlTemplate ask = Sparql.command("ASK { GRAPH <" + Sparql.RECEIPT_GRAPH + "> { ?receipt vm:at ?anyTime } }");
        ask.setIri("receipt", receipt);
        return store.ask(ask.toString());
    }

    /** Removes metadata and content in one transaction (D41, D42). */
    public void delete(String name) {
        SparqlTemplate pss = Sparql.command(
                "DELETE WHERE { GRAPH <" + Sparql.META_GRAPH + "> { ?ext ?p ?o } } ;\nDROP SILENT GRAPH ?contentGraph");
        pss.setIri("ext", Sparql.externalIri(name));
        pss.setIri("contentGraph", Sparql.externalContentGraph(name));
        store.update(pss.toString());
    }

    private static String literals(Collection<String> values) {
        StringBuilder text = new StringBuilder();
        for (String value : values) {
            text.append(NodeFmtLib.strNT(NodeFactory.createLiteralString(value)))
                    .append(' ');
        }
        return text.toString();
    }

    private static ExternalVocabulary toExternal(QuerySolution row) {
        List<String> aliases = JSON.readValue(Sparql.string(row, "aliases"), new TypeReference<List<String>>() {});
        return new ExternalVocabulary(
                Sparql.string(row, "name"),
                Sparql.string(row, "namespace"),
                List.copyOf(aliases),
                Sparql.string(row, "source"),
                Sparql.string(row, "description"),
                Sparql.string(row, "createdBy"),
                Instant.parse(Sparql.string(row, "created")));
    }
}
