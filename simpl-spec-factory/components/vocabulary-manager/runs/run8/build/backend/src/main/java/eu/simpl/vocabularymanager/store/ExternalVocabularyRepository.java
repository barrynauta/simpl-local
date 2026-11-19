package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreModel.CHANGE_GRAPH;
import static eu.simpl.vocabularymanager.store.StoreModel.EXTERNAL_GRAPH;
import static eu.simpl.vocabularymanager.store.StoreModel.PREFIXES;
import static eu.simpl.vocabularymanager.store.StoreModel.contentGraphIri;
import static eu.simpl.vocabularymanager.store.StoreModel.dateTime;
import static eu.simpl.vocabularymanager.store.StoreModel.externalIri;
import static eu.simpl.vocabularymanager.store.StoreModel.instant;
import static eu.simpl.vocabularymanager.store.StoreModel.number;
import static eu.simpl.vocabularymanager.store.StoreModel.str;
import static eu.simpl.vocabularymanager.store.StoreModel.strings;
import static eu.simpl.vocabularymanager.store.StoreModel.text;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.FieldRules;
import eu.simpl.vocabularymanager.domain.Page;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.jena.query.QuerySolution;
import org.springframework.stereotype.Repository;

/** Store access for external vocabularies (D30, D41, D56). Bounded queries (D85), conditional writes (D82). */
@Repository
public class ExternalVocabularyRepository {

    private static final String FIELDS = """
            ?ext a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?namespaceUri ; vm:aliasUris ?aliasUris ;
                 vm:createdBy ?createdBy ; vm:creationDate ?creationDate ; vm:contentGraph ?g .
            OPTIONAL { ?ext vm:sourceUri ?sourceUri }
            OPTIONAL { ?ext vm:description ?description }
            """;

    private final SparqlStore store;

    /**
     * @param store the SPARQL store
     */
    public ExternalVocabularyRepository(SparqlStore store) {
        this.store = store;
    }

    /**
     * @param offset rows to skip
     * @param limit rows to return, at most 100
     * @return the page, ordered by name ascending (D41)
     */
    public Page<ExternalVocabulary> list(long offset, int limit) {
        long total = store
                .select(PREFIXES + "SELECT (COUNT(*) AS ?total) WHERE { GRAPH " + EXTERNAL_GRAPH
                        + " { ?ext a vm:ExternalVocabulary } } LIMIT 1")
                .stream()
                .findFirst()
                .map(solution -> number(solution, "total"))
                .orElse(0L);
        if (total == 0 || offset >= total) {
            return new Page<>(List.of(), total);
        }
        String query = PREFIXES + "SELECT * WHERE { GRAPH " + EXTERNAL_GRAPH + " {\n" + FIELDS
                + "} } ORDER BY ASC(?name) LIMIT " + limit + " OFFSET " + offset;
        return new Page<>(store.select(query).stream().map(ExternalVocabularyRepository::toExternal).toList(), total);
    }

    /**
     * @param name exact name
     * @return the external vocabulary
     */
    public Optional<ExternalVocabulary> find(String name) {
        String query = PREFIXES + "SELECT * WHERE { GRAPH " + EXTERNAL_GRAPH + " {\n  VALUES ?name { " + str(name)
                + " }\n" + FIELDS + "} } LIMIT 1";
        return store.select(query).stream().findFirst().map(ExternalVocabularyRepository::toExternal);
    }

    /**
     * @param keys normalised URIs (D30)
     * @return for each key that matches, the external vocabulary it matches
     */
    public Map<String, ExternalVocabulary> findByKeys(Set<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        String query = PREFIXES + "SELECT * WHERE { GRAPH " + EXTERNAL_GRAPH + " {\n  VALUES ?key { " + strings(keys)
                + " }\n  ?ext vm:lookupKey ?key .\n" + FIELDS + "} } LIMIT " + (keys.size() + 1);
        Map<String, ExternalVocabulary> result = new LinkedHashMap<>();
        for (QuerySolution solution : store.select(query)) {
            result.put(text(solution, "key"), toExternal(solution));
        }
        return result;
    }

    /**
     * @param name exact name
     * @return the stored content bytes
     */
    public Optional<byte[]> content(String name) {
        String query = PREFIXES + "SELECT ?content WHERE {\n  GRAPH " + EXTERNAL_GRAPH + " { ?ext vm:name "
                + str(name) + " ; vm:contentGraph ?g }\n  GRAPH ?g { ?ext vm:content ?content }\n} LIMIT 1";
        return store.select(query).stream()
                .findFirst()
                .map(solution -> Base64.getDecoder().decode(text(solution, "content")));
    }

    /**
     * @param nameKey case-insensitive name key
     * @param keys normalised namespace and alias keys
     * @return the conflicts: "name" and/or the keys already registered
     */
    public Set<String> conflicts(String nameKey, Set<String> keys) {
        String query = PREFIXES + "SELECT ?kind ?key WHERE {\n"
                + "  { GRAPH " + EXTERNAL_GRAPH + " { ?e vm:nameKey " + str(nameKey) + " } BIND(\"name\" AS ?kind) }\n"
                + "  UNION { VALUES ?key { " + strings(keys) + " } GRAPH " + EXTERNAL_GRAPH
                + " { ?e vm:lookupKey ?key } BIND(\"key\" AS ?kind) }\n} LIMIT " + (keys.size() + 2);
        Set<String> conflicts = new LinkedHashSet<>();
        for (QuerySolution solution : store.select(query)) {
            conflicts.add("name".equals(text(solution, "kind")) ? "name" : text(solution, "key"));
        }
        return conflicts;
    }

    /**
     * Registers an external vocabulary, only when its name, namespace and aliases are still free (D30, D82).
     *
     * @param write the values
     * @return true when applied
     */
    public boolean insert(ExternalWrite write) {
        ExternalVocabulary vocabulary = write.vocabulary();
        String ext = externalIri(vocabulary.name());
        String graph = contentGraphIri(write.changeToken());
        StringBuilder update = new StringBuilder(PREFIXES)
                .append("INSERT {\n  GRAPH ")
                .append(EXTERNAL_GRAPH)
                .append(" {\n    ")
                .append(ext)
                .append(" a vm:ExternalVocabulary ; vm:name ")
                .append(str(vocabulary.name()))
                .append(" ; vm:nameKey ")
                .append(str(FieldRules.nameKey(vocabulary.name())))
                .append(" ;\n      vm:namespaceUri ")
                .append(str(vocabulary.namespaceUri()))
                .append(" ; vm:aliasUris ")
                .append(str(String.join(" ", vocabulary.aliasUris())))
                .append(" ;\n      vm:createdBy ")
                .append(str(vocabulary.createdBy()))
                .append(" ; vm:creationDate ")
                .append(dateTime(vocabulary.creationDate()))
                .append(" ; vm:contentGraph ")
                .append(graph)
                .append(" .\n");
        for (String key : write.keys()) {
            update.append("    ").append(ext).append(" vm:lookupKey ").append(str(key)).append(" .\n");
        }
        if (vocabulary.sourceUri() != null) {
            update.append("    ").append(ext).append(" vm:sourceUri ").append(str(vocabulary.sourceUri())).append(" .\n");
        }
        if (vocabulary.description() != null) {
            update.append("    ")
                    .append(ext)
                    .append(" vm:description ")
                    .append(str(vocabulary.description()))
                    .append(" .\n");
        }
        update.append("  }\n  GRAPH ")
                .append(graph)
                .append(" { ")
                .append(ext)
                .append(" vm:content \"")
                .append(Base64.getEncoder().encodeToString(write.content()))
                .append("\"^^xsd:base64Binary }\n")
                .append(VocabularyRepository.changeTriples(
                        new VocabularyRepository.ChangeRecord(
                                write.changeToken(), write.actor(), vocabulary.creationDate()),
                        "createExternalVocabulary",
                        ext))
                .append("}\nWHERE {\n  FILTER NOT EXISTS { GRAPH ")
                .append(EXTERNAL_GRAPH)
                .append(" { ?e vm:nameKey ")
                .append(str(FieldRules.nameKey(vocabulary.name())))
                .append(" } }\n  FILTER NOT EXISTS { VALUES ?key { ")
                .append(strings(write.keys()))
                .append(" } GRAPH ")
                .append(EXTERNAL_GRAPH)
                .append(" { ?e2 vm:lookupKey ?key } }\n}");
        store.update(update.toString());
        return VocabularyRepository.confirmed(store, write.changeToken());
    }

    /**
     * Hard deletes the metadata and the content graph (D41). Stored internal versions are not touched (D42).
     *
     * @param name exact name
     * @param change the change record values
     * @return true when something was deleted
     */
    public boolean delete(String name, VocabularyRepository.ChangeRecord change) {
        String ext = externalIri(name);
        String update = PREFIXES + "DELETE { GRAPH " + EXTERNAL_GRAPH + " { " + ext + " ?p ?o } GRAPH ?g { ?s ?cp ?co } }\n"
                + "INSERT {\n" + VocabularyRepository.changeTriples(change, "deleteExternalVocabulary", ext) + "}\n"
                + "WHERE { GRAPH " + EXTERNAL_GRAPH + " { " + ext + " vm:contentGraph ?g ; ?p ?o }\n"
                + "  OPTIONAL { GRAPH ?g { ?s ?cp ?co } } }";
        store.update(update);
        return VocabularyRepository.confirmed(store, change.token());
    }

    private static ExternalVocabulary toExternal(QuerySolution solution) {
        String aliases = text(solution, "aliasUris");
        List<String> aliasUris = aliases == null || aliases.isEmpty()
                ? List.of()
                : new ArrayList<>(Arrays.asList(aliases.split(" ")));
        return new ExternalVocabulary(
                text(solution, "name"),
                text(solution, "namespaceUri"),
                List.copyOf(aliasUris),
                text(solution, "sourceUri"),
                text(solution, "description"),
                text(solution, "createdBy"),
                instant(solution, "creationDate"));
    }

    /**
     * Values of a registration.
     *
     * @param vocabulary the metadata
     * @param keys normalised namespace and alias keys
     * @param content the uploaded bytes
     * @param actor token subject (D80)
     * @param changeToken unique id of this write
     */
    public record ExternalWrite(
            ExternalVocabulary vocabulary, Set<String> keys, byte[] content, String actor, String changeToken) {}
}
