package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.VocabularyManagerApplication;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.JsonNode;

/**
 * D82, D102: two service instances on one store. Parallel new versions through both instances get
 * distinct numbers; only the newest stays ACTIVE; overlapping creates of one name give one 201.
 */
class TwoInstancesTest extends ApiTestBase {

    static ConfigurableApplicationContext second;
    static int secondPort;

    @BeforeAll
    static void startSecond() {
        String base = "http://localhost:" + FUSEKI.getPort() + "/vocabulary";
        second = new SpringApplicationBuilder(VocabularyManagerApplication.class)
                .run(
                        "--server.port=0",
                        "--management.server.port=0",
                        "--vm.store.query-url=" + base + "/query",
                        "--vm.store.update-url=" + base + "/update",
                        "--vm.auth.issuer=" + TestIdp.ISSUER,
                        "--vm.auth.jwks-uri=" + IDP.jwksUri());
        secondPort = Integer.parseInt(second.getEnvironment().getProperty("local.server.port"));
    }

    @AfterAll
    static void stopSecond() {
        second.close();
    }

    @Test
    void versionsThroughTwoInstances() throws Exception {
        String name = unique("Two");
        assertThat(createVocabulary(name, ttl("ex:a a skos:Concept .\n")).statusCode())
                .isEqualTo(201);
        int firstPort = port;
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int n = i;
            futures.add(pool.submit(() -> {
                PORT_OVERRIDE.set(n % 2 == 0 ? firstPort : secondPort);
                return createVersion(name, "Instance " + n, ttl("ex:i" + n + " a skos:Concept .\n"));
            }));
        }
        List<Integer> versions = new ArrayList<>();
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).isEqualTo(201);
            versions.add(body(r).get("version").asInt());
        }
        assertThat(versions).containsExactlyInAnyOrder(2, 3, 4, 5);
        JsonNode items =
                body(get("/v1/vocabularies/" + name + "/versions", admin)).get("items");
        long active = 0;
        for (JsonNode n : items) {
            active += "ACTIVE".equals(n.get("status").asString()) ? 1 : 0;
        }
        assertThat(active).isEqualTo(1);

        String same = unique("Same");
        List<Future<HttpResponse<String>>> creates = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int target = i == 0 ? port : secondPort;
            creates.add(pool.submit(() -> {
                PORT_OVERRIDE.set(target);
                return createVocabulary(same, ttl("ex:s a skos:Concept .\n"));
            }));
        }
        List<Integer> codes = new ArrayList<>();
        for (Future<HttpResponse<String>> f : creates) {
            codes.add(f.get().statusCode());
        }
        pool.shutdown();
        assertThat(codes).containsExactlyInAnyOrder(201, 409);
    }
}
