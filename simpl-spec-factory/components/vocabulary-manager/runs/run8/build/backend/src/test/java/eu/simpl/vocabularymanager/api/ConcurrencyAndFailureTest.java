package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import eu.simpl.vocabularymanager.support.Api;
import eu.simpl.vocabularymanager.support.Api.Part;
import eu.simpl.vocabularymanager.support.Api.Result;
import eu.simpl.vocabularymanager.support.Instance;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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

/** Concurrent writes (D43, D82) on one and on two instances, and store failures (D44). */
class ConcurrencyAndFailureTest extends IntegrationTestBase {

    @Test
    void parallelVersionsOnOneInstanceGetDistinctNumbers() throws Exception {
        String id = Api.unique();
        String name = "Parallel" + id;
        create(name, simpleTurtle(id));
        List<Result> results = parallel(5, i -> newVersion(name, "Version " + i, simpleTurtle(id + i)));
        assertVersionsTwoToN(name, results, 6);
    }

    @Test
    void twoInstancesOnOneStoreSerialiseThroughTheStore() throws Exception {
        String id = Api.unique();
        String name = "TwoInst" + id;
        create(name, simpleTurtle(id));
        try (Instance second = Instance.start(ENV.storeUrl())) {
            Api other = second.api();
            List<Result> results = parallel(6, i -> {
                Api target = i % 2 == 0 ? api : other;
                return target.multipart("/v1/vocabularies/" + name + "/versions", ENV.admin(),
                        Part.field("description", "Version " + i), Part.field("changelog", "c"),
                        Part.file(simpleTurtle(id + i)));
            });
            assertVersionsTwoToN(name, results, 7);

            String shared = "Race" + id;
            List<Result> creations = parallel(4, i -> (i % 2 == 0 ? api : other).multipart("/v1/vocabularies",
                    ENV.admin(), Part.field("name", i < 2 ? shared : shared.toUpperCase()),
                    Part.field("description", "d"), Part.field("changelog", "c"), Part.file(simpleTurtle(id))));
            long created = creations.stream().filter(r -> r.status() == 201).count();
            long conflicts = creations.stream().filter(r -> r.status() == 409).count();
            assertThat(created).isEqualTo(1);
            assertThat(conflicts).isEqualTo(3);

            List<Result> patches = parallel(2, i -> (i == 0 ? api : other)
                    .json("PATCH", "/v1/vocabularies/" + name, ENV.admin(), "{\"status\":\"DEPRECATED\"}"));
            assertThat(patches.stream().map(Result::status).toList()).containsExactlyInAnyOrder(200, 409);
        }
    }

    @Test
    void unreachableStoreGives503AndHealthDown() {
        try (Instance down = Instance.start("http://127.0.0.1:9/vocabulary")) {
            Api broken = down.api();
            Result list = broken.get("/v1/vocabularies", ENV.viewer());
            assertThat(list.status()).isEqualTo(503);
            assertThat(list.header("Retry-After")).isEqualTo("5");
            assertThat(list.json().path("type").asString()).isEqualTo("urn:problem-type:belgif:serviceUnavailable");
            assertThat(list.json().path("title").asString()).isEqualTo("Service Unavailable");
            assertThat(list.json().path("detail").asString())
                    .isEqualTo("The vocabulary store is not available. Retry later.");
            Result post = broken.multipart("/v1/vocabularies", ENV.admin(), Part.field("name", "Down" + Api.unique()),
                    Part.field("description", "d"), Part.field("changelog", "c"),
                    Part.file(simpleTurtle(Api.unique())));
            assertThat(post.status()).isEqualTo(503);
            assertThat(broken.json("POST", "/v1/vocabularies/searches", ENV.viewer(),
                            "{\"items\":[{\"name\":\"Abc\"}]}").status())
                    .isEqualTo(503);
            assertThat(broken.get("/v1/external-vocabularies", ENV.viewer()).status()).isEqualTo(503);
            Result health = broken.get("/v1/health", null);
            assertThat(health.status()).isEqualTo(503);
            assertThat(health.json().path("status").asString()).isEqualTo("DOWN");
        }
    }

    @Test
    void storeErrorsAndTimeouts() throws Exception {
        HttpServer fake = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fake.createContext("/broken", exchange -> {
            byte[] body = "boom".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        fake.createContext("/slow", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        fake.start();
        String base = "http://127.0.0.1:" + fake.getAddress().getPort();
        try (Instance broken = Instance.start(base + "/broken");
                Instance slow = Instance.start(base + "/slow", "vocabulary-manager.store.read-timeout=300ms",
                        "vocabulary-manager.store.update-timeout=300ms",
                        "vocabulary-manager.store.health-timeout=300ms")) {
            Result error = broken.api().get("/v1/vocabularies", ENV.viewer());
            assertThat(error.status()).isEqualTo(500);
            assertThat(error.json().path("type").asString()).isEqualTo("urn:problem-type:belgif:internalServerError");
            assertThat(error.text()).doesNotContain("boom");
            Result timeout = slow.api().get("/v1/vocabularies", ENV.viewer());
            assertThat(timeout.status()).isEqualTo(503);
            Result write = slow.api().json("PATCH", "/v1/vocabularies/Abcd", ENV.admin(), "{\"status\":\"ACTIVE\"}");
            assertThat(write.status()).isEqualTo(503);
            assertThat(slow.api().get("/v1/health", null).status()).isEqualTo(503);
        } finally {
            fake.stop(0);
        }
    }

    @Test
    void healthIsDegradedWithoutKeysAndIdentityProvider() {
        ENV.identityProviderUp(false);
        try (Instance degraded = Instance.start(ENV.storeUrl())) {
            Result health = degraded.api().get("/v1/health", null);
            assertThat(health.status()).isEqualTo(200);
            assertThat(health.json().path("status").asString()).isEqualTo("DEGRADED");
            assertThat(degraded.api().get("/v1/vocabularies", ENV.viewer()).status()).isEqualTo(503);
            ENV.identityProviderUp(true);
            // The keys are fetched on the first token once the identity provider is back.
            assertThat(degraded.api().get("/v1/vocabularies", ENV.viewer()).status()).isEqualTo(200);
            assertThat(degraded.api().get("/v1/health", null).json().path("status").asString()).isEqualTo("UP");
        } finally {
            ENV.identityProviderUp(true);
        }
    }

    private void assertVersionsTwoToN(String name, List<Result> results, int highest) {
        Set<String> locations = new HashSet<>();
        for (Result result : results) {
            assertThat(result.status()).as(result.text()).isEqualTo(201);
            locations.add(result.header("Location"));
        }
        assertThat(locations).hasSize(highest - 1);
        JsonNode items = api.get("/v1/vocabularies/" + name + "/versions?pageSize=100", ENV.viewer())
                .json()
                .path("items");
        assertThat(items.size()).isEqualTo(highest);
        for (int i = 0; i < items.size(); i++) {
            assertThat(items.get(i).path("version").asInt()).isEqualTo(highest - i);
            assertThat(items.get(i).path("status").asString()).isEqualTo(i == 0 ? "ACTIVE" : "DEPRECATED");
        }
    }

    private interface Call {
        Result run(int index);
    }

    private static List<Result> parallel(int count, Call call) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            List<Callable<Result>> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                tasks.add(() -> call.run(index));
            }
            List<Result> results = new ArrayList<>();
            for (Future<Result> future : executor.invokeAll(tasks)) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
