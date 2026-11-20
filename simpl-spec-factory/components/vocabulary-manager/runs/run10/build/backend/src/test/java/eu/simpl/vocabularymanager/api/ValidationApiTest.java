package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Validation report and warnings through the API (D9 to D12, D47, D65, D69). */
class ValidationApiTest extends IntegrationTestBase {

    @Test
    void syntaxErrorGivesOneIssueWithLineAndNothingIsStored() {
        String name = uniqueName("Syn");
        ApiClient.Response response = api().multipart(
                        "/v1/vocabularies",
                        IDP.admin(),
                        ApiClient.createParts(
                                name, "D", "@prefix ex: <https://e.org/> .\nex:a ex:b ex:c .\nex:d ex:e .\n"));
        assertThat(response.status()).isEqualTo(400);
        JsonNode issues = response.json().get("issues");
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).get("detail").asString()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issues.get(0).get("location").asString()).startsWith("line 3");
        assertThat(issues.get(0).get("name").asString()).isEqualTo("vocabularyFile");
        assertThat(issues.get(0).get("type").asString())
                .isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
        assertThat(api().get("/v1/vocabularies?name=" + name, IDP.viewer())
                        .json()
                        .get("total")
                        .asInt())
                .isZero();
    }

    @Test
    void semanticErrorsAreAllReported() {
        String name = uniqueName("Sem");
        String turtle = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix ex: <http://example.org/> .\n"
                + "ex:op a owl:ObjectProperty . ex:dp a owl:DatatypeProperty .\n"
                + "ex:both a owl:ObjectProperty , owl:DatatypeProperty .\n"
                + "ex:s ex:op \"lit\" . ex:s ex:dp ex:o .\n"
                + "ex:s skos:prefLabel \"a\"@en , \"b\"@en .\n"
                + "ex:A owl:disjointWith ex:B . ex:i a ex:A , ex:B .\n";
        ApiClient.Response response =
                api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", turtle));
        assertThat(response.status()).isEqualTo(400);
        List<String> details = response.json()
                .get("issues")
                .valueStream()
                .map(issue -> issue.get("detail").asString())
                .toList();
        assertThat(details)
                .containsExactly(
                        "Literal value for object property",
                        "Resource value for datatype property",
                        "Property declared as both object and datatype property",
                        "Duplicate preferred label for the same language",
                        "Resource is an instance of disjoint classes");
        assertThat(response.json().get("issues").get(0).get("location").asString())
                .isEqualTo("statement 'http://example.org/s http://example.org/op lit'");
        assertThat(response.json().get("issues").get(3).get("location").asString())
                .isEqualTo("resource 'http://example.org/s', language 'en'");
    }

    @Test
    void emptyFileFailsWithS6() {
        ApiClient.Response response =
                api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(uniqueName("Emp"), "D", ""));
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.json().get("issues").get(0).get("location").asString())
                .isEqualTo("document");
    }

    @Test
    void internalReferencesGiveErrorsAndWarnings() {
        String target = uniqueName("Ref");
        api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(target, "D", simpleTurtle(target)));
        api().multipart(
                        "/v1/vocabularies/" + target + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("D2", "c", simpleTurtle(target) + "# v2\n"));
        String base = CONTENT_BASE + "/content/" + target;

        String missing = "@prefix m: <" + base + "/versions/9#> .\n@prefix u: <" + CONTENT_BASE
                + "/content/Unknown" + target + "#> .\n@prefix n: <" + base + "/versions/x#> .\n"
                + "@prefix ex: <https://example.org/x#> .\nex:a ex:b ex:c .\n";
        ApiClient.Response failed = api().multipart(
                        "/v1/vocabularies", IDP.admin(), ApiClient.createParts(uniqueName("Miss"), "D", missing));
        assertThat(failed.status()).isEqualTo(400);
        List<String> details = failed.json()
                .get("issues")
                .valueStream()
                .map(issue -> issue.get("detail").asString())
                .toList();
        assertThat(details)
                .containsExactly(
                        "Referenced internal vocabulary '" + target + "' version 9 does not exist.",
                        "Referenced internal vocabulary 'Unknown" + target + "' does not exist.",
                        "Referenced internal vocabulary '" + target + "' version x does not exist.");
        assertThat(failed.json().get("issues").get(0).get("location").asString())
                .isEqualTo("prefix 'm' -> '" + base + "/versions/9#'");

        String deprecated = "@prefix old: <" + base + "/versions/1#> .\n@prefix dc: <http://purl.org/dc/terms/> .\n"
                + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                + "@prefix own: <https://example.org/own#> .\nown:a dc:title \"t\" .\n";
        ApiClient.Response created = api().multipart(
                        "/v1/vocabularies", IDP.admin(), ApiClient.createParts(uniqueName("Warn"), "D", deprecated));
        assertThat(created.status()).isEqualTo(201);
        JsonNode warnings = created.json().get("validationWarnings");
        assertThat(warnings).hasSize(2);
        assertThat(warnings.get(0).get("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(warnings.get(0).get("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + target
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(warnings.get(1).get("code").asString()).isEqualTo("externalReferenceSkipped");
        assertThat(warnings.get(1).get("location").asString()).isEqualTo("prefix 'dc' -> 'http://purl.org/dc/terms/'");

        api().json("PATCH", "/v1/vocabularies/" + target, IDP.admin(), "{\"status\":\"DEPRECATED\"}");
        String latest =
                "@prefix t: <" + base + "#> .\n@prefix own: <https://example.org/own2#> .\nown:a own:b own:c .\n";
        ApiClient.Response withDeprecatedVocabulary =
                api().multipart("/v1/vocabularies", IDP.admin(), ApiClient.createParts(uniqueName("Dv"), "D", latest));
        assertThat(withDeprecatedVocabulary
                        .json()
                        .get("validationWarnings")
                        .get(0)
                        .get("description")
                        .asString())
                .isEqualTo("Referenced internal vocabulary '" + target + "' version 2 is deprecated. The vocabulary '"
                        + target + "' is deprecated.");
    }

    @Test
    void registeredExternalNamespaceGivesNoWarning() {
        String external = uniqueName("Ext");
        String namespace = "https://ns.example.org/" + external + "/";
        ApiClient.Response registered = api().multipart(
                        "/v1/external-vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", external),
                                ApiClient.Part.text("namespaceUri", namespace),
                                ApiClient.Part.file(
                                        "vocabularyFile", simpleTurtle(external).getBytes())));
        assertThat(registered.status()).isEqualTo(201);
        String turtle = "@prefix x: <" + namespace.substring(0, namespace.length() - 1) + "#> .\n"
                + "@prefix own: <https://example.org/o#> .\nown:a x:b own:c .\n";
        ApiClient.Response created = api().multipart(
                        "/v1/vocabularies", IDP.admin(), ApiClient.createParts(uniqueName("Clean"), "D", turtle));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.json().get("validationWarnings")).isEmpty();
    }
}
