package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.Api;
import eu.simpl.vocabularymanager.support.Api.Part;
import eu.simpl.vocabularymanager.support.Api.Result;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** External vocabularies (D30, D38, D41, D42, D56, D66, D70, D73). */
class ExternalVocabularyApiTest extends IntegrationTestBase {

    private Result register(String name, String namespace, String turtle, String... aliases) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.field("name", name));
        parts.add(Part.field("namespaceUri", namespace));
        for (String alias : aliases) {
            parts.add(Part.field("aliasUris", alias));
        }
        parts.add(Part.field("description", "External " + name));
        parts.add(Part.file(turtle));
        return api.multipart("/v1/external-vocabularies", ENV.admin(), parts.toArray(Part[]::new));
    }

    @Test
    void registerGetListContentAndDelete() {
        String id = Api.unique();
        String name = "Ext" + id;
        String namespace = "https://ext.example/" + id + "#";
        String turtle = simpleTurtle(id);
        Result registered = register(name, namespace, turtle, "https://alias.example/" + id + "/",
                "https://alias2.example/" + id);
        assertThat(registered.status()).as(registered.text()).isEqualTo(201);
        assertThat(registered.header("Location")).isEqualTo("/external-vocabularies/" + name);
        JsonNode body = registered.json();
        assertThat(body.path("href").asString()).isEqualTo("/external-vocabularies/" + name);
        assertThat(body.path("namespaceUri").asString()).isEqualTo(namespace);
        assertThat(body.path("aliasUris").get(0).asString()).isEqualTo("https://alias.example/" + id + "/");
        assertThat(body.path("aliasUris").get(1).asString()).isEqualTo("https://alias2.example/" + id);
        assertThat(body.path("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.has("creationDate")).isTrue();

        assertThat(api.get("/v1/external-vocabularies/" + name, ENV.viewer()).json().path("name").asString())
                .isEqualTo(name);
        Result content = api.get("/v1/external-vocabularies/" + name + "/content", ENV.viewer());
        assertThat(content.text()).isEqualTo(turtle);
        assertThat(content.contentType()).isEqualTo("text/turtle");
        assertThat(content.header("Content-Disposition")).contains(name + ".ttl");
        Result list = api.get("/v1/external-vocabularies?pageSize=100", ENV.viewer());
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.json().path("total").asInt()).isPositive();
        assertThat(api.get("/v1/external-vocabularies?sort=name", ENV.viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/external-vocabularies?pageSize=101", ENV.viewer()).status()).isEqualTo(400);

        Result byNamespace = api.json("POST", "/v1/external-vocabularies/content", ENV.viewer(),
                "{\"namespaceUri\":\"https://alias2.example/" + id + "/\"}");
        assertThat(byNamespace.status()).isEqualTo(200);
        assertThat(byNamespace.text()).isEqualTo(turtle);
        Result unknownNamespace = api.json("POST", "/v1/external-vocabularies/content", ENV.viewer(),
                "{\"namespaceUri\":\"https://unknown.example/" + id + "\"}");
        assertThat(unknownNamespace.status()).isEqualTo(404);
        assertThat(unknownNamespace.json().path("detail").asString())
                .isEqualTo("External vocabulary with namespaceUri 'https://unknown.example/" + id
                        + "' was not found.");
        assertThat(unknownNamespace.issueNames()).containsExactly("namespaceUri");
        assertThat(api.json("POST", "/v1/external-vocabularies/content", ENV.viewer(),
                        "{\"namespaceUri\":\"relative\"}").status())
                .isEqualTo(400);
        assertThat(api.json("POST", "/v1/external-vocabularies/content", ENV.viewer(),
                        "{\"namespaceUri\":\"https://a.example/\",\"x\":1}").status())
                .isEqualTo(400);

        // A registered namespace no longer gives externalReferenceSkipped (D12).
        Result internal = create("UsesExt" + id, "@prefix e: <" + namespace + "> .\n"
                + "@prefix a: <https://alias.example/" + id + "> .\n"
                + "<https://own.example/" + id + "#x> e:p a:q .\n");
        assertThat(internal.json().path("validationWarnings").size()).isZero();

        assertThat(api.delete("/v1/external-vocabularies/" + name, ENV.admin()).status()).isEqualTo(204);
        Result gone = api.get("/v1/external-vocabularies/" + name, ENV.viewer());
        assertThat(gone.status()).isEqualTo(404);
        assertThat(gone.json().path("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
        assertThat(gone.issueNames()).containsExactly("name");
        assertThat(api.get("/v1/external-vocabularies/" + name + "/content", ENV.viewer()).status())
                .isEqualTo(404);
        assertThat(api.delete("/v1/external-vocabularies/" + name, ENV.admin()).status()).isEqualTo(404);
        // Internal vocabulary untouched (D42).
        assertThat(api.get("/v1/vocabularies/UsesExt" + id, ENV.viewer()).status()).isEqualTo(200);
        assertThat(api.get("/v1/external-vocabularies/bad", ENV.viewer()).status()).isEqualTo(400);
    }

    @Test
    void registrationConflictsAndRules() {
        String id = Api.unique();
        String name = "Conf" + id;
        String namespace = "https://conf.example/" + id + "/";
        assertThat(register(name, namespace, simpleTurtle(id), "https://conf-alias.example/" + id).status())
                .isEqualTo(201);
        Result sameName = register("CONF" + id.toUpperCase(), "https://other.example/" + id, simpleTurtle(id));
        assertThat(sameName.status()).isEqualTo(409);
        Result sameNamespace = register("Conf2" + id, "https://conf.example/" + id, simpleTurtle(id));
        assertThat(sameNamespace.status()).isEqualTo(409);
        assertThat(sameNamespace.json().path("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias 'https://conf.example/" + id
                        + "' is already registered.");
        Result aliasClash = register("Conf3" + id, "https://third.example/" + id, simpleTurtle(id),
                "https://conf-alias.example/" + id + "#");
        assertThat(aliasClash.status()).isEqualTo(409);
        // An internal vocabulary may share the name (D56).
        create(name, simpleTurtle(id));

        Result aliasEqualsNamespace = register("Conf4" + id, "https://four.example/" + id, simpleTurtle(id),
                "https://four.example/" + id + "/");
        assertThat(aliasEqualsNamespace.status()).isEqualTo(400);
        assertThat(aliasEqualsNamespace.issueNames()).containsExactly("aliasUris");
        Result duplicateAlias = register("Conf5" + id, "https://five.example/" + id, simpleTurtle(id),
                "https://a.example/" + id, "https://a.example/" + id + "#");
        assertThat(duplicateAlias.status()).isEqualTo(400);
        String[] tooMany = new String[21];
        for (int i = 0; i < tooMany.length; i++) {
            tooMany[i] = "https://many.example/" + id + "/" + i;
        }
        assertThat(register("Conf6" + id, "https://six.example/" + id, simpleTurtle(id), tooMany).status())
                .isEqualTo(400);
        assertThat(register("Conf7" + id, "https://seven.example/" + id, simpleTurtle(id), "a,b").status())
                .isEqualTo(400);
        assertThat(register("Conf8" + id, "not absolute", simpleTurtle(id)).status()).isEqualTo(400);
        Result invalidTurtle = register("Conf9" + id, "https://nine.example/" + id, "not turtle at all");
        assertThat(invalidTurtle.status()).isEqualTo(400);
        assertThat(invalidTurtle.json().path("issues").size()).isEqualTo(1);
        assertThat(invalidTurtle.issueNames()).containsExactly("vocabularyFile");
        Result empty = register("Conf10" + id, "https://ten.example/" + id, "");
        assertThat(empty.status()).isEqualTo(400);
        assertThat(empty.issueDetails()).containsExactly("Vocabulary contains no statements");
        // Semantic problems are not checked on registration (D30).
        String semantic = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "<https://x.example/p> a owl:ObjectProperty , owl:DatatypeProperty .\n";
        Result notChecked = register("Conf11" + id, "https://eleven.example/" + id, semantic);
        assertThat(notChecked.status()).isEqualTo(201);
        assertThat(notChecked.json().has("validationWarnings")).isFalse();
        Result longDescription = api.multipart("/v1/external-vocabularies", ENV.admin(),
                Part.field("name", "Conf12" + id), Part.field("namespaceUri", "https://twelve.example/" + id),
                Part.field("description", "x".repeat(1001)), Part.file(simpleTurtle(id)));
        assertThat(longDescription.status()).isEqualTo(400);
        assertThat(longDescription.issueNames()).containsExactly("description");
        Result missing = api.multipart("/v1/external-vocabularies", ENV.admin(), Part.field("name", "Conf13" + id));
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.issueNames()).contains("namespaceUri", "vocabularyFile");
    }

    @Test
    void searchByUris() {
        String id = Api.unique();
        register("SearchA" + id, "https://sa.example/" + id + "#", simpleTurtle(id), "https://sa-alias.example/" + id);
        register("SearchB" + id, "https://sb.example/" + id, simpleTurtle(id));
        Result result = api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(),
                "{\"namespaceUris\":[\"https://sb.example/" + id + "/\",\"https://none.example/\"],"
                        + "\"aliasUris\":[\"https://sa-alias.example/" + id + "#\",\"https://sa.example/" + id
                        + "\"]}");
        assertThat(result.status()).isEqualTo(200);
        JsonNode items = result.json().path("items");
        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0).path("name").asString()).isEqualTo("SearchB" + id);
        assertThat(items.get(1).path("name").asString()).isEqualTo("SearchA" + id);
        assertThat(result.json().path("total").asInt()).isEqualTo(2);

        Result single = api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(),
                "{\"aliasUri\":\"https://sa-alias.example/" + id + "\"}");
        assertThat(single.json().path("items").size()).isEqualTo(1);
        Result byNamespace = api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(),
                "{\"namespaceUri\":\"https://sa.example/" + id + "\"}");
        assertThat(byNamespace.json().path("items").size()).isEqualTo(1);
        Result none = api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(),
                "{\"namespaceUri\":\"https://zero.example/" + id + "\"}");
        assertThat(none.json().path("totalPages").asInt()).isZero();

        for (String bad : List.of("{}", "{\"namespaceUri\":\"https://a.example\",\"aliasUri\":\"https://b.example\"}",
                "{\"namespaceUri\":\"https://a.example\",\"namespaceUris\":[\"https://b.example\"]}",
                "{\"namespaceUris\":[]}", "{\"namespaceUri\":\"relative\"}", "{\"namespaceUris\":[1]}",
                "{\"other\":1}", "[]", "{\"namespaceUri\":null}")) {
            Result rejected = api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(), bad);
            assertThat(rejected.status()).as(bad).isEqualTo(400);
        }
        assertThat(api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(), "{}").issueNames())
                .containsExactly("body");
        StringBuilder uris = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            uris.append(i == 0 ? "" : ",").append("\"https://u.example/").append(i).append('"');
        }
        Result tooMany = api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(),
                "{\"namespaceUris\":[" + uris + "],\"aliasUris\":[" + uris + "]}");
        assertThat(tooMany.status()).isEqualTo(400);
        assertThat(tooMany.issueNames()).containsExactly("aliasUris");
        Result tooManyNamespaces = api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(),
                "{\"namespaceUris\":[" + uris + "," + uris + "]}");
        assertThat(tooManyNamespaces.issueNames()).containsExactly("namespaceUris");
    }
}
