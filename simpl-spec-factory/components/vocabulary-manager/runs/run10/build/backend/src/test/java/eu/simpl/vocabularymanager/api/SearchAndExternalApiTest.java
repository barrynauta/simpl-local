package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Searches (D38, D68) and external vocabularies (D30, D41, D56, D66, D70, D73). */
class SearchAndExternalApiTest extends IntegrationTestBase {

    @Test
    void internalSearchKeepsOrderDeduplicatesAndOmitsMissing() {
        String name = uniqueName("Srch");
        api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", simpleTurtle(name)));
        api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("D", "c", simpleTurtle(name) + "#2\n"));
        String body = "{\"items\":[{\"name\":\"" + name + "\",\"version\":1},{\"name\":\"" + name
                + "\"},{\"name\":\"Missing" + name + "\"},{\"name\":\"" + name + "\",\"version\":1},{\"name\":\""
                + name + "\",\"version\":2}]}";
        ApiClient.Response response = api().json("POST", "/v1/vocabularies/searches", IDP.viewer(), body);
        assertThat(response.status()).isEqualTo(200);
        JsonNode json = response.json();
        assertThat(json.get("total").asInt()).isEqualTo(3);
        assertThat(json.get("page").asInt()).isEqualTo(1);
        assertThat(json.get("pageSize").asInt()).isEqualTo(100);
        assertThat(json.get("totalPages").asInt()).isEqualTo(1);
        assertThat(json.has("self")).isFalse();
        JsonNode first = json.get("items").get(0);
        assertThat(first.get("latest").asBoolean()).isFalse();
        assertThat(first.get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(first.get("href").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        JsonNode second = json.get("items").get(1);
        assertThat(second.get("latest").asBoolean()).isTrue();
        assertThat(second.get("version").asInt()).isEqualTo(2);
        assertThat(second.get("href").asString()).isEqualTo("/vocabularies/" + name);
        assertThat(second.get("contentHref").asString()).isEqualTo(CONTENT_BASE + "/content/" + name);
        assertThat(json.get("items").get(2).get("latest").asBoolean()).isFalse();
    }

    @Test
    void internalSearchValidation() {
        assertIssue("{\"items\":[]}", "items");
        assertIssue("{\"items\":[{\"name\":\"bad\"}]}", "items[0].name");
        assertIssue("{\"items\":[{\"name\":\"Abcd\",\"version\":2.0}]}", "items[0].version");
        assertIssue("{\"items\":[{\"name\":\"Abcd\",\"version\":0}]}", "items[0].version");
        assertIssue("{\"items\":[{\"name\":\"Abcd\",\"extra\":1}]}", "items[0].extra");
        assertIssue("{\"items\":[1]}", "items[0]");
        assertIssue("{\"items\":[{\"name\":\"Abcd\"}],\"page\":1}", "page");
        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Abcd\"}");
        }
        assertIssue(many.append("]}").toString(), "items");
        assertThat(api().json("POST", "/v1/vocabularies/searches", IDP.viewer(), "[1]")
                        .status())
                .isEqualTo(400);
        assertThat(api().json("POST", "/v1/vocabularies/searches", IDP.viewer(), "{\"items\":")
                        .status())
                .isEqualTo(400);
        assertThat(api().json(
                                "POST",
                                "/v1/vocabularies/searches?page=1",
                                IDP.viewer(),
                                "{\"items\":[{\"name\":\"Abc\"}]}")
                        .status())
                .isEqualTo(400);
    }

    @Test
    void externalVocabularyRegistrationLookupAndRemoval() {
        String name = uniqueName("Dc");
        String namespace = "http://purl.example.org/" + name + "/terms/";
        String alias = "https://purl.example.org/" + name + "/terms";
        List<ApiClient.Part> parts = new ArrayList<>(List.of(
                ApiClient.Part.text("name", name),
                ApiClient.Part.text("namespaceUri", namespace),
                ApiClient.Part.text("aliasUris", alias),
                ApiClient.Part.text("aliasUris", "urn:x:" + name),
                ApiClient.Part.text("description", "Cached copy."),
                ApiClient.Part.text("sourceUri", "https://www.example.org/dc.ttl"),
                ApiClient.Part.file("vocabularyFile", simpleTurtle(name).getBytes())));
        ApiClient.Response created = api().multipart("/v1/external-vocabularies", IDP.admin(), parts);
        assertThat(created.status()).as(created.text()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/external-vocabularies/" + name);
        assertThat(created.json().get("aliasUris").get(0).asString()).isEqualTo(alias);
        assertThat(created.json().get("aliasUris").get(1).asString()).isEqualTo("urn:x:" + name);
        assertThat(created.json().get("createdBy").asString()).isEqualTo("ga-admin@example.com");

        JsonNode got =
                api().get("/v1/external-vocabularies/" + name, IDP.viewer()).json();
        assertThat(got.get("namespaceUri").asString()).isEqualTo(namespace);
        assertThat(got.get("sourceUri").asString()).isEqualTo("https://www.example.org/dc.ttl");

        ApiClient.Response content = api().get("/v1/external-vocabularies/" + name + "/content", IDP.viewer());
        assertThat(content.header("Content-Type")).isEqualTo("text/turtle");
        assertThat(content.text()).isEqualTo(simpleTurtle(name));

        ApiClient.Response byNamespace = api().json(
                        "POST",
                        "/v1/external-vocabularies/content",
                        IDP.viewer(),
                        "{\"namespaceUri\":\"" + alias + "/\"}");
        assertThat(byNamespace.status()).isEqualTo(200);
        assertThat(byNamespace.header("Content-Disposition")).contains(name + ".ttl");

        JsonNode search = api().json(
                        "POST",
                        "/v1/external-vocabulary/searches",
                        IDP.viewer(),
                        "{\"namespaceUris\":[\"" + namespace.substring(0, namespace.length() - 1) + "\"],"
                                + "\"aliasUris\":[\"" + alias + "#\"]}")
                .json();
        assertThat(search.get("total").asInt()).isEqualTo(1);
        assertThat(search.get("items").get(0).get("name").asString()).isEqualTo(name);

        ApiClient.Response conflict = api().multipart(
                        "/v1/external-vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", uniqueName("Other")),
                                ApiClient.Part.text("namespaceUri", alias + "#"),
                                ApiClient.Part.file("vocabularyFile", "bad".getBytes())));
        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.json().get("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias '" + alias + "#' is already registered.");

        ApiClient.Response sameName = api().multipart(
                        "/v1/external-vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", name.toUpperCase()),
                                ApiClient.Part.text("namespaceUri", "https://other.example.org/" + name),
                                ApiClient.Part.file(
                                        "vocabularyFile", simpleTurtle(name).getBytes())));
        assertThat(sameName.status()).isEqualTo(409);

        assertThat(api().delete("/v1/external-vocabularies/" + name, IDP.admin())
                        .status())
                .isEqualTo(204);
        ApiClient.Response gone = api().get("/v1/external-vocabularies/" + name, IDP.viewer());
        assertThat(gone.status()).isEqualTo(404);
        assertThat(gone.json().get("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
        assertThat(api().get("/v1/external-vocabularies/" + name + "/content", IDP.viewer())
                        .status())
                .isEqualTo(404);
        assertThat(api().delete("/v1/external-vocabularies/" + name, IDP.admin())
                        .status())
                .isEqualTo(404);
        ApiClient.Response unknown = api().json(
                        "POST",
                        "/v1/external-vocabularies/content",
                        IDP.viewer(),
                        "{\"namespaceUri\":\"" + alias + "\"}");
        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.json().get("detail").asString())
                .isEqualTo("External vocabulary with namespaceUri '" + alias + "' was not found.");
        assertThat(unknown.json().get("issues").get(0).get("name").asString()).isEqualTo("namespaceUri");
    }

    @Test
    void externalRegistrationValidation() {
        String name = uniqueName("Bad");
        ApiClient.Response response = api().multipart(
                        "/v1/external-vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", name),
                                ApiClient.Part.text("namespaceUri", "https://a.example.org/ns#"),
                                ApiClient.Part.text("aliasUris", "https://a.example.org/ns/"),
                                ApiClient.Part.text("aliasUris", "a,b"),
                                ApiClient.Part.text("sourceUri", "relative/path"),
                                ApiClient.Part.file(
                                        "vocabularyFile", simpleTurtle(name).getBytes())));
        assertThat(response.status()).isEqualTo(400);
        List<String> names = response.json()
                .get("issues")
                .valueStream()
                .map(issue -> issue.get("name").asString())
                .toList();
        assertThat(names).containsExactly("aliasUris", "aliasUris", "sourceUri");

        ApiClient.Response empty = api().multipart(
                        "/v1/external-vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", name),
                                ApiClient.Part.text("namespaceUri", "https://empty.example.org/" + name),
                                ApiClient.Part.file("vocabularyFile", "@prefix a: <urn:a> .".getBytes())));
        assertThat(empty.status()).isEqualTo(400);
        assertThat(empty.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Vocabulary contains no statements");

        ApiClient.Response syntax = api().multipart(
                        "/v1/external-vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", name),
                                ApiClient.Part.text("namespaceUri", "https://syntax.example.org/" + name),
                                ApiClient.Part.file("vocabularyFile", "<a> <b> .".getBytes())));
        assertThat(syntax.status()).isEqualTo(400);
        assertThat(syntax.json().get("issues")).hasSize(1);
    }

    @Test
    void externalSearchValidation() {
        assertExternalIssue("{}", "body");
        assertExternalIssue("{\"namespaceUri\":\"http://a\",\"aliasUri\":\"http://b\"}", "body");
        assertExternalIssue("{\"namespaceUri\":\"http://a\",\"namespaceUris\":[\"http://b\"]}", "body");
        assertExternalIssue("{\"namespaceUris\":[]}", "namespaceUris");
        assertExternalIssue("{\"aliasUris\":[\"not a uri\"]}", "aliasUris[0]");
        assertExternalIssue("{\"aliasUri\":\"relative\"}", "aliasUri");
        assertExternalIssue("{\"other\":1,\"aliasUri\":\"http://a\"}", "other");
        StringBuilder many = new StringBuilder("{\"namespaceUris\":[");
        for (int i = 0; i < 60; i++) {
            many.append(i == 0 ? "" : ",").append("\"http://a/").append(i).append("\"");
        }
        many.append("],\"aliasUris\":[");
        for (int i = 0; i < 41; i++) {
            many.append(i == 0 ? "" : ",").append("\"http://b/").append(i).append("\"");
        }
        assertExternalIssue(many.append("]}").toString(), "aliasUris");
        ApiClient.Response none = api().json(
                        "POST",
                        "/v1/external-vocabulary/searches",
                        IDP.viewer(),
                        "{\"aliasUri\":\"http://nothing.example\"}");
        assertThat(none.status()).isEqualTo(200);
        assertThat(none.json().get("totalPages").asInt()).isZero();
        assertThat(api().get("/v1/external-vocabularies?page=1&pageSize=5", IDP.viewer())
                        .status())
                .isEqualTo(200);
        assertThat(api().get("/v1/external-vocabularies?sort=name", IDP.viewer())
                        .status())
                .isEqualTo(400);
    }

    private void assertIssue(String body, String name) {
        ApiClient.Response response = api().json("POST", "/v1/vocabularies/searches", IDP.viewer(), body);
        assertThat(response.status()).as(body).isEqualTo(400);
        assertThat(response.json()
                        .get("issues")
                        .valueStream()
                        .map(issue -> issue.get("name").asString()))
                .as(body)
                .contains(name);
    }

    private void assertExternalIssue(String body, String name) {
        ApiClient.Response response = api().json("POST", "/v1/external-vocabulary/searches", IDP.viewer(), body);
        assertThat(response.status()).as(body).isEqualTo(400);
        assertThat(response.json()
                        .get("issues")
                        .valueStream()
                        .map(issue -> issue.get("name").asString()))
                .as(body)
                .contains(name);
    }
}
