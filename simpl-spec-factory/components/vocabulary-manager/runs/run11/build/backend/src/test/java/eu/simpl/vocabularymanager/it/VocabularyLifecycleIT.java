package eu.simpl.vocabularymanager.it;

import static eu.simpl.vocabularymanager.it.TestEnvironment.admin;
import static eu.simpl.vocabularymanager.it.TestEnvironment.viewer;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Lifecycle of vocabularies and versions (SIMPL-18170, 18173, 18175, 18187, 18190, 18191; D3 to D8, D43, D46). */
class VocabularyLifecycleIT extends IntegrationTest {

    @Test
    void createsVersionOneActiveWithLocationAndCreator() {
        String name = uniqueName("Life");
        ApiClient.Result created = create(name, ttl(name));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/1");
        JsonNode body = created.json();
        assertThat(body.get("version").asInt()).isEqualTo(1);
        assertThat(body.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.get("validationWarnings").isArray()).isTrue();
        assertThat(body.get("latestVersionHref").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.get("contentHref").asString()).isEqualTo(CONTENT_BASE + "/content/" + name + "/versions/1");

        JsonNode vocabulary = api.get("/v1/vocabularies/" + name, viewer()).json();
        assertThat(vocabulary.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(vocabulary.get("href").asString()).isEqualTo("/vocabularies/" + name);
    }

    @Test
    void newVersionDeprecatesOlderActiveVersionsAndDownloadsBytesAsUploaded() {
        String name = uniqueName("Ver");
        create(name, ttl(name));
        String second = ttl(name) + "# second\r\n";
        ApiClient.Result created = newVersion(name, "Second", second);
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.json().get("version").asInt()).isEqualTo(2);
        assertThat(api.get("/v1/vocabularies/" + name + "/versions/1", viewer())
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("DEPRECATED");
        JsonNode vocabulary = api.get("/v1/vocabularies/" + name, viewer()).json();
        assertThat(vocabulary.get("version").asInt()).isEqualTo(2);
        assertThat(vocabulary.get("description").asString()).isEqualTo("Second");

        ApiClient.Result latest = api.get("/v1/content/" + name, viewer());
        assertThat(latest.header("Content-Type")).isEqualTo("text/turtle");
        assertThat(latest.header("Content-Disposition")).isEqualTo("attachment; filename=\"" + name + ".ttl\"");
        assertThat(latest.text()).isEqualTo(second);
        ApiClient.Result first = api.get("/v1/content/" + name + "/versions/1", viewer(), "Accept", "text/turtle");
        assertThat(first.header("Content-Disposition")).contains(name + "-v1.ttl");
        assertThat(first.text()).isEqualTo(ttl(name));

        JsonNode versions =
                api.get("/v1/vocabularies/" + name + "/versions", viewer()).json();
        assertThat(versions.get("items").get(0).get("version").asInt()).isEqualTo(2);
        assertThat(versions.get("total").asLong()).isEqualTo(2);
    }

    @Test
    void identicalVersionIsRefusedAndDescriptionOnlyChangeIsAccepted() {
        String name = uniqueName("Same");
        create(name, ttl(name));
        ApiClient.Result same = newVersion(name, "Description of " + name, ttl(name));
        assertThat(same.status()).isEqualTo(409);
        assertThat(same.json().get("detail").asString())
                .isEqualTo("Version 1 of vocabulary '" + name + "' already has this content and description.");
        assertThat(newVersion(name, "Other text", ttl(name)).status()).isEqualTo(201);
    }

    @Test
    void duplicateNameIsAConflictCaseInsensitivelyEvenWithInvalidContent() {
        String name = uniqueName("Dup");
        create(name, ttl(name));
        ApiClient.Result again =
                create(name.toUpperCase().charAt(0) + name.substring(1).toLowerCase(), "not turtle");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.type()).isEqualTo("urn:problem-type:simpl:conflict");
    }

    @Test
    void statusChangesFollowTheStateTable() {
        String name = uniqueName("Stat");
        create(name, ttl(name));
        ApiClient.Result same = api.patch("/v1/vocabularies/" + name, admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(same.status()).isEqualTo(409);
        assertThat(same.json().get("detail").asString()).isEqualTo("Vocabulary is already in status ACTIVE");
        ApiClient.Result deprecated = api.patch("/v1/vocabularies/" + name, admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.status()).isEqualTo(200);
        assertThat(deprecated.json().get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(api.get("/v1/vocabularies/" + name + "/versions/1", viewer())
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("ACTIVE");
        assertThat(newVersion(name, "Next", ttl(name) + "#2").status()).isEqualTo(201);
        assertThat(api.get("/v1/vocabularies/" + name, viewer())
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("DEPRECATED");
        ApiClient.Result bad = api.patch("/v1/vocabularies/" + name, admin(), "{\"status\":\"RETIRED\"}");
        assertThat(bad.status()).isEqualTo(400);

        ApiClient.Result version =
                api.patch("/v1/vocabularies/" + name + "/versions/1", admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(version.status()).isEqualTo(200);
        ApiClient.Result versionSame =
                api.patch("/v1/vocabularies/" + name + "/versions/1", admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(versionSame.json().get("detail").asString())
                .isEqualTo("Vocabulary version is already in status ACTIVE");
        ApiClient.Result missing =
                api.patch("/v1/vocabularies/" + name + "/versions/9", admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.json().get("detail").asString())
                .isEqualTo("Version 9 of vocabulary '" + name + "' was not found.");
    }

    @Test
    void parallelNewVersionsAreSerialisedThroughTheStore() throws Exception {
        String name = uniqueName("Par");
        create(name, ttl(name));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<ApiClient.Result>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int n = i;
            results.add(pool.submit(() -> newVersion(name, "Parallel " + n, ttl(name) + "# " + n)));
        }
        Set<String> locations = new TreeSet<>();
        for (Future<ApiClient.Result> result : results) {
            ApiClient.Result r = result.get();
            assertThat(r.status()).isEqualTo(201);
            locations.add(r.header("Location"));
        }
        pool.shutdown();
        assertThat(locations).hasSize(4);
        JsonNode versions = api.get("/v1/vocabularies/" + name + "/versions?pageSize=100", viewer())
                .json();
        assertThat(versions.get("total").asLong()).isEqualTo(5);
        long active = 0;
        for (JsonNode v : versions.get("items")) {
            if ("ACTIVE".equals(v.get("status").asString())) {
                active++;
                assertThat(v.get("version").asInt()).isEqualTo(5);
            }
        }
        assertThat(active).isEqualTo(1);
    }

    @Test
    void notFoundTextsAndPathRules() {
        ApiClient.Result missing = api.get("/v1/vocabularies/Missing" + System.nanoTime(), viewer());
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.json().get("issues").get(0).get("name").asString()).isEqualTo("vocabularyName");
        ApiClient.Result badName = api.get("/v1/vocabularies/lower", viewer());
        assertThat(badName.status()).isEqualTo(400);
        assertThat(badName.json().get("issues").get(0).get("in").asString()).isEqualTo("path");
        assertThat(api.get("/v1/vocabularies/Abc/versions/0", viewer()).status())
                .isEqualTo(400);
        assertThat(api.get("/v1/vocabularies/Abc/versions/2.0", viewer()).status())
                .isEqualTo(400);
        assertThat(api.get("/v1/content/Missing" + System.nanoTime(), viewer()).status())
                .isEqualTo(404);
        ApiClient.Result versionMissing = newVersion("Missing" + System.nanoTime(), "x", "x");
        assertThat(versionMissing.status()).isEqualTo(404);
    }
}
