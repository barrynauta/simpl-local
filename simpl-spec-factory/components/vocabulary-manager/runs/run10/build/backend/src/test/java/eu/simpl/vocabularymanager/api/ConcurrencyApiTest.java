package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.VocabularyManagerApplication;
import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import eu.simpl.vocabularymanager.support.TestStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * D43 and D82: overlapping changes to one vocabulary are serialised through conditional store updates, also across
 * two backend instances on one store.
 */
class ConcurrencyApiTest extends IntegrationTestBase {

    private static ConfigurableApplicationContext second;

    @BeforeAll
    static void startSecondInstance() {
        second = new SpringApplicationBuilder(VocabularyManagerApplication.class)
                .run(
                        "--server.port=0",
                        "--management.server.port=0",
                        "--logging.structured.format.console=",
                        "--vm.store.url=" + STORE.url(),
                        "--vm.store.read-user=" + TestStore.READ_USER,
                        "--vm.store.read-password=" + TestStore.READ_PASSWORD,
                        "--vm.store.update-user=" + TestStore.UPDATE_USER,
                        "--vm.store.update-password=" + TestStore.UPDATE_PASSWORD,
                        "--vm.security.issuer-uri=" + eu.simpl.vocabularymanager.support.TestIdentityProvider.ISSUER,
                        "--vm.security.jwks-uri=" + IDP.jwksUri(),
                        "--vm.content-base-url=" + CONTENT_BASE);
    }

    @AfterAll
    static void stopSecondInstance() {
        second.close();
    }

    private ApiClient secondApi() {
        return new ApiClient(
                ((WebServerApplicationContext) second).getWebServer().getPort());
    }

    @Test
    void parallelNewVersionsOnTwoInstancesGetConsecutiveNumbers() throws Exception {
        String name = uniqueName("Conc");
        assertThat(api().multipart(
                                "/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", simpleTurtle(name)))
                        .status())
                .isEqualTo(201);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<ApiClient.Response>> results = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ApiClient client = i % 2 == 0 ? api() : secondApi();
            String body = simpleTurtle(name) + "# change " + i + "\n";
            Callable<ApiClient.Response> call = () -> client.multipart(
                    "/v1/vocabularies/" + name + "/versions",
                    IDP.admin(),
                    ApiClient.versionParts("D", "Change.", body));
            results.add(pool.submit(call));
        }
        Set<String> locations = new HashSet<>();
        for (Future<ApiClient.Response> result : results) {
            ApiClient.Response response = result.get();
            assertThat(response.status()).as(response.text()).isEqualTo(201);
            locations.add(response.header("Location"));
        }
        pool.shutdown();
        assertThat(locations).hasSize(6);
        var versions = api().get("/v1/vocabularies/" + name + "/versions?pageSize=100", IDP.viewer())
                .json();
        assertThat(versions.get("total").asInt()).isEqualTo(7);
        List<String> statuses = versions.get("items")
                .valueStream()
                .map(item ->
                        item.get("version").asInt() + ":" + item.get("status").asString())
                .toList();
        assertThat(statuses.getFirst()).isEqualTo("7:ACTIVE");
        assertThat(statuses.subList(1, 7)).allMatch(status -> status.endsWith(":DEPRECATED"));
    }

    @Test
    void overlappingIdenticalStatusChangesGiveOneSuccessAndOneConflict() throws Exception {
        String name = uniqueName("Pat");
        api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", simpleTurtle(name)));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<ApiClient.Response> one = pool.submit(
                () -> api().json("PATCH", "/v1/vocabularies/" + name, IDP.admin(), "{\"status\":\"DEPRECATED\"}"));
        Future<ApiClient.Response> two = pool.submit(() ->
                secondApi().json("PATCH", "/v1/vocabularies/" + name, IDP.admin(), "{\"status\":\"DEPRECATED\"}"));
        List<Integer> statuses = List.of(one.get().status(), two.get().status());
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    void parallelCreatesWithTheSameNameGiveOneVocabulary() throws Exception {
        String name = uniqueName("Race");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<ApiClient.Response> one = pool.submit(() ->
                api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", simpleTurtle(name))));
        Future<ApiClient.Response> two = pool.submit(() -> secondApi()
                .multipart(
                        "/v1/vocabularies",
                        IDP.admin(),
                        ApiClient.createParts(name.toUpperCase(), "D", simpleTurtle(name))));
        List<Integer> statuses = List.of(one.get().status(), two.get().status());
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(api().get("/v1/vocabularies?name=" + name, IDP.viewer())
                        .json()
                        .get("total")
                        .asInt())
                .isEqualTo(1);
    }
}
