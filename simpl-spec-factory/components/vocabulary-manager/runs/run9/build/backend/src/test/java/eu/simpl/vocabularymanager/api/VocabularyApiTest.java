package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import eu.simpl.vocabularymanager.support.Http;
import eu.simpl.vocabularymanager.support.TestIdentityProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** End-to-end API behaviour over a real port: stories SIMPL-181xx and the decisions they need. */
class VocabularyApiTest extends ApiTestBase {

    @Test
    void createsVocabularyWithVersionOneActive() {
        String name = uniqueName("Create");
        Http.Response response = create(name, content(name, ""));
        assertThat(response.status()).isEqualTo(201);
        assertThat(response.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/1");
        JsonNode body = response.json();
        assertThat(body.get("version").asInt()).isEqualTo(1);
        assertThat(body.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.get("validationWarnings").isArray()).isTrue();
        assertThat(body.get("validationWarnings").size()).isZero();
        assertThat(body.get("contentHref").asString()).isEqualTo(CONTENT_BASE + "/content/" + name + "/versions/1");
        assertThat(body.get("creationDate").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

        JsonNode vocabulary = http().get("/v1/vocabularies/" + name, viewer()).json();
        assertThat(vocabulary.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(vocabulary.get("version").asInt()).isEqualTo(1);
        assertThat(vocabulary.get("description").asString()).isEqualTo("Description of " + name);
        assertThat(vocabulary.get("href").asString()).isEqualTo("/vocabularies/" + name);
    }

    @Test
    void duplicateNameIsConflictCaseInsensitively() {
        String name = uniqueName("Dup");
        assertThat(create(name, content(name, "")).status()).isEqualTo(201);
        Http.Response again = create(name.toUpperCase(), "not turtle at all");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:conflict");
        assertThat(again.json().get("detail").asString())
                .isEqualTo("Vocabulary with name '" + name.toUpperCase() + "' already exists.");
    }

    @Test
    void newVersionDeprecatesOlderActiveVersions() {
        String name = uniqueName("Ver");
        create(name, content(name, ""));
        Http.Response second = newVersion(name, content(name, "ex:A a skos:Concept ."), "Second");
        assertThat(second.status()).isEqualTo(201);
        assertThat(second.json().get("version").asInt()).isEqualTo(2);
        JsonNode versions =
                http().get("/v1/vocabularies/" + name + "/versions", viewer()).json();
        assertThat(versions.get("total").asInt()).isEqualTo(2);
        assertThat(versions.get("items").get(0).get("version").asInt()).isEqualTo(2);
        assertThat(versions.get("items").get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(versions.get("items").get(1).get("latestVersionHref").asString())
                .isEqualTo("/vocabularies/" + name + "/versions/2");
        JsonNode vocabulary = http().get("/v1/vocabularies/" + name, viewer()).json();
        assertThat(vocabulary.get("description").asString()).isEqualTo("Second");
    }

    @Test
    void identicalVersionIsConflictAndDescriptionOnlyChangeIsAllowed() {
        String name = uniqueName("Same");
        String turtle = content(name, "");
        create(name, turtle);
        Http.Response same = newVersion(name, turtle, "Description of " + name);
        assertThat(same.status()).isEqualTo(409);
        assertThat(same.json().get("detail").asString())
                .isEqualTo("Version 1 of vocabulary '" + name + "' already has this content and description.");
        assertThat(newVersion(name, turtle, "Other description").status()).isEqualTo(201);
    }

    @Test
    void versionFieldIsRejected() {
        String name = uniqueName("VerField");
        create(name, content(name, ""));
        Http.Response response = http().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        admin(),
                        new Http.Multipart()
                                .file(content(name, "ex:B a skos:Concept ."))
                                .field("description", "d")
                                .field("changelog", "c")
                                .field("version", "7"));
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.json().get("issues").get(0).get("name").asString()).isEqualTo("version");
        assertThat(http().get("/v1/vocabularies/" + name, viewer())
                        .json()
                        .get("version")
                        .asInt())
                .isEqualTo(1);
    }

    @Test
    void syntaxErrorGivesOneIssueWithLine() {
        String name = uniqueName("Syntax");
        Http.Response response = create(name, "@prefix ex: <https://example.eu/x#> .\nex:a ex:b \"open .\n");
        assertThat(response.status()).isEqualTo(400);
        JsonNode issues = response.json().get("issues");
        assertThat(issues.size()).isEqualTo(1);
        assertThat(issues.get(0).get("detail").asString()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issues.get(0).get("location").asString()).startsWith("line ");
        assertThat(issues.get(0).get("name").asString()).isEqualTo("vocabularyFile");
        assertThat(http().get("/v1/vocabularies?name=" + name, viewer())
                        .json()
                        .get("total")
                        .asInt())
                .isZero();
    }

    @Test
    void semanticErrorsAreAllReported() {
        String name = uniqueName("Sem");
        String turtle = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix ex: <https://example.eu/" + name + "#> .\n"
                + "ex:op a owl:ObjectProperty . ex:dp a owl:DatatypeProperty .\n"
                + "ex:s ex:op \"literal\" ; ex:dp ex:o ; skos:prefLabel \"a\"@en, \"b\"@en .\n";
        Http.Response response = create(name, turtle);
        assertThat(response.status()).isEqualTo(400);
        List<String> details = new ArrayList<>();
        response.json().get("issues").forEach(i -> details.add(i.get("detail").asString()));
        assertThat(details)
                .containsExactly(
                        "Literal value for object property",
                        "Resource value for datatype property",
                        "Duplicate preferred label for the same language");
    }

    @Test
    void emptyFileFailsWithNoStatements() {
        String name = uniqueName("Empty");
        Http.Response response = http().multipart(
                        "/v1/vocabularies",
                        admin(),
                        new Http.Multipart()
                                .file("vocabularyFile", "e.ttl", new byte[0])
                                .field("name", name)
                                .field("description", "d")
                                .field("changelog", "c"));
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.json().get("issues").get(0).get("location").asString())
                .isEqualTo("document");
    }

    @Test
    void referencesGiveWarningsAndMissingReferencesFail() {
        String base = uniqueName("Base");
        create(base, content(base, ""));
        newVersion(base, content(base, "ex:X a skos:Concept ."), "v2");
        String name = uniqueName("Ref");
        String turtle = "@prefix old: <" + CONTENT_BASE + "/content/" + base + "/versions/1#> .\n"
                + "@prefix dc: <http://purl.org/dc/terms/> .\n" + content(name, "ex:Scheme dc:source old:x .");
        Http.Response ok = create(name, turtle);
        assertThat(ok.status()).isEqualTo(201);
        JsonNode warnings = ok.json().get("validationWarnings");
        assertThat(warnings.size()).isEqualTo(2);
        assertThat(warnings.get(0).get("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(warnings.get(0).get("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + base
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(warnings.get(0).get("location").asString())
                .isEqualTo("prefix 'old' -> '" + CONTENT_BASE + "/content/" + base + "/versions/1#'");
        assertThat(warnings.get(1).get("code").asString()).isEqualTo("externalReferenceSkipped");

        String missing = uniqueName("Missing");
        String bad = "@prefix m: <" + CONTENT_BASE + "/content/" + base + "/versions/9#> .\n" + "@prefix n: <"
                + CONTENT_BASE + "/content/NoSuchVocabularyX/> .\n" + content(missing, "");
        Http.Response failed = create(missing, bad);
        assertThat(failed.status()).isEqualTo(400);
        assertThat(failed.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Referenced internal vocabulary '" + base + "' version 9 does not exist.");
        assertThat(failed.json().get("issues").get(1).get("detail").asString())
                .isEqualTo("Referenced internal vocabulary 'NoSuchVocabularyX' does not exist.");
    }

    @Test
    void statusChangesAndConflicts() {
        String name = uniqueName("Status");
        create(name, content(name, ""));
        Http.Response deprecated =
                http().json("PATCH", "/v1/vocabularies/" + name, admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.status()).isEqualTo(200);
        assertThat(deprecated.json().get("status").asString()).isEqualTo("DEPRECATED");
        Http.Response again = http().json("PATCH", "/v1/vocabularies/" + name, admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("detail").asString()).isEqualTo("Vocabulary is already in status DEPRECATED");
        assertThat(http().json("PATCH", "/v1/vocabularies/" + name, admin(), "{\"status\":\"OTHER\"}")
                        .status())
                .isEqualTo(400);
        assertThat(http().json("PATCH", "/v1/vocabularies/" + name, admin(), "{\"status\":\"ACTIVE\",\"x\":1}")
                        .status())
                .isEqualTo(400);
        JsonNode version =
                http().get("/v1/vocabularies/" + name + "/versions/1", viewer()).json();
        assertThat(version.get("status").asString()).isEqualTo("ACTIVE");

        Http.Response versionPatch = http().json(
                        "PATCH", "/v1/vocabularies/" + name + "/versions/1", admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(versionPatch.status()).isEqualTo(200);
        assertThat(http().json(
                                "PATCH",
                                "/v1/vocabularies/" + name + "/versions/1",
                                admin(),
                                "{\"status\":\"DEPRECATED\"}")
                        .json()
                        .get("detail")
                        .asString())
                .isEqualTo("Vocabulary version is already in status DEPRECATED");
        Http.Response unknownVersion =
                http().json("PATCH", "/v1/vocabularies/" + name + "/versions/5", admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(unknownVersion.status()).isEqualTo(404);
        assertThat(unknownVersion.json().get("detail").asString())
                .isEqualTo("Version 5 of vocabulary '" + name + "' was not found.");
    }

    @Test
    void listFiltersSortsAndPages() {
        String prefix = uniqueName("List");
        for (String suffix : List.of("B", "A", "C")) {
            create(prefix + suffix, content(prefix + suffix, ""));
        }
        http().json("PATCH", "/v1/vocabularies/" + prefix + "C", admin(), "{\"status\":\"DEPRECATED\"}");
        Http.Response page = http().get("/v1/vocabularies?name=" + prefix.toLowerCase() + "&pageSize=2", viewer());
        assertThat(page.status()).isEqualTo(200);
        JsonNode body = page.json();
        assertThat(body.get("total").asInt()).isEqualTo(3);
        assertThat(body.get("totalPages").asInt()).isEqualTo(2);
        assertThat(body.get("items").get(0).get("name").asString()).isEqualTo(prefix + "A");
        assertThat(body.get("next").asString()).contains("page=2").contains("name=" + prefix.toLowerCase());
        assertThat(body.has("prev")).isFalse();

        JsonNode beyond = http().get("/v1/vocabularies?name=" + prefix + "&page=5&pageSize=2", viewer())
                .json();
        assertThat(beyond.get("items").size()).isZero();
        assertThat(beyond.get("prev").asString()).contains("page=2");

        JsonNode sorted = http().get("/v1/vocabularies?name=" + prefix + "&sort=-name", viewer())
                .json();
        assertThat(sorted.get("items").get(0).get("name").asString()).isEqualTo(prefix + "C");
        JsonNode deprecated = http().get("/v1/vocabularies?name=" + prefix + "&status=DEPRECATED", viewer())
                .json();
        assertThat(deprecated.get("total").asInt()).isEqualTo(1);
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        assertThat(http().get("/v1/vocabularies?name=" + prefix + "&creationDate=" + today, viewer())
                        .json()
                        .get("total")
                        .asInt())
                .isEqualTo(3);
        assertThat(http().get("/v1/vocabularies?name=" + prefix + "&lastUpdated=2001-01-01", viewer())
                        .json()
                        .get("total")
                        .asInt())
                .isZero();
        JsonNode empty =
                http().get("/v1/vocabularies?name=NoSuchThingAtAll", viewer()).json();
        assertThat(empty.get("total").asInt()).isZero();
        assertThat(empty.has("last")).isFalse();
    }

    @Test
    void invalidQueryParametersAreRejected() {
        for (String query : List.of(
                "pageSize=101",
                "page=0",
                "page=2147483648",
                "sort=nope",
                "unknown=1",
                "name=a-b",
                "status=active",
                "creationDate=2024-02-30",
                "lastUpdated=2024-01-01Tjunk",
                "page=1&page=2")) {
            Http.Response response = http().get("/v1/vocabularies?" + query, viewer());
            assertThat(response.status()).as(query).isEqualTo(400);
            assertThat(response.json().get("issues").get(0).get("in").asString())
                    .isEqualTo("query");
        }
    }

    @Test
    void downloadsReturnTheBytesAsUploaded() {
        String name = uniqueName("Download");
        String turtle = content(name, "# a comment kept as uploaded\n");
        create(name, turtle);
        Http.Response latest = http().get("/v1/content/" + name, viewer());
        assertThat(latest.status()).isEqualTo(200);
        assertThat(latest.header("Content-Type")).isEqualTo("text/turtle");
        assertThat(latest.header("Content-Disposition")).isEqualTo("attachment; filename=\"" + name + ".ttl\"");
        assertThat(latest.text()).isEqualTo(turtle);
        Http.Response versioned = http().get("/v1/content/" + name + "/versions/1", viewer());
        assertThat(versioned.header("Content-Disposition")).contains(name + "-v1.ttl");
        assertThat(http().get("/v1/content/" + name + "/versions/3", viewer()).status())
                .isEqualTo(404);
        assertThat(http().get("/v1/content/" + name, viewer(), "Accept", "application/json")
                        .status())
                .isEqualTo(406);
    }

    @Test
    void searchResolvesReferencesInOrder() {
        String name = uniqueName("Search");
        create(name, content(name, ""));
        newVersion(name, content(name, "ex:Y a skos:Concept ."), "v2");
        String body = "{\"items\":[{\"name\":\"" + name + "\"},{\"name\":\"" + name + "\",\"version\":1},"
                + "{\"name\":\"" + name + "\"},{\"name\":\"NoSuchName\"},{\"name\":\"" + name + "\",\"version\":2}]}";
        Http.Response response = http().json("POST", "/v1/vocabularies/searches", viewer(), body);
        assertThat(response.status()).isEqualTo(200);
        JsonNode items = response.json().get("items");
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).get("latest").asBoolean()).isTrue();
        assertThat(items.get(0).get("version").asInt()).isEqualTo(2);
        assertThat(items.get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(2).get("latest").asBoolean()).isFalse();
        assertThat(response.json().get("total").asInt()).isEqualTo(3);
        assertThat(response.json().has("self")).isFalse();

        assertThat(http().json("POST", "/v1/vocabularies/searches", viewer(), "{\"items\":[]}")
                        .status())
                .isEqualTo(400);
        Http.Response badItem = http().json(
                        "POST", "/v1/vocabularies/searches", viewer(), "{\"items\":[{\"name\":\"x\",\"version\":0}]}");
        assertThat(badItem.json().get("issues").get(0).get("name").asString()).isEqualTo("items[0].name");
        assertThat(badItem.json().get("issues").get(1).get("name").asString()).isEqualTo("items[0].version");
        assertThat(http().send("POST", "/v1/vocabularies/searches", viewer(), "text/plain", "{}".getBytes())
                        .status())
                .isEqualTo(415);
    }

    @Test
    void accessRulesAndTokenChecks() {
        assertThat(http().get("/v1/vocabularies", null).status()).isEqualTo(401);
        Http.Response unauthorized = http().get("/v1/vocabularies", "not-a-jwt");
        assertThat(unauthorized.status()).isEqualTo(401);
        assertThat(unauthorized.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:unauthorized");
        assertThat(http().get("/v1/vocabularies", norole()).status()).isEqualTo(403);
        Http.Response forbidden = http().multipart(
                        "/v1/vocabularies",
                        viewer(),
                        new Http.Multipart().file("x").field("name", "Abc"));
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:forbidden");
        TestIdentityProvider idp = TestIdentityProvider.get();
        assertThat(http().get("/v1/vocabularies", idp.tokenWithoutAudience("ga-admin", ADMIN))
                        .status())
                .isEqualTo(401);
        assertThat(http().get("/v1/vocabularies", idp.tokenOtherIssuer("ga-admin", ADMIN))
                        .status())
                .isEqualTo(401);
        assertThat(http().get("/v1/health", null).status()).isEqualTo(200);
        assertThat(http().get("/v1/health", null).json().get("status").asString())
                .isEqualTo("UP");
    }

    @Test
    void requestsRejectedBeforeRouting() {
        Http.Response undeclared = http().send("DELETE", "/v1/vocabularies", null, null, null);
        assertThat(undeclared.status()).isEqualTo(405);
        assertThat(undeclared.header("Content-Type")).startsWith("application/problem+json");
        assertThat(http().send("QUERY", "/v1/nowhere", null, null, null).status())
                .isEqualTo(405);
        assertThat(http().get("/v1/vocabularies/searches", admin()).status()).isEqualTo(405);
        assertThat(http().get("/v1/nowhere", null).status()).isEqualTo(401);
        assertThat(http().get("/v1/nowhere", admin()).status()).isEqualTo(404);
        assertThat(http().get("/actuator/health", admin()).status()).isEqualTo(404);
        assertThat(http().get("/error", admin()).status()).isEqualTo(404);
        Http.Response encoded = http().get("/v1/vocabularies/A%2FB", admin());
        assertThat(encoded.status()).isEqualTo(400);
        assertThat(encoded.header("Content-Type")).startsWith("application/problem+json");
        Http.Response path = http().get("/v1/vocabularies/lowercase", admin());
        assertThat(path.status()).isEqualTo(400);
        assertThat(path.json().get("issues").get(0).get("in").asString()).isEqualTo("path");
        assertThat(http().get("/v1/vocabularies/Abcdef/versions/0", admin()).status())
                .isEqualTo(400);
        assertThat(http().get("/v1/vocabularies/NoSuchVocabularyHere", admin()).status())
                .isEqualTo(404);
        assertThat(http().get("/v1/vocabularies", admin(), "Accept", "text/html")
                        .status())
                .isEqualTo(406);
    }

    @Test
    void orderOfChecksOnWrites() {
        String name = uniqueName("Order");
        Http.Response notMultipart = http().json("POST", "/v1/vocabularies", admin(), "{}");
        assertThat(notMultipart.status()).isEqualTo(415);
        Http.Response notAcceptable = http().send(
                        "POST", "/v1/vocabularies", admin(), "application/json", "{}".getBytes(), "Accept", "text/csv");
        assertThat(notAcceptable.status()).isEqualTo(406);
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        java.util.Arrays.fill(big, (byte) ' ');
        Http.Response tooLarge = http().multipart(
                        "/v1/vocabularies",
                        admin(),
                        new Http.Multipart()
                                .file("vocabularyFile", "big.ttl", big)
                                .field("name", "bad name"));
        assertThat(tooLarge.status()).isEqualTo(413);
        assertThat(tooLarge.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:payloadTooLarge");
        byte[] huge = new byte[7 * 1024 * 1024];
        Http.Response requestTooLarge =
                http().multipart("/v1/vocabularies", admin(), new Http.Multipart().file("other", "x", huge));
        assertThat(requestTooLarge.status()).isEqualTo(413);
        Http.Response shape = http().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        admin(),
                        new Http.Multipart()
                                .file("x")
                                .field("unknownField", "1")
                                .field("", "empty"));
        assertThat(shape.status()).isEqualTo(400);
        List<String> names = new ArrayList<>();
        shape.json().get("issues").forEach(i -> names.add(i.get("name").asString()));
        assertThat(names).contains("unknownField", "", "description", "changelog");
        Http.Response notFound = newVersion(name, "x", "d");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.json().get("detail").asString())
                .isEqualTo("Vocabulary with name '" + name + "' was not found.");
        String longText = "x".repeat(1001);
        Http.Response tooLong = http().multipart(
                        "/v1/vocabularies",
                        admin(),
                        new Http.Multipart()
                                .file("x")
                                .field("name", name)
                                .field("description", longText)
                                .field("changelog", "   "));
        assertThat(tooLong.status()).isEqualTo(400);
        assertThat(tooLong.json().get("issues").size()).isEqualTo(2);
    }

    @Test
    void parallelVersionCreationsAllSucceed() throws Exception {
        String name = uniqueName("Parallel");
        create(name, content(name, ""));
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<Http.Response>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int n = i;
            futures.add(pool.submit(() -> newVersion(name, content(name, "ex:P" + n + " a skos:Concept ."), "p" + n)));
        }
        List<String> locations = new ArrayList<>();
        for (Future<Http.Response> future : futures) {
            Http.Response response = future.get();
            assertThat(response.status()).isEqualTo(201);
            locations.add(response.header("Location"));
        }
        pool.shutdown();
        assertThat(locations).doesNotHaveDuplicates().hasSize(5);
        JsonNode versions =
                http().get("/v1/vocabularies/" + name + "/versions", viewer()).json();
        assertThat(versions.get("total").asInt()).isEqualTo(6);
        assertThat(versions.get("items").get(0).get("status").asString()).isEqualTo("ACTIVE");
        for (int i = 1; i < 6; i++) {
            assertThat(versions.get("items").get(i).get("status").asString()).isEqualTo("DEPRECATED");
        }
    }

    @Test
    void responsesCarryCorrelationId() {
        Http.Response response = http().get("/v1/health", null, "X-Correlation-Id", "abc-123");
        assertThat(response.header("X-Correlation-Id")).isEqualTo("abc-123");
        assertThat(http().get("/v1/health", null).header("X-Correlation-Id")).isNotBlank();
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).contains("UP");
    }
}
