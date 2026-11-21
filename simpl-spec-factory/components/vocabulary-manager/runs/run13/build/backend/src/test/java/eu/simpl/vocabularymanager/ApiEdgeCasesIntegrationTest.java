package eu.simpl.vocabularymanager;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.ApiClient.Part;
import eu.simpl.vocabularymanager.support.ApiClient.Response;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Order of checks, limits and odd input (D21, D49, D51, D52, D63, D67, D76, D92, D110, D150, Q2). */
class ApiEdgeCasesIntegrationTest extends IntegrationTestBase {

    private JsonNode firstIssue(Response response) {
        return response.json().get("issues").get(0);
    }

    private String create(String name) {
        Response created =
                api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, simpleTurtle(name), "D."));
        assertThat(created.status()).as(created.text()).isEqualTo(201);
        return name;
    }

    @Test
    void orderOfChecksOnWrites() {
        String name = create(uniqueName("Order"));
        Response dupInvalid =
                api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, "not turtle at all", "Desc."));
        assertThat(dupInvalid.status()).as("409 before content validation").isEqualTo(409);
        assertThat(dupInvalid.json().get("detail").asString())
                .isEqualTo("Vocabulary with name '" + name + "' already exists.");
        Response caseDuplicate = api.multipart(
                "/v1/vocabularies", admin, ApiClient.createParts(name.toUpperCase(), simpleTurtle("x"), "Desc."));
        assertThat(caseDuplicate.status()).isEqualTo(409);
        Response malformedUnknown = api.multipart(
                "/v1/vocabularies/Unknown" + name + "/versions", admin, List.of(Part.field("description", "x")));
        assertThat(malformedUnknown.status()).as("400 before 404").isEqualTo(400);
        Response versionField = api.multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                List.of(
                        Part.file("vocabularyFile", "<a:b> <a:c> <a:d> .".getBytes(StandardCharsets.UTF_8)),
                        Part.field("description", "x"),
                        Part.field("changelog", "y"),
                        Part.field("version", "7")));
        assertThat(versionField.status()).isEqualTo(400);
        assertThat(firstIssue(versionField).get("name").asString()).isEqualTo("version");
        Response unknown404 = api.multipart(
                "/v1/vocabularies/Unknown" + name + "/versions",
                admin,
                ApiClient.versionParts("<a:b> <a:c> <a:d> .", "d", "c"));
        assertThat(unknown404.status()).isEqualTo(404);
        assertThat(unknown404.json().get("detail").asString())
                .isEqualTo("Vocabulary with name 'Unknown" + name + "' was not found.");
        Response queryOnPost = api.multipart(
                "/v1/vocabularies?x=1", admin, ApiClient.createParts(uniqueName("Q"), simpleTurtle("q"), "D."));
        assertThat(queryOnPost.status()).isEqualTo(400);
        assertThat(firstIssue(queryOnPost).get("in").asString()).isEqualTo("query");
        Response unknownField = api.multipart(
                "/v1/vocabularies", admin, withExtra(ApiClient.createParts(uniqueName("U"), simpleTurtle("u"), "D.")));
        assertThat(unknownField.status()).isEqualTo(400);
        assertThat(firstIssue(unknownField).get("name").asString()).isEqualTo("extra");
        Response missingFile = api.multipart(
                "/v1/vocabularies",
                admin,
                List.of(
                        Part.field("name", uniqueName("M")),
                        Part.field("description", "d"),
                        Part.field("changelog", "c")));
        assertThat(missingFile.status()).isEqualTo(400);
        Response notMultipart = api.json("POST", "/v1/vocabularies", admin, "{}");
        assertThat(notMultipart.status()).isEqualTo(415);
        Response forbiddenFirst = api.json("POST", "/v1/vocabularies", viewer, "{}");
        assertThat(forbiddenFirst.status()).as("403 before 415").isEqualTo(403);
        Response notAcceptable = api.send(
                "POST",
                "/v1/vocabularies",
                admin,
                ApiClient.headers("Accept", "text/csv", "Content-Type", "application/json"),
                "{}".getBytes(StandardCharsets.UTF_8));
        assertThat(notAcceptable.status()).as("406 before 415").isEqualTo(406);
    }

    private static List<Part> withExtra(List<Part> parts) {
        List<Part> extended = new ArrayList<>(parts);
        extended.add(Part.field("extra", "1"));
        extended.add(new Part("", null, "ignored".getBytes(StandardCharsets.UTF_8)));
        return extended;
    }

    @Test
    void sizeLimits() {
        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        Arrays.fill(tooLarge, (byte) '#');
        Response file = api.multipart(
                "/v1/vocabularies",
                admin,
                List.of(
                        Part.file("vocabularyFile", tooLarge), Part.field("name", uniqueName("Big")),
                        Part.field("description", "d"), Part.field("changelog", "c")));
        assertThat(file.status()).isEqualTo(413);
        assertThat(file.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:payloadTooLarge");
        assertThat(firstIssue(file).get("name").asString()).isEqualTo("vocabularyFile");
        byte[] atLimit = new byte[5 * 1024 * 1024];
        Arrays.fill(atLimit, (byte) '#');
        Response limit = api.multipart(
                "/v1/vocabularies",
                admin,
                List.of(
                        Part.file("vocabularyFile", atLimit), Part.field("name", uniqueName("Limit")),
                        Part.field("description", "d"), Part.field("changelog", "c")));
        assertThat(limit.status())
                .as("comment-only file at the limit fails S6, not 413")
                .isEqualTo(400);
        assertThat(firstIssue(limit).get("location").asString()).isEqualTo("document");
        String bigJson = "{\"status\":\"" + "x".repeat(1024 * 1024) + "\"}";
        Response json = api.json("PATCH", "/v1/vocabularies/Abcdef", admin, bigJson);
        assertThat(json.status()).isEqualTo(413);
        assertThat(json.json().get("detail").asString()).isEqualTo("The request body is larger than 1 MB.");
        List<Part> many = new ArrayList<>();
        for (int i = 0; i < 70; i++) {
            many.add(Part.field("aliasUris", "http://a.example/" + i));
        }
        assertThat(api.multipart("/v1/external-vocabularies", admin, many).status())
                .isEqualTo(400);
    }

    @Test
    void queryParameterRules() {
        assertThat(api.get("/v1/vocabularies?unknown=1", viewer).status()).isEqualTo(400);
        Response twice = api.get("/v1/vocabularies?page=1&page=1", viewer);
        assertThat(twice.status()).isEqualTo(400);
        assertThat(twice.json().get("issues").size()).isEqualTo(1);
        assertThat(firstIssue(twice).get("name").asString()).isEqualTo("page");
        for (String query : List.of(
                "pageSize=0",
                "pageSize=101",
                "page=0",
                "page=1.0",
                "page=2147483648",
                "sort=size",
                "status=active",
                "creationDate=2026-02-30",
                "lastUpdated=yesterday",
                "name=a-b",
                "name=")) {
            assertThat(api.get("/v1/vocabularies?" + query, viewer).status())
                    .as(query)
                    .isEqualTo(400);
        }
        Response beyond = api.get("/v1/vocabularies?page=100000&pageSize=100", viewer);
        assertThat(beyond.status()).isEqualTo(200);
        assertThat(beyond.json().get("items").size()).isZero();
        assertThat(api.get("/v1/vocabularies/" + "Abc/versions?sort=-status&pageSize=1", viewer)
                        .status())
                .isEqualTo(404);
        assertThat(api.get("/v1/external-vocabularies?sort=name", viewer).status())
                .isEqualTo(400);
        assertThat(api.get("/v1/vocabularies/Abc/versions/0", viewer).status()).isEqualTo(400);
        Response badVersion = api.get("/v1/content/Abc/versions/x", viewer);
        assertThat(badVersion.status()).isEqualTo(400);
        assertThat(firstIssue(badVersion).get("in").asString()).isEqualTo("path");
        assertThat(firstIssue(badVersion).get("type").asString())
                .isEqualTo("urn:problem-type:belgif:input-validation:schemaViolation");
    }

    @Test
    void listFiltersSortAndPaging() {
        String stem = uniqueName("Sort");
        String a = create(stem + "A");
        String b = create(stem + "B");
        api.json("PATCH", "/v1/vocabularies/" + b, admin, "{\"status\":\"DEPRECATED\"}");
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        Response list = api.get(
                "/v1/vocabularies?name=" + stem.toLowerCase() + "&sort=-name&pageSize=1&creationDate=" + today, viewer);
        JsonNode body = list.json();
        assertThat(body.get("total").asLong()).isEqualTo(2);
        assertThat(body.get("items").get(0).get("name").asString()).isEqualTo(b);
        assertThat(body.get("next").asString()).startsWith("/vocabularies?page=2&pageSize=1&name=");
        Response active = api.get("/v1/vocabularies?name=" + stem + "&status=ACTIVE", viewer);
        assertThat(active.json().get("items").get(0).get("name").asString()).isEqualTo(a);
        for (String sort :
                List.of("status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated")) {
            assertThat(api.get("/v1/vocabularies?name=" + stem + "&sort=" + sort, viewer)
                            .json()
                            .get("total")
                            .asLong())
                    .as(sort)
                    .isEqualTo(2);
        }
        for (String sort : List.of("name", "-name", "status", "-status", "creationDate", "-creationDate")) {
            assertThat(api.get("/v1/vocabularies/" + a + "/versions?sort=" + sort, viewer)
                            .status())
                    .isEqualTo(200);
        }
        assertThat(api.get("/v1/vocabularies?lastUpdated=2000-01-01T00:00:00Z", viewer)
                        .json()
                        .get("total")
                        .asLong())
                .isZero();
    }

    @Test
    void versionStatusAndNotFoundTexts() {
        String name = create(uniqueName("Status"));
        Response same = api.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(same.status()).isEqualTo(409);
        assertThat(same.json().get("detail").asString()).isEqualTo("Vocabulary version is already in status ACTIVE");
        Response missing =
                api.json("PATCH", "/v1/vocabularies/" + name + "/versions/5", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.json().get("detail").asString())
                .isEqualTo("Version 5 of vocabulary '" + name + "' was not found.");
        assertThat(api.get("/v1/content/" + name + "/versions/5", viewer).status())
                .isEqualTo(404);
        for (String body : List.of(
                "{\"status\":\"active\"}",
                "{}",
                "{\"status\":1}",
                "[]",
                "{\"status\":\"ACTIVE\",\"x\":1}",
                "{bad json",
                "{\"status\":\"ACTIVE\",\"status\":\"DEPRECATED\"}")) {
            assertThat(api.json("PATCH", "/v1/vocabularies/" + name, admin, body)
                            .status())
                    .as(body)
                    .isEqualTo(400);
        }
        assertThat(api.json("PATCH", "/v1/vocabularies/" + name.toUpperCase(), admin, "{\"status\":\"DEPRECATED\"}")
                        .status())
                .isEqualTo(404);
        Response deprecated =
                api.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.json().get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(api.get("/v1/vocabularies/" + name, viewer)
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("ACTIVE");
    }

    @Test
    void searchValidation() {
        assertThat(api.json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[]}")
                        .status())
                .isEqualTo(400);
        Response badItem = api.json(
                "POST",
                "/v1/vocabularies/searches",
                viewer,
                "{\"items\":[{\"name\":\"Good\"},{\"name\":\"bad\"},{\"name\":\"Ok1\",\"version\":2.0}]}");
        assertThat(badItem.status()).isEqualTo(400);
        assertThat(badItem.json().get("issues").get(0).get("name").asString()).isEqualTo("items[1].name");
        assertThat(badItem.json().get("issues").get(1).get("name").asString()).isEqualTo("items[2].version");
        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Name").append(i).append("\"}");
        }
        assertThat(api.json("POST", "/v1/vocabularies/searches", viewer, many + "]}")
                        .status())
                .isEqualTo(400);
        assertThat(api.json("POST", "/v1/vocabularies/searches?page=1", viewer, "{\"items\":[{\"name\":\"Abc\"}]}")
                        .status())
                .isEqualTo(400);
        Response empty = api.json("POST", "/v1/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Absent\"}]}");
        assertThat(empty.json().get("totalPages").asInt()).isZero();

        for (String body : List.of(
                "{}",
                "{\"namespaceUri\":\"http://a\",\"aliasUri\":\"http://b\"}",
                "{\"namespaceUri\":\"http://a\",\"namespaceUris\":[\"http://b\"]}",
                "{\"namespaceUris\":[]}",
                "{\"namespaceUri\":\"relative\"}",
                "{\"aliasUris\":[\"http://ok\", 5]}",
                "{\"other\":1}")) {
            assertThat(api.json("POST", "/v1/external-vocabulary/searches", viewer, body)
                            .status())
                    .as(body)
                    .isEqualTo(400);
        }
        StringBuilder uris = new StringBuilder("{\"namespaceUris\":[");
        for (int i = 0; i < 60; i++) {
            uris.append(i == 0 ? "" : ",")
                    .append("\"http://n.example/")
                    .append(i)
                    .append('"');
        }
        uris.append("],\"aliasUris\":[");
        for (int i = 0; i < 41; i++) {
            uris.append(i == 0 ? "" : ",")
                    .append("\"http://a.example/")
                    .append(i)
                    .append('"');
        }
        Response tooMany = api.json("POST", "/v1/external-vocabulary/searches", viewer, uris + "]}");
        assertThat(tooMany.status()).isEqualTo(400);
        assertThat(firstIssue(tooMany).get("name").asString()).isEqualTo("aliasUris");
        Response notFound = api.json(
                "POST",
                "/v1/external-vocabularies/content",
                viewer,
                "{\"namespaceUri\":\"http://nothing.example/ns#\"}");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.json().get("detail").asString())
                .isEqualTo("External vocabulary with namespaceUri 'http://nothing.example/ns#' was not found.");
        assertThat(api.json("POST", "/v1/external-vocabularies/content", viewer, "{}")
                        .status())
                .isEqualTo(400);
    }

    @Test
    void externalRegistrationRules() {
        String name = uniqueName("Reg");
        String ns = "https://reg.example/" + name + "#";
        byte[] content = ("<" + ns + "a> <" + ns + "b> \"c\" .").getBytes(StandardCharsets.UTF_8);
        List<Part> aliasSame = List.of(
                Part.file("vocabularyFile", content),
                Part.field("name", name),
                Part.field("namespaceUri", ns),
                Part.field("aliasUris", "https://reg.example/" + name + "/"));
        assertThat(api.multipart("/v1/external-vocabularies", admin, aliasSame).status())
                .isEqualTo(400);
        List<Part> relative = List.of(
                Part.file("vocabularyFile", content),
                Part.field("name", name),
                Part.field("namespaceUri", "not-absolute"));
        assertThat(api.multipart("/v1/external-vocabularies", admin, relative).status())
                .isEqualTo(400);
        List<Part> empty = List.of(
                Part.file("vocabularyFile", new byte[0]), Part.field("name", name), Part.field("namespaceUri", ns));
        Response emptyFile = api.multipart("/v1/external-vocabularies", admin, empty);
        assertThat(emptyFile.status()).isEqualTo(400);
        assertThat(firstIssue(emptyFile).get("detail").asString()).isEqualTo("Vocabulary contains no statements");
        List<Part> ok = List.of(
                Part.file("vocabularyFile", content),
                Part.field("name", name),
                Part.field("namespaceUri", ns),
                Part.field("description", "   "),
                Part.field("sourceUri", "https://src.example/x.ttl"));
        Response registered = api.multipart("/v1/external-vocabularies", admin, ok);
        assertThat(registered.status()).as(registered.text()).isEqualTo(201);
        assertThat(registered.json().has("description")).isFalse();
        assertThat(registered.header("Location")).isEqualTo("/external-vocabularies/" + name);
        List<Part> sameNamespace = List.of(
                Part.file("vocabularyFile", content),
                Part.field("name", name + "Two"),
                Part.field("namespaceUri", "https://reg.example/" + name + "/"));
        Response conflict = api.multipart("/v1/external-vocabularies", admin, sameNamespace);
        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.json().get("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias 'https://reg.example/" + name
                        + "/' is already registered.");
        List<Part> sameName = List.of(
                Part.file("vocabularyFile", content),
                Part.field("name", name.toLowerCase().replaceFirst("^r", "R")),
                Part.field("namespaceUri", "https://other.example/x"));
        assertThat(api.multipart("/v1/external-vocabularies", admin, sameName)
                        .json()
                        .get("detail")
                        .asString())
                .contains("is already registered.");
        Response list = api.get("/v1/external-vocabularies?pageSize=100", viewer);
        assertThat(list.status()).isEqualTo(200);
        Response byName = api.get("/v1/external-vocabularies/" + name + "/content", viewer);
        assertThat(byName.status()).isEqualTo(200);
        assertThat(byName.body()).isEqualTo(content);
        assertThat(api.get("/v1/external-vocabularies/Nothing" + name + "/content", viewer)
                        .status())
                .isEqualTo(404);
        assertThat(api.send("DELETE", "/v1/external-vocabularies/Nothing" + name, admin, Map.of(), null)
                        .status())
                .isEqualTo(404);
        assertThat(api.send("DELETE", "/v1/external-vocabularies/" + name, viewer, Map.of(), null)
                        .status())
                .isEqualTo(403);
    }

    @Test
    void routingAndMethods() {
        Response head = api.send("HEAD", "/v1/vocabularies", null, Map.of(), null);
        assertThat(head.status()).isEqualTo(405);
        assertThat(head.header("Allow")).isEqualTo("GET, POST");
        assertThat(api.send("OPTIONS", "/v1/health", null, Map.of(), null).status())
                .isEqualTo(405);
        assertThat(api.send("DELETE", "/v1/vocabularies/Abc", admin, Map.of(), null)
                        .status())
                .isEqualTo(405);
        assertThat(api.get("/v1/vocabularies/Abc%2Fdef", viewer).status()).isEqualTo(400);
        assertThat(api.get("/v1/error", viewer).status()).isEqualTo(404);
        assertThat(api.get("/error", viewer).status()).isEqualTo(404);
        Response correlation =
                api.send("GET", "/v1/health", null, ApiClient.headers("X-Correlation-Id", "abc-123"), null);
        assertThat(correlation.header("X-Correlation-Id")).isEqualTo("abc-123");
        assertThat(api.send("GET", "/v1/health", null, ApiClient.headers("Accept", "text/plain"), null)
                        .status())
                .isEqualTo(406);
        assertThat(api.send(
                                "GET",
                                "/v1/vocabularies",
                                viewer,
                                ApiClient.headers("Accept", "application/problem+json"),
                                null)
                        .header("Content-Type"))
                .startsWith("application/json");
        assertThat(api.send("GET", "/v1/vocabularies", viewer, ApiClient.headers("Authorization", "Basic abc"), null)
                        .status())
                .isEqualTo(401);
    }
}
