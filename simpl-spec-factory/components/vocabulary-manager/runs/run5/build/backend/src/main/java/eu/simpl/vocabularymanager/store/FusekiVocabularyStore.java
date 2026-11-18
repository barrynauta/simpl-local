package eu.simpl.vocabularymanager.store;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.jena.query.ParameterizedSparqlString;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Fuseki implementation of the store (E3). All values reach SPARQL through parameterised strings (OWT-A05). Content
 * is kept as base64, so the bytes come back exactly as uploaded (D29).
 */
@Repository
public class FusekiVocabularyStore implements VocabularyStore {

    private static final String NS = "urn:simpl:vocabulary-manager:";
    private static final String PREFIX = "PREFIX vm: <" + NS + ">\n";
    private static final String ACTIVE = "ACTIVE";

    private static final String VOCABULARY_SELECT = PREFIX
            + "SELECT ?name ?status ?createdBy ?creationDate ?lastUpdateDate ?latest ?description WHERE {\n"
            + "  ?v a vm:Vocabulary ; vm:name ?name ; vm:status ?status ; vm:createdBy ?createdBy ;\n"
            + "     vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate ; vm:latestVersion ?latest .\n"
            + "  OPTIONAL { ?lv vm:ofVocabulary ?v ; vm:version ?latest ; vm:description ?description }\n"
            + "}";

    private static final String VERSION_SELECT = PREFIX
            + "SELECT ?version ?status ?description ?changelog ?createdBy ?creationDate ?lastUpdateDate WHERE {\n"
            + "  ?ver vm:ofVocabulary ?v ; vm:version ?version ; vm:status ?status ; vm:description ?description ;\n"
            + "       vm:changelog ?changelog ; vm:createdBy ?createdBy ; vm:creationDate ?creationDate ;\n"
            + "       vm:lastUpdateDate ?lastUpdateDate .\n"
            + "}";

    private static final String EXTERNAL_SELECT = PREFIX
            + "SELECT ?name ?namespaceUri ?aliasUris ?sourceUri ?description ?createdBy ?creationDate WHERE {\n"
            + "  ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?namespaceUri ; vm:aliasUris ?aliasUris ;\n"
            + "     vm:createdBy ?createdBy ; vm:creationDate ?creationDate .\n"
            + "  OPTIONAL { ?e vm:sourceUri ?sourceUri }\n"
            + "  OPTIONAL { ?e vm:description ?description }\n"
            + "}";

    private final SparqlClient client;
    private final ObjectMapper mapper;

    public FusekiVocabularyStore(SparqlClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    static String vocabularyIri(String name) {
        return NS + "vocabulary:" + name;
    }

    static String versionIri(String name, int version) {
        return NS + "vocabulary:" + name + ":version:" + version;
    }

    static String externalIri(String name) {
        return NS + "external:" + name;
    }

    @Override
    public Optional<Vocabulary> findVocabulary(String name) {
        ParameterizedSparqlString q = new ParameterizedSparqlString(VOCABULARY_SELECT);
        q.setIri("v", vocabularyIri(name));
        return client.select(q.toString()).stream().findFirst().map(FusekiVocabularyStore::toVocabulary);
    }

    @Override
    public boolean vocabularyNameTaken(String name) {
        ParameterizedSparqlString q = new ParameterizedSparqlString(
                PREFIX + "SELECT ?v WHERE { ?v a vm:Vocabulary ; vm:nameKey ?key } LIMIT 1");
        q.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return !client.select(q.toString()).isEmpty();
    }

    @Override
    public List<Vocabulary> listVocabularies() {
        return client.select(VOCABULARY_SELECT).stream()
                .map(FusekiVocabularyStore::toVocabulary)
                .toList();
    }

    @Override
    public List<VocabularyVersion> listVersions(String name) {
        ParameterizedSparqlString q = new ParameterizedSparqlString(VERSION_SELECT);
        q.setIri("v", vocabularyIri(name));
        return client.select(q.toString()).stream()
                .map(row -> toVersion(name, row))
                .toList();
    }

    @Override
    public Optional<VocabularyVersion> findVersion(String name, int version) {
        ParameterizedSparqlString q = new ParameterizedSparqlString(VERSION_SELECT);
        q.setIri("ver", versionIri(name, version));
        q.setIri("v", vocabularyIri(name));
        return client.select(q.toString()).stream().findFirst().map(row -> toVersion(name, row));
    }

    @Override
    public Optional<byte[]> findContent(String name, int version) {
        ParameterizedSparqlString q =
                new ParameterizedSparqlString(PREFIX + "SELECT ?content WHERE { ?ver vm:content ?content }");
        q.setIri("ver", versionIri(name, version));
        return client.select(q.toString()).stream()
                .findFirst()
                .map(row -> Base64.getDecoder().decode(row.get("content")));
    }

    @Override
    public void createVocabulary(Vocabulary vocabulary, VocabularyVersion first, byte[] content) {
        ParameterizedSparqlString u = new ParameterizedSparqlString(PREFIX
                + "INSERT DATA {\n"
                + "  ?v a vm:Vocabulary ; vm:name ?name ; vm:nameKey ?key ; vm:status ?status ; vm:createdBy ?by ;\n"
                + "     vm:creationDate ?created ; vm:lastUpdateDate ?updated ; vm:latestVersion ?latest .\n"
                + versionInsert()
                + "}");
        u.setIri("v", vocabularyIri(vocabulary.name()));
        u.setLiteral("name", vocabulary.name());
        u.setLiteral("key", vocabulary.name().toLowerCase(Locale.ROOT));
        u.setLiteral("status", vocabulary.status().name());
        u.setLiteral("by", vocabulary.createdBy());
        u.setLiteral("created", vocabulary.creationDate());
        u.setLiteral("updated", vocabulary.lastUpdateDate());
        u.setLiteral("latest", vocabulary.latestVersion());
        bindVersion(u, first, content);
        client.update(u.toString());
    }

    @Override
    public void createVersion(VocabularyVersion version, byte[] content, String now) {
        ParameterizedSparqlString u = new ParameterizedSparqlString(PREFIX
                + "DELETE { ?old vm:status \"ACTIVE\" . ?old vm:lastUpdateDate ?oldDate }\n"
                + "INSERT { ?old vm:status \"DEPRECATED\" . ?old vm:lastUpdateDate ?now }\n"
                + "WHERE { ?old vm:ofVocabulary ?v ; vm:status \"ACTIVE\" ; vm:lastUpdateDate ?oldDate } ;\n"
                + "DELETE { ?v vm:latestVersion ?oldLatest . ?v vm:lastUpdateDate ?oldUpdated }\n"
                + "WHERE { ?v vm:latestVersion ?oldLatest ; vm:lastUpdateDate ?oldUpdated } ;\n"
                + "INSERT DATA {\n"
                + "  ?v vm:latestVersion ?latest ; vm:lastUpdateDate ?now .\n"
                + versionInsert()
                + "}");
        u.setIri("v", vocabularyIri(version.name()));
        u.setLiteral("now", now);
        u.setLiteral("latest", version.version());
        bindVersion(u, version, content);
        client.update(u.toString());
    }

    @Override
    public void updateVocabularyStatus(String name, Status status, String now) {
        ParameterizedSparqlString u = new ParameterizedSparqlString(PREFIX
                + "DELETE { ?v vm:status ?oldStatus . ?v vm:lastUpdateDate ?oldDate }\n"
                + "INSERT { ?v vm:status ?status . ?v vm:lastUpdateDate ?now }\n"
                + "WHERE { ?v vm:status ?oldStatus ; vm:lastUpdateDate ?oldDate }");
        u.setIri("v", vocabularyIri(name));
        u.setLiteral("status", status.name());
        u.setLiteral("now", now);
        client.update(u.toString());
    }

    @Override
    public void updateVersionStatus(String name, int version, Status status, String now) {
        ParameterizedSparqlString u = new ParameterizedSparqlString(PREFIX
                + "DELETE { ?ver vm:status ?oldStatus . ?ver vm:lastUpdateDate ?oldDate }\n"
                + "INSERT { ?ver vm:status ?status . ?ver vm:lastUpdateDate ?now }\n"
                + "WHERE { ?ver vm:status ?oldStatus ; vm:lastUpdateDate ?oldDate } ;\n"
                + "DELETE { ?v vm:lastUpdateDate ?oldUpdated } INSERT { ?v vm:lastUpdateDate ?now }\n"
                + "WHERE { ?v vm:lastUpdateDate ?oldUpdated }");
        u.setIri("v", vocabularyIri(name));
        u.setIri("ver", versionIri(name, version));
        u.setLiteral("status", status.name());
        u.setLiteral("now", now);
        client.update(u.toString());
    }

    @Override
    public List<ExternalVocabulary> listExternalVocabularies() {
        return client.select(EXTERNAL_SELECT).stream().map(this::toExternal).toList();
    }

    @Override
    public Optional<ExternalVocabulary> findExternalVocabulary(String name) {
        ParameterizedSparqlString q = new ParameterizedSparqlString(EXTERNAL_SELECT);
        q.setIri("e", externalIri(name));
        return client.select(q.toString()).stream().findFirst().map(this::toExternal);
    }

    @Override
    public Optional<byte[]> findExternalContent(String name) {
        ParameterizedSparqlString q =
                new ParameterizedSparqlString(PREFIX + "SELECT ?content WHERE { ?e vm:content ?content }");
        q.setIri("e", externalIri(name));
        return client.select(q.toString()).stream()
                .findFirst()
                .map(row -> Base64.getDecoder().decode(row.get("content")));
    }

    @Override
    public void createExternalVocabulary(ExternalVocabulary vocabulary, byte[] content) {
        StringBuilder optional = new StringBuilder();
        if (vocabulary.sourceUri() != null) {
            optional.append("  ?e vm:sourceUri ?source .\n");
        }
        if (vocabulary.description() != null) {
            optional.append("  ?e vm:description ?description .\n");
        }
        ParameterizedSparqlString u = new ParameterizedSparqlString(PREFIX
                + "INSERT DATA {\n"
                + "  ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:nameKey ?key ; vm:namespaceUri ?namespace ;\n"
                + "     vm:aliasUris ?aliases ; vm:createdBy ?by ; vm:creationDate ?created ; vm:content ?content .\n"
                + optional
                + "}");
        u.setIri("e", externalIri(vocabulary.name()));
        u.setLiteral("name", vocabulary.name());
        u.setLiteral("key", vocabulary.name().toLowerCase(Locale.ROOT));
        u.setLiteral("namespace", vocabulary.namespaceUri());
        u.setLiteral("aliases", toJson(vocabulary.aliasUris()));
        u.setLiteral("by", vocabulary.createdBy());
        u.setLiteral("created", vocabulary.creationDate());
        u.setLiteral("content", Base64.getEncoder().encodeToString(content));
        if (vocabulary.sourceUri() != null) {
            u.setLiteral("source", vocabulary.sourceUri());
        }
        if (vocabulary.description() != null) {
            u.setLiteral("description", vocabulary.description());
        }
        client.update(u.toString());
    }

    @Override
    public void deleteExternalVocabulary(String name) {
        ParameterizedSparqlString u = new ParameterizedSparqlString(PREFIX + "DELETE WHERE { ?e ?p ?o }");
        u.setIri("e", externalIri(name));
        client.update(u.toString());
    }

    @Override
    public boolean isAvailable(Duration timeout) {
        try {
            client.ask("ASK { }", timeout);
            return true;
        } catch (StoreUnavailableException | StoreException e) {
            return false;
        }
    }

    private static String versionInsert() {
        return "  ?ver a vm:VocabularyVersion ; vm:ofVocabulary ?v ; vm:version ?number ; vm:status ?verStatus ;\n"
                + "       vm:description ?verDescription ; vm:changelog ?verChangelog ; vm:createdBy ?verBy ;\n"
                + "       vm:creationDate ?verCreated ; vm:lastUpdateDate ?verUpdated ; vm:content ?content .\n";
    }

    private static void bindVersion(ParameterizedSparqlString u, VocabularyVersion version, byte[] content) {
        u.setIri("ver", versionIri(version.name(), version.version()));
        u.setLiteral("number", version.version());
        u.setLiteral("verStatus", version.status().name());
        u.setLiteral("verDescription", version.description());
        u.setLiteral("verChangelog", version.changelog());
        u.setLiteral("verBy", version.createdBy());
        u.setLiteral("verCreated", version.creationDate());
        u.setLiteral("verUpdated", version.lastUpdateDate());
        u.setLiteral("content", Base64.getEncoder().encodeToString(content));
    }

    private static Vocabulary toVocabulary(Map<String, String> row) {
        return new Vocabulary(
                row.get("name"),
                Status.valueOf(row.getOrDefault("status", ACTIVE)),
                row.get("createdBy"),
                row.get("creationDate"),
                row.get("lastUpdateDate"),
                Integer.parseInt(row.get("latest")),
                row.get("description"));
    }

    private static VocabularyVersion toVersion(String name, Map<String, String> row) {
        return new VocabularyVersion(
                name,
                Integer.parseInt(row.get("version")),
                Status.valueOf(row.get("status")),
                row.get("description"),
                row.get("changelog"),
                row.get("createdBy"),
                row.get("creationDate"),
                row.get("lastUpdateDate"));
    }

    private ExternalVocabulary toExternal(Map<String, String> row) {
        return new ExternalVocabulary(
                row.get("name"),
                row.get("namespaceUri"),
                fromJson(row.get("aliasUris")),
                row.get("sourceUri"),
                row.get("description"),
                row.get("createdBy"),
                row.get("creationDate"));
    }

    private String toJson(List<String> values) {
        try {
            return mapper.writeValueAsString(values);
        } catch (JacksonException e) {
            throw new StoreException("Alias list could not be written", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            return json == null
                    ? List.of()
                    : new ArrayList<>(mapper.readValue(json, new TypeReference<List<String>>() {}));
        } catch (JacksonException e) {
            throw new StoreException("Alias list could not be read", e);
        }
    }
}
