package eu.simpl.vocabularymanager.it;

import static eu.simpl.vocabularymanager.it.TestEnvironment.admin;
import static eu.simpl.vocabularymanager.it.TestEnvironment.viewer;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Searches and external vocabularies (D30, D38, D41, D56, D66, D68, D73, D137). */
class SearchAndExternalIT extends IntegrationTest {

    @Test
    void internalSearchKeepsRequestOrderAndDropsMissingAndRepeated() {
        String name = uniqueName("Find");
        create(name, ttl(name));
        newVersion(name, "Two", ttl(name) + "#2");
        String body = "{\"items\":[{\"name\":\"" + name + "\",\"version\":1},{\"name\":\"" + name
                + "\"},{\"name\":\"Nope" + System.nanoTime() + "\"},{\"name\":\"" + name + "\",\"version\":1}]}";
        JsonNode result =
                api.postJson("/v1/vocabularies/searches", viewer(), body).json();
        assertThat(result.get("total").asInt()).isEqualTo(2);
        assertThat(result.get("items").get(0).get("latest").asBoolean()).isFalse();
        assertThat(result.get("items").get(0).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(result.get("items").get(1).get("version").asInt()).isEqualTo(2);
        assertThat(result.get("items").get(1).get("href").asString()).isEqualTo("/vocabularies/" + name);
        assertThat(result.has("self")).isFalse();
    }

    @Test
    void internalSearchBodyRules() {
        assertThat(issueName(api.postJson("/v1/vocabularies/searches", viewer(), "{\"items\":[]}")
                        .json()))
                .isEqualTo("items");
        assertThat(issueName(api.postJson(
                                "/v1/vocabularies/searches",
                                viewer(),
                                "{\"items\":[{\"name\":\"Abc\",\"version\":1.0}]}")
                        .json()))
                .isEqualTo("items[0].version");
        assertThat(issueName(api.postJson("/v1/vocabularies/searches", viewer(), "{\"items\":[{\"name\":\"abc\"}]}")
                        .json()))
                .isEqualTo("items[0].name");
        assertThat(issueName(
                        api.postJson("/v1/vocabularies/searches", viewer(), "{\"items\":[{\"name\":\"Abc\"}],\"x\":1}")
                                .json()))
                .isEqualTo("x");
        assertThat(api.postJson("/v1/vocabularies/searches", viewer(), "[1]").status())
                .isEqualTo(400);
        assertThat(api.postJson("/v1/vocabularies/searches", viewer(), "{bad").status())
                .isEqualTo(400);
        assertThat(api.postJson("/v1/vocabularies/searches?page=1", viewer(), "{\"items\":[{\"name\":\"Abc\"}]}")
                        .status())
                .isEqualTo(400);
    }

    @Test
    void externalLifecycle() {
        String name = uniqueName("Dc");
        String ns = "http://purl.example.org/" + name + "/terms/";
        String alias = "https://purl.example.org/" + name + "/terms";
        ApiClient.Result created = api.post(
                "/v1/external-vocabularies",
                admin(),
                new ApiClient.Form()
                        .field("name", name)
                        .field("namespaceUri", ns)
                        .field("aliasUris", alias)
                        .field("description", "  Cached copy  ")
                        .field("sourceUri", "https://source.example.org/dc.ttl")
                        .file("vocabularyFile", "@prefix d: <" + ns + "> . d:title a d:Property ."));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/external-vocabularies/" + name);
        assertThat(created.json().get("aliasUris").get(0).asString()).isEqualTo(alias);
        assertThat(created.json().get("description").asString()).isEqualTo("Cached copy");

        ApiClient.Result conflict = api.post(
                "/v1/external-vocabularies",
                admin(),
                new ApiClient.Form()
                        .field("name", uniqueName("Other"))
                        .field("namespaceUri", alias + "#")
                        .file("vocabularyFile", "<a> <b> <c> ."));
        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.json().get("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias '" + alias + "#' is already registered.");

        JsonNode search = api.postJson(
                        "/v1/external-vocabulary/searches",
                        viewer(),
                        "{\"namespaceUris\":[\"" + alias + "\"],\"aliasUris\":[\"" + ns + "\"]}")
                .json();
        assertThat(search.get("total").asInt()).isEqualTo(1);
        assertThat(api.postJson("/v1/external-vocabulary/searches", viewer(), "{\"aliasUri\":\"" + alias + "/\"}")
                        .json()
                        .get("total")
                        .asInt())
                .isEqualTo(1);

        ApiClient.Result content =
                api.postJson("/v1/external-vocabularies/content", viewer(), "{\"namespaceUri\":\"" + alias + "\"}");
        assertThat(content.status()).isEqualTo(200);
        assertThat(content.header("Content-Disposition")).contains(name + ".ttl");
        assertThat(api.get("/v1/external-vocabularies/" + name + "/content", viewer())
                        .status())
                .isEqualTo(200);
        assertThat(api.get("/v1/external-vocabularies/" + name, viewer())
                        .json()
                        .get("name")
                        .asString())
                .isEqualTo(name);
        JsonNode list =
                api.get("/v1/external-vocabularies?pageSize=100", viewer()).json();
        assertThat(list.get("total").asLong()).isPositive();

        assertThat(api.send("DELETE", "/v1/external-vocabularies/" + name, viewer(), null, null)
                        .status())
                .isEqualTo(403);
        assertThat(api.send("DELETE", "/v1/external-vocabularies/" + name, admin(), null, null)
                        .status())
                .isEqualTo(204);
        ApiClient.Result gone = api.get("/v1/external-vocabularies/" + name, viewer());
        assertThat(gone.status()).isEqualTo(404);
        assertThat(gone.json().get("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
        ApiClient.Result goneContent =
                api.postJson("/v1/external-vocabularies/content", viewer(), "{\"namespaceUri\":\"" + alias + "\"}");
        assertThat(goneContent.status()).isEqualTo(404);
        assertThat(goneContent.json().get("issues").get(0).get("in").asString()).isEqualTo("body");
    }

    @Test
    void externalRegistrationRules() {
        ApiClient.Result bad = api.post(
                "/v1/external-vocabularies",
                admin(),
                new ApiClient.Form()
                        .field("name", uniqueName("Bad"))
                        .field("namespaceUri", "relative/path")
                        .field("aliasUris", "https://a.example/x")
                        .field("aliasUris", "https://a.example/x/")
                        .file("vocabularyFile", "<a> <b> <c> ."));
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.json().get("issues").get(0).get("name").asString()).isEqualTo("namespaceUri");
        ApiClient.Result empty = api.post(
                "/v1/external-vocabularies",
                admin(),
                new ApiClient.Form()
                        .field("name", uniqueName("Empty"))
                        .field("namespaceUri", "https://empty.example/" + System.nanoTime() + "#")
                        .file("vocabularyFile", "# nothing"));
        assertThat(empty.status()).isEqualTo(400);
        assertThat(empty.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Vocabulary contains no statements");
        ApiClient.Result syntax = api.post(
                "/v1/external-vocabularies",
                admin(),
                new ApiClient.Form()
                        .field("name", uniqueName("Syn"))
                        .field("namespaceUri", "https://syntax.example/" + System.nanoTime() + "#")
                        .file("vocabularyFile", "<a> <b> ."));
        assertThat(syntax.json().get("issues")).hasSize(1);
    }

    @Test
    void externalSearchBodyRules() {
        assertThat(issueName(api.postJson("/v1/external-vocabulary/searches", viewer(), "{}")
                        .json()))
                .isEqualTo("body");
        assertThat(issueName(api.postJson(
                                "/v1/external-vocabulary/searches",
                                viewer(),
                                "{\"namespaceUri\":\"https://a\",\"aliasUri\":\"https://b\"}")
                        .json()))
                .isEqualTo("body");
        assertThat(issueName(api.postJson("/v1/external-vocabulary/searches", viewer(), "{\"aliasUris\":[]}")
                        .json()))
                .isEqualTo("aliasUris");
        assertThat(issueName(api.postJson("/v1/external-vocabulary/searches", viewer(), "{\"namespaceUris\":[\"x\"]}")
                        .json()))
                .isEqualTo("namespaceUris[0]");
        StringBuilder many = new StringBuilder("{\"namespaceUris\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",")
                    .append("\"https://n.example/")
                    .append(i)
                    .append("\"");
        }
        many.append("]}");
        assertThat(issueName(api.postJson("/v1/external-vocabulary/searches", viewer(), many.toString())
                        .json()))
                .isEqualTo("namespaceUris");
        assertThat(api.postJson("/v1/external-vocabularies/content", viewer(), "{}")
                        .status())
                .isEqualTo(400);
    }

    private static String issueName(JsonNode problem) {
        return problem.get("issues").get(0).get("name").asString();
    }
}
