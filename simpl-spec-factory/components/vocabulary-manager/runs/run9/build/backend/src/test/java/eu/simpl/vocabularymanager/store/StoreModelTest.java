package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.PageRequest;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyFilter;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.service.CreateVersionCommand;
import eu.simpl.vocabularymanager.service.CreateVocabularyCommand;
import eu.simpl.vocabularymanager.service.UploadSlots;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.support.TestFuseki;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import eu.simpl.vocabularymanager.validation.SemanticRules;
import eu.simpl.vocabularymanager.validation.TurtleParser;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Test;

/** Physical model checks (D87 SHACL shapes on stored data) and two service instances on one store (D82). */
class StoreModelTest {

    private static VocabularyManagerProperties properties() {
        return new VocabularyManagerProperties(
                "https://authority.example/vocabulary-manager",
                new VocabularyManagerProperties.Store(
                        TestFuseki.queryUrl(),
                        TestFuseki.updateUrl(),
                        "r",
                        "r",
                        "w",
                        "w",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(45),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(30)),
                new VocabularyManagerProperties.Identity("i", "j", "a", Duration.ofSeconds(2), Duration.ofSeconds(10)),
                new VocabularyManagerProperties.Limits(5_242_880, 6_291_456, 1_048_576, 4, Duration.ofSeconds(5)));
    }

    /** One complete service instance with its own store client, repository and upload slots. */
    private static VocabularyService instance() {
        VocabularyManagerProperties properties = properties();
        SparqlStore store = new FusekiSparqlStore(properties, Map::of);
        VocabularyRepository repository = new FusekiVocabularyRepository(store);
        ContentValidator validator =
                new ContentValidator(new TurtleParser(), new SemanticRules(), repository, properties);
        return new VocabularyService(repository, validator, new UploadSlots(properties), Clock.systemUTC(), properties);
    }

    private static byte[] turtle(String name, int n) {
        return ("@prefix ex: <https://example.eu/" + name + "#> .\nex:s" + n + " ex:p ex:o .\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void twoInstancesOnOneStoreKeepVersionNumbersUnique() throws Exception {
        VocabularyService first = instance();
        VocabularyService second = instance();
        String name = "TwoInstances" + System.nanoTime();
        first.create(new CreateVocabularyCommand(name, "d", "c", turtle(name, 0), "u"));
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            VocabularyService service = i % 2 == 0 ? first : second;
            int n = i;
            futures.add(pool.submit(() -> service.createVersion(
                            name, new CreateVersionCommand("d" + n, "c", turtle(name, n), "u"))
                    .version()
                    .version()));
        }
        List<Integer> numbers = new ArrayList<>();
        for (Future<Integer> future : futures) {
            numbers.add(future.get());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5, 6, 7);
        Vocabulary stored = first.get(name);
        assertThat(stored.latestVersion()).isEqualTo(7);
        assertThat(first.versions(name, VocabularyFilter.SortField.VERSION, true, new PageRequest(1, 100))
                        .items())
                .filteredOn(v -> v.status() == VocabularyStatus.ACTIVE)
                .hasSize(1);
        // Two overlapping creates of one name: one wins.
        String racing = "Racing" + System.nanoTime();
        ExecutorService racers = Executors.newFixedThreadPool(2);
        Future<Boolean> a = racers.submit(() -> tryCreate(first, racing));
        Future<Boolean> b = racers.submit(() -> tryCreate(second, racing.toUpperCase()));
        assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(true, false);
        racers.shutdown();
    }

    private static boolean tryCreate(VocabularyService service, String name) {
        try {
            service.create(new CreateVocabularyCommand(name, "d", "c", turtle(name, 1), "u"));
            return true;
        } catch (eu.simpl.vocabularymanager.domain.ConflictException ex) {
            return false;
        }
    }

    @Test
    void storedDataConformsToTheShapes() {
        VocabularyService service = instance();
        String name = "Shapes" + System.nanoTime();
        service.create(new CreateVocabularyCommand(name, "d", "c", turtle(name, 0), "u"));
        service.createVersion(name, new CreateVersionCommand("d2", "c2", turtle(name, 2), "u"));
        service.changeStatus(name, VocabularyStatus.DEPRECATED);
        ExternalVocabularyRepository externals =
                new FusekiExternalVocabularyRepository(new FusekiSparqlStore(properties(), Map::of));
        String external = "ShapesExt" + System.nanoTime();
        externals.insert(
                new ExternalVocabulary(
                        external,
                        "http://shapes.example.org/" + external + "/",
                        List.of("https://shapes.example.org/" + external),
                        null,
                        "desc",
                        "u",
                        Instant.now()),
                turtle(external, 1));
        assertThat(externals.find(external)).isPresent();
        assertThat(externals.list(new PageRequest(1, 100)).total()).isPositive();

        Txn.executeRead(TestFuseki.dataset(), () -> {
            check("shapes/vocabularies-graph.ttl", StoreModel.VOCABULARY_GRAPH);
            check("shapes/external-vocabularies-graph.ttl", StoreModel.EXTERNAL_GRAPH);
            Graph content = GraphFactory.createDefaultGraph();
            TestFuseki.dataset().listGraphNodes().forEachRemaining(g -> {
                if (g.getURI().contains(":content:") || g.getURI().contains(":external-content:")) {
                    TestFuseki.dataset().getGraph(g).find().forEachRemaining(content::add);
                }
            });
            assertThat(content.isEmpty()).isFalse();
            validate("shapes/content-graph.ttl", content);
        });
        externals.delete(external);
        assertThat(externals.find(external)).isEmpty();
    }

    private static void check(String shapes, String graphName) {
        Node node = NodeFactory.createURI(graphName);
        validate(shapes, TestFuseki.dataset().getGraph(node));
    }

    private static void validate(String shapes, Graph data) {
        Graph shapesGraph = RDFDataMgr.loadGraph(
                StoreModelTest.class.getClassLoader().getResource(shapes).toString());
        ValidationReport report = ShaclValidator.get().validate(shapesGraph, data);
        if (!report.conforms()) {
            report.getEntries().forEach(e -> System.err.println(e));
        }
        assertThat(report.conforms()).as(shapes).isTrue();
    }
}
