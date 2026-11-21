package eu.simpl.vocabularymanager;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.ApiClient.Part;
import eu.simpl.vocabularymanager.support.ApiClient.Response;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Main flow of the decisions end to end against the embedded store (stories SIMPL-18161 to 18196). */
class VocabularyFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void createVersionListDownloadSearchAndStatusChange() {
        String name = uniqueName("Flow");
        String turtle = simpleTurtle(name);
        Response created = api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, turtle, "First."));
        assertThat(created.status()).as(created.text()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/1");
        JsonNode body = created.json();
        assertThat(body.get("version").asInt()).isEqualTo(1);
        assertThat(body.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.get("latestVersionHref").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.get("validationWarnings").isArray()).isTrue();
        assertThat(body.get("contentHref").asString()).isEqualTo(CONTENT_BASE + "/content/" + name + "/versions/1");

        Response vocabulary = api.get("/v1/vocabularies/" + name, viewer);
        assertThat(vocabulary.status()).isEqualTo(200);
        assertThat(vocabulary.json().get("description").asString()).isEqualTo("First.");
        assertThat(vocabulary.header("Content-Type")).startsWith("application/json");

        Response second = api.multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                ApiClient.versionParts(turtle + "# changed\n", "Second.", "Changed."));
        assertThat(second.status()).as(second.text()).isEqualTo(201);
        assertThat(second.json().get("version").asInt()).isEqualTo(2);

        Response versions = api.get("/v1/vocabularies/" + name + "/versions", viewer);
        assertThat(versions.status()).isEqualTo(200);
        JsonNode items = versions.json().get("items");
        assertThat(items.get(0).get("version").asInt()).isEqualTo(2);
        assertThat(items.get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(1).get("latestVersionHref").asString()).endsWith("/versions/2");

        Response list = api.get("/v1/vocabularies?name=" + name.toLowerCase() + "&sort=-name", viewer);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.json().get("total").asLong()).isEqualTo(1);
        assertThat(list.json().get("items").get(0).get("version").asInt()).isEqualTo(2);
        assertThat(list.json().get("self").asString())
                .isEqualTo("/vocabularies?page=1&pageSize=20&name=" + name.toLowerCase() + "&sort=-name");

        Response download = api.get("/v1/content/" + name, viewer);
        assertThat(download.status()).isEqualTo(200);
        assertThat(download.header("Content-Type")).isEqualTo("text/turtle");
        assertThat(download.header("Content-Disposition")).contains(name + ".ttl");
        assertThat(download.text()).isEqualTo(turtle + "# changed\n");
        Response first = api.get("/v1/content/" + name + "/versions/1", viewer);
        assertThat(first.text()).isEqualTo(turtle);
        assertThat(first.header("Content-Disposition")).contains(name + "-v1.ttl");

        Response search = api.json(
                "POST",
                "/v1/vocabularies/searches",
                viewer,
                "{\"items\":[{\"name\":\"" + name + "\"},{\"name\":\"" + name
                        + "\",\"version\":1},{\"name\":\"Missing123\"}]}");
        assertThat(search.status()).as(search.text()).isEqualTo(200);
        JsonNode found = search.json().get("items");
        assertThat(found.size()).isEqualTo(2);
        assertThat(found.get(0).get("latest").asBoolean()).isTrue();
        assertThat(found.get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(search.json().get("totalPages").asInt()).isEqualTo(1);

        Response deprecate = api.json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecate.status()).as(deprecate.text()).isEqualTo(200);
        assertThat(deprecate.json().get("status").asString()).isEqualTo("DEPRECATED");
        Response again = api.json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("detail").asString()).isEqualTo("Vocabulary is already in status DEPRECATED");

        Response reactivate =
                api.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(reactivate.status()).as(reactivate.text()).isEqualTo(200);
        assertThat(reactivate.json().get("status").asString()).isEqualTo("ACTIVE");
    }

    @Test
    void accessRules() {
        String name = uniqueName("Access");
        Response forbidden =
                api.multipart("/v1/vocabularies", viewer, ApiClient.createParts(name, simpleTurtle(name), "Desc."));
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(api.get("/v1/vocabularies", null).status()).isEqualTo(401);
        assertThat(api.get("/v1/vocabularies", norole).status()).isEqualTo(403);
        assertThat(api.get("/v1/vocabularies", "not.a.token").status()).isEqualTo(401);
        assertThat(api.get("/v1/unknown", null).status()).isEqualTo(401);
        assertThat(api.get("/v1/unknown", viewer).status()).isEqualTo(404);
        assertThat(api.get("/actuator/health", viewer).status()).isEqualTo(404);
        Response put = api.send("PUT", "/v1/vocabularies", null, java.util.Map.of(), null);
        assertThat(put.status()).isEqualTo(405);
        assertThat(put.header("Allow")).isEqualTo("GET, POST");
        assertThat(api.send("QUERY", "/v1/vocabularies", null, java.util.Map.of(), null)
                        .status())
                .isEqualTo(405);
        assertThat(api.get("/v1/vocabularies/searches", viewer).status()).isEqualTo(405);
        Response accept = api.send("GET", "/v1/vocabularies", viewer, ApiClient.headers("Accept", "text/html"), null);
        assertThat(accept.status()).isEqualTo(406);
        Response wrongAudience = api.get(
                "/v1/vocabularies",
                KEYS.token(
                        "ga-admin",
                        List.of("GA_VOCABULARY_ADMIN"),
                        "http://localhost:8193/realms/simpl",
                        "other",
                        java.time.Instant.now().plusSeconds(60)));
        assertThat(wrongAudience.status()).isEqualTo(401);
        assertThat(api.get("/v1/health", null).status()).isEqualTo(200);
        assertThat(api.get("/v1/health?x=1", null).status()).isEqualTo(400);
    }

    @Test
    void validationReportAndWarnings() {
        String name = uniqueName("Report");
        String broken = "@prefix ex: <https://example.eu/x#> .\nex:a ex:b \"open .\n";
        Response syntax = api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, broken, "Desc."));
        assertThat(syntax.status()).isEqualTo(400);
        JsonNode issue = syntax.json().get("issues").get(0);
        assertThat(issue.get("detail").asString()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issue.get("location").asString()).startsWith("line ");
        assertThat(issue.get("name").asString()).isEqualTo("vocabularyFile");

        String semantic = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix ex: <https://example.eu/s#> .\n"
                + "ex:p a owl:ObjectProperty .\nex:a ex:p \"literal\" .\n"
                + "ex:q a owl:DatatypeProperty , owl:ObjectProperty .\n";
        Response errors = api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, semantic, "Desc."));
        assertThat(errors.status()).isEqualTo(400);
        assertThat(errors.json().get("issues").size()).isEqualTo(2);
        assertThat(api.get("/v1/vocabularies?name=" + name, admin)
                        .json()
                        .get("total")
                        .asLong())
                .isZero();

        String withReferences = "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                + "@prefix ex: <https://example.eu/" + name + "#> .\n"
                + "ex:a dcterms:title \"A\" .\n";
        Response warned =
                api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, withReferences, "Desc."));
        assertThat(warned.status()).as(warned.text()).isEqualTo(201);
        JsonNode warning = warned.json().get("validationWarnings").get(0);
        assertThat(warning.get("code").asString()).isEqualTo("externalReferenceSkipped");
        assertThat(warning.get("location").asString()).isEqualTo("prefix 'dcterms' -> 'http://purl.org/dc/terms/'");

        Response duplicate = api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, broken, "Desc."));
        assertThat(duplicate.status()).isEqualTo(409);
        Response identical = api.multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                ApiClient.versionParts(withReferences, "Desc.", "Same."));
        assertThat(identical.status()).isEqualTo(409);

        String referencing = "@prefix ref: <" + CONTENT_BASE + "/content/" + name + "/versions/1#> .\n"
                + "@prefix missing: <" + CONTENT_BASE + "/content/Nothing" + name + "#> .\n"
                + "@prefix ex: <https://example.eu/r#> .\nex:a ex:b ref:c .\n";
        String other = uniqueName("Other");
        Response missing = api.multipart("/v1/vocabularies", admin, ApiClient.createParts(other, referencing, "Desc."));
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Referenced internal vocabulary 'Nothing" + name + "' does not exist.");
    }

    @Test
    void externalVocabularies() {
        String name = uniqueName("Ext");
        String namespace = "https://ext.example/" + name + "/";
        List<Part> parts = new ArrayList<>();
        parts.add(Part.file(
                "vocabularyFile",
                ("@prefix t: <" + namespace + "> .\nt:title a <http://www.w3.org/2000/01/rdf-schema#Class> .\n")
                        .getBytes(StandardCharsets.UTF_8)));
        parts.add(Part.field("name", name));
        parts.add(Part.field("namespaceUri", namespace));
        parts.add(Part.field("aliasUris", "https://alias.example/" + name + "#"));
        Response registered = api.multipart("/v1/external-vocabularies", admin, parts);
        assertThat(registered.status()).as(registered.text()).isEqualTo(201);
        assertThat(registered.json().get("aliasUris").size()).isEqualTo(1);
        Response conflict = api.multipart("/v1/external-vocabularies", admin, parts);
        assertThat(conflict.status()).isEqualTo(409);

        Response search = api.json(
                "POST",
                "/v1/external-vocabulary/searches",
                viewer,
                "{\"namespaceUris\":[\"https://ext.example/" + name + "\"]}");
        assertThat(search.status()).as(search.text()).isEqualTo(200);
        assertThat(search.json().get("items").get(0).get("name").asString()).isEqualTo(name);

        Response content = api.json(
                "POST",
                "/v1/external-vocabularies/content",
                viewer,
                "{\"namespaceUri\":\"https://alias.example/" + name + "\"}");
        assertThat(content.status()).as(content.text()).isEqualTo(200);
        assertThat(content.header("Content-Type")).isEqualTo("text/turtle");

        String user = uniqueName("User");
        String referencing = "@prefix t: <" + namespace + "> .\n@prefix ex: <https://example.eu/" + user + "#> .\n"
                + "ex:a t:title \"x\" .\n";
        Response clean = api.multipart("/v1/vocabularies", admin, ApiClient.createParts(user, referencing, "Desc."));
        assertThat(clean.status()).as(clean.text()).isEqualTo(201);
        assertThat(clean.json().get("validationWarnings").size()).isZero();

        assertThat(api.send("DELETE", "/v1/external-vocabularies/" + name, admin, java.util.Map.of(), null)
                        .status())
                .isEqualTo(204);
        assertThat(api.get("/v1/external-vocabularies/" + name, admin).status()).isEqualTo(404);
    }
}
