package eu.simpl.vocabularymanager.store;

import static eu.simpl.vocabularymanager.store.StoreModel.command;

import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.springframework.stereotype.Repository;

/** Stores external vocabularies as RDF in the external graph of the Fuseki dataset 'vocabulary'. */
@Repository
public class FusekiExternalVocabularyRepository implements ExternalVocabularyRepository {

    private static final String SELECT = """
            SELECT ?name ?namespaceUri ?aliasUris ?sourceUri ?description ?createdBy ?creationDate WHERE {
              GRAPH ?externalGraph {
                ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:namespaceUri ?namespaceUri ; vm:aliasUris ?aliasUris ;
                   vm:createdBy ?createdBy ; vm:creationDate ?creationDate .
                OPTIONAL { ?e vm:sourceUri ?sourceUri }
                OPTIONAL { ?e vm:description ?description }
              }
            }
            """;

    private final SparqlClient sparql;

    public FusekiExternalVocabularyRepository(SparqlClient sparql) {
        this.sparql = sparql;
    }

    @Override
    public List<ExternalVocabulary> findAll() {
        return sparql.select(command(SELECT).toString()).stream()
                .map(FusekiExternalVocabularyRepository::toExternal)
                .toList();
    }

    @Override
    public Optional<ExternalVocabulary> findByName(String name) {
        ParameterizedSparqlString query = command(SELECT);
        query.setIri("e", StoreModel.externalIri(name));
        return sparql.select(query.toString()).stream()
                .map(FusekiExternalVocabularyRepository::toExternal)
                .findFirst();
    }

    @Override
    public Optional<byte[]> findContent(String name) {
        ParameterizedSparqlString query =
                command("SELECT ?content WHERE { GRAPH ?externalGraph { ?e vm:content ?content } }");
        query.setIri("e", StoreModel.externalIri(name));
        return sparql.select(query.toString()).stream()
                .map(row -> StoreModel.bytes(row, "content"))
                .findFirst();
    }

    @Override
    public void create(ExternalVocabulary vocabulary, byte[] content) {
        StringBuilder text = new StringBuilder("""
                INSERT DATA { GRAPH ?externalGraph {
                  ?e a vm:ExternalVocabulary ; vm:name ?name ; vm:nameKey ?key ; vm:namespaceUri ?namespaceUri ;
                     vm:aliasUris ?aliasUris ; vm:createdBy ?createdBy ; vm:creationDate ?creationDate ;
                     vm:content ?content .
                """);
        if (vocabulary.sourceUri() != null) {
            text.append("  ?e vm:sourceUri ?sourceUri .\n");
        }
        if (vocabulary.description() != null) {
            text.append("  ?e vm:description ?description .\n");
        }
        text.append("} }");
        ParameterizedSparqlString update = command(text.toString());
        update.setIri("e", StoreModel.externalIri(vocabulary.name()));
        update.setLiteral("name", vocabulary.name());
        update.setLiteral("key", vocabulary.name().toLowerCase(Locale.ROOT));
        update.setLiteral("namespaceUri", vocabulary.namespaceUri());
        update.setLiteral("aliasUris", String.join(" ", vocabulary.aliasUris()));
        update.setLiteral("createdBy", vocabulary.createdBy());
        update.setLiteral("creationDate", StoreModel.dateTime(vocabulary.creationDate()));
        update.setLiteral("content", StoreModel.CONTENT_MARKER);
        if (vocabulary.sourceUri() != null) {
            update.setLiteral("sourceUri", vocabulary.sourceUri());
        }
        if (vocabulary.description() != null) {
            update.setLiteral("description", vocabulary.description());
        }
        sparql.update(StoreModel.withContent(update, content));
    }

    @Override
    public void delete(String name) {
        ParameterizedSparqlString update = command("DELETE WHERE { GRAPH ?externalGraph { ?e ?p ?o } }");
        update.setIri("e", StoreModel.externalIri(name));
        sparql.update(update.toString());
    }

    private static ExternalVocabulary toExternal(QuerySolution row) {
        String aliases = StoreModel.text(row, "aliasUris");
        List<String> aliasUris = aliases == null || aliases.isBlank()
                ? List.of()
                : Arrays.stream(aliases.split(" ")).filter(s -> !s.isBlank()).toList();
        return new ExternalVocabulary(
                StoreModel.text(row, "name"),
                StoreModel.text(row, "namespaceUri"),
                aliasUris,
                StoreModel.text(row, "sourceUri"),
                StoreModel.text(row, "description"),
                StoreModel.text(row, "createdBy"),
                StoreModel.instant(row, "creationDate"));
    }
}
