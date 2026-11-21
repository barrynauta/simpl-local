package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Main flows of the vocabulary operations against the real service and an in-process store. */
class VocabularyApiTest extends ApiTestBase {

    @Test
    void createVersionListDownloadSearch() throws Exception {
        String name = unique("Flow");
        HttpResponse<String> c = createVocabulary(name, ttl("ex:a a skos:Concept .\n"));
        assertThat(c.statusCode()).isEqualTo(201);
        assertThat(c.headers().firstValue("Location")).contains("/vocabularies/" + name + "/versions/1");
        JsonNode b = body(c);
        assertThat(b.get("version").asInt()).isEqualTo(1);
        assertThat(b.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(b.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(b.get("validationWarnings").isArray()).isTrue();
        assertThat(b.get("validationWarnings").size()).isZero();
        assertThat(b.get("latestVersionHref").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");

        HttpResponse<String> v2 = createVersion(name, "Second", ttl("ex:b a skos:Concept .\n"));
        assertThat(v2.statusCode()).isEqualTo(201);
        assertThat(body(v2).get("version").asInt()).isEqualTo(2);

        HttpResponse<String> one = get("/v1/vocabularies/" + name, viewer);
        assertThat(one.statusCode()).isEqualTo(200);
        assertThat(body(one).get("version").asInt()).isEqualTo(2);
        assertThat(body(one).get("description").asString()).isEqualTo("Second");
        assertThat(body(one).get("createdBy").asString()).isEqualTo("ga-admin@example.com");

        JsonNode versions = body(get("/v1/vocabularies/" + name + "/versions", viewer));
        assertThat(versions.get("items").get(0).get("version").asInt()).isEqualTo(2);
        assertThat(versions.get("items").get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(versions.get("items").get(1).get("latestVersionHref").asString())
                .endsWith("/versions/2");

        HttpResponse<String> v1 = get("/v1/vocabularies/" + name + "/versions/1", viewer);
        assertThat(body(v1).get("changelog").asString()).isEqualTo("Initial version.");

        HttpResponse<String> dl = get("/v1/content/" + name, viewer);
        assertThat(dl.statusCode()).isEqualTo(200);
        assertThat(dl.headers().firstValue("Content-Type")).contains("text/turtle");
        assertThat(dl.headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"" + name + ".ttl\"");
        assertThat(dl.body()).contains("ex:b");
        HttpResponse<String> dl1 = get("/v1/content/" + name + "/versions/1", viewer);
        assertThat(dl1.headers().firstValue("Content-Disposition"))
                .get()
                .asString()
                .contains(name + "-v1.ttl");
        assertThat(dl1.body().getBytes(StandardCharsets.UTF_8)).isEqualTo(ttl("ex:a a skos:Concept .\n"));

        HttpResponse<String> s = json(
                "POST",
                "/v1/vocabularies/searches",
                viewer,
                "{\"items\":[{\"name\":\""
                        + name
                        + "\"},{\"name\":\""
                        + name
                        + "\",\"version\":1},{\"name\":\""
                        + name
                        + "\"},{\"name\":\"Missing"
                        + name
                        + "\"}]}");
        assertThat(s.statusCode()).isEqualTo(200);
        JsonNode sb = body(s);
        assertThat(sb.get("total").asInt()).isEqualTo(2);
        assertThat(sb.get("totalPages").asInt()).isEqualTo(1);
        assertThat(sb.has("self")).isFalse();
        assertThat(sb.get("items").get(0).get("latest").asBoolean()).isTrue();
        assertThat(sb.get("items").get(1).get("status").asString()).isEqualTo("DEPRECATED");
    }

    @Test
    void statusChangesAndConflicts() throws Exception {
        String name = unique("Status");
        createVocabulary(name, ttl("ex:a a skos:Concept .\n"));
        HttpResponse<String> p = json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(p.statusCode()).isEqualTo(200);
        assertThat(body(p).get("status").asString()).isEqualTo("DEPRECATED");
        HttpResponse<String> same = json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(same.statusCode()).isEqualTo(409);
        assertThat(body(same).get("detail").asString()).isEqualTo("Vocabulary is already in status DEPRECATED");
        assertThat(body(same).get("type").asString()).isEqualTo("urn:problem-type:simpl:conflict");
        // a new version of a DEPRECATED vocabulary keeps the vocabulary status (D3)
        createVersion(name, "Second", ttl("ex:b a skos:Concept .\n"));
        assertThat(body(get("/v1/vocabularies/" + name, admin)).get("status").asString())
                .isEqualTo("DEPRECATED");
        HttpResponse<String> bad = json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"X\"}");
        assertThat(bad.statusCode()).isEqualTo(400);
        HttpResponse<String> vp =
                json("PATCH", "/v1/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(vp.statusCode()).isEqualTo(200);
        assertThat(body(vp).get("status").asString()).isEqualTo("ACTIVE");
        HttpResponse<String> vsame =
                json("PATCH", "/v1/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(body(vsame).get("detail").asString()).isEqualTo("Vocabulary version is already in status ACTIVE");
        HttpResponse<String> v9 =
                json("PATCH", "/v1/vocabularies/" + name + "/versions/9", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(v9.statusCode()).isEqualTo(404);
        assertThat(body(v9).get("detail").asString())
                .isEqualTo("Version 9 of vocabulary '" + name + "' was not found.");
        // next version deprecates both ACTIVE versions (D4)
        createVersion(name, "Third", ttl("ex:c a skos:Concept .\n"));
        JsonNode items = body(get("/v1/vocabularies/" + name + "/versions?sort=status", admin))
                .get("items");
        assertThat(items.get(0).get("version").asInt()).isEqualTo(3);
        assertThat(items.get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(2).get("status").asString()).isEqualTo("DEPRECATED");
    }

    @Test
    void duplicateNameAndIdenticalVersion() throws Exception {
        String name = unique("Dup");
        byte[] content = ttl("ex:a a skos:Concept .\n");
        createVocabulary(name, content);
        HttpResponse<String> dup = createVocabulary(name.toUpperCase(), "not turtle".getBytes());
        assertThat(dup.statusCode()).isEqualTo(409);
        HttpResponse<String> same = multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                fields("description", "Description of " + name, "changelog", "Other."),
                content);
        assertThat(same.statusCode()).isEqualTo(409);
        assertThat(body(same).get("detail").asString())
                .isEqualTo("Version 1 of vocabulary '" + name + "' already has this content and description.");
        HttpResponse<String> descOnly = multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                fields("description", "New description", "changelog", "Other."),
                content);
        assertThat(descOnly.statusCode()).isEqualTo(201);
    }

    @Test
    void validationReportAndWarnings() throws Exception {
        String base = unique("Ref");
        createVocabulary(base, ttl("ex:a a skos:Concept .\n"));
        createVersion(base, "Second", ttl("ex:b a skos:Concept .\n"));
        byte[] bad = ttl("ex:p a owl:ObjectProperty . ex:d a owl:DatatypeProperty .\n"
                + "ex:s ex:p \"x\" . ex:s ex:d ex:o .\n"
                + "@prefix nope: <https://authority.example/vocabulary-manager/content/Nope"
                + base
                + "#> .\n");
        HttpResponse<String> r = createVocabulary(unique("Bad"), bad);
        assertThat(r.statusCode()).isEqualTo(400);
        JsonNode issues = body(r).get("issues");
        assertThat(issues.size()).isEqualTo(3);
        assertThat(issues.get(0).get("detail").asString()).isEqualTo("Literal value for object property");
        assertThat(issues.get(2).get("detail").asString())
                .isEqualTo("Referenced internal vocabulary 'Nope" + base + "' does not exist.");

        HttpResponse<String> syntax = createVocabulary(unique("Syn"), "@prefix x <a> .".getBytes());
        assertThat(syntax.statusCode()).isEqualTo(400);
        assertThat(body(syntax).get("issues").get(0).get("detail").asString())
                .startsWith("Encountered invalid Turtle syntax");
        assertThat(body(syntax).get("issues").get(0).get("location").asString()).startsWith("line 1");

        byte[] refs = ttl("@prefix old: <https://authority.example/vocabulary-manager/content/"
                + base
                + "/versions/1#> .\n@prefix dc: <http://purl.org/dc/terms/> .\nex:x a skos:Concept .\n");
        HttpResponse<String> ok = createVocabulary(unique("Warn"), refs);
        assertThat(ok.statusCode()).isEqualTo(201);
        JsonNode w = body(ok).get("validationWarnings");
        assertThat(w.size()).isEqualTo(2);
        assertThat(w.get(0).get("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(w.get(0).get("description").asString())
                .isEqualTo("Referenced internal vocabulary '"
                        + base
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(w.get(1).get("code").asString()).isEqualTo("externalReferenceSkipped");

        json("PATCH", "/v1/vocabularies/" + base, admin, "{\"status\":\"DEPRECATED\"}");
        HttpResponse<String> dep = createVocabulary(unique("Warn"), refs);
        assertThat(body(dep).get("validationWarnings").get(0).get("description").asString())
                .endsWith(" The vocabulary '" + base + "' is deprecated.");

        HttpResponse<String> empty = createVocabulary(unique("Empty"), new byte[0]);
        assertThat(body(empty).get("issues").get(0).get("location").asString()).isEqualTo("document");
    }

    @Test
    void listFiltersSortAndPaging() throws Exception {
        String prefix = unique("List");
        for (String s : List.of("Bb", "Aa", "Cc")) {
            createVocabulary(prefix + s, ttl("ex:a a skos:Concept .\n"));
        }
        json("PATCH", "/v1/vocabularies/" + prefix + "Bb", admin, "{\"status\":\"DEPRECATED\"}");
        JsonNode page = body(get("/v1/vocabularies?name=" + prefix.toLowerCase() + "&pageSize=2&sort=-name", viewer));
        assertThat(page.get("total").asInt()).isEqualTo(3);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("items").get(0).get("name").asString()).isEqualTo(prefix + "Cc");
        assertThat(page.get("next").asString())
                .isEqualTo("/vocabularies?page=2&pageSize=2&name=" + prefix.toLowerCase() + "&sort=-name");
        assertThat(page.has("prev")).isFalse();
        JsonNode beyond = body(get("/v1/vocabularies?name=" + prefix + "&page=9&pageSize=2", viewer));
        assertThat(beyond.get("items").size()).isZero();
        assertThat(beyond.get("prev").asString()).startsWith("/vocabularies?page=2&pageSize=2");
        JsonNode deprecated = body(get("/v1/vocabularies?name=" + prefix + "&status=DEPRECATED", viewer));
        assertThat(deprecated.get("total").asInt()).isEqualTo(1);
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        JsonNode created = body(get("/v1/vocabularies?name=" + prefix + "&creationDate=" + today, viewer));
        assertThat(created.get("total").asInt()).isEqualTo(3);
        JsonNode none = body(get("/v1/vocabularies?name=" + prefix + "&lastUpdated=2001-01-01", viewer));
        assertThat(none.get("total").asInt()).isZero();
        assertThat(none.has("last")).isFalse();
        JsonNode byStatus = body(get("/v1/vocabularies?name=" + prefix + "&sort=status", viewer));
        assertThat(byStatus.get("items").get(2).get("status").asString()).isEqualTo("DEPRECATED");
        for (String sort : List.of("creationDate", "-lastUpdated", "lastUpdated", "-status", "name")) {
            assertThat(get("/v1/vocabularies?name=" + prefix + "&sort=" + sort, viewer)
                            .statusCode())
                    .isEqualTo(200);
        }
    }

    @Test
    void parallelNewVersionsGetDistinctNumbers() throws Exception {
        String name = unique("Par");
        createVocabulary(name, ttl("ex:a a skos:Concept .\n"));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int n = i;
            Callable<HttpResponse<String>> c =
                    () -> createVersion(name, "Parallel " + n, ttl("ex:p" + n + " a skos:Concept .\n"));
            futures.add(pool.submit(c));
        }
        List<Integer> versions = new ArrayList<>();
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).isEqualTo(201);
            versions.add(body(r).get("version").asInt());
        }
        pool.shutdown();
        assertThat(versions).containsExactlyInAnyOrder(2, 3, 4, 5);
        JsonNode items =
                body(get("/v1/vocabularies/" + name + "/versions", admin)).get("items");
        assertThat(items.get(0).get("status").asString()).isEqualTo("ACTIVE");
        for (int i = 1; i < 5; i++) {
            assertThat(items.get(i).get("status").asString()).isEqualTo("DEPRECATED");
        }
    }

    @Test
    void requestShapeErrors() throws Exception {
        String name = unique("Shape");
        createVocabulary(name, ttl("ex:a a skos:Concept .\n"));
        HttpResponse<String> version = multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                fields("description", "d", "changelog", "c", "version", "7"),
                ttl("ex:z a skos:Concept .\n"));
        assertThat(version.statusCode()).isEqualTo(400);
        assertThat(body(version).get("issues").get(0).get("name").asString()).isEqualTo("version");
        HttpResponse<String> missing = multipart("/v1/vocabularies", admin, fields("name", "Abc"), null);
        assertThat(missing.statusCode()).isEqualTo(400);
        assertThat(body(missing).get("issues").size()).isEqualTo(3);
        HttpResponse<String> badName = multipart(
                "/v1/vocabularies", admin, fields("name", "ab", "description", "d", "changelog", "c"), ttl(""));
        assertThat(body(badName).get("issues").get(0).get("name").asString()).isEqualTo("name");
        HttpResponse<String> query = multipart(
                "/v1/vocabularies?x=1", admin, fields("name", "Abc", "description", "d", "changelog", "c"), ttl(""));
        assertThat(body(query).get("issues").get(0).get("in").asString()).isEqualTo("query");
        HttpResponse<String> unknownVoc = createVersion("Unknown" + name, "d", ttl(""));
        assertThat(unknownVoc.statusCode()).isEqualTo(404);
        HttpResponse<String> blank = multipart(
                "/v1/vocabularies", admin, fields("name", "Abcd", "description", "   ", "changelog", "c"), ttl(""));
        assertThat(body(blank).get("issues").get(0).get("name").asString()).isEqualTo("description");
        for (String q : List.of(
                "page=0",
                "pageSize=101",
                "page=1.0",
                "status=X",
                "sort=x",
                "name=a-b",
                "creationDate=2026-02-30",
                "lastUpdated=2026-01-01T00:00:00+19:00",
                "page=1&page=2",
                "foo=1")) {
            assertThat(get("/v1/vocabularies?" + q, viewer).statusCode()).as(q).isEqualTo(400);
        }
        for (String q : List.of(
                "creationDate=2026-01-01T00:00:00",
                "creationDate=2026-01-01T00:00:00Z",
                "lastUpdated=2026-01-01T00:00:00.5+02:00")) {
            assertThat(get("/v1/vocabularies?" + q, viewer).statusCode()).as(q).isEqualTo(200);
        }
        assertThat(get("/v1/vocabularies/" + name + "/versions?sort=lastUpdated", viewer)
                        .statusCode())
                .isEqualTo(400);
        assertThat(get("/v1/vocabularies/" + name + "/versions/x", viewer).statusCode())
                .isEqualTo(400);
        assertThat(get("/v1/vocabularies/" + name + "/versions/7", viewer).statusCode())
                .isEqualTo(404);
        assertThat(get("/v1/content/" + name + "/versions/7", viewer).statusCode())
                .isEqualTo(404);
        assertThat(get("/v1/content/x", viewer).statusCode()).isEqualTo(400);
        assertThat(get("/v1/vocabularies/" + name + "?x=1", viewer).statusCode())
                .isEqualTo(400);
        assertThat(json("PATCH", "/v1/vocabularies/" + name, admin, "[]").statusCode())
                .isEqualTo(400);
        assertThat(json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"ACTIVE\",\"x\":1}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(json("PATCH", "/v1/vocabularies/" + name, admin, "{").statusCode())
                .isEqualTo(400);
        assertThat(json("PATCH", "/v1/vocabularies/" + name, admin, "").statusCode())
                .isEqualTo(400);
        assertThat(json("PATCH", "/v1/vocabularies/Missing" + name, admin, "{\"status\":\"ACTIVE\"}")
                        .statusCode())
                .isEqualTo(404);
        HttpResponse<String> big = send(
                "PATCH",
                "/v1/vocabularies/" + name,
                admin,
                Map.of("Content-Type", "application/json"),
                HttpRequest.BodyPublishers.ofString("{\"status\":\"" + "x".repeat(1_100_000) + "\"}"));
        assertThat(big.statusCode()).isEqualTo(413);
        assertThat(body(big).get("detail").asString()).isEqualTo("The request body is larger than 1 MB.");
        List<String[]> manyParts = new ArrayList<>(fields("name", "Abcd", "description", "d", "changelog", "c"));
        for (int i = 0; i < 70; i++) {
            manyParts.add(new String[] {"p" + i, "x"});
        }
        assertThat(multipart("/v1/vocabularies", admin, manyParts, ttl("")).statusCode())
                .isEqualTo(400);
        List<String[]> bigRequest = new ArrayList<>(fields("name", "Abcd", "description", "d", "changelog", "c"));
        bigRequest.add(new String[] {"other", "x".repeat(2_600_000)});
        assertThat(multipart("/v1/vocabularies", admin, bigRequest, new byte[4_000_000])
                        .statusCode())
                .isEqualTo(413);
        HttpResponse<String> tooLarge = createVocabulary(unique("Big"), new byte[5 * 1024 * 1024 + 1]);
        assertThat(tooLarge.statusCode()).isEqualTo(413);
    }

    @Test
    void searchValidation() throws Exception {
        for (String b : List.of(
                "[]",
                "{}",
                "{\"items\":[]}",
                "{\"items\":[{\"name\":\"ab\"}]}",
                "{\"items\":[{\"name\":\"Abc\",\"version\":1.5}]}",
                "{\"items\":[{\"name\":\"Abc\",\"x\":1}]}",
                "{\"items\":[1]}",
                "{\"items\":[{\"name\":\"Abc\"}],\"x\":1}")) {
            HttpResponse<String> r = json("POST", "/v1/vocabularies/searches", viewer, b);
            assertThat(r.statusCode()).as(b).isEqualTo(400);
        }
        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Abc\"}");
        }
        many.append("]}");
        HttpResponse<String> r = json("POST", "/v1/vocabularies/searches", viewer, many.toString());
        assertThat(body(r).get("issues").get(0).get("name").asString()).isEqualTo("items");
        HttpResponse<String> v =
                json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Abc\",\"version\":0}]}");
        assertThat(body(v).get("issues").get(0).get("name").asString()).isEqualTo("items[0].version");
        HttpResponse<String> empty = json(
                "POST", "/v1/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Nothing" + unique("X") + "\"}]}");
        assertThat(body(empty).get("totalPages").asInt()).isZero();
    }
}
