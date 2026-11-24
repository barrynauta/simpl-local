package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Content validation through the API (SIMPL-18181, 18183, 18184, 18194, D9 to D12, D47, D65). */
class ValidationApiTest extends ApiTestBase {

    private static final String CONTENT = "https://authority.example/vocabulary-manager/content/";

    @Test
    void syntaxErrorIsOneIssueWithLineAndColumn() {
        Reply reply = createVocabulary(uniqueName("Syn"), "@prefix ex: <https://e.org/> .\nex:a ex:b .\n");
        assertThat(reply.status()).isEqualTo(400);
        JsonNode issue = reply.json().path("issues").get(0);
        assertThat(reply.json().path("issues").size()).isEqualTo(1);
        assertThat(issue.path("detail").asString()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issue.path("location").asString()).isEqualTo("line 2, column 11");
        assertThat(issue.path("type").asString()).isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
        assertThat(createVocabulary(uniqueName("Bin"), "\u0000\u0001binary").status())
                .isEqualTo(400);
    }

    @Test
    void allSemanticErrorsInOneReport() {
        String content = """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix ex: <https://example.org/sem#> .
                ex:op a owl:ObjectProperty . ex:dp a owl:DatatypeProperty .
                ex:both a owl:ObjectProperty , owl:DatatypeProperty .
                ex:C1 owl:disjointWith ex:C2 .
                ex:s ex:op "lit"@en ; ex:dp ex:o ; skos:prefLabel "a"@EN , "b"@en ; a ex:C1 , ex:C2 .
                ex:t skos:prefLabel "x" , "y" ; ex:op "1"^^<http://www.w3.org/2001/XMLSchema#int> .
                _:b1 ex:dp [ ] .
                """;
        Reply reply = createVocabulary(uniqueName("Sem"), content);
        assertThat(reply.status()).isEqualTo(400);
        List<String> found = new ArrayList<>();
        for (JsonNode issue : reply.json().path("issues")) {
            found.add(issue.path("detail").asString() + " | "
                    + issue.path("location").asString());
        }
        assertThat(found)
                .contains(
                        "Literal value for object property | statement '<https://example.org/sem#s> <https://example.org/sem#op> \"lit\"@en'",
                        "Literal value for object property | statement '<https://example.org/sem#t> <https://example.org/sem#op> \"1\"^^<http://www.w3.org/2001/XMLSchema#int>'",
                        "Resource value for datatype property | statement '<https://example.org/sem#s> <https://example.org/sem#dp> <https://example.org/sem#o>'",
                        "Resource value for datatype property | statement '_:b1 <https://example.org/sem#dp> []'",
                        "Property declared as both object and datatype property | resource 'https://example.org/sem#both'",
                        "Duplicate preferred label for the same language | resource 'https://example.org/sem#s', language 'en'",
                        "Duplicate preferred label for the same language | resource 'https://example.org/sem#t', language 'none'",
                        "Resource is an instance of disjoint classes | resource 'https://example.org/sem#s'");
        Reply empty = createVocabulary(uniqueName("Emp"), "");
        assertThat(empty.json().path("issues").get(0).path("location").asString())
                .isEqualTo("document");
    }

    @Test
    void referencesGiveErrorsAndWarnings() {
        String legacy = uniqueName("Leg");
        createVocabulary(legacy, turtle(legacy, "one"));
        createVersion(legacy, "two", turtle(legacy, "two"));
        String user = uniqueName("Use");
        String content = "@prefix old: <" + CONTENT + legacy + "/versions/1#> .\n"
                + "@prefix dc: <http://purl.org/dc/terms/> .\n"
                + "@prefix rdfx: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix own: <https://example.org/" + user + "#> .\n"
                + "own:a dc:title \"x\" ; own:b old:c .\n";
        Reply created = createVocabulary(user, content);
        assertThat(created.status()).isEqualTo(201);
        JsonNode warnings = created.json().path("validationWarnings");
        assertThat(warnings.size()).isEqualTo(2);
        assertThat(warnings.get(0).path("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(warnings.get(0).path("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + legacy
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(warnings.get(0).path("location").asString())
                .isEqualTo("prefix 'old' -> '" + CONTENT + legacy + "/versions/1#'");
        assertThat(warnings.get(1).path("code").asString()).isEqualTo("externalReferenceSkipped");

        json("PATCH", "/v1/vocabularies/" + legacy, admin, "{\"status\":\"DEPRECATED\"}");
        Reply deprecatedVocabulary = createVersion(
                user,
                "with latest",
                "@prefix l: <" + CONTENT + legacy + "> .\n@prefix own: <https://example.org/" + user
                        + "#> .\nown:a own:b l:c .\n");
        assertThat(deprecatedVocabulary
                        .json()
                        .path("validationWarnings")
                        .get(0)
                        .path("description")
                        .asString())
                .isEqualTo("Referenced internal vocabulary '" + legacy + "' version 2 is deprecated. The vocabulary '"
                        + legacy + "' is deprecated.");

        String missing = "@prefix a: <" + CONTENT + "Missing" + user + "/versions/3#> .\n"
                + "@prefix b: <" + CONTENT + "Absent" + user + "/x> .\n"
                + "@prefix c: <" + CONTENT + legacy + "/versions/abc> .\n"
                + "@prefix d: <" + CONTENT + legacy + "/versions/> .\n"
                + "@prefix e: <" + CONTENT + legacy + "/versions/99> .\n"
                + "@prefix f: <" + CONTENT + "lower/x> .\n"
                + "@prefix self: <" + CONTENT + user + "/versions/1#> .\n"
                + "@prefix own: <https://example.org/" + user + "#> .\nown:a own:b own:c .\n";
        Reply errors = createVersion(user, "missing refs", missing);
        assertThat(errors.status()).isEqualTo(400);
        List<String> details = new ArrayList<>();
        for (JsonNode issue : errors.json().path("issues")) {
            details.add(issue.path("detail").asString());
        }
        assertThat(details)
                .containsExactly(
                        "Referenced internal vocabulary 'Missing" + user + "' version 3 does not exist.",
                        "Referenced internal vocabulary 'Absent" + user + "' does not exist.",
                        "Referenced internal vocabulary '" + legacy + "' version abc does not exist.",
                        "Referenced internal vocabulary '" + legacy + "' version  does not exist.",
                        "Referenced internal vocabulary '" + legacy + "' version 99 does not exist.");
    }

    @Test
    void registeredExternalNamespaceGivesNoWarning() {
        String ext = uniqueName("Ext");
        String namespace = "https://ext.example.org/" + ext + "/";
        Reply registered = post(
                "/v1/external-vocabularies",
                admin,
                new Form()
                        .field("name", ext)
                        .field("namespaceUri", namespace)
                        .file("@prefix e: <" + namespace + "> . e:a e:b e:c ."));
        assertThat(registered.status()).isEqualTo(201);
        String name = uniqueName("Ref");
        Reply created = createVocabulary(
                name,
                "@prefix e: <" + namespace + "> .\n@prefix own: <https://example.org/" + name
                        + "#> .\nown:a e:b \"x\" .\n");
        assertThat(created.json().path("validationWarnings").size()).isZero();
    }
}
