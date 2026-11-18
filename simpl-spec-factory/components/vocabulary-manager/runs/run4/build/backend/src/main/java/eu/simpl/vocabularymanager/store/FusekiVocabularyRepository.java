package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreModel.command;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.springframework.stereotype.Repository;

/** Stores vocabularies and versions as RDF in the internal graph of the Fuseki dataset 'vocabulary'. */
@Repository
public class FusekiVocabularyRepository implements VocabularyRepository {

    private static final String VOCABULARY_PATTERN = """
            GRAPH ?internalGraph {
              ?v a vm:Vocabulary ; vm:name ?name ; vm:status ?status ; vm:createdBy ?createdBy ;
                 vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate ; vm:latestVersion ?latest .
              ?latestVersion vm:ofVocabulary ?v ; vm:version ?latest ; vm:description ?description .
            }
            """;
    private static final String VERSION_PATTERN = """
            GRAPH ?internalGraph {
              ?ver a vm:VocabularyVersion ; vm:ofVocabulary ?v ; vm:version ?version ; vm:description ?description ;
                   vm:changelog ?changelog ; vm:status ?status ; vm:createdBy ?createdBy ;
                   vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate .
              ?v vm:name ?name .
            }
            """;
    private static final String VOCABULARY_SELECT =
            "SELECT ?name ?status ?createdBy ?creationDate ?lastUpdateDate ?latest ?description WHERE {\n";
    private static final String VERSION_SELECT = "SELECT ?name ?version ?description ?changelog ?status ?createdBy"
            + " ?creationDate ?lastUpdateDate WHERE {\n";

    private final SparqlClient sparql;

    public FusekiVocabularyRepository(SparqlClient sparql) {
        this.sparql = sparql;
    }

    @Override
    public List<Vocabulary> findAll() {
        ParameterizedSparqlString query = command(VOCABULARY_SELECT + VOCABULARY_PATTERN + "}");
        return sparql.select(query.toString()).stream()
                .map(FusekiVocabularyRepository::toVocabulary)
                .toList();
    }

    @Override
    public Optional<Vocabulary> findByName(String name) {
        ParameterizedSparqlString query = command(VOCABULARY_SELECT + VOCABULARY_PATTERN + "}");
        query.setIri("v", StoreModel.vocabularyIri(name));
        return sparql.select(query.toString()).stream()
                .map(FusekiVocabularyRepository::toVocabulary)
                .findFirst();
    }

    @Override
    public Optional<String> findNameIgnoringCase(String name) {
        ParameterizedSparqlString query =
                command("SELECT ?name WHERE { GRAPH ?internalGraph { ?v vm:nameKey ?key ; vm:name ?name } }");
        query.setLiteral("key", name.toLowerCase(Locale.ROOT));
        return sparql.select(query.toString()).stream()
                .map(row -> StoreModel.text(row, "name"))
                .findFirst();
    }

    @Override
    public List<VocabularyVersion> findVersions(String name) {
        ParameterizedSparqlString query = command(VERSION_SELECT + VERSION_PATTERN + "}");
        query.setIri("v", StoreModel.vocabularyIri(name));
        return sparql.select(query.toString()).stream()
                .map(FusekiVocabularyRepository::toVersion)
                .toList();
    }

    @Override
    public Optional<VocabularyVersion> findVersion(String name, int version) {
        ParameterizedSparqlString query = command(VERSION_SELECT + VERSION_PATTERN + "}");
        query.setIri("ver", StoreModel.versionIri(name, version));
        return sparql.select(query.toString()).stream()
                .map(FusekiVocabularyRepository::toVersion)
                .findFirst();
    }

    @Override
    public Optional<byte[]> findContent(String name, int version) {
        ParameterizedSparqlString query =
                command("SELECT ?content WHERE { GRAPH ?internalGraph { ?ver vm:content ?content } }");
        query.setIri("ver", StoreModel.versionIri(name, version));
        return sparql.select(query.toString()).stream()
                .map(row -> StoreModel.bytes(row, "content"))
                .findFirst();
    }

    @Override
    public void create(Vocabulary vocabulary, VocabularyVersion firstVersion, byte[] content) {
        ParameterizedSparqlString update = command("""
                INSERT DATA { GRAPH ?internalGraph {
                  ?v a vm:Vocabulary ; vm:name ?name ; vm:nameKey ?key ; vm:status ?status ; vm:createdBy ?createdBy ;
                     vm:creationDate ?creationDate ; vm:lastUpdateDate ?lastUpdateDate ; vm:latestVersion ?latest .
                } } ;
                """ + insertVersion());
        update.setIri("v", StoreModel.vocabularyIri(vocabulary.name()));
        update.setLiteral("name", vocabulary.name());
        update.setLiteral("key", vocabulary.name().toLowerCase(Locale.ROOT));
        update.setLiteral("status", vocabulary.status().name());
        update.setLiteral("createdBy", vocabulary.createdBy());
        update.setLiteral("creationDate", StoreModel.dateTime(vocabulary.creationDate()));
        update.setLiteral("lastUpdateDate", StoreModel.dateTime(vocabulary.lastUpdateDate()));
        update.setLiteral("latest", StoreModel.integer(vocabulary.latestVersion()));
        bindVersion(update, firstVersion);
        sparql.update(StoreModel.withContent(update, content));
    }

    @Override
    public void addVersion(VocabularyVersion version, byte[] content) {
        ParameterizedSparqlString update = command("""
                WITH ?internalGraph
                DELETE { ?old vm:status "ACTIVE" ; vm:lastUpdateDate ?oldDate }
                INSERT { ?old vm:status "DEPRECATED" ; vm:lastUpdateDate ?now }
                WHERE { ?old vm:ofVocabulary ?v ; vm:status "ACTIVE" ; vm:lastUpdateDate ?oldDate } ;
                WITH ?internalGraph
                DELETE { ?v vm:latestVersion ?previousLatest ; vm:lastUpdateDate ?previousDate }
                INSERT { ?v vm:latestVersion ?newLatest ; vm:lastUpdateDate ?now }
                WHERE { ?v vm:latestVersion ?previousLatest ; vm:lastUpdateDate ?previousDate } ;
                """ + insertVersion());
        update.setIri("v", StoreModel.vocabularyIri(version.name()));
        update.setLiteral("now", StoreModel.dateTime(version.creationDate()));
        update.setLiteral("newLatest", StoreModel.integer(version.version()));
        bindVersion(update, version);
        sparql.update(StoreModel.withContent(update, content));
    }

    @Override
    public void updateVocabularyStatus(String name, VocabularyStatus status, Instant now) {
        ParameterizedSparqlString update = command("""
                WITH ?internalGraph
                DELETE { ?v vm:status ?oldStatus ; vm:lastUpdateDate ?oldDate }
                INSERT { ?v vm:status ?newStatus ; vm:lastUpdateDate ?now }
                WHERE { ?v vm:status ?oldStatus ; vm:lastUpdateDate ?oldDate }
                """);
        update.setIri("v", StoreModel.vocabularyIri(name));
        update.setLiteral("newStatus", status.name());
        update.setLiteral("now", StoreModel.dateTime(now));
        sparql.update(update.toString());
    }

    @Override
    public void updateVersionStatus(String name, int version, VocabularyStatus status, Instant now) {
        ParameterizedSparqlString update = command("""
                WITH ?internalGraph
                DELETE { ?ver vm:status ?oldStatus ; vm:lastUpdateDate ?oldDate }
                INSERT { ?ver vm:status ?newStatus ; vm:lastUpdateDate ?now }
                WHERE { ?ver vm:status ?oldStatus ; vm:lastUpdateDate ?oldDate } ;
                WITH ?internalGraph
                DELETE { ?v vm:lastUpdateDate ?vocabularyDate }
                INSERT { ?v vm:lastUpdateDate ?now }
                WHERE { ?v vm:lastUpdateDate ?vocabularyDate }
                """);
        update.setIri("ver", StoreModel.versionIri(name, version));
        update.setIri("v", StoreModel.vocabularyIri(name));
        update.setLiteral("newStatus", status.name());
        update.setLiteral("now", StoreModel.dateTime(now));
        sparql.update(update.toString());
    }

    private static String insertVersion() {
        return """
                INSERT DATA { GRAPH ?internalGraph {
                  ?ver a vm:VocabularyVersion ; vm:ofVocabulary ?v ; vm:version ?versionNumber ;
                       vm:description ?versionDescription ; vm:changelog ?versionChangelog ;
                       vm:status ?versionStatus ; vm:createdBy ?versionCreatedBy ;
                       vm:creationDate ?versionCreationDate ; vm:lastUpdateDate ?versionLastUpdateDate ;
                       vm:content ?versionContent .
                } }
                """;
    }

    private static void bindVersion(ParameterizedSparqlString update, VocabularyVersion version) {
        update.setIri("ver", StoreModel.versionIri(version.name(), version.version()));
        update.setLiteral("versionNumber", StoreModel.integer(version.version()));
        update.setLiteral("versionDescription", version.description());
        update.setLiteral("versionChangelog", version.changelog());
        update.setLiteral("versionStatus", version.status().name());
        update.setLiteral("versionCreatedBy", version.createdBy());
        update.setLiteral("versionCreationDate", StoreModel.dateTime(version.creationDate()));
        update.setLiteral("versionLastUpdateDate", StoreModel.dateTime(version.lastUpdateDate()));
        update.setLiteral("versionContent", StoreModel.CONTENT_MARKER);
    }

    private static Vocabulary toVocabulary(QuerySolution row) {
        return new Vocabulary(
                StoreModel.text(row, "name"),
                VocabularyStatus.valueOf(StoreModel.text(row, "status")),
                StoreModel.text(row, "createdBy"),
                StoreModel.instant(row, "creationDate"),
                StoreModel.instant(row, "lastUpdateDate"),
                StoreModel.number(row, "latest"),
                StoreModel.text(row, "description"));
    }

    private static VocabularyVersion toVersion(QuerySolution row) {
        return new VocabularyVersion(
                StoreModel.text(row, "name"),
                StoreModel.number(row, "version"),
                StoreModel.text(row, "description"),
                StoreModel.text(row, "changelog"),
                VocabularyStatus.valueOf(StoreModel.text(row, "status")),
                StoreModel.text(row, "createdBy"),
                StoreModel.instant(row, "creationDate"),
                StoreModel.instant(row, "lastUpdateDate"));
    }
}
