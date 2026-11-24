package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.ConditionalWrite;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.support.TestStore;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.junit.jupiter.api.Test;

/** Store behaviour: D44 timeouts and retries, D103 breaker, D201, D82 two instances, D87 SHACL shapes. */
class StoreBehaviourTest {

    private static final TestStore STORE = TestStore.shared();
    private static final AuthenticatedUser ADMIN =
            new AuthenticatedUser("sub-admin", "ga-admin@example.com", Set.of(AuthenticatedUser.ADMIN_ROLE));

    private static AppProperties properties(String url, String readPassword) {
        return new AppProperties(
                "https://authority.example/vocabulary-manager",
                4,
                new AppProperties.Store(
                        url,
                        "vocabulary",
                        TestStore.READ_USER,
                        readPassword,
                        TestStore.UPDATE_USER,
                        TestStore.UPDATE_PASSWORD,
                        500,
                        2000,
                        45000),
                null);
    }

    private static VocabularyService service(StoreClient client) {
        VocabularyRepository repository = new VocabularyRepository(client);
        AppProperties properties = properties(STORE.url(), TestStore.READ_PASSWORD);
        return new VocabularyService(
                repository,
                new ContentValidator(properties, repository),
                new ConditionalWrite(Clock.systemUTC()),
                Clock.systemUTC());
    }

    private static byte[] turtle(String name, String value) {
        return ("@prefix ex: <https://example.org/" + name + "#> .\nex:a ex:b \"" + value + "\" .\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void unreachableStoreGives503AfterRetries() {
        StoreClient client =
                new StoreClient(properties("http://127.0.0.1:9", "x"), CircuitBreakerRegistry.ofDefaults());
        long start = System.nanoTime();
        assertThatThrownBy(() -> client.ask("ASK {}")).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.type()).isEqualTo(ProblemType.SERVICE_UNAVAILABLE);
            assertThat(e.headers()).containsEntry("Retry-After", "5");
        });
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(600));
        assertThatThrownBy(() -> client.update("INSERT DATA {}")).isInstanceOf(ApiException.class);
        assertThat(client.probe(Duration.ofMillis(500))).isFalse();
    }

    @Test
    void breakerOpensAndRejectsAtOnce() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());
        StoreClient client = new StoreClient(properties("http://127.0.0.1:9", "x"), registry);
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.update("INSERT DATA {}")).isInstanceOf(ApiException.class);
        }
        long start = System.nanoTime();
        assertThatThrownBy(() -> client.select("SELECT * {} LIMIT 1")).isInstanceOf(ApiException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(300));
    }

    @Test
    void storeRefusingTheUserIs500() {
        StoreClient client = new StoreClient(properties(STORE.url(), "wrong"), CircuitBreakerRegistry.ofDefaults());
        assertThatThrownBy(() -> client.ask("ASK {}"))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.type()).isEqualTo(ProblemType.INTERNAL_SERVER_ERROR));
        StoreClient good =
                new StoreClient(properties(STORE.url(), TestStore.READ_PASSWORD), CircuitBreakerRegistry.ofDefaults());
        assertThat(good.probe(Duration.ofSeconds(2))).isTrue();
        assertThatThrownBy(() -> good.select("SELECT nonsense")).isInstanceOf(ApiException.class);
    }

    @Test
    void twoInstancesOnOneStoreNeverReuseANumber() throws Exception {
        VocabularyService first = service(
                new StoreClient(properties(STORE.url(), TestStore.READ_PASSWORD), CircuitBreakerRegistry.ofDefaults()));
        VocabularyService second = service(
                new StoreClient(properties(STORE.url(), TestStore.READ_PASSWORD), CircuitBreakerRegistry.ofDefaults()));
        String name = "Two" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        first.create(name, "d", "Initial version.", turtle(name, "0"), ADMIN);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<VocabularyService.Created>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            VocabularyService instance = i % 2 == 0 ? first : second;
            int n = i;
            futures.add(pool.submit(() -> instance.createVersion(name, "d" + n, "c", turtle(name, "v" + n), ADMIN)));
        }
        List<Long> numbers = new ArrayList<>();
        for (Future<VocabularyService.Created> future : futures) {
            numbers.add(future.get().version().version());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        assertThat(first.get(name).latestVersion()).isEqualTo(9);
        assertThat(first.version(name, 9).version().status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(first.version(name, 5).version().status()).isEqualTo(VocabularyStatus.DEPRECATED);
        first.changeStatus(name, VocabularyStatus.DEPRECATED, ADMIN);
        first.changeVersionStatus(name, 5, VocabularyStatus.ACTIVE, ADMIN);
        assertThat(second.search(List.of(
                        new VocabularyService.Reference(name, null), new VocabularyService.Reference(name, 5L))))
                .extracting(VocabularyService.SearchResult::status)
                .containsExactly(VocabularyStatus.DEPRECATED, VocabularyStatus.ACTIVE);
    }

    @Test
    void storedDataConformsToTheShapes() {
        VocabularyService service = service(
                new StoreClient(properties(STORE.url(), TestStore.READ_PASSWORD), CircuitBreakerRegistry.ofDefaults()));
        String name = "Shp" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        service.create(name, "d", "Initial version.", turtle(name, "0"), ADMIN);
        service.createVersion(name, "d2", "c", turtle(name, "1"), ADMIN);
        Dataset dataset = DatasetFactory.create();
        RDFParser.source(STORE.url() + "/vocabulary/dump")
                .lang(Lang.NQUADS)
                .httpHeader("Accept", "application/n-quads")
                .httpHeader(
                        "Authorization",
                        "Basic "
                                + java.util.Base64.getEncoder()
                                        .encodeToString((TestStore.ADMIN_USER + ":" + TestStore.ADMIN_PASSWORD)
                                                .getBytes(StandardCharsets.UTF_8)))
                .parse(dataset);
        Shapes shapes = Shapes.parse(RDFDataMgr.loadGraph("../docs/shapes.ttl"));
        int graphs = 0;
        var names = dataset.listModelNames();
        while (names.hasNext()) {
            Model model = dataset.getNamedModel(names.next());
            ValidationReport report = ShaclValidator.get().validate(shapes, model.getGraph());
            assertThat(report.conforms()).as(report.toString()).isTrue();
            graphs++;
        }
        assertThat(graphs).isGreaterThanOrEqualTo(3);
    }
}
