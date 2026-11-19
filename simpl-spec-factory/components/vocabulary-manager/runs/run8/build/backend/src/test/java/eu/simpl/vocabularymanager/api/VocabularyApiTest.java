package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.Api;
import eu.simpl.vocabularymanager.support.Api.Part;
import eu.simpl.vocabularymanager.support.Api.Result;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Lifecycle, listing, content and validation of internal vocabularies (SIMPL-181xx, D3 to D37, D46 to D70). */
class VocabularyApiTest extends IntegrationTestBase {

    @Test
    void createReturnsVersionOneActiveWithLinksAndCreator() {
        String id = Api.unique();
        String name = "Create" + id;
        Result result = create(name, simpleTurtle(id));
        JsonNode body = result.json();
        assertThat(result.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.path("href").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.path("latestVersionHref").asString()).isEqualTo("/vocabularies/" + name + "/versions/1");
        assertThat(body.path("contentHref").asString())
                .isEqualTo(CONTENT_BASE + "/content/" + name + "/versions/1");
        assertThat(body.path("version").asInt()).isEqualTo(1);
        assertThat(body.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.path("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.path("changelog").asString()).isEqualTo("Initial version.");
        assertThat(body.path("validationWarnings").isArray()).isTrue();
        assertThat(body.path("validationWarnings").size()).isZero();
        assertThat(body.path("creationDate").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

        Result vocabulary = api.get("/v1/vocabularies/" + name, ENV.viewer());
        assertThat(vocabulary.status()).isEqualTo(200);
        assertThat(vocabulary.json().path("href").asString()).isEqualTo("/vocabularies/" + name);
        assertThat(vocabulary.json().path("contentHref").asString()).isEqualTo(CONTENT_BASE + "/content/" + name);
        assertThat(vocabulary.json().path("description").asString()).isEqualTo("Description of " + name);
    }

    @Test
    void newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus() {
        String id = Api.unique();
        String name = "Versions" + id;
        create(name, simpleTurtle(id));
        Result second = newVersion(name, "Second", simpleTurtle(id + "b"));
        assertThat(second.status()).isEqualTo(201);
        assertThat(second.json().path("version").asInt()).isEqualTo(2);

        Result versions = api.get("/v1/vocabularies/" + name + "/versions", ENV.viewer());
        assertThat(versions.status()).isEqualTo(200);
        JsonNode items = versions.json().path("items");
        assertThat(items.get(0).path("version").asInt()).isEqualTo(2);
        assertThat(items.get(0).path("status").asString()).isEqualTo("ACTIVE");
        assertThat(items.get(1).path("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(1).path("latestVersionHref").asString())
                .isEqualTo("/vocabularies/" + name + "/versions/2");

        // D3: deprecating the vocabulary leaves version statuses alone; a new version is still allowed.
        Result deprecated = api.json("PATCH", "/v1/vocabularies/" + name, ENV.admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.status()).isEqualTo(200);
        assertThat(deprecated.json().path("status").asString()).isEqualTo("DEPRECATED");
        Result v2 = api.get("/v1/vocabularies/" + name + "/versions/2", ENV.viewer());
        assertThat(v2.json().path("status").asString()).isEqualTo("ACTIVE");
        Result third = newVersion(name, "Third", simpleTurtle(id + "c"));
        assertThat(third.status()).isEqualTo(201);
        assertThat(api.get("/v1/vocabularies/" + name, ENV.viewer()).json().path("status").asString())
                .isEqualTo("DEPRECATED");

        // D4: same status gives 409 with the fixed text.
        Result again = api.json("PATCH", "/v1/vocabularies/" + name, ENV.admin(), "{\"status\":\"DEPRECATED\"}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().path("detail").asString()).isEqualTo("Vocabulary is already in status DEPRECATED");
        assertThat(again.json().path("type").asString()).isEqualTo("urn:problem-type:simpl:conflict");
        Result activate = api.json("PATCH", "/v1/vocabularies/" + name, ENV.admin(), "{\"status\":\"ACTIVE\"}");
        assertThat(activate.status()).isEqualTo(200);
    }

    @Test
    void versionStatusChangeAndReactivation() {
        String id = Api.unique();
        String name = "VerStatus" + id;
        create(name, simpleTurtle(id));
        newVersion(name, "Second", simpleTurtle(id + "b"));
        Result same = api.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", ENV.admin(),
                "{\"status\":\"DEPRECATED\"}");
        assertThat(same.status()).isEqualTo(409);
        assertThat(same.json().path("detail").asString())
                .isEqualTo("Vocabulary version is already in status DEPRECATED");
        Result reactivated = api.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", ENV.admin(),
                "{\"status\":\"ACTIVE\"}");
        assertThat(reactivated.status()).isEqualTo(200);
        assertThat(reactivated.json().path("status").asString()).isEqualTo("ACTIVE");
        // Two ACTIVE versions now; the next new version deprecates both (D4).
        newVersion(name, "Third", simpleTurtle(id + "c"));
        JsonNode items = api.get("/v1/vocabularies/" + name + "/versions", ENV.viewer()).json().path("items");
        assertThat(items.get(0).path("status").asString()).isEqualTo("ACTIVE");
        assertThat(items.get(1).path("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(2).path("status").asString()).isEqualTo("DEPRECATED");

        Result bad = api.json("PATCH", "/v1/vocabularies/" + name + "/versions/1", ENV.admin(),
                "{\"status\":\"RETIRED\"}");
        assertThat(bad.status()).isEqualTo(400);
        Result missing = api.json("PATCH", "/v1/vocabularies/" + name + "/versions/99", ENV.admin(),
                "{\"status\":\"ACTIVE\"}");
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.json().path("detail").asString())
                .isEqualTo("Version 99 of vocabulary '" + name + "' was not found.");
        Result unknownVocabulary = api.json("PATCH", "/v1/vocabularies/Unknown" + id, ENV.admin(),
                "{\"status\":\"ACTIVE\"}");
        assertThat(unknownVocabulary.status()).isEqualTo(404);
        assertThat(unknownVocabulary.json().path("detail").asString())
                .isEqualTo("Vocabulary with name 'Unknown" + id + "' was not found.");
        assertThat(unknownVocabulary.issueNames()).containsExactly("vocabularyName");
    }

    @Test
    void identicalVersionIsRefusedAndDescriptionOnlyChangeIsAccepted() {
        String id = Api.unique();
        String name = "Same" + id;
        create(name, simpleTurtle(id));
        Result identical = newVersion(name, "Description of " + name + "  ", simpleTurtle(id));
        assertThat(identical.status()).isEqualTo(409);
        assertThat(identical.json().path("detail").asString())
                .isEqualTo("Version 1 of vocabulary '" + name + "' already has this content and description.");
        Result descriptionOnly = newVersion(name, "Other description", simpleTurtle(id));
        assertThat(descriptionOnly.status()).isEqualTo(201);
        assertThat(descriptionOnly.json().path("version").asInt()).isEqualTo(2);
    }

    @Test
    void duplicateNameIsCaseInsensitiveAndWinsOverInvalidContent() {
        String id = Api.unique();
        String name = "Dup" + id;
        create(name, simpleTurtle(id));
        Result duplicate = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "DUP" + id.toUpperCase()),
                Part.field("description", "d"),
                Part.field("changelog", "c"),
                Part.file("this is not turtle"));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.json().path("detail").asString())
                .isEqualTo("Vocabulary with name 'DUP" + id.toUpperCase() + "' already exists.");
        // D54: paths match exactly.
        assertThat(api.get("/v1/vocabularies/DUP" + id.toUpperCase(), ENV.viewer()).status()).isEqualTo(404);
    }

    @Test
    void fieldRulesOnCreateAndNewVersion() {
        String id = Api.unique();
        Result shortName = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "Ab"), Part.field("description", "d"), Part.field("changelog", "c"),
                Part.file(simpleTurtle(id)));
        assertThat(shortName.status()).isEqualTo(400);
        assertThat(shortName.issueNames()).containsExactly("name");
        Result lowerCase = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "abc" + id), Part.field("description", " "), Part.field("changelog", "c"),
                Part.file(simpleTurtle(id)));
        assertThat(lowerCase.status()).isEqualTo(400);
        assertThat(lowerCase.issueNames()).contains("name", "description");
        Result longDescription = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "Long" + id), Part.field("description", "x".repeat(1001)),
                Part.field("changelog", "c"), Part.file(simpleTurtle(id)));
        assertThat(longDescription.status()).isEqualTo(400);
        assertThat(longDescription.issueNames()).containsExactly("description");
        Result atLimit = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "L" + "x".repeat(63)), Part.field("description", "x".repeat(1000)),
                Part.field("changelog", "c"), Part.file(simpleTurtle(id)));
        assertThat(atLimit.status()).isIn(201, 409);
        Result missingFile = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "NoFile" + id), Part.field("description", "d"), Part.field("changelog", "c"));
        assertThat(missingFile.status()).isEqualTo(400);
        assertThat(missingFile.issueNames()).containsExactly("vocabularyFile");
        Result unknownField = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "Unknown" + id), Part.field("description", "d"), Part.field("changelog", "c"),
                Part.field("colour", "red"), Part.field("", "empty"), Part.file(simpleTurtle(id)));
        assertThat(unknownField.status()).isEqualTo(400);
        assertThat(unknownField.issueNames()).contains("colour", "");
        Result repeated = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "Rep" + id), Part.field("name", "Rep2" + id), Part.field("description", "d"),
                Part.field("changelog", "c"), Part.file(simpleTurtle(id)));
        assertThat(repeated.status()).isEqualTo(400);

        String name = "Fields" + id;
        create(name, simpleTurtle(id));
        Result versionField = api.multipart("/v1/vocabularies/" + name + "/versions", ENV.admin(),
                Part.field("version", "5"), Part.field("description", "d"), Part.field("changelog", "c"),
                Part.file(simpleTurtle(id + "x")));
        assertThat(versionField.status()).isEqualTo(400);
        assertThat(versionField.issueNames()).containsExactly("version");
        Result noChangelog = api.multipart("/v1/vocabularies/" + name + "/versions", ENV.admin(),
                Part.field("description", "d"), Part.file(simpleTurtle(id + "x")));
        assertThat(noChangelog.status()).isEqualTo(400);
        assertThat(noChangelog.issueNames()).containsExactly("changelog");
        Result unknownVocabulary = newVersion("Nope" + id, "d", simpleTurtle(id));
        assertThat(unknownVocabulary.status()).isEqualTo(404);
        Result badPath = newVersion("nope" + id, "d", simpleTurtle(id));
        assertThat(badPath.status()).isEqualTo(400);
        assertThat(badPath.issueNames()).containsExactly("vocabularyName");
        // D8: nothing stored after a failure.
        assertThat(api.get("/v1/vocabularies/" + name + "/versions", ENV.viewer()).json().path("total").asInt())
                .isEqualTo(1);
    }

    @Test
    void syntaxErrorStopsBeforeSemanticValidation() {
        String id = Api.unique();
        Result result = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "Syntax" + id), Part.field("description", "d"), Part.field("changelog", "c"),
                Part.file("@prefix ex: <https://example.org/x#> .\nex:a ex:b"));
        assertThat(result.status()).isEqualTo(400);
        JsonNode issues = result.json().path("issues");
        assertThat(issues.size()).isEqualTo(1);
        assertThat(issues.get(0).path("detail").asString()).startsWith("Encountered invalid Turtle syntax");
        assertThat(issues.get(0).path("location").asString()).startsWith("line 2");
        assertThat(issues.get(0).path("type").asString())
                .isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
        assertThat(issues.get(0).path("name").asString()).isEqualTo("vocabularyFile");
        assertThat(api.get("/v1/vocabularies?name=Syntax" + id, ENV.viewer()).json().path("total").asInt())
                .isZero();
    }

    @Test
    void semanticRulesReportEveryError() {
        String id = Api.unique();
        String turtle = """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix ex: <https://example.org/%s#> .
                @prefix gone: <%s/content/Missing%s/versions/2#> .
                @prefix gonelatest: <%s/content/Missing%s> .
                @prefix odd: <%s/content/Missing%s/versions/abc> .
                ex:p a owl:ObjectProperty .
                ex:q a owl:DatatypeProperty .
                ex:r a owl:ObjectProperty , owl:DatatypeProperty .
                ex:s ex:p "literal" ; ex:q ex:o ; skos:prefLabel "a" , "b" ; skos:prefLabel "x"@en , "y"@en .
                ex:A owl:disjointWith ex:B .
                ex:i a ex:A , ex:B .
                """.formatted(id, CONTENT_BASE, id, CONTENT_BASE, id, CONTENT_BASE, id);
        Result result = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "Semantic" + id), Part.field("description", "d"), Part.field("changelog", "c"),
                Part.file(turtle));
        assertThat(result.status()).isEqualTo(400);
        List<String> details = result.issueDetails();
        assertThat(details)
                .contains(
                        "Literal value for object property",
                        "Resource value for datatype property",
                        "Property declared as both object and datatype property",
                        "Duplicate preferred label for the same language",
                        "Resource is an instance of disjoint classes",
                        "Referenced internal vocabulary 'Missing" + id + "' version 2 does not exist.",
                        "Referenced internal vocabulary 'Missing" + id + "' does not exist.",
                        "Referenced internal vocabulary 'Missing" + id + "' version abc does not exist.");
        List<String> locations = new ArrayList<>();
        result.json().path("issues").forEach(issue -> locations.add(issue.path("location").asString()));
        assertThat(locations).contains(
                "statement 'https://example.org/" + id + "#s https://example.org/" + id + "#p literal'",
                "resource 'https://example.org/" + id + "#r'",
                "resource 'https://example.org/" + id + "#s', language 'none'",
                "resource 'https://example.org/" + id + "#s', language 'en'",
                "resource 'https://example.org/" + id + "#i'");
    }

    @Test
    void emptyFileFailsWithNoStatements() {
        String id = Api.unique();
        Result result = api.multipart("/v1/vocabularies", ENV.admin(),
                Part.field("name", "Empty" + id), Part.field("description", "d"), Part.field("changelog", "c"),
                Part.file(new byte[0]));
        assertThat(result.status()).isEqualTo(400);
        assertThat(result.issueDetails()).containsExactly("Vocabulary contains no statements");
        assertThat(result.json().path("issues").get(0).path("location").asString()).isEqualTo("document");
    }

    @Test
    void referenceWarningsForDeprecatedAndExternalNamespaces() {
        String id = Api.unique();
        String target = "Target" + id;
        create(target, simpleTurtle(id));
        newVersion(target, "Second", simpleTurtle(id + "b"));
        String turtle = """
                @prefix old: <%s/content/%s/versions/1#> .
                @prefix latest: <%s/content/%s/> .
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix ext: <https://unregistered.example/%s#> .
                @prefix own: <https://own.example/%s/> .
                own:x old:p ext:y ; skos:prefLabel "x" ; latest:q own:z .
                """.formatted(CONTENT_BASE, target, CONTENT_BASE, target, id, id);
        Result result = create("Referrer" + id, turtle);
        JsonNode warnings = result.json().path("validationWarnings");
        assertThat(warnings.size()).isEqualTo(2);
        assertThat(warnings.get(0).path("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(warnings.get(0).path("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + target
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
        assertThat(warnings.get(0).path("location").asString())
                .isEqualTo("prefix 'old' -> '" + CONTENT_BASE + "/content/" + target + "/versions/1#'");
        assertThat(warnings.get(1).path("code").asString()).isEqualTo("externalReferenceSkipped");

        // D47: deprecated vocabulary, no recommendation.
        api.json("PATCH", "/v1/vocabularies/" + target, ENV.admin(), "{\"status\":\"DEPRECATED\"}");
        Result second = create("Referrer2" + id, turtle);
        assertThat(second.json().path("validationWarnings").get(0).path("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + target + "' version 1 is deprecated. The vocabulary '"
                        + target + "' is deprecated.");
        // latest reference to a deprecated vocabulary is a warning too (effectively deprecated, D3)
        assertThat(second.json().path("validationWarnings").size()).isEqualTo(3);
    }

    @Test
    void contentDownloadsReturnTheStoredBytes() {
        String id = Api.unique();
        String name = "Content" + id;
        String first = simpleTurtle(id) + "# comment kept\n";
        create(name, first);
        String second = simpleTurtle(id + "b");
        newVersion(name, "Second", second);
        Result latest = api.get("/v1/content/" + name, ENV.viewer());
        assertThat(latest.status()).isEqualTo(200);
        assertThat(latest.contentType()).isEqualTo("text/turtle");
        assertThat(latest.text()).isEqualTo(second);
        assertThat(latest.header("Content-Disposition")).contains("attachment").contains(name + ".ttl");
        Result v1 = api.get("/v1/content/" + name + "/versions/1", ENV.viewer(), "Accept", "text/turtle");
        assertThat(v1.text()).isEqualTo(first);
        assertThat(v1.header("Content-Disposition")).contains(name + "-v1.ttl");
        assertThat(api.get("/v1/content/" + name + "/versions/7", ENV.viewer()).status()).isEqualTo(404);
        assertThat(api.get("/v1/content/Missing" + id, ENV.viewer()).status()).isEqualTo(404);
        assertThat(api.get("/v1/content/" + name, ENV.viewer(), "Accept", "application/json").status())
                .isEqualTo(406);
        assertThat(api.get("/v1/content/" + name + "/versions/0", ENV.viewer()).status()).isEqualTo(400);
    }

    @Test
    void listFiltersSortsAndPages() {
        String id = Api.unique();
        String prefix = "List" + id;
        create(prefix + "B", simpleTurtle(id + "b"));
        create(prefix + "A", simpleTurtle(id + "a"));
        create(prefix + "C", simpleTurtle(id + "c"));
        sleepPastSecond();
        api.json("PATCH", "/v1/vocabularies/" + prefix + "C", ENV.admin(), "{\"status\":\"DEPRECATED\"}");

        Result all = api.get("/v1/vocabularies?name=" + prefix.toLowerCase(), ENV.viewer());
        assertThat(all.status()).isEqualTo(200);
        assertThat(names(all)).containsExactly(prefix + "A", prefix + "B", prefix + "C");
        assertThat(all.json().path("total").asInt()).isEqualTo(3);
        assertThat(names(api.get("/v1/vocabularies?name=" + prefix + "&sort=-name", ENV.viewer())))
                .containsExactly(prefix + "C", prefix + "B", prefix + "A");
        assertThat(names(api.get("/v1/vocabularies?name=" + prefix + "&status=DEPRECATED", ENV.viewer())))
                .containsExactly(prefix + "C");
        assertThat(names(api.get("/v1/vocabularies?name=" + prefix + "&sort=-lastUpdated", ENV.viewer())).get(0))
                .isEqualTo(prefix + "C");
        for (String sort : List.of("status", "-status", "creationDate", "-creationDate", "lastUpdated")) {
            assertThat(api.get("/v1/vocabularies?name=" + prefix + "&sort=" + sort, ENV.viewer()).status())
                    .isEqualTo(200);
        }
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        assertThat(api.get("/v1/vocabularies?name=" + prefix + "&creationDate=" + today, ENV.viewer())
                        .json().path("total").asInt())
                .isEqualTo(3);
        assertThat(api.get("/v1/vocabularies?name=" + prefix + "&lastUpdated=2001-01-01", ENV.viewer())
                        .json().path("total").asInt())
                .isZero();
        assertThat(api.get("/v1/vocabularies?name=" + prefix + "&creationDate=2001-01-01T10:00:00Z", ENV.viewer())
                        .json().path("total").asInt())
                .isZero();

        Result page2 = api.get("/v1/vocabularies?name=" + prefix + "&pageSize=1&page=2&sort=name", ENV.viewer());
        JsonNode body = page2.json();
        assertThat(names(page2)).containsExactly(prefix + "B");
        assertThat(body.path("totalPages").asInt()).isEqualTo(3);
        assertThat(body.path("self").asString()).contains("name=" + prefix).contains("sort=name").contains("page=2");
        assertThat(body.path("prev").asString()).contains("page=1");
        assertThat(body.path("next").asString()).contains("page=3");
        assertThat(body.path("last").asString()).contains("page=3");
        Result beyond = api.get("/v1/vocabularies?name=" + prefix + "&pageSize=1&page=9", ENV.viewer());
        assertThat(beyond.json().path("items").size()).isZero();
        assertThat(beyond.json().path("prev").asString()).contains("page=3");
        assertThat(beyond.json().has("next")).isFalse();
        Result none = api.get("/v1/vocabularies?name=Zz" + id, ENV.viewer());
        assertThat(none.json().path("total").asInt()).isZero();
        assertThat(none.json().has("last")).isFalse();
        assertThat(none.json().has("prev")).isFalse();

        for (String query : List.of("page=0", "pageSize=101", "pageSize=0", "page=x", "sort=colour", "name=a-b",
                "status=GONE", "creationDate=2026-02-30", "lastUpdated=yesterday", "unknown=1", "page=1&page=2",
                "name=" + "a".repeat(65), "creationDate=2026-01-01T25:00:00Z")) {
            Result bad = api.get("/v1/vocabularies?" + query, ENV.viewer());
            assertThat(bad.status()).as(query).isEqualTo(400);
            assertThat(bad.json().path("issues").get(0).path("in").asString()).as(query).isEqualTo("query");
        }
    }

    @Test
    void versionListSortsAndValidates() {
        String id = Api.unique();
        String name = "VerList" + id;
        create(name, simpleTurtle(id));
        newVersion(name, "Second", simpleTurtle(id + "b"));
        assertThat(versionNumbers(api.get("/v1/vocabularies/" + name + "/versions", ENV.viewer())))
                .containsExactly(2, 1);
        assertThat(versionNumbers(api.get("/v1/vocabularies/" + name + "/versions?sort=creationDate", ENV.viewer())))
                .hasSize(2);
        assertThat(versionNumbers(api.get("/v1/vocabularies/" + name + "/versions?sort=status", ENV.viewer())))
                .containsExactly(2, 1);
        assertThat(versionNumbers(api.get("/v1/vocabularies/" + name + "/versions?sort=-status", ENV.viewer())))
                .containsExactly(1, 2);
        assertThat(api.get("/v1/vocabularies/" + name + "/versions?sort=lastUpdated", ENV.viewer()).status())
                .isEqualTo(400);
        assertThat(api.get("/v1/vocabularies/" + name + "/versions?status=ACTIVE", ENV.viewer()).status())
                .isEqualTo(400);
        assertThat(api.get("/v1/vocabularies/Missing" + id + "/versions", ENV.viewer()).status()).isEqualTo(404);
        Result version = api.get("/v1/vocabularies/" + name + "/versions/1", ENV.viewer());
        assertThat(version.json().path("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(version.json().has("validationWarnings")).isFalse();
        assertThat(api.get("/v1/vocabularies/" + name + "/versions/abc", ENV.viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/vocabularies/" + name + "?x=1", ENV.viewer()).status()).isEqualTo(400);
    }

    @Test
    void searchReturnsResultsInRequestOrder() {
        String id = Api.unique();
        String name = "Search" + id;
        create(name, simpleTurtle(id));
        newVersion(name, "Second", simpleTurtle(id + "b"));
        String body = "{\"items\":[{\"name\":\"" + name + "\",\"version\":1},{\"name\":\"" + name + "\"},"
                + "{\"name\":\"Missing" + id + "\"},{\"name\":\"" + name + "\",\"version\":9},{\"name\":\"" + name
                + "\",\"version\":1},{\"name\":\"" + name + "\",\"version\":2}]}";
        Result result = api.json("POST", "/v1/vocabularies/searches", ENV.viewer(), body);
        assertThat(result.status()).isEqualTo(200);
        JsonNode items = result.json().path("items");
        assertThat(items.size()).isEqualTo(3);
        assertThat(items.get(0).path("version").asInt()).isEqualTo(1);
        assertThat(items.get(0).path("latest").asBoolean()).isFalse();
        assertThat(items.get(0).path("status").asString()).isEqualTo("DEPRECATED");
        assertThat(items.get(1).path("version").asInt()).isEqualTo(2);
        assertThat(items.get(1).path("latest").asBoolean()).isTrue();
        assertThat(items.get(2).path("version").asInt()).isEqualTo(2);
        assertThat(result.json().path("total").asInt()).isEqualTo(3);
        assertThat(result.json().path("totalPages").asInt()).isEqualTo(1);
        assertThat(result.json().has("self")).isFalse();

        Result empty = api.json("POST", "/v1/vocabularies/searches", ENV.viewer(),
                "{\"items\":[{\"name\":\"Missing" + id + "\"}]}");
        assertThat(empty.json().path("totalPages").asInt()).isZero();

        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Abc").append(i).append("\"}");
        }
        many.append("]}");
        Result tooMany = api.json("POST", "/v1/vocabularies/searches", ENV.viewer(), many.toString());
        assertThat(tooMany.status()).isEqualTo(400);
        assertThat(tooMany.issueNames()).containsExactly("items");
        for (String bad : List.of("{\"items\":[]}", "{}", "{\"items\":[{\"name\":\"ab\"}]}",
                "{\"items\":[{\"name\":\"Abc\",\"version\":0}]}", "{\"items\":[{\"name\":\"Abc\",\"x\":1}]}",
                "{\"items\":[{\"name\":\"Abc\"}],\"extra\":true}", "[1]", "{nope", "{\"items\":\"x\"}",
                "{\"items\":[{\"name\":\"Abc\",\"version\":\"1\"}]}", "{\"items\":[null]}")) {
            Result rejected = api.json("POST", "/v1/vocabularies/searches", ENV.viewer(), bad);
            assertThat(rejected.status()).as(bad).isEqualTo(400);
        }
        assertThat(api.json("POST", "/v1/vocabularies/searches", ENV.viewer(),
                        "{\"items\":[{\"name\":\"Abc\",\"version\":0}]}").issueNames())
                .containsExactly("items[0].version");
        assertThat(api.send("POST", "/v1/vocabularies/searches?page=1", ENV.viewer(), "application/json",
                        "{\"items\":[{\"name\":\"Abc\"}]}".getBytes(StandardCharsets.UTF_8)).status())
                .isEqualTo(400);
    }

    private static void sleepPastSecond() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> names(Result result) {
        List<String> names = new ArrayList<>();
        result.json().path("items").forEach(item -> names.add(item.path("name").asString()));
        return names;
    }

    private static List<Integer> versionNumbers(Result result) {
        List<Integer> numbers = new ArrayList<>();
        result.json().path("items").forEach(item -> numbers.add(item.path("version").asInt()));
        return numbers;
    }
}
