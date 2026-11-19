package eu.simpl.vocabulary.api;

import static eu.simpl.vocabulary.support.Http.field;
import static eu.simpl.vocabulary.support.Http.file;
import static eu.simpl.vocabulary.support.Http.parts;
import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabulary.support.Http;
import eu.simpl.vocabulary.support.TestTokens;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** External vocabulary operations (D30, D38, D41, D42, D56, D66, D70, D73). */
class ExternalVocabularyApiIT extends AbstractApiIT {

    static final String ADMIN = TestTokens.admin();
    static final String VIEWER = TestTokens.viewer();
    static final String CONTENT =
            "@prefix dcterms: <http://purl.org/dc/terms/> .\ndcterms:title a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .\n";

    HttpResponse<String> register(String name, String namespace, List<String> aliases, String content) {
        List<Http.Part> parts = parts(
                field("name", name),
                field("namespaceUri", namespace),
                field("description", " Cached copy "),
                field("sourceUri", "https://www.dublincore.org/dc.ttl"),
                file("vocabularyFile", content));
        aliases.forEach(a -> parts.add(field("aliasUris", a)));
        return http.multipart("/v1/external-vocabularies", ADMIN, parts);
    }

    static JsonNode body(HttpResponse<String> response) {
        return VocabularyApiIT.body(response);
    }

    @Test
    void registerSearchDownloadDelete() {
        String name = unique("Ext");
        String namespace = "http://ns.test/" + name + "/";
        String alias = "https://ns.test/" + name + "#";
        HttpResponse<String> created = register(name, namespace, List.of(alias), CONTENT);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("Location")).contains("/external-vocabularies/" + name);
        JsonNode external = body(created);
        assertThat(external.get("namespaceUri").asString()).isEqualTo(namespace);
        assertThat(external.get("aliasUris").get(0).asString()).isEqualTo(alias);
        assertThat(external.get("description").asString()).isEqualTo("Cached copy");
        assertThat(external.get("createdBy").asString()).isEqualTo("ga-admin@example.com");

        assertThat(http.get("/v1/external-vocabularies/" + name, VIEWER).statusCode())
                .isEqualTo(200);
        JsonNode list = body(http.get("/v1/external-vocabularies?pageSize=100", VIEWER));
        assertThat(list.get("total").asInt()).isPositive();
        assertThat(http.get("/v1/external-vocabularies?sort=name", VIEWER).statusCode())
                .isEqualTo(400);

        HttpResponse<String> content = http.get("/v1/external-vocabularies/" + name + "/content", VIEWER);
        assertThat(content.body()).isEqualTo(CONTENT);
        assertThat(content.headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"" + name + ".ttl\"");

        HttpResponse<String> byNamespace = http.json(
                "POST",
                "/v1/external-vocabularies/content",
                VIEWER,
                "{\"namespaceUri\":\"https://ns.test/" + name + "\"}");
        assertThat(byNamespace.statusCode()).isEqualTo(200);
        assertThat(byNamespace.body()).isEqualTo(CONTENT);
        HttpResponse<String> unknown = http.json(
                "POST", "/v1/external-vocabularies/content", VIEWER, "{\"namespaceUri\":\"http://none.test/x\"}");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(body(unknown).get("detail").asString())
                .isEqualTo("External vocabulary with namespaceUri 'http://none.test/x' was not found.");
        assertThat(http.json("POST", "/v1/external-vocabularies/content", VIEWER, "{\"namespaceUri\":\"relative\"}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("POST", "/v1/external-vocabularies/content", VIEWER, "{}")
                        .statusCode())
                .isEqualTo(400);

        HttpResponse<String> search = http.json(
                "POST",
                "/v1/external-vocabulary/searches",
                VIEWER,
                "{\"namespaceUris\":[\"http://ns.test/" + name + "\",\"http://other.test\"],\"aliasUris\":[\"" + alias
                        + "\"]}");
        assertThat(body(search).get("total").asInt()).isEqualTo(1);
        assertThat(body(http.json(
                                "POST", "/v1/external-vocabulary/searches", VIEWER, "{\"aliasUri\":\"" + alias + "\"}"))
                        .get("total")
                        .asInt())
                .isEqualTo(1);
        assertThat(http.json(
                                "POST",
                                "/v1/external-vocabulary/searches",
                                VIEWER,
                                "{\"namespaceUri\":\"a:b\",\"aliasUri\":\"a:c\"}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("POST", "/v1/external-vocabulary/searches", VIEWER, "{}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("POST", "/v1/external-vocabulary/searches", VIEWER, "{\"namespaceUris\":[]}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(http.json("POST", "/v1/external-vocabulary/searches", VIEWER, "{\"namespaceUris\":[\"no uri\"]}")
                        .statusCode())
                .isEqualTo(400);
        StringBuilder many = new StringBuilder("{\"namespaceUris\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("\"http://x.test/").append(i).append('"');
        }
        HttpResponse<String> tooMany = http.json("POST", "/v1/external-vocabulary/searches", VIEWER, many + "]}");
        assertThat(body(tooMany).get("issues").get(0).get("name").asString()).isEqualTo("namespaceUris");

        HttpResponse<String> conflict = register(unique("Ext"), alias, List.of(), CONTENT);
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(body(conflict).get("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias '" + alias + "' is already registered.");
        assertThat(register(
                                name.toLowerCase().substring(0, 1).toUpperCase()
                                        + name.substring(1).toLowerCase(),
                                "http://fresh.test/" + name,
                                List.of(),
                                CONTENT)
                        .statusCode())
                .isEqualTo(409);

        assertThat(http.send("DELETE", "/v1/external-vocabularies/" + name, VIEWER, null, null)
                        .statusCode())
                .isEqualTo(403);
        assertThat(http.send("DELETE", "/v1/external-vocabularies/" + name, ADMIN, null, null)
                        .statusCode())
                .isEqualTo(204);
        HttpResponse<String> gone = http.get("/v1/external-vocabularies/" + name, VIEWER);
        assertThat(gone.statusCode()).isEqualTo(404);
        assertThat(body(gone).get("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
        assertThat(http.send("DELETE", "/v1/external-vocabularies/" + name, ADMIN, null, null)
                        .statusCode())
                .isEqualTo(404);
        assertThat(http.get("/v1/external-vocabularies/" + name + "/content", VIEWER)
                        .statusCode())
                .isEqualTo(404);
    }

    @Test
    void registrationRules() {
        String name = unique("Rules");
        HttpResponse<String> aliasRepeatsNamespace =
                register(name, "http://r.test/" + name, List.of("http://r.test/" + name + "/"), CONTENT);
        assertThat(aliasRepeatsNamespace.statusCode()).isEqualTo(400);
        assertThat(body(aliasRepeatsNamespace).get("issues").get(0).get("name").asString())
                .isEqualTo("aliasUris");
        assertThat(register(name, "http://r.test/" + name, List.of("a,b"), CONTENT)
                        .statusCode())
                .isEqualTo(400);
        List<String> aliases = new java.util.ArrayList<>();
        for (int i = 0; i < 21; i++) {
            aliases.add("http://alias.test/" + name + "/" + i);
        }
        assertThat(register(name, "http://r.test/" + name, aliases, CONTENT).statusCode())
                .isEqualTo(400);
        assertThat(register(name, "not a uri", List.of(), CONTENT).statusCode()).isEqualTo(400);
        HttpResponse<String> syntax =
                register(name, "http://r.test/" + name, List.of(), "@prefix x: <http://x/> .\nx:a x:b .");
        assertThat(syntax.statusCode()).isEqualTo(400);
        assertThat(body(syntax).get("issues").get(0).get("name").asString()).isEqualTo("vocabularyFile");
        HttpResponse<String> empty = register(name, "http://r.test/" + name, List.of(), "@prefix x: <http://x/> .\n");
        assertThat(body(empty).get("issues").get(0).get("detail").asString())
                .isEqualTo("Vocabulary contains no statements");
        assertThat(http.get("/v1/external-vocabularies/lower", VIEWER).statusCode())
                .isEqualTo(400);
    }
}
