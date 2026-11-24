package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** External vocabularies (D30, D38, D41, D42, D56, D66, D70, D73, D147, D148). */
class ExternalApiTest extends ApiTestBase {

    private Reply register(String name, String namespace, String file, String... aliases) {
        Form form = new Form().field("name", name).field("namespaceUri", namespace);
        for (String alias : aliases) {
            form.field("aliasUris", alias);
        }
        return post(
                "/v1/external-vocabularies",
                admin,
                form.field("description", "Cached copy")
                        .field("sourceUri", "https://source.example/x.ttl")
                        .file(file));
    }

    @Test
    void registerReadSearchAndRemove() {
        String name = uniqueName("Dc");
        String namespace = "http://purl.example/" + name + "/";
        Reply created = register(
                name,
                namespace,
                "@prefix d: <" + namespace + "> . d:title a d:Property .",
                "https://alias.example/" + name,
                "http://second.example/" + name + "#");
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/external-vocabularies/" + name);
        assertThat(created.json().path("aliasUris").size()).isEqualTo(2);
        assertThat(created.json().path("createdBy").asString()).isEqualTo("ga-admin@example.com");

        assertThat(get("/v1/external-vocabularies/" + name, viewer)
                        .json()
                        .path("namespaceUri")
                        .asString())
                .isEqualTo(namespace);
        Reply content = get("/v1/external-vocabularies/" + name + "/content", viewer);
        assertThat(content.header("Content-Type")).isEqualTo("text/turtle");
        assertThat(content.header("Content-Disposition")).contains(name + ".ttl");
        Reply byNamespace = json(
                "POST",
                "/v1/external-vocabularies/content",
                viewer,
                "{\"namespaceUri\":\"https://alias.example/" + name + "/\"}");
        assertThat(byNamespace.status()).isEqualTo(200);
        Reply missing = json(
                "POST",
                "/v1/external-vocabularies/content",
                viewer,
                "{\"namespaceUri\":\"https://none.example/" + name + "\"}");
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.json().path("detail").asString())
                .isEqualTo("External vocabulary with namespaceUri 'https://none.example/" + name + "' was not found.");
        assertThat(json("POST", "/v1/external-vocabularies/content", viewer, "{\"namespaceUri\":\"relative\"}")
                        .status())
                .isEqualTo(400);

        Reply search = json(
                "POST",
                "/v1/external-vocabulary/searches",
                viewer,
                "{\"namespaceUris\":[\"" + namespace + "\"],\"aliasUris\":[\"http://second.example/" + name
                        + "\",\"http://x.example/\"]}");
        assertThat(search.json().path("total").asInt()).isEqualTo(1);
        assertThat(json(
                                "POST",
                                "/v1/external-vocabulary/searches",
                                viewer,
                                "{\"aliasUri\":\"https://alias.example/" + name + "#\"}")
                        .json()
                        .path("total")
                        .asInt())
                .isEqualTo(1);
        assertThat(json("POST", "/v1/external-vocabulary/searches", viewer, "{\"namespaceUri\":\"" + namespace + "\"}")
                        .json()
                        .path("total")
                        .asInt())
                .isEqualTo(1);
        for (String body : new String[] {
            "{}",
            "{\"namespaceUri\":\"http://a\",\"aliasUri\":\"http://b\"}",
            "{\"namespaceUri\":\"http://a\",\"namespaceUris\":[\"http://b\"]}"
        }) {
            assertThat(json("POST", "/v1/external-vocabulary/searches", viewer, body)
                            .json()
                            .path("issues")
                            .get(0)
                            .path("name")
                            .asString())
                    .isEqualTo("body");
        }
        assertThat(json("POST", "/v1/external-vocabulary/searches", viewer, "{\"aliasUris\":[]}")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("aliasUris");
        assertThat(json("POST", "/v1/external-vocabulary/searches", viewer, "{\"aliasUris\":[\"bad\"]}")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("aliasUris[0]");
        StringBuilder many = new StringBuilder("{\"namespaceUris\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",")
                    .append("\"http://n.example/")
                    .append(i)
                    .append("\"");
        }
        assertThat(json(
                                "POST",
                                "/v1/external-vocabulary/searches",
                                viewer,
                                many.append("]}").toString())
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("namespaceUris");

        Reply list = get("/v1/external-vocabularies?pageSize=100", viewer);
        assertThat(list.status()).isEqualTo(200);
        assertThat(get("/v1/external-vocabularies?sort=name", viewer).status()).isEqualTo(400);
        assertThat(get("/v1/external-vocabularies?page=100000", viewer)
                        .json()
                        .path("items")
                        .size())
                .isZero();

        assertThat(send("DELETE", "/v1/external-vocabularies/" + name, admin, null, null)
                        .status())
                .isEqualTo(204);
        Reply gone = get("/v1/external-vocabularies/" + name, viewer);
        assertThat(gone.status()).isEqualTo(404);
        assertThat(gone.json().path("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
        assertThat(get("/v1/external-vocabularies/" + name + "/content", viewer).status())
                .isEqualTo(404);
        assertThat(send("DELETE", "/v1/external-vocabularies/" + name, admin, null, null)
                        .status())
                .isEqualTo(404);
    }

    @Test
    void registrationRulesAndConflicts() {
        String name = uniqueName("Cf");
        String namespace = "http://conflict.example/" + name + "#";
        String file = "@prefix d: <" + namespace + "> . d:a d:b d:c .";
        assertThat(register(name, namespace, file, "http://alias.example/" + name)
                        .status())
                .isEqualTo(201);
        JsonNode nameConflict = register(
                        name.toLowerCase().replaceFirst("^c", "C"), "http://other.example/" + name, file)
                .json();
        assertThat(nameConflict.path("detail").asString())
                .isEqualTo("An external vocabulary with name '"
                        + name.toLowerCase().replaceFirst("^c", "C") + "' is already registered.");
        Reply namespaceConflict = register(name + "B", "http://conflict.example/" + name + "/", file);
        assertThat(namespaceConflict.status()).isEqualTo(409);
        assertThat(namespaceConflict.json().path("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias 'http://conflict.example/" + name
                        + "/' is already registered.");
        Reply aliasConflict = register(
                name + "C",
                "http://new.example/" + name,
                file,
                "http://fresh.example/" + name,
                "http://alias.example/" + name + "/");
        assertThat(aliasConflict.json().path("detail").asString()).contains("http://alias.example/" + name + "/");

        assertThat(register(name + "D", "not a uri", file)
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("namespaceUri");
        assertThat(register(name + "D", "http://d.example/" + name, file, "http://d.example/" + name + "/")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("aliasUris");
        assertThat(register(name + "D", "http://d.example/" + name, file, "a,b")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("aliasUris");
        String[] many = new String[21];
        for (int i = 0; i < many.length; i++) {
            many[i] = "http://many.example/" + name + "/" + i;
        }
        assertThat(register(name + "D", "http://d.example/" + name, file, many).status())
                .isEqualTo(400);
        Reply syntax = register(name + "D", "http://d.example/" + name, "@prefix d: <x> . d:a d:b .");
        assertThat(syntax.json().path("issues").get(0).path("detail").asString())
                .startsWith("Encountered invalid Turtle syntax");
        Reply empty = register(name + "D", "http://d.example/" + name, "@prefix d: <http://d.example/> .");
        assertThat(empty.json().path("issues").get(0).path("detail").asString())
                .isEqualTo("Vocabulary contains no statements");
        Reply blank = post(
                "/v1/external-vocabularies",
                admin,
                new Form()
                        .field("name", name + "E")
                        .field("namespaceUri", "http://e.example/" + name)
                        .field("description", "   ")
                        .field("sourceUri", "bad")
                        .file(file));
        assertThat(blank.json().path("issues").get(0).path("name").asString()).isEqualTo("sourceUri");
        Reply noDescription = post(
                "/v1/external-vocabularies",
                admin,
                new Form()
                        .field("name", name + "F")
                        .field("namespaceUri", "http://f.example/" + name)
                        .field("description", "   ")
                        .file(file));
        assertThat(noDescription.status()).isEqualTo(201);
        assertThat(noDescription.json().has("description")).isFalse();
        assertThat(noDescription.json().path("aliasUris").isArray()).isTrue();
        assertThat(post("/v1/external-vocabularies", admin, new Form().field("name", name + "G"))
                        .json()
                        .path("issues")
                        .size())
                .isEqualTo(2);
        assertThat(get("/v1/external-vocabularies/lower", viewer).status()).isEqualTo(400);
    }
}
