package eu.simpl.vocabularymanager.it;

import static eu.simpl.vocabularymanager.it.TestEnvironment.admin;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Content validation through the API (SIMPL-18181, 18183, 18184, 18194; D9 to D12, D47, D65, D69, D70). */
class ValidationIT extends IntegrationTest {

    @Test
    void syntaxErrorGivesOneIssueWithLineAndColumnAndStoresNothing() {
        String name = uniqueName("Syn");
        ApiClient.Result result = create(name, "@prefix ex: <http://ex.org/> .\nex:a ex:b .\n");
        assertThat(result.status()).isEqualTo(400);
        JsonNode issues = result.json().get("issues");
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).get("detail").asString()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issues.get(0).get("location").asString()).startsWith("line 2");
        assertThat(issues.get(0).get("name").asString()).isEqualTo("vocabularyFile");
        assertThat(api.get("/v1/vocabularies?name=" + name, TestEnvironment.viewer())
                        .json()
                        .get("total")
                        .asLong())
                .isZero();
    }

    @Test
    void semanticErrorsAreAllReportedInFileOrder() {
        String name = uniqueName("Sem");
        String content = """
                @prefix ex: <https://example.org/sem#> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                ex:op a owl:ObjectProperty .
                ex:dp a owl:DatatypeProperty .
                ex:both a owl:ObjectProperty , owl:DatatypeProperty .
                ex:s ex:op "lit"@en .
                ex:s ex:dp ex:o .
                ex:s skos:prefLabel "a" , "b" .
                ex:A owl:disjointWith ex:B .
                ex:x a ex:A , ex:B .
                """;
        ApiClient.Result result = create(name, content);
        assertThat(result.status()).isEqualTo(400);
        List<String> details = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        for (JsonNode issue : result.json().get("issues")) {
            details.add(issue.get("detail").asString());
            locations.add(issue.get("location").asString());
        }
        assertThat(details)
                .containsExactly(
                        "Property declared as both object and datatype property",
                        "Literal value for object property",
                        "Resource value for datatype property",
                        "Duplicate preferred label for the same language",
                        "Resource is an instance of disjoint classes");
        assertThat(locations.get(1))
                .isEqualTo("statement 'https://example.org/sem#s https://example.org/sem#op \"lit\"@en'");
        assertThat(locations.get(3)).isEqualTo("resource 'https://example.org/sem#s', language 'none'");
    }

    @Test
    void emptyFileIsS6() {
        ApiClient.Result result = create(uniqueName("Empty"), "");
        assertThat(result.status()).isEqualTo(400);
        assertThat(result.json().get("issues").get(0).get("location").asString())
                .isEqualTo("document");
    }

    @Test
    void internalReferencesGiveErrorsAndDeprecationWarnings() {
        String referenced = uniqueName("Ref");
        create(referenced, ttl(referenced));
        newVersion(referenced, "Second", ttl(referenced) + "#2");
        String user = uniqueName("User");
        String content = "@prefix old: <" + CONTENT_BASE + "/content/" + referenced + "/versions/1#> .\n"
                + "@prefix ex: <https://example.org/" + user + "#> .\nex:a ex:b old:c .\n";
        ApiClient.Result created = create(user, content);
        assertThat(created.status()).isEqualTo(201);
        JsonNode warning = created.json().get("validationWarnings").get(0);
        assertThat(warning.get("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(warning.get("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + referenced
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(warning.get("location").asString()).startsWith("prefix 'old' -> '");

        String missing = "@prefix m: <" + CONTENT_BASE + "/content/" + referenced + "/versions/7#> .\n"
                + "@prefix n: <" + CONTENT_BASE + "/content/Nothing" + System.nanoTime() + "/> .\n"
                + "@prefix ex: <https://example.org/x#> .\nex:a ex:b m:c .\n";
        ApiClient.Result failed = create(uniqueName("Miss"), missing);
        assertThat(failed.status()).isEqualTo(400);
        assertThat(failed.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Referenced internal vocabulary '" + referenced + "' version 7 does not exist.");
        assertThat(failed.json().get("issues").get(1).get("detail").asString()).endsWith("does not exist.");

        api.patch("/v1/vocabularies/" + referenced, admin(), "{\"status\":\"DEPRECATED\"}");
        String latest = "@prefix r: <" + CONTENT_BASE + "/content/" + referenced + "#> .\n"
                + "@prefix ex: <https://example.org/y#> .\nex:a ex:b r:c .\n";
        JsonNode warning2 = create(uniqueName("Dep"), latest)
                .json()
                .get("validationWarnings")
                .get(0);
        assertThat(warning2.get("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + referenced
                        + "' version 2 is deprecated. The vocabulary '" + referenced + "' is deprecated.");
    }

    @Test
    void registeredExternalNamespaceClearsTheWarning() {
        String ext = uniqueName("Ext");
        String ns = "https://ns.example.org/" + ext + "/";
        ApiClient.Result registered = api.post(
                "/v1/external-vocabularies",
                admin(),
                new ApiClient.Form()
                        .field("name", ext)
                        .field("namespaceUri", ns)
                        .file("vocabularyFile", "<a> <b> <c> ."));
        assertThat(registered.status()).isEqualTo(201);
        String user = uniqueName("Uses");
        String content = "@prefix e: <" + ns + "> .\n@prefix u: <https://unknown.example/x#> .\n"
                + "@prefix me: <https://example.org/" + user + "#> .\nme:a e:p u:c .\n";
        JsonNode warnings = create(user, content).json().get("validationWarnings");
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).get("code").asString()).isEqualTo("externalReferenceSkipped");
        assertThat(warnings.get(0).get("location").asString()).isEqualTo("prefix 'u' -> 'https://unknown.example/x#'");
    }
}
