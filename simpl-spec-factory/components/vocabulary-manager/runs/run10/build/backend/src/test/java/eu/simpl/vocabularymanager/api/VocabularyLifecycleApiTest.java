package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.ApiClient.Part;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Main flow of the decisions through real HTTP: create, version, status changes, reads and downloads. */
class VocabularyLifecycleApiTest extends IntegrationTestBase {

    @Test
    void createsVocabularyWithActiveVersionOne() {
        String name = uniqueName("Life");
        ApiClient.Response created = api().multipart(
                        "/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, " Desc ", simpleTurtle(name)));

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(created.header("Content-Type")).startsWith("application/json");
        JsonNode body = created.json();
        assertThat(body.get("href").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.get("contentHref").asString()).isEqualTo(CONTENT_BASE + "/content/" + name + "/versions/1");
        assertThat(body.get("latestVersionHref").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.get("version").asInt()).isEqualTo(1);
        assertThat(body.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.get("description").asString()).isEqualTo("Desc");
        assertThat(body.get("changelog").asString()).isEqualTo("Initial version.");
        assertThat(body.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.get("creationDate").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(body.get("validationWarnings").isArray()).isTrue();
        assertThat(body.get("validationWarnings")).isEmpty();

        JsonNode vocabulary =
                api().get("/v1/vocabularies/" + name, IDP.viewer()).json();
        assertThat(vocabulary.get("href").asString()).isEqualTo("/vocabularies/" + name);
        assertThat(vocabulary.get("contentHref").asString()).isEqualTo(CONTENT_BASE + "/content/" + name);
        assertThat(vocabulary.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(vocabulary.get("version").asInt()).isEqualTo(1);
        assertThat(vocabulary.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
    }

    @Test
    void newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus() {
        String name = uniqueName("Ver");
        create(name);
        ApiClient.Response v2 = api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("Second", "Added things.", simpleTurtle(name) + "# v2\n"));
        assertThat(v2.status()).isEqualTo(201);
        assertThat(v2.json().get("version").asInt()).isEqualTo(2);
        assertThat(v2.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/2");

        JsonNode v1 = api().get("/v1/vocabularies/" + name + "/versions/1", IDP.viewer())
                .json();
        assertThat(v1.get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(v1.get("latestVersionHref").asString()).isEqualTo("/vocabularies/" + name + "/versions/2");
        assertThat(v1.get("createdBy").asString()).isEqualTo("ga-admin@example.com");

        JsonNode list = api().get("/v1/vocabularies/" + name + "/versions", IDP.viewer())
                .json();
        assertThat(list.get("total").asInt()).isEqualTo(2);
        assertThat(list.get("items").get(0).get("version").asInt()).isEqualTo(2);
        assertThat(list.get("items").get(1).get("version").asInt()).isEqualTo(1);

        JsonNode vocabulary =
                api().get("/v1/vocabularies/" + name, IDP.viewer()).json();
        assertThat(vocabulary.get("version").asInt()).isEqualTo(2);
        assertThat(vocabulary.get("description").asString()).isEqualTo("Second");
        assertThat(vocabulary.get("status").asString()).isEqualTo("ACTIVE");
    }

    @Test
    void identicalVersionIsRefusedWith409() {
        String name = uniqueName("Same");
        create(name);
        ApiClient.Response again = api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("Desc", "Nothing changed.", simpleTurtle(name)));
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("detail").asString())
                .isEqualTo("Version 1 of vocabulary '" + name + "' already has this content and description.");
        assertThat(again.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:conflict");
    }

    @Test
    void descriptionOnlyChangeCreatesNewVersion() {
        String name = uniqueName("Desc");
        create(name);
        ApiClient.Response v2 = api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("Other description", "Text only.", simpleTurtle(name)));
        assertThat(v2.status()).isEqualTo(201);
    }

    @Test
    void vocabularyStatusChangesAndSameStatusConflicts() {
        String name = uniqueName("Stat");
        create(name);
        ApiClient.Response deprecated =
                api().json("PATCH", "/v1/vocabularies/" + name, IDP.admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.status()).isEqualTo(200);
        assertThat(deprecated.json().get("status").asString()).isEqualTo("DEPRECATED");

        JsonNode v1 = api().get("/v1/vocabularies/" + name + "/versions/1", IDP.viewer())
                .json();
        assertThat(v1.get("status").asString()).isEqualTo("ACTIVE");

        ApiClient.Response again =
                api().json("PATCH", "/v1/vocabularies/" + name, IDP.admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("detail").asString()).isEqualTo("Vocabulary is already in status DEPRECATED");

        ApiClient.Response v2 = api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("New", "More.", simpleTurtle(name) + "# 2\n"));
        assertThat(v2.status()).isEqualTo(201);
        assertThat(api().get("/v1/vocabularies/" + name, IDP.viewer())
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("DEPRECATED");

        ApiClient.Response bad = api().json("PATCH", "/v1/vocabularies/" + name, IDP.admin(), "{\"status\":\"OLD\"}");
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.json().get("issues").get(0).get("name").asString()).isEqualTo("status");
    }

    @Test
    void versionStatusChangeAllowsTwoActiveVersions() {
        String name = uniqueName("Two");
        create(name);
        api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("Two", "Second.", simpleTurtle(name) + "# 2\n"));
        ApiClient.Response reactivated =
                api().json("PATCH", "/v1/vocabularies/" + name + "/versions/1", IDP.admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(reactivated.status()).isEqualTo(200);
        assertThat(reactivated.json().get("status").asString()).isEqualTo("ACTIVE");
        assertThat(api().get("/v1/vocabularies/" + name + "/versions/2", IDP.viewer())
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("ACTIVE");

        ApiClient.Response same =
                api().json("PATCH", "/v1/vocabularies/" + name + "/versions/1", IDP.admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(same.status()).isEqualTo(409);
        assertThat(same.json().get("detail").asString()).isEqualTo("Vocabulary version is already in status ACTIVE");

        api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("Three", "Third.", simpleTurtle(name) + "# 3\n"));
        assertThat(api().get("/v1/vocabularies/" + name + "/versions/1", IDP.viewer())
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("DEPRECATED");
        assertThat(api().get("/v1/vocabularies/" + name + "/versions/2", IDP.viewer())
                        .json()
                        .get("status")
                        .asString())
                .isEqualTo("DEPRECATED");

        ApiClient.Response missing =
                api().json("PATCH", "/v1/vocabularies/" + name + "/versions/9", IDP.admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.json().get("detail").asString())
                .isEqualTo("Version 9 of vocabulary '" + name + "' was not found.");
    }

    @Test
    void downloadsReturnStoredBytes() {
        String name = uniqueName("Down");
        String turtle = simpleTurtle(name) + "# é ünïcode\n";
        api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", turtle));

        ApiClient.Response latest = api().get("/v1/content/" + name, IDP.viewer());
        assertThat(latest.status()).isEqualTo(200);
        assertThat(latest.header("Content-Type")).isEqualTo("text/turtle");
        assertThat(latest.header("Content-Disposition")).isEqualTo("attachment; filename=\"" + name + ".ttl\"");
        assertThat(latest.body()).isEqualTo(turtle.getBytes(StandardCharsets.UTF_8));

        ApiClient.Response v1 = api().get("/v1/content/" + name + "/versions/1", IDP.viewer(), "text/turtle");
        assertThat(v1.header("Content-Disposition")).isEqualTo("attachment; filename=\"" + name + "-v1.ttl\"");
        assertThat(v1.body()).isEqualTo(turtle.getBytes(StandardCharsets.UTF_8));

        assertThat(api().get("/v1/content/" + name + "/versions/7", IDP.viewer())
                        .status())
                .isEqualTo(404);
        assertThat(api().get("/v1/content/Missing" + name, IDP.viewer()).status())
                .isEqualTo(404);
        assertThat(api().get("/v1/content/" + name, IDP.viewer(), "application/json")
                        .status())
                .isEqualTo(406);
    }

    @Test
    void largeContentIsChunkedAndReturnedExactly() {
        String name = uniqueName("Big");
        StringBuilder turtle = new StringBuilder("@prefix ex: <https://example.org/big#> .\n");
        int i = 0;
        while (turtle.length() < 2_500_000) {
            turtle.append("ex:t")
                    .append(i++)
                    .append(" ex:label \"term ")
                    .append(i)
                    .append("\" .\n");
        }
        ApiClient.Response created =
                api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", turtle.toString()));
        assertThat(created.status()).isEqualTo(201);
        assertThat(api().get("/v1/content/" + name, IDP.viewer()).body())
                .isEqualTo(turtle.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void duplicateNameIgnoringCaseIsConflictBeforeValidation() {
        String name = uniqueName("Dup");
        create(name);
        ApiClient.Response duplicate = api().multipart(
                        "/v1/vocabularies",
                        IDP.admin(),
                        ApiClient.createParts(name.toUpperCase(), "D", "not turtle @@"));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.json().get("detail").asString())
                .isEqualTo("Vocabulary with name '" + name.toUpperCase() + "' already exists.");
        assertThat(api().get("/v1/vocabularies/" + name.toUpperCase(), IDP.viewer())
                        .status())
                .isEqualTo(404);
    }

    @Test
    void manualVersionFieldIsRejected() {
        String name = uniqueName("Man");
        create(name);
        List<Part> parts = new ArrayList<>(ApiClient.versionParts("X", "Y", simpleTurtle(name) + "#x\n"));
        parts.add(Part.text("version", "5"));
        ApiClient.Response response = api().multipart("/v1/vocabularies/" + name + "/versions", IDP.admin(), parts);
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.json().get("issues").get(0).get("name").asString()).isEqualTo("version");
        assertThat(response.json().get("issues").get(0).get("in").asString()).isEqualTo("body");
        assertThat(api().get("/v1/vocabularies/" + name, IDP.viewer())
                        .json()
                        .get("version")
                        .asInt())
                .isEqualTo(1);
    }

    @Test
    void newVersionForUnknownVocabularyIs404() {
        ApiClient.Response response = api().multipart(
                        "/v1/vocabularies/Unknown" + uniqueName("X") + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("X", "Y", simpleTurtle("x")));
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.json().get("issues").get(0).get("name").asString()).isEqualTo("vocabularyName");
    }

    private void create(String name) {
        ApiClient.Response response = api().multipart(
                        "/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "Desc", simpleTurtle(name)));
        assertThat(response.status()).as(response.text()).isEqualTo(201);
    }
}
