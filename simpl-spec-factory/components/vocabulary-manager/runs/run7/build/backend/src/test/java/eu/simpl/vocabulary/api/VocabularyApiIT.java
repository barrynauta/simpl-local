package eu.simpl.vocabulary.api;

import static eu.simpl.vocabulary.support.Http.field;
import static eu.simpl.vocabulary.support.Http.file;
import static eu.simpl.vocabulary.support.Http.parts;
import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabulary.support.Http;
import eu.simpl.vocabulary.support.TestKeyConfig;
import eu.simpl.vocabulary.support.TestTokens;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** End-to-end behaviour of the vocabulary operations against the real server and store. */
class VocabularyApiIT extends AbstractApiIT {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final String ADMIN = TestTokens.admin();
    static final String VIEWER = TestTokens.viewer();
    static final String BASE = "https://authority.example/vocabulary-manager";
    static final String VALID = """
            @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
            @prefix ex: <https://example.eu/vocab#> .
            ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Scheme"@en .
            """;

    static JsonNode body(HttpResponse<String> response) {
        return JSON.readTree(response.body());
    }

    HttpResponse<String> create(String name, String content) {
        return http.multipart(
                "/v1/vocabularies",
                ADMIN,
                parts(
                        field("name", name),
                        field("description", "Description of " + name),
                        field("changelog", "Initial version."),
                        file("vocabularyFile", content)));
    }

    HttpResponse<String> newVersion(String name, String description, String content) {
        return http.multipart(
                "/v1/vocabularies/" + name + "/versions",
                ADMIN,
                parts(
                        field("description", description),
                        field("changelog", "Changed"),
                        file("vocabularyFile", content)));
    }

    static List<String> issueNames(HttpResponse<String> response) {
        List<String> names = new ArrayList<>();
        body(response).get("issues").forEach(i -> names.add(i.get("name").asString()));
        return names;
    }

    @Test
    void createsVocabularyWithVersionOneActive() {
        String name = unique("Create");
        HttpResponse<String> response = create(name, VALID);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location")).contains("/vocabularies/" + name + "/versions/1");
        JsonNode created = body(response);
        assertThat(created.get("version").asInt()).isEqualTo(1);
        assertThat(created.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(created.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(created.get("contentHref").asString()).isEqualTo(BASE + "/content/" + name + "/versions/1");
        assertThat(created.get("validationWarnings").isArray()).isTrue();
        assertThat(created.get("validationWarnings").size()).isZero();
        assertThat(created.get("creationDate").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

        JsonNode vocabulary = body(http.get("/v1/vocabularies/" + name, VIEWER));
        assertThat(vocabulary.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(vocabulary.get("version").asInt()).isEqualTo(1);
        assertThat(vocabulary.get("href").asString()).isEqualTo("/vocabularies/" + name);
        assertThat(http.get("/v1/vocabularies/" + name.toUpperCase(), VIEWER).statusCode())
                .isEqualTo(404);
    }

    @Test
    void creatorFallsBackToUsername() {
        String name = unique("Creator");
        HttpResponse<String> response = http.multipart(
                "/v1/vocabularies",
                TestTokens.withoutEmail(),
                parts(
                        field("name", name),
                        field("description", "d"),
                        field("changelog", "c"),
                        file("vocabularyFile", VALID)));
        assertThat(body(response).get("createdBy").asString()).isEqualTo("plain-user");
    }

    @Test
    void duplicateNameIsConflictEvenWithInvalidContent() {
        String name = unique("Dup");
        create(name, VALID);
        HttpResponse<String> response = create(
                name.toLowerCase().substring(0, 1).toUpperCase()
                        + name.substring(1).toLowerCase(),
                "not turtle");
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(body(response).get("type").asString()).isEqualTo("urn:problem-type:simpl:conflict");
    }

    @Test
    void requestShapeErrorsAreReportedTogether() {
        HttpResponse<String> response = http.multipart(
                "/v1/vocabularies?x=1",
                ADMIN,
                parts(field("name", "lower"), field("description", "   "), field("extra", "e"), field("", "empty")));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(issueNames(response))
                .contains("x", "name", "description", "changelog", "vocabularyFile", "extra", "");
    }

    @Test
    void syntaxErrorStopsBeforeSemanticValidation() {
        HttpResponse<String> response = create(unique("Syntax"), "@prefix ex: <http://e.org/> .\nex:a ex:b .\n");
        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode issue = body(response).get("issues").get(0);
        assertThat(body(response).get("issues").size()).isEqualTo(1);
        assertThat(issue.get("detail").asString()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issue.get("location").asString()).startsWith("line 2");
        assertThat(issue.get("name").asString()).isEqualTo("vocabularyFile");
        assertThat(issue.get("type").asString()).isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
    }

    @Test
    void emptyFileFailsWithNoStatements() {
        HttpResponse<String> response = http.multipart(
                "/v1/vocabularies",
                ADMIN,
                parts(
                        field("name", unique("Empty")),
                        field("description", "d"),
                        field("changelog", "c"),
                        file("vocabularyFile", new byte[0])));
        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode issue = body(response).get("issues").get(0);
        assertThat(issue.get("detail").asString()).isEqualTo("Vocabulary contains no statements");
        assertThat(issue.get("location").asString()).isEqualTo("document");
    }

    @Test
    void newVersionDeprecatesOlderActiveVersions() {
        String name = unique("Versions");
        create(name, VALID);
        HttpResponse<String> second = newVersion(name, "Second", VALID);
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(body(second).get("version").asInt()).isEqualTo(2);
        JsonNode versions = body(http.get("/v1/vocabularies/" + name + "/versions", VIEWER));
        assertThat(versions.get("total").asInt()).isEqualTo(2);
        assertThat(versions.get("items").get(0).get("version").asInt()).isEqualTo(2);
        assertThat(versions.get("items").get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(versions.get("items").get(1).get("latestVersionHref").asString())
                .isEqualTo("/vocabularies/" + name + "/versions/2");
        assertThat(body(http.get("/v1/vocabularies/" + name, VIEWER))
                        .get("description")
                        .asString())
                .isEqualTo("Second");

        HttpResponse<String> identical = newVersion(name, " Second ", VALID);
        assertThat(identical.statusCode()).isEqualTo(409);
        assertThat(body(identical).get("detail").asString())
                .isEqualTo("Version 2 of vocabulary '" + name + "' already has this content and description.");

        HttpResponse<String> withVersion = http.multipart(
                "/v1/vocabularies/" + name + "/versions",
                ADMIN,
                parts(
                        field("description", "x"),
                        field("changelog", "y"),
                        field("version", "9"),
                        file("vocabularyFile", VALID)));
        assertThat(withVersion.statusCode()).isEqualTo(400);
        assertThat(issueNames(withVersion)).contains("version");

        assertThat(newVersion(unique("Missing"), "d", VALID).statusCode()).isEqualTo(404);
        assertThat(newVersion("lower", "d", VALID).statusCode()).isEqualTo(400);
    }

    @Test
    void parallelVersionsAreSerialised() throws Exception {
        String name = unique("Parallel");
        create(name, VALID);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String description = "Parallel " + i;
            futures.add(pool.submit(() -> newVersion(name, description, VALID)));
        }
        List<Integer> numbers = new ArrayList<>();
        for (Future<HttpResponse<String>> future : futures) {
            HttpResponse<String> response = future.get();
            assertThat(response.statusCode()).isEqualTo(201);
            numbers.add(body(response).get("version").asInt());
        }
        pool.shutdown();
        assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5, 6);
        JsonNode versions = body(http.get("/v1/vocabularies/" + name + "/versions", VIEWER));
        assertThat(versions.get("items").get(0).get("status").asString()).isEqualTo("ACTIVE");
        for (int i = 1; i < 6; i++) {
            assertThat(versions.get("items").get(i).get("status").asString()).isEqualTo("DEPRECATED");
        }
    }

    @Test
    void statusChangesAndConflicts() {
        String name = unique("Status");
        create(name, VALID);
        HttpResponse<String> deprecated =
                http.json("PATCH", "/v1/vocabularies/" + name, ADMIN, "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.statusCode()).isEqualTo(200);
        assertThat(body(deprecated).get("status").asString()).isEqualTo("DEPRECATED");
        HttpResponse<String> again =
                http.json("PATCH", "/v1/vocabularies/" + name, ADMIN, "{\"status\":\"DEPRECATED\"}");
        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(body(again).get("detail").asString()).isEqualTo("Vocabulary is already in status DEPRECATED");
        assertThat(http.json("PATCH", "/v1/vocabularies/" + name, ADMIN, "{\"status\":\"OTHER\"}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("PATCH", "/v1/vocabularies/" + name, ADMIN, "{\"status\":\"ACTIVE\",\"x\":1}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("PATCH", "/v1/vocabularies/" + name, ADMIN, "[]").statusCode())
                .isEqualTo(400);
        assertThat(http.json("PATCH", "/v1/vocabularies/" + name, ADMIN, "{bad").statusCode())
                .isEqualTo(400);
        assertThat(http.json("PATCH", "/v1/vocabularies/" + unique("None"), ADMIN, "{\"status\":\"ACTIVE\"}")
                        .statusCode())
                .isEqualTo(404);

        HttpResponse<String> version =
                http.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", ADMIN, "{\"status\":\"DEPRECATED\"}");
        assertThat(version.statusCode()).isEqualTo(200);
        assertThat(body(version).get("status").asString()).isEqualTo("DEPRECATED");
        HttpResponse<String> versionAgain =
                http.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", ADMIN, "{\"status\":\"DEPRECATED\"}");
        assertThat(body(versionAgain).get("detail").asString())
                .isEqualTo("Vocabulary version is already in status DEPRECATED");
        HttpResponse<String> missingVersion =
                http.json("PATCH", "/v1/vocabularies/" + name + "/versions/7", ADMIN, "{\"status\":\"ACTIVE\"}");
        assertThat(missingVersion.statusCode()).isEqualTo(404);
        assertThat(body(missingVersion).get("detail").asString())
                .isEqualTo("Version 7 of vocabulary '" + name + "' was not found.");
        assertThat(http.json("PATCH", "/v1/vocabularies/" + name + "/versions/0", ADMIN, "{\"status\":\"ACTIVE\"}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("PATCH", "/v1/vocabularies/" + name, VIEWER, "{\"status\":\"ACTIVE\"}")
                        .statusCode())
                .isEqualTo(403);
        assertThat(http.send("PATCH", "/v1/vocabularies/" + name, ADMIN, "text/plain", "x".getBytes())
                        .statusCode())
                .isEqualTo(415);
    }

    @Test
    void referencesGiveErrorsAndWarnings() {
        String target = unique("Target");
        create(target, VALID);
        newVersion(target, "Second", VALID);
        String content = "@prefix old: <" + BASE + "/content/" + target + "/versions/1#> .\n"
                + "@prefix gone: <" + BASE + "/content/" + target + "/versions/9#> .\n"
                + "@prefix nobody: <" + BASE + "/content/Nobody" + target + "#> .\n"
                + "@prefix bad: <" + BASE + "/content/" + target + "/versions/abc#> .\n"
                + "@prefix dc: <http://purl.org/dc/terms/> .\n"
                + "<http://example.org/x> <http://example.org/p> \"o\" .\n";
        HttpResponse<String> failed = create(unique("Refs"), content);
        assertThat(failed.statusCode()).isEqualTo(400);
        List<String> details = new ArrayList<>();
        body(failed).get("issues").forEach(i -> details.add(i.get("detail").asString()));
        assertThat(details)
                .containsExactly(
                        "Referenced internal vocabulary '" + target + "' version 9 does not exist.",
                        "Referenced internal vocabulary 'Nobody" + target + "' does not exist.",
                        "Referenced internal vocabulary '" + target + "' version abc does not exist.");

        String warningContent = "@prefix old: <" + BASE + "/content/" + target + "/versions/1#> .\n"
                + "@prefix dc: <http://purl.org/dc/terms/> .\n"
                + "<http://example.org/x> <http://example.org/p> \"o\" .\n";
        HttpResponse<String> created = create(unique("Warn"), warningContent);
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode warnings = body(created).get("validationWarnings");
        assertThat(warnings.get(0).get("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(warnings.get(0).get("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + target
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(warnings.get(0).get("location").asString())
                .isEqualTo("prefix 'old' -> '" + BASE + "/content/" + target + "/versions/1#'");
        assertThat(warnings.get(1).get("code").asString()).isEqualTo("externalReferenceSkipped");

        http.json("PATCH", "/v1/vocabularies/" + target, ADMIN, "{\"status\":\"DEPRECATED\"}");
        HttpResponse<String> deprecatedVocabulary = create(unique("Warn"), warningContent);
        assertThat(body(deprecatedVocabulary)
                        .get("validationWarnings")
                        .get(0)
                        .get("description")
                        .asString())
                .isEqualTo("Referenced internal vocabulary '" + target + "' version 1 is deprecated. The vocabulary '"
                        + target + "' is deprecated.");
    }

    @Test
    void listFiltersSortsAndPages() {
        String prefix = unique("List");
        for (String suffix : List.of("C", "A", "B")) {
            create(prefix + suffix, VALID);
        }
        http.json("PATCH", "/v1/vocabularies/" + prefix + "B", ADMIN, "{\"status\":\"DEPRECATED\"}");
        String filter = "/v1/vocabularies?name=" + prefix.toLowerCase();
        JsonNode all = body(http.get(filter, VIEWER));
        assertThat(all.get("total").asInt()).isEqualTo(3);
        assertThat(all.get("items").get(0).get("name").asString()).isEqualTo(prefix + "A");
        JsonNode descending = body(http.get(filter + "&sort=-name", VIEWER));
        assertThat(descending.get("items").get(0).get("name").asString()).isEqualTo(prefix + "C");
        JsonNode deprecated = body(http.get(filter + "&status=DEPRECATED", VIEWER));
        assertThat(deprecated.get("total").asInt()).isEqualTo(1);
        JsonNode paged = body(http.get(filter + "&pageSize=2&page=2&sort=name", VIEWER));
        assertThat(paged.get("items").size()).isEqualTo(1);
        assertThat(paged.get("prev").asString())
                .isEqualTo("/vocabularies?page=1&pageSize=2&name=" + prefix.toLowerCase() + "&sort=name");
        assertThat(paged.has("next")).isFalse();
        JsonNode beyond = body(http.get(filter + "&pageSize=2&page=5", VIEWER));
        assertThat(beyond.get("items").size()).isZero();
        assertThat(beyond.get("prev").asString()).contains("page=2");
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        assertThat(body(http.get(filter + "&creationDate=" + today, VIEWER))
                        .get("total")
                        .asInt())
                .isEqualTo(3);
        assertThat(body(http.get(filter + "&creationDate=2001-01-01", VIEWER))
                        .get("total")
                        .asInt())
                .isZero();
        assertThat(body(http.get(filter + "&lastUpdated=2001-01-01T00:00:00Z", VIEWER))
                        .get("total")
                        .asInt())
                .isZero();

        assertThat(http.get("/v1/vocabularies?unknown=1", VIEWER).statusCode()).isEqualTo(400);
        assertThat(http.get("/v1/vocabularies?page=1&page=2", VIEWER).statusCode())
                .isEqualTo(400);
        assertThat(http.get("/v1/vocabularies?pageSize=101", VIEWER).statusCode())
                .isEqualTo(400);
        assertThat(http.get("/v1/vocabularies?page=0", VIEWER).statusCode()).isEqualTo(400);
        assertThat(http.get("/v1/vocabularies?sort=bad", VIEWER).statusCode()).isEqualTo(400);
        assertThat(http.get("/v1/vocabularies?creationDate=2024-02-30", VIEWER).statusCode())
                .isEqualTo(400);
        assertThat(http.get("/v1/vocabularies?name=a-b", VIEWER).statusCode()).isEqualTo(400);
        assertThat(http.get("/v1/vocabularies?status=active", VIEWER).statusCode())
                .isEqualTo(400);
        assertThat(http.get("/v1/vocabularies/" + prefix + "A/versions?sort=-creationDate&pageSize=1", VIEWER)
                        .statusCode())
                .isEqualTo(200);
        assertThat(http.get("/v1/vocabularies/" + prefix + "A/versions?sort=lastUpdated", VIEWER)
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.get("/v1/vocabularies/" + unique("Nope") + "/versions", VIEWER)
                        .statusCode())
                .isEqualTo(404);
    }

    @Test
    void downloadsReturnStoredBytes() {
        String name = unique("Download");
        String content = VALID + "# comment kept as uploaded\n";
        create(name, content);
        HttpResponse<String> latest = http.get("/v1/content/" + name, VIEWER, "Accept", "text/turtle");
        assertThat(latest.statusCode()).isEqualTo(200);
        assertThat(latest.body()).isEqualTo(content);
        assertThat(latest.headers().firstValue("Content-Type")).contains("text/turtle");
        assertThat(latest.headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"" + name + ".ttl\"");
        HttpResponse<String> version = http.get("/v1/content/" + name + "/versions/1", VIEWER);
        assertThat(version.headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"" + name + "-v1.ttl\"");
        assertThat(http.get("/v1/content/" + name + "/versions/2", VIEWER).statusCode())
                .isEqualTo(404);
        assertThat(http.get("/v1/content/" + unique("Nope") + "/versions/1", VIEWER)
                        .statusCode())
                .isEqualTo(404);
        assertThat(http.get("/v1/content/" + unique("Nope"), VIEWER).statusCode())
                .isEqualTo(404);
        assertThat(http.get("/v1/content/" + name, VIEWER, "Accept", "application/json")
                        .statusCode())
                .isEqualTo(406);
        assertThat(http.get("/v1/vocabularies/" + name + "/versions/1", VIEWER).statusCode())
                .isEqualTo(200);
        assertThat(http.get("/v1/vocabularies/" + name + "/versions/99999999999999999999", VIEWER)
                        .statusCode())
                .isEqualTo(404);
        assertThat(http.get("/v1/vocabularies/" + name + "/versions/x", VIEWER).statusCode())
                .isEqualTo(400);
    }

    @Test
    void searchResolvesReferencesInOrder() {
        String name = unique("Search");
        create(name, VALID);
        newVersion(name, "Second", VALID);
        String request =
                "{\"items\":[{\"name\":\"" + name + "\",\"version\":1},{\"name\":\"" + name + "\"}," + "{\"name\":\""
                        + name + "\"},{\"name\":\"Absent" + name + "\"},{\"name\":\"" + name + "\",\"version\":2}]}";
        HttpResponse<String> response = http.json("POST", "/v1/vocabularies/searches", VIEWER, request);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode result = body(response);
        assertThat(result.get("total").asInt()).isEqualTo(3);
        assertThat(result.get("pageSize").asInt()).isEqualTo(100);
        assertThat(result.has("self")).isFalse();
        JsonNode items = result.get("items");
        assertThat(items.get(0).get("latest").asBoolean()).isFalse();
        assertThat(items.get(0).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(1).get("latest").asBoolean()).isTrue();
        assertThat(items.get(1).get("version").asInt()).isEqualTo(2);
        assertThat(items.get(2).get("latest").asBoolean()).isFalse();

        HttpResponse<String> invalid = http.json(
                "POST",
                "/v1/vocabularies/searches",
                VIEWER,
                "{\"items\":[{\"name\":\"ok\"},{\"name\":\"Fine\",\"version\":0},{\"name\":\"Fine\",\"extra\":1}]}");
        assertThat(issueNames(invalid)).containsExactly("items[0].name", "items[1].version", "items[2].extra");
        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Abc\"}");
        }
        assertThat(issueNames(http.json("POST", "/v1/vocabularies/searches", VIEWER, many + "]}")))
                .containsExactly("items");
        assertThat(http.json("POST", "/v1/vocabularies/searches?page=1", VIEWER, "{\"items\":[{\"name\":\"Abc\"}]}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("POST", "/v1/vocabularies/searches", VIEWER, "{}").statusCode())
                .isEqualTo(400);
        assertThat(http.json("POST", "/v1/vocabularies/searches", TestTokens.noRole(), "{}")
                        .statusCode())
                .isEqualTo(403);
    }

    @Test
    void accessRules() {
        assertThat(http.get("/v1/vocabularies", null).statusCode()).isEqualTo(401);
        assertThat(body(http.get("/v1/vocabularies", null)).get("type").asString())
                .isEqualTo("urn:problem-type:simpl:unauthorized");
        assertThat(http.get("/v1/vocabularies", "not-a-token").statusCode()).isEqualTo(401);
        assertThat(http.get("/v1/vocabularies", TestTokens.wrongAudience()).statusCode())
                .isEqualTo(401);
        assertThat(http.get("/v1/vocabularies", TestTokens.wrongIssuer()).statusCode())
                .isEqualTo(401);
        HttpResponse<String> forbidden = http.get("/v1/vocabularies", TestTokens.noRole());
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(body(forbidden).get("type").asString()).isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(http.multipart("/v1/vocabularies", VIEWER, parts(field("name", "Abc")))
                        .statusCode())
                .isEqualTo(403);
        assertThat(http.get("/v1/vocabularies", VIEWER, "Accept", "text/html").statusCode())
                .isEqualTo(406);
        assertThat(http.get("/v1/vocabularies", VIEWER, "Accept", "application/problem+json")
                        .statusCode())
                .isEqualTo(200);
        assertThat(http.json("POST", "/v1/vocabularies", ADMIN, "{}").statusCode())
                .isEqualTo(415);
    }

    @Test
    void healthNeedsNoToken() {
        HttpResponse<String> health = http.get("/v1/health", "garbage");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(body(health).get("status").asString()).isEqualTo("UP");
        assertThat(http.get("/v1/health?x=1", null).statusCode()).isEqualTo(400);
    }

    @Test
    void routingRules() {
        HttpResponse<String> put = http.send("PUT", "/v1/vocabularies", ADMIN, null, null);
        assertThat(put.statusCode()).isEqualTo(405);
        assertThat(put.headers().firstValue("Allow")).contains("GET, POST");
        assertThat(http.send("PUT", "/v1/vocabularies", null, null, null).statusCode())
                .isEqualTo(405);
        assertThat(http.get("/v1/vocabularies/searches", ADMIN).statusCode()).isEqualTo(405);
        assertThat(http.get("/v1/external-vocabularies/content", ADMIN).statusCode())
                .isEqualTo(405);
        assertThat(http.send("QUERY", "/v1/vocabularies", null, null, null).statusCode())
                .isEqualTo(405);
        assertThat(http.send("HEAD", "/v1/vocabularies", ADMIN, null, null).statusCode())
                .isEqualTo(405);
        assertThat(http.get("/v1/unknown", null).statusCode()).isEqualTo(401);
        assertThat(http.get("/v1/unknown", ADMIN).statusCode()).isEqualTo(404);
        assertThat(http.get("/actuator/health", ADMIN).statusCode()).isEqualTo(404);
        assertThat(http.get("/v1/vocabularies/a%2Fb", ADMIN).statusCode()).isEqualTo(400);
        assertThat(http.get("/v1/vocabularies/ab;c", ADMIN).statusCode()).isEqualTo(400);
    }

    @Test
    void uploadLimits() {
        byte[] large = new byte[5 * 1024 * 1024 + 1];
        java.util.Arrays.fill(large, (byte) 'a');
        HttpResponse<String> fileTooLarge =
                http.multipart("/v1/vocabularies", ADMIN, parts(field("name", "Abc"), file("vocabularyFile", large)));
        assertThat(fileTooLarge.statusCode()).isEqualTo(413);
        assertThat(body(fileTooLarge).get("type").asString()).isEqualTo("urn:problem-type:simpl:payloadTooLarge");
        byte[] huge = new byte[6 * 1024 * 1024 + 10];
        HttpResponse<String> requestTooLarge = http.multipart(
                "/v1/vocabularies", ADMIN, parts(field("x", new String(huge, StandardCharsets.ISO_8859_1))));
        assertThat(requestTooLarge.statusCode()).isEqualTo(413);
        HttpResponse<String> broken =
                http.send("POST", "/v1/vocabularies", ADMIN, "multipart/form-data; boundary=zzz", "garbage".getBytes());
        assertThat(broken.statusCode()).isEqualTo(400);
        byte[] endlessHeaders = ("--zzz\r\n" + "x".repeat(4000) + "\r\n--zzz--\r\n").getBytes(StandardCharsets.UTF_8);
        assertThat(http.send("POST", "/v1/vocabularies", ADMIN, "multipart/form-data; boundary=zzz", endlessHeaders)
                        .statusCode())
                .isEqualTo(400);
    }

    @Test
    void unknownSigningKeyWithIdentityProviderDownFailsClosed() {
        TestKeyConfig.IDP_DOWN.set(true);
        HttpResponse<String> response = http.get("/v1/vocabularies", TestTokens.unknownKey());
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(body(response).get("detail").asString())
                .isEqualTo("The identity provider is not available. Retry later.");
        assertThat(response.headers().firstValue("Retry-After")).contains("5");
        assertThat(http.get("/v1/vocabularies", VIEWER).statusCode()).isEqualTo(200);
    }

    @Test
    void multipartHelperBuildsBody() {
        assertThat(new String(Http.multipartBody("b", parts(field("a", "1"))), StandardCharsets.UTF_8))
                .contains("name=\"a\"");
    }
}
