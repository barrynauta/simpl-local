package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** External vocabulary operations (D30, D38, D41, D42, D56, D66, D73, D147, D148). */
class ExternalApiTest extends ApiTestBase {

    private static final byte[] DC = "@prefix dc: <http://purl.org/dc/terms/> . dc:title a dc:Property .".getBytes();

    HttpResponse<String> register(List<String[]> fields, byte[] file) throws Exception {
        return multipart("/v1/external-vocabularies", admin, fields, file);
    }

    @Test
    void registerSearchDownloadDelete() throws Exception {
        String name = unique("Ext");
        String ns = "http://purl.org/" + name + "/terms/";
        String alias = "https://purl.org/" + name + "/terms/";
        HttpResponse<String> r = register(
                fields(
                        "name",
                        name,
                        "namespaceUri",
                        ns,
                        "aliasUris",
                        alias,
                        "description",
                        "  ",
                        "sourceUri",
                        "https://example.org/dc.ttl"),
                DC);
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.headers().firstValue("Location")).contains("/external-vocabularies/" + name);
        JsonNode b = body(r);
        assertThat(b.get("namespaceUri").asString()).isEqualTo(ns);
        assertThat(b.get("aliasUris").get(0).asString()).isEqualTo(alias);
        assertThat(b.has("description")).isFalse();
        assertThat(b.get("createdBy").asString()).isEqualTo("ga-admin@example.com");

        assertThat(register(
                                fields(
                                        "name",
                                        name.toLowerCase().substring(0, 1).toUpperCase()
                                                + name.substring(1).toLowerCase(),
                                        "namespaceUri",
                                        "http://x.org/" + name),
                                DC)
                        .statusCode())
                .isEqualTo(409);
        HttpResponse<String> nsConflict = register(fields("name", unique("Other"), "namespaceUri", alias), DC);
        assertThat(body(nsConflict).get("detail").asString())
                .isEqualTo("An external vocabulary with namespace or alias '" + alias + "' is already registered.");

        JsonNode got = body(get("/v1/external-vocabularies/" + name, viewer));
        assertThat(got.get("sourceUri").asString()).isEqualTo("https://example.org/dc.ttl");
        HttpResponse<String> content = get("/v1/external-vocabularies/" + name + "/content", viewer);
        assertThat(content.headers().firstValue("Content-Disposition"))
                .get()
                .asString()
                .contains(name + ".ttl");
        HttpResponse<String> byNs = json(
                "POST",
                "/v1/external-vocabularies/content",
                viewer,
                "{\"namespaceUri\":\"" + alias.substring(0, alias.length() - 1) + "#\"}");
        assertThat(byNs.statusCode()).isEqualTo(200);
        HttpResponse<String> nsMissing =
                json("POST", "/v1/external-vocabularies/content", viewer, "{\"namespaceUri\":\"http://none.org/x\"}");
        assertThat(nsMissing.statusCode()).isEqualTo(404);
        assertThat(body(nsMissing).get("issues").get(0).get("name").asString()).isEqualTo("namespaceUri");

        JsonNode s = body(json(
                "POST",
                "/v1/external-vocabulary/searches",
                viewer,
                "{\"namespaceUris\":[\"" + ns + "\"],\"aliasUris\":[\"" + alias + "\"]}"));
        assertThat(s.get("total").asInt()).isEqualTo(1);
        assertThat(body(json("POST", "/v1/external-vocabulary/searches", viewer, "{\"aliasUri\":\"" + alias + "\"}"))
                        .get("total")
                        .asInt())
                .isEqualTo(1);
        assertThat(body(json(
                                "POST",
                                "/v1/external-vocabulary/searches",
                                viewer,
                                "{\"namespaceUri\":\"http://nothing.org/\"}"))
                        .get("totalPages")
                        .asInt())
                .isZero();

        JsonNode list = body(get("/v1/external-vocabularies?pageSize=100", viewer));
        assertThat(list.get("total").asInt()).isGreaterThanOrEqualTo(1);

        // a reference to the registered namespace gives no warning (D12)
        HttpResponse<String> voc =
                createVocabulary(unique("UsesExt"), ttl("@prefix d: <" + ns + "> .\nex:a d:title \"x\" .\n"));
        assertThat(body(voc).get("validationWarnings").size()).isZero();

        assertThat(json("DELETE", "/v1/external-vocabularies/" + name, admin, "")
                        .statusCode())
                .isEqualTo(204);
        assertThat(get("/v1/external-vocabularies/" + name, viewer).statusCode())
                .isEqualTo(404);
        assertThat(get("/v1/external-vocabularies/" + name + "/content", viewer).statusCode())
                .isEqualTo(404);
        HttpResponse<String> del = json("DELETE", "/v1/external-vocabularies/" + name, admin, "");
        assertThat(body(del).get("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
    }

    @Test
    void registrationValidation() throws Exception {
        String name = unique("Bad");
        assertThat(register(fields("name", name, "namespaceUri", "not a uri"), DC)
                        .statusCode())
                .isEqualTo(400);
        assertThat(register(fields("name", name, "namespaceUri", "http://a.org/x", "aliasUris", "http://a.org/x/"), DC)
                        .statusCode())
                .isEqualTo(400);
        assertThat(register(
                                fields(
                                        "name",
                                        name,
                                        "namespaceUri",
                                        "http://a.org/x",
                                        "aliasUris",
                                        "http://b.org/",
                                        "aliasUris",
                                        "http://b.org"),
                                DC)
                        .statusCode())
                .isEqualTo(400);
        List<String[]> many = new ArrayList<>(fields("name", name, "namespaceUri", "http://a.org/x"));
        for (int i = 0; i < 21; i++) {
            many.add(new String[] {"aliasUris", "http://alias.org/" + i});
        }
        assertThat(register(many, DC).statusCode()).isEqualTo(400);
        assertThat(register(fields("name", name, "namespaceUri", "http://a.org/x", "sourceUri", "rel/path"), DC)
                        .statusCode())
                .isEqualTo(400);
        assertThat(register(fields("name", name, "namespaceUri", "http://a.org/x", "description", "x".repeat(1001)), DC)
                        .statusCode())
                .isEqualTo(400);
        assertThat(register(fields("name", name, "namespaceUri", "http://a.org/x", "other", "1"), DC)
                        .statusCode())
                .isEqualTo(400);
        assertThat(register(fields("namespaceUri", "http://a.org/x"), null).statusCode())
                .isEqualTo(400);
        HttpResponse<String> syntax =
                register(fields("name", name, "namespaceUri", "http://a.org/x"), "garbage .".getBytes());
        assertThat(body(syntax).get("issues").get(0).get("name").asString()).isEqualTo("vocabularyFile");
        HttpResponse<String> empty = register(fields("name", name, "namespaceUri", "http://a.org/x"), new byte[0]);
        assertThat(body(empty).get("issues").get(0).get("detail").asString())
                .isEqualTo("Vocabulary contains no statements");
        assertThat(get("/v1/external-vocabularies/" + name, viewer).statusCode())
                .isEqualTo(404);
        assertThat(get("/v1/external-vocabularies/ab", viewer).statusCode()).isEqualTo(400);
        assertThat(get("/v1/external-vocabularies?sort=name", viewer).statusCode())
                .isEqualTo(400);
    }

    @Test
    void searchValidation() throws Exception {
        for (String b : List.of(
                "{}",
                "[]",
                "{\"namespaceUri\":\"http://a\",\"aliasUri\":\"http://b\"}",
                "{\"namespaceUri\":\"http://a\",\"namespaceUris\":[\"http://b\"]}",
                "{\"namespaceUris\":[]}",
                "{\"aliasUris\":[\"x y\"]}",
                "{\"namespaceUri\":5}",
                "{\"x\":1}",
                "{\"aliasUris\":\"http://a\"}")) {
            assertThat(json("POST", "/v1/external-vocabulary/searches", viewer, b)
                            .statusCode())
                    .as(b)
                    .isEqualTo(400);
        }
        StringBuilder sb = new StringBuilder("{\"namespaceUris\":[");
        for (int i = 0; i < 60; i++) {
            sb.append(i == 0 ? "" : ",").append("\"http://n.org/").append(i).append("\"");
        }
        sb.append("],\"aliasUris\":[");
        for (int i = 0; i < 41; i++) {
            sb.append(i == 0 ? "" : ",").append("\"http://a.org/").append(i).append("\"");
        }
        sb.append("]}");
        HttpResponse<String> r = json("POST", "/v1/external-vocabulary/searches", viewer, sb.toString());
        assertThat(body(r).get("issues").get(0).get("name").asString()).isEqualTo("aliasUris");
        assertThat(json("POST", "/v1/external-vocabularies/content", viewer, "{}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(json("POST", "/v1/external-vocabularies/content", viewer, "{\"namespaceUri\":\"http://a\",\"x\":1}")
                        .statusCode())
                .isEqualTo(400);
        assertThat(json("POST", "/v1/external-vocabularies/content", viewer, "[]")
                        .statusCode())
                .isEqualTo(400);
    }
}
