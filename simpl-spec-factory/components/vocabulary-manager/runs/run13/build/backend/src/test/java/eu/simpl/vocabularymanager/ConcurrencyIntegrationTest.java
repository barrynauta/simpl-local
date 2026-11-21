package eu.simpl.vocabularymanager;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.service.VocabularyService;
import eu.simpl.vocabularymanager.service.VocabularyService.Submission;
import eu.simpl.vocabularymanager.service.WriteRetry;
import eu.simpl.vocabularymanager.store.SparqlStoreClient;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.ApiClient.Response;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** D43, D82, D92, D119: overlapping writes through the API and through two service instances on one store. */
class ConcurrencyIntegrationTest extends IntegrationTestBase {

    @Test
    void fourParallelNewVersionsGetDistinctNumbers() throws Exception {
        String name = uniqueName("Para");
        assertThat(api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, simpleTurtle(name), "D."))
                        .status())
                .isEqualTo(201);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Response>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int index = i;
            futures.add(pool.submit(() -> api.multipart(
                    "/v1/vocabularies/" + name + "/versions",
                    admin,
                    ApiClient.versionParts(simpleTurtle(name) + "# " + index + "\n", "D" + index, "C" + index))));
        }
        Set<String> locations = new HashSet<>();
        for (Future<Response> future : futures) {
            Response response = future.get();
            assertThat(response.status()).as(response.text()).isEqualTo(201);
            locations.add(response.header("Location"));
        }
        pool.shutdown();
        assertThat(locations).hasSize(4);
        JsonNode items =
                api.get("/v1/vocabularies/" + name + "/versions", viewer).json().get("items");
        assertThat(items.size()).isEqualTo(5);
        assertThat(items.get(0).get("version").asInt()).isEqualTo(5);
        assertThat(items.get(0).get("status").asString()).isEqualTo("ACTIVE");
        for (int i = 1; i < 5; i++) {
            assertThat(items.get(i).get("status").asString()).isEqualTo("DEPRECATED");
        }
    }

    @Test
    void twoServiceInstancesOnOneStore() throws Exception {
        VocabularyService first = service();
        VocabularyService second = service();
        AuthenticatedUser user = new AuthenticatedUser("sub", "creator@example.com", Set.of("GA_VOCABULARY_ADMIN"));
        String name = uniqueName("Twin");
        first.create(new Submission(name, "D", "C", simpleTurtle(name).getBytes(StandardCharsets.UTF_8)), user);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            VocabularyService target = i % 2 == 0 ? first : second;
            byte[] content = (simpleTurtle(name) + "# twin " + i + "\n").getBytes(StandardCharsets.UTF_8);
            String description = "D" + i;
            tasks.add(() -> target.createVersion(new Submission(name, description, "C", content), user)
                    .version()
                    .version());
        }
        Set<Integer> numbers = new HashSet<>();
        for (Future<Integer> future : pool.invokeAll(tasks)) {
            numbers.add(future.get());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5, 6, 7);
        Response race = api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, simpleTurtle(name), "D"));
        assertThat(race.status()).isEqualTo(409);
    }

    private static VocabularyService service() {
        VocabularyManagerProperties.Store store = new VocabularyManagerProperties.Store(
                STORE.queryUrl(),
                STORE.updateUrl(),
                "reader",
                "",
                "writer",
                "",
                Duration.ofSeconds(2),
                Duration.ofSeconds(10),
                Duration.ofSeconds(45),
                Duration.ofSeconds(2));
        SparqlStoreClient client = new SparqlStoreClient(
                store, CircuitBreaker.ofDefaults("twin"), Retry.ofDefaults("twin"), new TraceHeaders(null, null));
        return new VocabularyService(
                new VocabularyRepository(client),
                CONTENT_BASE,
                new WriteRetry(8, Duration.ofSeconds(30), Clock.systemUTC()),
                Clock.systemUTC());
    }
}
