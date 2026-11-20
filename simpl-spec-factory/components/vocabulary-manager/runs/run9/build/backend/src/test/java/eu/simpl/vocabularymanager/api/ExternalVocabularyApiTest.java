package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import eu.simpl.vocabularymanager.support.Http;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** External vocabulary operations (D30, D38, D41, D42, D56, D66, D70, D73). */
class ExternalVocabularyApiTest extends ApiTestBase {

    private Http.Response register(String name, String namespace, String... aliases) {
        Http.Multipart form = new Http.Multipart()
                .file("@prefix t: <" + namespace
                        + "> .\nt:title a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .\n")
                .field("name", name)
                .field("namespaceUri", namespace)
                .field("description", "Copy for validation.")
                .field("sourceUri", "https://example.org/source.ttl");
        for (String alias : aliases) {
            form.field("aliasUris", alias);
        }
        return http().multipart("/v1/external-vocabularies", admin(), form);
    }

    @Test
    void registerFindSearchDownloadAndDelete() {
        String name = uniqueName("Ext");
        String namespace = "http://ext.example.org/" + name + "/";
        String alias = "https://ext.example.org/" + name + "#";
        Http.Response created = register(name, namespace, alias);
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/external-vocabularies/" + name);
        JsonNode body = created.json();
        assertThat(body.get("aliasUris").get(0).asString()).isEqualTo(alias);
        assertThat(body.get("createdBy").asString()).isEqualTo("ga-admin@example.com");

        assertThat(register(uniqueName("Ext"), namespace.substring(0, namespace.length() - 1))
                        .status())
                .isEqualTo(409);
        Http.Response aliasConflict = register(uniqueName("Ext"), "http://other.example.org/" + name, alias);
        assertThat(aliasConflict.json().get("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias '" + alias + "' is already registered.");
        assertThat(register(name.toLowerCase().replaceFirst("^e", "E"), "http://x.example.org/" + name)
                        .status())
                .isEqualTo(409);

        assertThat(http().get("/v1/external-vocabularies/" + name, viewer()).status())
                .isEqualTo(200);
        Http.Response content = http().get("/v1/external-vocabularies/" + name + "/content", viewer());
        assertThat(content.header("Content-Disposition")).contains(name + ".ttl");
        Http.Response byNamespace = http().json(
                        "POST",
                        "/v1/external-vocabularies/content",
                        viewer(),
                        "{\"namespaceUri\":\"https://ext.example.org/" + name + "/\"}");
        assertThat(byNamespace.status()).isEqualTo(200);
        assertThat(byNamespace.header("Content-Type")).isEqualTo("text/turtle");

        Http.Response search = http().json(
                        "POST",
                        "/v1/external-vocabulary/searches",
                        viewer(),
                        "{\"namespaceUris\":[\"" + namespace + "\",\"https://nowhere.example.org\"],\"aliasUris\":[\""
                                + alias + "\"]}");
        assertThat(search.status()).isEqualTo(200);
        assertThat(search.json().get("items").size()).isEqualTo(1);
        assertThat(http().json(
                                "POST",
                                "/v1/external-vocabulary/searches",
                                viewer(),
                                "{\"namespaceUri\":\"" + namespace + "\",\"aliasUri\":\"" + alias + "\"}")
                        .json()
                        .get("issues")
                        .get(0)
                        .get("name")
                        .asString())
                .isEqualTo("body");
        assertThat(http().json("POST", "/v1/external-vocabulary/searches", viewer(), "{}")
                        .status())
                .isEqualTo(400);

        String user = uniqueName("User");
        Http.Response internal =
                create(user, "@prefix x: <" + namespace + "> .\n" + content(user, "ex:Scheme x:title \"t\" ."));
        assertThat(internal.json().get("validationWarnings").size()).isZero();

        JsonNode list =
                http().get("/v1/external-vocabularies?pageSize=100", viewer()).json();
        assertThat(list.get("total").asInt()).isPositive();
        assertThat(http().get("/v1/external-vocabularies?sort=name", viewer()).status())
                .isEqualTo(400);

        assertThat(http().send("DELETE", "/v1/external-vocabularies/" + name, viewer(), null, null)
                        .status())
                .isEqualTo(403);
        assertThat(http().send("DELETE", "/v1/external-vocabularies/" + name, admin(), null, null)
                        .status())
                .isEqualTo(204);
        Http.Response gone = http().get("/v1/external-vocabularies/" + name, viewer());
        assertThat(gone.status()).isEqualTo(404);
        assertThat(gone.json().get("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
        Http.Response goneByUri = http().json(
                        "POST",
                        "/v1/external-vocabularies/content",
                        viewer(),
                        "{\"namespaceUri\":\"" + namespace + "\"}");
        assertThat(goneByUri.status()).isEqualTo(404);
        assertThat(goneByUri.json().get("issues").get(0).get("name").asString()).isEqualTo("namespaceUri");
    }

    @Test
    void registrationRules() {
        String name = uniqueName("Rules");
        assertThat(register(name, "not a uri").status()).isEqualTo(400);
        Http.Response duplicateAlias =
                register(name, "http://r.example.org/" + name, "http://r.example.org/" + name + "/");
        assertThat(duplicateAlias.json().get("issues").get(0).get("name").asString())
                .isEqualTo("aliasUris");
        Http.Response invalid = http().multipart(
                        "/v1/external-vocabularies",
                        admin(),
                        new Http.Multipart()
                                .file("this is ( not turtle")
                                .field("name", name)
                                .field("namespaceUri", "http://r.example.org/" + name));
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.json().get("issues").size()).isEqualTo(1);
        Http.Response empty = http().multipart(
                        "/v1/external-vocabularies",
                        admin(),
                        new Http.Multipart()
                                .file("# only a comment\n")
                                .field("name", name)
                                .field("namespaceUri", "http://r.example.org/" + name));
        assertThat(empty.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Vocabulary contains no statements");
        Http.Multipart many = new Http.Multipart()
                .file("@prefix a: <http://a.example.org/> . a:x a a:y .")
                .field("name", name)
                .field("namespaceUri", "http://r.example.org/" + name);
        for (int i = 0; i < 21; i++) {
            many.field("aliasUris", "http://alias.example.org/" + name + "/" + i);
        }
        assertThat(http().multipart("/v1/external-vocabularies", admin(), many).status())
                .isEqualTo(400);
    }
}
