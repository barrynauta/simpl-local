package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreSchema.instant;
import static eu.simpl.vocabularymanager.store.StoreSchema.integer;
import static eu.simpl.vocabularymanager.store.StoreSchema.sparql;
import static eu.simpl.vocabularymanager.store.StoreSchema.text;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.sparql.engine.binding.Binding;
import org.springframework.stereotype.Repository;

/**
 * Data access for external vocabularies (D30, D41, D56, D73). The namespace URI and the alias URIs
 * are stored as sent; their normalised forms are stored as lookup keys.
 */
@Repository
public class ExternalVocabularyRepository {

    private static final String SELECT_FIELDS = "SELECT ?name ?ns ?source ?desc ?createdBy ?created ?aliasIndex ?alias";
    private static final String PATTERN = """
            ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?ns ; vm:createdBy ?createdBy ;
               vm:creationDate ?created .
            OPTIONAL { ?e vm:sourceUri ?source }
            OPTIONAL { ?e vm:description ?desc }
            OPTIONAL { ?e vm:alias ?a . ?a vm:index ?aliasIndex ; vm:uri ?alias }
            """;

    private final SparqlClient client;

    public ExternalVocabularyRepository(SparqlClient client) {
        this.client = client;
    }

    /** Removes one trailing '#' or '/' (D30). */
    public static String normalise(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    /**
     * The external vocabulary with this exact name.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Optional<ExternalVocabulary> find(String name) {
        ParameterizedSparqlString pss = sparql(
                SELECT_FIELDS + " WHERE { GRAPH ?gExternal { " + PATTERN + " } }" + " ORDER BY ?aliasIndex LIMIT 21");
        pss.setIri("e", StoreSchema.externalIri(name));
        List<ExternalVocabulary> found = group(client.select(pss.toString()));
        return found.stream().findFirst();
    }

    /**
     * True when an external name is taken, case-insensitively (D56).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public boolean nameTaken(String name) {
        ParameterizedSparqlString pss = sparql("ASK { GRAPH ?gExternal { ?e vm:nameKey ?key } }");
        pss.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return client.ask(pss.toString());
    }

    /**
     * The registered lookup keys among the given normalised keys.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public List<String> takenKeys(List<String> keys) {
        ParameterizedSparqlString pss = sparql("SELECT DISTINCT ?key WHERE { VALUES ?key { " + literals(keys) + " }"
                + " GRAPH ?gExternal { ?e vm:lookupKey ?key } } LIMIT 101");
        return client.select(pss.toString()).stream()
                .map(b -> text(b.get("key")))
                .toList();
    }

    /**
     * One page of external vocabularies, name ascending (D41, D85).
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Page<ExternalVocabulary> list(long page, int size) {
        ParameterizedSparqlString count =
                sparql("SELECT (COUNT(?e) AS ?n) WHERE { GRAPH ?gExternal { ?e a vm:ExternalVocabulary } }");
        long total = Long.parseLong(text(client.select(count.toString()).get(0).get("n")));
        long offset = (page - 1) * size;
        if (total == 0 || offset >= total) {
            return new Page<>(List.of(), total);
        }
        ParameterizedSparqlString names = sparql("SELECT ?e WHERE { GRAPH ?gExternal { ?e a vm:ExternalVocabulary ;"
                + " vm:name ?name } } ORDER BY ASC(?name) LIMIT " + size + " OFFSET " + offset);
        List<String> iris = client.select(names.toString()).stream()
                .map(b -> b.get("e").getURI())
                .toList();
        return new Page<>(byIris(iris), total);
    }

    /**
     * The external vocabularies whose namespace or alias key is one of the keys, in key order
     * (D38). At most 100 keys.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public List<ExternalVocabulary> findByKeys(List<String> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        ParameterizedSparqlString pss = sparql("SELECT ?key ?e WHERE { VALUES ?key { " + literals(keys) + " }"
                + " GRAPH ?gExternal { ?e vm:lookupKey ?key } } LIMIT 101");
        Map<String, String> byKey = new LinkedHashMap<>();
        for (Binding row : client.select(pss.toString())) {
            byKey.putIfAbsent(text(row.get("key")), row.get("e").getURI());
        }
        List<String> ordered = new ArrayList<>();
        for (String key : keys) {
            String iri = byKey.get(key);
            if (iri != null && !ordered.contains(iri)) {
                ordered.add(iri);
            }
        }
        return byIris(ordered);
    }

    /**
     * The name of the external vocabulary that matches one namespace key.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Optional<String> nameByKey(String key) {
        ParameterizedSparqlString pss =
                sparql("SELECT ?name WHERE { GRAPH ?gExternal { ?e vm:lookupKey ?key ; vm:name ?name } } LIMIT 1");
        pss.setLiteral("key", key);
        return client.select(pss.toString()).stream().findFirst().map(b -> text(b.get("name")));
    }

    /**
     * The stored content bytes.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public Optional<byte[]> content(String name) {
        ParameterizedSparqlString pss =
                sparql("SELECT ?content WHERE { GRAPH ?gExternalContent { ?e vm:content ?content } } LIMIT 1");
        pss.setIri("e", StoreSchema.externalIri(name));
        return client.select(pss.toString()).stream()
                .findFirst()
                .map(b -> Base64.getDecoder().decode(text(b.get("content"))));
    }

    /**
     * Registers an external vocabulary when neither its name nor any of its keys is taken (D30,
     * D82). One store transaction.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public void create(ExternalVocabulary data, byte[] content, String writeId) {
        List<String> keys = new ArrayList<>();
        keys.add(normalise(data.namespaceUri()));
        data.aliasUris().forEach(a -> keys.add(normalise(a)));
        StringBuilder aliases = new StringBuilder();
        for (int i = 0; i < data.aliasUris().size(); i++) {
            ParameterizedSparqlString one =
                    new ParameterizedSparqlString("?e vm:alias [ vm:index ?i ; vm:uri ?u ] . ?e vm:lookupKey ?k . ");
            one.setLiteral("i", i);
            one.setLiteral("u", data.aliasUris().get(i));
            one.setLiteral("k", normalise(data.aliasUris().get(i)));
            aliases.append(one.toString());
        }
        String optional = (data.sourceUri() == null ? "" : " ?e vm:sourceUri ?source . ")
                + (data.description() == null ? "" : " ?e vm:description ?desc . ");
        ParameterizedSparqlString pss = sparql("INSERT { GRAPH ?gExternal {"
                + " ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:nameKey ?nameKey ; vm:namespaceUri ?ns ;"
                + " vm:lookupKey ?nsKey ; vm:createdBy ?createdBy ; vm:creationDate ?now ; vm:writeId ?writeId . "
                + aliases + optional + " }"
                + " GRAPH ?gExternalContent { ?e vm:content ?content ; vm:contentLength ?length . } }"
                + " WHERE { FILTER NOT EXISTS { GRAPH ?gExternal { ?x vm:nameKey ?nameKey } }"
                + " FILTER NOT EXISTS { VALUES ?anyKey { " + literals(keys) + " }"
                + " GRAPH ?gExternal { ?y vm:lookupKey ?anyKey } } }");
        pss.setIri("e", StoreSchema.externalIri(data.name()));
        pss.setLiteral("name", data.name());
        pss.setLiteral("nameKey", data.name().toLowerCase(Locale.ROOT));
        pss.setLiteral("ns", data.namespaceUri());
        pss.setLiteral("nsKey", normalise(data.namespaceUri()));
        pss.setLiteral("createdBy", data.createdBy());
        pss.setLiteral("writeId", writeId);
        if (data.sourceUri() != null) {
            pss.setLiteral("source", data.sourceUri());
        }
        if (data.description() != null) {
            pss.setLiteral("desc", data.description());
        }
        pss.setLiteral("content", Base64.getEncoder().encodeToString(content), XSDDatatype.XSDbase64Binary);
        pss.setLiteral("length", content.length);
        StoreSchema.setTime(pss, "now", data.creationDate());
        client.update(pss.toString());
    }

    /**
     * True when the registration with this write id is stored.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public boolean writeApplied(String name, String writeId) {
        ParameterizedSparqlString pss = sparql("ASK { GRAPH ?gExternal { ?e vm:writeId ?writeId } }");
        pss.setIri("e", StoreSchema.externalIri(name));
        pss.setLiteral("writeId", writeId);
        return client.ask(pss.toString());
    }

    /**
     * Hard delete of metadata and content (D41). One store transaction.
     *
     * @throws eu.simpl.vocabularymanager.error.ApiException on store failure
     */
    public void delete(String name) {
        ParameterizedSparqlString pss = sparql("""
                DELETE { GRAPH ?gExternal { ?a ?ap ?ao } } WHERE { GRAPH ?gExternal { ?e vm:alias ?a . ?a ?ap ?ao } } ;
                DELETE WHERE { GRAPH ?gExternal { ?e ?p ?o } } ;
                DELETE WHERE { GRAPH ?gExternalContent { ?e ?cp ?co } }
                """);
        pss.setIri("e", StoreSchema.externalIri(name));
        client.update(pss.toString());
    }

    private List<ExternalVocabulary> byIris(List<String> iris) {
        if (iris.isEmpty()) {
            return List.of();
        }
        StringBuilder values = new StringBuilder();
        for (String iri : iris) {
            values.append('<').append(iri).append("> ");
        }
        ParameterizedSparqlString pss = sparql(SELECT_FIELDS + " ?e WHERE { VALUES ?e { " + values + " }"
                + " GRAPH ?gExternal { " + PATTERN + " } } ORDER BY ?e ?aliasIndex LIMIT 2200");
        List<ExternalVocabulary> found = group(client.select(pss.toString()));
        Map<String, ExternalVocabulary> byName = new LinkedHashMap<>();
        found.forEach(e -> byName.put(StoreSchema.externalIri(e.name()), e));
        return iris.stream().map(byName::get).filter(java.util.Objects::nonNull).toList();
    }

    private static List<ExternalVocabulary> group(List<Binding> rows) {
        Map<String, List<Binding>> byName = new LinkedHashMap<>();
        for (Binding row : rows) {
            byName.computeIfAbsent(text(row.get("name")), k -> new ArrayList<>())
                    .add(row);
        }
        List<ExternalVocabulary> result = new ArrayList<>();
        for (List<Binding> group : byName.values()) {
            Binding first = group.get(0);
            TreeMap<Integer, String> aliases = new TreeMap<>();
            for (Binding row : group) {
                if (row.get("alias") != null) {
                    aliases.put(integer(row.get("aliasIndex")), text(row.get("alias")));
                }
            }
            result.add(new ExternalVocabulary(
                    text(first.get("name")),
                    text(first.get("ns")),
                    new ArrayList<>(aliases.values()),
                    text(first.get("source")),
                    text(first.get("desc")),
                    text(first.get("createdBy")),
                    instant(first.get("created"))));
        }
        return result;
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

    /** Visible for the service: creation time helper. */
    public static Instant now() {
        return StoreSchema.now();
    }
}
