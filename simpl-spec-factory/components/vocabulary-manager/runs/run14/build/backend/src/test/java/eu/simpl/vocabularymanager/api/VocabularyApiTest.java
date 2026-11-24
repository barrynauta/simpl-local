package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Lifecycle, list, versions, downloads and search (SIMPL-18162 to 18175, 18187, 18190, D3 to D8, D22 to D29). */
class VocabularyApiTest extends ApiTestBase {

    @Test
    void createAndVersionLifecycle() {
        String name = uniqueName("Life");
        Reply created = createVocabulary(name, turtle(name, "one"));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/1");
        JsonNode body = created.json();
        assertThat(body.path("version").asInt()).isEqualTo(1);
        assertThat(body.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.path("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.path("validationWarnings").isArray()).isTrue();
        assertThat(body.path("latestVersionHref").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.path("contentHref").asString())
                .isEqualTo("https://authority.example/vocabulary-manager/content/" + name + "/versions/1");

        Reply second = createVersion(name, "Second", turtle(name, "two"));
        assertThat(second.status()).isEqualTo(201);
        assertThat(second.json().path("version").asInt()).isEqualTo(2);

        Reply versions = get("/v1/vocabularies/" + name + "/versions", viewer);
        assertThat(versions.status()).isEqualTo(200);
        JsonNode items = versions.json().path("items");
        assertThat(items.get(0).path("version").asInt()).isEqualTo(2);
        assertThat(items.get(1).path("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(1).path("latestVersionHref").asString()).endsWith("/versions/2");

        Reply vocabulary = get("/v1/vocabularies/" + name, viewer);
        assertThat(vocabulary.json().path("version").asInt()).isEqualTo(2);
        assertThat(vocabulary.json().path("description").asString()).isEqualTo("Second");
        assertThat(vocabulary.json().path("href").asString()).isEqualTo("/vocabularies/" + name);

        Reply latest = get("/v1/content/" + name, viewer, "Accept", "text/turtle");
        assertThat(latest.status()).isEqualTo(200);
        assertThat(latest.header("Content-Type")).isEqualTo("text/turtle");
        assertThat(latest.header("Content-Disposition")).contains(name + ".ttl");
        assertThat(latest.text()).isEqualTo(turtle(name, "two"));
        Reply first = get("/v1/content/" + name + "/versions/1", viewer);
        assertThat(first.text()).isEqualTo(turtle(name, "one"));
        assertThat(first.header("Content-Disposition")).contains(name + "-v1.ttl");
        assertThat(get("/v1/content/" + name + "/versions/9", viewer).status()).isEqualTo(404);

        Reply identical = createVersion(name, "Second", turtle(name, "two"));
        assertThat(identical.status()).isEqualTo(409);
        assertThat(identical.json().path("detail").asString())
                .isEqualTo("Version 2 of vocabulary '" + name + "' already has this content and description.");
        assertThat(createVersion(name, "Only the description changes", turtle(name, "two"))
                        .status())
                .isEqualTo(201);
    }

    @Test
    void statusChangesFollowTheStateTables() {
        String name = uniqueName("Stat");
        createVocabulary(name, turtle(name, "one"));
        Reply same = json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"ACTIVE\"}");
        assertThat(same.status()).isEqualTo(409);
        assertThat(same.json().path("detail").asString()).isEqualTo("Vocabulary is already in status ACTIVE");
        Reply deprecated = json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.status()).isEqualTo(200);
        assertThat(deprecated.json().path("status").asString()).isEqualTo("DEPRECATED");
        Reply version = get("/v1/vocabularies/" + name + "/versions/1", viewer);
        assertThat(version.json().path("status").asString()).isEqualTo("ACTIVE");

        assertThat(createVersion(name, "Two", turtle(name, "two")).status()).isEqualTo(201);
        assertThat(get("/v1/vocabularies/" + name, viewer).json().path("status").asString())
                .isEqualTo("DEPRECATED");

        Reply reactivate = json("PATCH", "/v1/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(reactivate.status()).isEqualTo(200);
        assertThat(reactivate.json().path("status").asString()).isEqualTo("ACTIVE");
        Reply again = json("PATCH", "/v1/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(again.json().path("detail").asString()).isEqualTo("Vocabulary version is already in status ACTIVE");
        assertThat(json("PATCH", "/v1/vocabularies/" + name + "/versions/7", admin, "{\"status\":\"ACTIVE\"}")
                        .status())
                .isEqualTo(404);
        Reply bad = json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"RETIRED\"}");
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.json().path("issues").get(0).path("name").asString()).isEqualTo("status");
        assertThat(json("PATCH", "/v1/vocabularies/Unknown" + name, admin, "{\"status\":\"ACTIVE\"}")
                        .status())
                .isEqualTo(404);
    }

    @Test
    void listFiltersSortsAndPages() {
        String prefix = uniqueName("Lst");
        for (String suffix : List.of("Bb", "Aa", "Cc")) {
            createVocabulary(prefix + suffix, turtle(prefix + suffix, suffix));
        }
        json("PATCH", "/v1/vocabularies/" + prefix + "Cc", admin, "{\"status\":\"DEPRECATED\"}");
        String filter = prefix.toLowerCase();
        Reply page = get("/v1/vocabularies?name=" + filter + "&pageSize=2&sort=-name", viewer);
        JsonNode body = page.json();
        assertThat(body.path("total").asInt()).isEqualTo(3);
        assertThat(body.path("totalPages").asInt()).isEqualTo(2);
        assertThat(body.path("items").get(0).path("name").asString()).isEqualTo(prefix + "Cc");
        assertThat(body.path("self").asString())
                .isEqualTo("/vocabularies?page=1&pageSize=2&name=" + filter + "&sort=-name");
        assertThat(body.path("next").asString())
                .isEqualTo("/vocabularies?page=2&pageSize=2&name=" + filter + "&sort=-name");
        assertThat(body.has("prev")).isFalse();

        Reply beyond = get("/v1/vocabularies?name=" + filter + "&pageSize=2&page=7", viewer);
        assertThat(beyond.json().path("items").size()).isZero();
        assertThat(beyond.json().path("prev").asString()).contains("page=2&");

        Reply deprecated = get("/v1/vocabularies?name=" + filter + "&status=DEPRECATED", viewer);
        assertThat(deprecated.json().path("total").asInt()).isEqualTo(1);
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        for (String sort :
                List.of("name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated")) {
            assertThat(get("/v1/vocabularies?name=" + filter + "&sort=" + sort + "&creationDate=" + today, viewer)
                            .json()
                            .path("total")
                            .asInt())
                    .isEqualTo(3);
        }
        assertThat(get("/v1/vocabularies?name=" + filter + "&lastUpdated=2001-01-01T00:00:00Z", viewer)
                        .json()
                        .path("total")
                        .asInt())
                .isZero();
        Reply empty = get("/v1/vocabularies?name=" + filter + "Zz", viewer);
        assertThat(empty.json().has("last")).isFalse();
        assertThat(empty.json().path("totalPages").asInt()).isZero();

        Reply versions = get("/v1/vocabularies/" + prefix + "Aa/versions?sort=-status&page=1&pageSize=5", viewer);
        assertThat(versions.status()).isEqualTo(200);
        assertThat(get("/v1/vocabularies/" + prefix + "Aa/versions?page=3", viewer)
                        .json()
                        .path("items")
                        .size())
                .isZero();
        assertThat(get("/v1/vocabularies/" + prefix + "Aa/versions?sort=lastUpdated", viewer)
                        .status())
                .isEqualTo(400);
    }

    @Test
    void listParameterErrors() {
        for (String query : List.of(
                "page=0",
                "pageSize=101",
                "page=2.0",
                "page=+2",
                "sort=size",
                "status=active",
                "name=a-b",
                "creationDate=2026-02-30",
                "creationDate=2026-01-01T10:00:00+19:00",
                "foo=1",
                "page=1&page=2",
                "name=",
                "lastUpdated=yesterday")) {
            Reply reply = get("/v1/vocabularies?" + query, viewer);
            assertThat(reply.status()).as(query).isEqualTo(400);
            assertThat(reply.json().path("issues").get(0).path("in").asString()).isEqualTo("query");
        }
        assertThat(get("/v1/vocabularies?creationDate=2026-01-01T10:00:00%2B01:00", viewer)
                        .status())
                .isEqualTo(200);
        assertThat(get("/v1/vocabularies?creationDate=2026-01-01T10:00", viewer).status())
                .isEqualTo(200);
    }

    @Test
    void searchFollowsD38() {
        String name = uniqueName("Srch");
        createVocabulary(name, turtle(name, "one"));
        createVersion(name, "Two", turtle(name, "two"));
        Reply reply = json(
                "POST",
                "/v1/vocabularies/searches",
                viewer,
                "{\"items\":[{\"name\":\"" + name + "\"},{\"name\":\"" + name + "\",\"version\":1},{\"name\":\"" + name
                        + "\"},{\"name\":\"Missing" + name + "\"},{\"name\":\"" + name + "\",\"version\":9}]}");
        assertThat(reply.status()).isEqualTo(200);
        JsonNode body = reply.json();
        assertThat(body.path("total").asInt()).isEqualTo(2);
        assertThat(body.path("pageSize").asInt()).isEqualTo(100);
        assertThat(body.has("self")).isFalse();
        JsonNode latest = body.path("items").get(0);
        assertThat(latest.path("latest").asBoolean()).isTrue();
        assertThat(latest.path("version").asInt()).isEqualTo(2);
        assertThat(latest.path("href").asString()).isEqualTo("/vocabularies/" + name + "/versions/2");
        JsonNode first = body.path("items").get(1);
        assertThat(first.path("status").asString()).isEqualTo("DEPRECATED");
        assertThat(first.path("contentHref").asString()).endsWith("/content/" + name + "/versions/1");

        Reply none =
                json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Nothing" + name + "\"}]}");
        assertThat(none.json().path("totalPages").asInt()).isZero();
        assertThat(json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[]}")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("items");
        assertThat(json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Abc\",\"version\":1e1}]}")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("items[0].version");
        assertThat(json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Abc\",\"x\":1}]}")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("items[0].x");
        assertThat(json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[1]}")
                        .status())
                .isEqualTo(400);
        assertThat(json("POST", "/v1/vocabularies/searches", viewer, "[1]").status())
                .isEqualTo(400);
        assertThat(json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[],\"items\":[]}")
                        .status())
                .isEqualTo(400);
        assertThat(json("POST", "/v1/vocabularies/searches", viewer, "{not json")
                        .status())
                .isEqualTo(400);
        assertThat(send("POST", "/v1/vocabularies/searches", viewer, "application/json", new byte[0])
                        .status())
                .isEqualTo(400);
        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Abc\"}");
        }
        assertThat(json(
                                "POST",
                                "/v1/vocabularies/searches",
                                viewer,
                                many.append("]}").toString())
                        .status())
                .isEqualTo(400);
        assertThat(json("POST", "/v1/vocabularies/searches?x=1", viewer, "{\"items\":[{\"name\":\"Abc\"}]}")
                        .status())
                .isEqualTo(400);
    }

    @Test
    void createFieldRules() {
        String name = uniqueName("Fld");
        Reply missing = post("/v1/vocabularies", admin, new Form().field("name", name));
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.json().path("issues").size()).isEqualTo(3);
        Reply badName = post(
                "/v1/vocabularies",
                admin,
                new Form()
                        .field("name", "lower")
                        .field("description", " ")
                        .field("changelog", "c")
                        .file("x"));
        assertThat(badName.json().path("issues").size()).isEqualTo(2);
        Reply unknown = post(
                "/v1/vocabularies",
                admin,
                new Form()
                        .field("name", name)
                        .field("foo", "x")
                        .field("description", "d")
                        .field("changelog", "c")
                        .file("x"));
        assertThat(unknown.json().path("issues").get(0).path("name").asString()).isEqualTo("foo");
        Reply twice = post(
                "/v1/vocabularies",
                admin,
                new Form()
                        .field("name", name)
                        .field("name", name)
                        .field("description", "d")
                        .field("changelog", "c")
                        .file("x"));
        assertThat(twice.json().path("issues").get(0).path("name").asString()).isEqualTo("name");
        Form many = new Form();
        for (int i = 0; i < 65; i++) {
            many.field("f" + i, "x");
        }
        assertThat(post("/v1/vocabularies", admin, many)
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("body");
        Reply query = send("POST", "/v1/vocabularies?x=1", admin, new Form().contentType(), new Form().bytes());
        assertThat(query.status()).isEqualTo(400);
        assertThat(createVocabulary(name, turtle(name, "ok")).status()).isEqualTo(201);
        Reply duplicate = createVocabulary(name.toUpperCase(), "not turtle at all");
        assertThat(duplicate.status()).isEqualTo(409);
        Reply version = post(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                new Form()
                        .field("description", "d")
                        .field("changelog", "c")
                        .field("version", "3")
                        .file("x"));
        assertThat(version.json().path("issues").get(0).path("name").asString()).isEqualTo("version");
        assertThat(createVersion("Unknown" + name, "d", "x").status()).isEqualTo(404);
        Reply bigFile = post(
                "/v1/vocabularies",
                admin,
                new Form()
                        .field("name", name + "B")
                        .field("description", "d")
                        .field("changelog", "c")
                        .file("vocabularyFile", new byte[5 * 1024 * 1024 + 1]));
        assertThat(bigFile.status()).isEqualTo(413);
        assertThat(bigFile.json().path("issues").get(0).path("name").asString()).isEqualTo("vocabularyFile");
        Reply bigRequest = post("/v1/vocabularies", admin, new Form().file("other", new byte[6 * 1024 * 1024 + 10]));
        assertThat(bigRequest.status()).isEqualTo(413);
        String longText = "x".repeat(1001);
        assertThat(createVersion(name, longText, "x").status()).isEqualTo(400);
        assertThat(new String(new byte[0], StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void parallelVersionsGetDistinctNumbers() throws Exception {
        String name = uniqueName("Par");
        createVocabulary(name, turtle(name, "base"));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Reply>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int n = i;
            futures.add(pool.submit(() -> createVersion(name, "parallel " + n, turtle(name, "v" + n))));
        }
        List<Integer> numbers = new ArrayList<>();
        for (Future<Reply> future : futures) {
            Reply reply = future.get();
            assertThat(reply.status()).isEqualTo(201);
            numbers.add(reply.json().path("version").asInt());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5);
        JsonNode items =
                get("/v1/vocabularies/" + name + "/versions", viewer).json().path("items");
        assertThat(items.get(0).path("status").asString()).isEqualTo("ACTIVE");
        for (int i = 1; i < items.size(); i++) {
            assertThat(items.get(i).path("status").asString()).isEqualTo("DEPRECATED");
        }
    }
}
