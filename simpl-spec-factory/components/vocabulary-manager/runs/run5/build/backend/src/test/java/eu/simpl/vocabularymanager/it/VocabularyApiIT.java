package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.HttpApi;
import eu.simpl.vocabularymanager.support.HttpApi.Form;
import eu.simpl.vocabularymanager.support.HttpApi.Result;
import eu.simpl.vocabularymanager.support.TestServers;
import eu.simpl.vocabularymanager.support.TestTokens;
import eu.simpl.vocabularymanager.support.Turtle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * End-to-end tests of the API on a real Tomcat with an embedded Fuseki store and a mocked identity provider (JWKS
 * endpoint with a test key).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VocabularyApiIT {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final String CB = "https://authority.example/vocabulary-manager/content/";
    private static final TestTokens TOKENS = TestServers.TOKENS;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vocabulary.store.url", TestServers::storeUrl);
        registry.add("vocabulary.identity.jwks-url", TestServers::jwksUrl);
        registry.add("vocabulary.identity.issuer", () -> TestTokens.ISSUER);
        registry.add("logging.structured.format.console", () -> "");
    }

    @LocalServerPort
    private int port;

    private HttpApi api;
    private String admin;
    private String viewer;

    @BeforeEach
    void setUp() {
        api = new HttpApi("http://127.0.0.1:" + port + "/v1");
        admin = TOKENS.admin();
        viewer = TOKENS.viewer();
    }

    private static String unique(String prefix) {
        return prefix + System.nanoTime() % 1_000_000_000L + COUNTER.incrementAndGet();
    }

    private Result create(String name, byte[] content) {
        return api.post(
                "/vocabularies",
                admin,
                new Form()
                        .field("name", name)
                        .field("description", "Description of " + name)
                        .field("changelog", "Initial version.")
                        .file("vocabularyFile", name + ".ttl", content));
    }

    private Result newVersion(String name, String description, byte[] content) {
        return api.post(
                "/vocabularies/" + name + "/versions",
                admin,
                new Form()
                        .field("description", description)
                        .field("changelog", "Changed.")
                        .file("vocabularyFile", name + ".ttl", content));
    }

    @Test
    void healthNeedsNoToken() {
        Result r = api.get("/health", null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.json().get("status").asString()).isEqualTo("UP");
    }

    @Test
    void mainFlowCreateVersionListDownloadSearch() {
        String name = unique("Flow");
        Result created = create(name, Turtle.valid(name));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.header("Location")).isEqualTo("/vocabularies/" + name + "/versions/1");
        JsonNode body = created.json();
        assertThat(body.get("version").asInt()).isEqualTo(1);
        assertThat(body.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.get("createdBy").asString()).isEqualTo("ga-admin@example.com");
        assertThat(body.get("contentHref").asString()).isEqualTo(CB + name + "/versions/1");
        assertThat(body.get("validationWarnings").isArray()).isTrue();
        assertThat(body.get("creationDate").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

        byte[] second =
                Turtle.bytes(new String(Turtle.valid(name)) + "<https://example.eu/x> a <https://example.eu/Y> .\n");
        Result version = newVersion(name, "Second description", second);
        assertThat(version.status()).isEqualTo(201);
        assertThat(version.json().get("version").asInt()).isEqualTo(2);

        Result versions = api.get("/vocabularies/" + name + "/versions", viewer);
        assertThat(versions.status()).isEqualTo(200);
        assertThat(versions.json().get("items").get(0).get("version").asInt()).isEqualTo(2);
        assertThat(versions.json().get("items").get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(versions.json().get("items").get(1).get("latestVersionHref").asString())
                .isEqualTo("/vocabularies/" + name + "/versions/2");

        Result vocabulary = api.get("/vocabularies/" + name, viewer);
        assertThat(vocabulary.json().get("version").asInt()).isEqualTo(2);
        assertThat(vocabulary.json().get("description").asString()).isEqualTo("Second description");
        assertThat(vocabulary.json().get("href").asString()).isEqualTo("/vocabularies/" + name);
        assertThat(api.get("/vocabularies/" + name.toUpperCase(), viewer).status())
                .isEqualTo(404);

        Result list = api.get("/vocabularies?name=" + name.toLowerCase(), viewer);
        assertThat(list.json().get("total").asInt()).isEqualTo(1);
        assertThat(list.json().get("self").asString()).startsWith("/vocabularies?page=1&pageSize=20&name=");

        Result latest = api.get("/content/" + name, viewer);
        assertThat(latest.status()).isEqualTo(200);
        assertThat(latest.header("Content-Type")).startsWith("text/turtle");
        assertThat(latest.header("Content-Disposition")).contains("filename=\"" + name + ".ttl\"");
        assertThat(latest.body()).isEqualTo(second);
        Result first = api.get("/content/" + name + "/versions/1", viewer);
        assertThat(first.body()).isEqualTo(Turtle.valid(name));
        assertThat(first.header("Content-Disposition")).contains(name + "-v1.ttl");

        Result search = api.postJson(
                "/vocabularies/searches",
                viewer,
                "{\"items\":[{\"name\":\"" + name + "\"},{\"name\":\"" + name
                        + "\",\"version\":1},{\"name\":\"Zzzmissing\"}]}");
        assertThat(search.status()).isEqualTo(200);
        assertThat(search.json().get("total").asInt()).isEqualTo(2);
        assertThat(search.json().get("items").get(0).get("latest").asBoolean()).isTrue();
        assertThat(search.json().get("items").get(1).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(search.json().has("self")).isFalse();
    }

    @Test
    void accessRules() {
        assertThat(api.get("/vocabularies", null).status()).isEqualTo(401);
        Result unauthorized = api.get("/vocabularies", "garbage");
        assertThat(unauthorized.status()).isEqualTo(401);
        assertThat(unauthorized.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:unauthorized");
        Result forbidden = api.get("/vocabularies", TOKENS.noRole());
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(api.post("/vocabularies", viewer, new Form()).status()).isEqualTo(403);
        assertThat(api.patchJson("/vocabularies/Abc", viewer, "{}").status()).isEqualTo(403);
        assertThat(api.postJson("/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Abc\"}]}")
                        .status())
                .isEqualTo(200);
        assertThat(api.delete("/external-vocabularies/Abc", viewer).status()).isEqualTo(403);
    }

    @Test
    void orderOfChecksOnWrites() {
        String name = unique("Order");
        assertThat(create(name, Turtle.valid(name)).status()).isEqualTo(201);
        assertThat(api.send("POST", "/vocabularies", admin, "application/json", "{}")
                        .status())
                .isEqualTo(415);
        Form big = new Form().field("name", "lower").file("vocabularyFile", "big.ttl", new byte[5_242_881]);
        assertThat(api.post("/vocabularies", admin, big).status()).isEqualTo(413);
        Form exact = new Form().field("name", "lower").file("vocabularyFile", "b.ttl", new byte[5_242_880]);
        assertThat(api.post("/vocabularies", admin, exact).status()).isEqualTo(400);
        assertThat(api.post("/vocabularies/Nonexisting" + name + "/versions", admin, new Form())
                        .status())
                .isEqualTo(400);
        Result duplicateWithBadContent = create(name.toUpperCase(), Turtle.bytes("not turtle {"));
        assertThat(duplicateWithBadContent.status()).isEqualTo(409);
        assertThat(newVersion("Nonexisting" + name, "d", Turtle.valid(name)).status())
                .isEqualTo(404);
        Result withVersion = api.post(
                "/vocabularies/" + name + "/versions",
                admin,
                new Form()
                        .field("description", "d")
                        .field("changelog", "c")
                        .field("version", "7")
                        .file("vocabularyFile", "f.ttl", Turtle.valid(name)));
        assertThat(withVersion.status()).isEqualTo(400);
        assertThat(withVersion.json().get("issues").get(0).get("name").asString())
                .isEqualTo("version");
        Result query = api.post("/vocabularies?x=1", admin, new Form());
        assertThat(query.json().get("issues").get(0).get("in").asString()).isEqualTo("query");
    }

    @Test
    void fieldRules() {
        Result r = api.post(
                "/vocabularies",
                admin,
                new Form().field("name", "ab").field("description", " ").field("unknown", "x"));
        assertThat(r.status()).isEqualTo(400);
        List<String> names = new ArrayList<>();
        r.json().get("issues").forEach(i -> names.add(i.get("name").asString()));
        assertThat(names).containsExactlyInAnyOrder("unknown", "vocabularyFile", "name", "description", "changelog");
    }

    @Test
    void validationReport() {
        Result syntax = create(unique("Syntax"), Turtle.bytes("@prefix ex: <http://e/> .\nex:a ex:b .\n"));
        assertThat(syntax.status()).isEqualTo(400);
        assertThat(syntax.header("Content-Type")).startsWith("application/problem+json");
        JsonNode issue = syntax.json().get("issues").get(0);
        assertThat(issue.get("type").asString()).isEqualTo("urn:problem-type:belgif:input-validation:invalidInput");
        assertThat(issue.get("name").asString()).isEqualTo("vocabularyFile");
        assertThat(issue.get("location").asString()).startsWith("line 2, column ");

        String missing = unique("Missing");
        Result s7 = create(
                unique("Refs"),
                Turtle.bytes("@prefix m: <" + CB + missing + "#> .\n<http://e/s> <http://e/p> \"x\" ."));
        assertThat(s7.status()).isEqualTo(400);
        assertThat(s7.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Referenced internal vocabulary '" + missing + "' does not exist.");
    }

    @Test
    void deprecatedReferenceWarning() {
        String used = unique("Used");
        create(used, Turtle.valid(used));
        newVersion(used, "Second", Turtle.bytes(new String(Turtle.valid(used)) + "# second\n"));
        String user = unique("User");
        Result r = create(
                user, Turtle.bytes("@prefix u: <" + CB + used + "/versions/1#> .\n" + new String(Turtle.valid(user))));
        assertThat(r.status()).isEqualTo(201);
        JsonNode warning = r.json().get("validationWarnings").get(0);
        assertThat(warning.get("code").asString()).isEqualTo("deprecatedVocabularyReference");
        assertThat(warning.get("description").asString())
                .isEqualTo("Referenced internal vocabulary '" + used
                        + "' version 1 is deprecated. Use version 2, the latest ACTIVE version.");
    }

    @Test
    void statusChanges() {
        String name = unique("Status");
        create(name, Turtle.valid(name));
        Result deprecated = api.patchJson("/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(deprecated.status()).isEqualTo(200);
        assertThat(deprecated.json().get("status").asString()).isEqualTo("DEPRECATED");
        Result again = api.patchJson("/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("detail").asString()).isEqualTo("Vocabulary is already in status DEPRECATED");
        assertThat(api.patchJson("/vocabularies/" + name, admin, "{\"status\":\"ARCHIVED\"}")
                        .status())
                .isEqualTo(400);
        assertThat(api.patchJson("/vocabularies/" + name, admin, "{\"status\":\"ACTIVE\",\"x\":1}")
                        .status())
                .isEqualTo(400);
        assertThat(api.patchJson("/vocabularies/" + name, admin, "[1]").status())
                .isEqualTo(400);
        assertThat(api.patchJson("/vocabularies/Unknown" + name, admin, "{\"status\":\"ACTIVE\"}")
                        .status())
                .isEqualTo(404);
        Result version = api.patchJson("/vocabularies/" + name + "/versions/1", admin, "{\"status\":\"DEPRECATED\"}");
        assertThat(version.status()).isEqualTo(200);
        assertThat(version.json().get("status").asString()).isEqualTo("DEPRECATED");
        Result missingVersion =
                api.patchJson("/vocabularies/" + name + "/versions/5", admin, "{\"status\":\"ACTIVE\"}");
        assertThat(missingVersion.status()).isEqualTo(404);
        assertThat(api.get("/vocabularies?status=DEPRECATED&name=" + name, viewer)
                        .json()
                        .get("total")
                        .asInt())
                .isEqualTo(1);
    }

    @Test
    void queryAndPathRules() {
        assertThat(api.get("/vocabularies?unknown=1", viewer).status()).isEqualTo(400);
        assertThat(api.get("/vocabularies?page=1&page=2", viewer).status()).isEqualTo(400);
        assertThat(api.get("/vocabularies?pageSize=101", viewer).status()).isEqualTo(400);
        assertThat(api.get("/vocabularies?sort=bogus", viewer).status()).isEqualTo(400);
        assertThat(api.get("/vocabularies?status=OLD", viewer).status()).isEqualTo(400);
        assertThat(api.get("/vocabularies?name=a-b", viewer).status()).isEqualTo(400);
        assertThat(api.get("/vocabularies?creationDate=2026-02-30", viewer).status())
                .isEqualTo(400);
        assertThat(api.get("/vocabularies?lastUpdated=2026-11-18&sort=-lastUpdated&page=99", viewer)
                        .status())
                .isEqualTo(200);
        Result path = api.get("/vocabularies/lowercase", viewer);
        assertThat(path.status()).isEqualTo(400);
        assertThat(path.json().get("issues").get(0).get("in").asString()).isEqualTo("path");
        assertThat(api.get("/vocabularies/Abc/versions/zero", viewer).status()).isEqualTo(400);
        assertThat(api.get("/vocabularies/Abc/versions?sort=-creationDate&pageSize=5", viewer)
                        .status())
                .isEqualTo(404);
        assertThat(api.get("/content/Abcunknown", viewer).status()).isEqualTo(404);
        assertThat(api.get("/content/Abcunknown/versions/1", viewer).status()).isEqualTo(404);
        assertThat(api.get("/vocabularies", viewer, "text/turtle").status()).isEqualTo(406);
        Result unknown = api.get("/unknown", viewer);
        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.header("Content-Type")).startsWith("application/problem+json");
        assertThat(api.send("PUT", "/vocabularies", admin, null, null).status()).isEqualTo(405);
    }

    @Test
    void searchRules() {
        assertThat(api.postJson("/vocabularies/searches", viewer, "{\"items\":[]}")
                        .status())
                .isEqualTo(400);
        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Abc\"}");
        }
        assertThat(api.postJson("/vocabularies/searches", viewer, many + "]}").status())
                .isEqualTo(400);
        assertThat(api.postJson("/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Abc\",\"version\":0}]}")
                        .status())
                .isEqualTo(400);
        assertThat(api.postJson("/vocabularies/searches", viewer, "{\"items\":[{\"name\":\"Abc\",\"x\":1}],\"y\":2}")
                        .status())
                .isEqualTo(400);
        assertThat(api.postJson("/vocabularies/searches?page=1", viewer, "{\"items\":[{\"name\":\"Abc\"}]}")
                        .status())
                .isEqualTo(400);
        assertThat(api.send("POST", "/vocabularies/searches", viewer, "text/plain", "x")
                        .status())
                .isEqualTo(415);
        assertThat(api.postJson("/vocabularies/searches", viewer, "{bad json").status())
                .isEqualTo(400);
    }

    @Test
    void externalVocabularies() {
        String name = unique("Ext");
        String ns = "http://ext.example/" + name + "/";
        Result registered = api.post(
                "/external-vocabularies",
                admin,
                new Form()
                        .field("name", name)
                        .field("namespaceUri", ns)
                        .field("aliasUris", "https://ext.example/" + name)
                        .field("description", "Cached copy")
                        .field("sourceUri", "https://ext.example/source.ttl")
                        .file("vocabularyFile", "e.ttl", Turtle.bytes("<http://a> <http://b> <http://c> .")));
        assertThat(registered.status()).isEqualTo(201);
        assertThat(registered.header("Location")).isEqualTo("/external-vocabularies/" + name);
        assertThat(registered.json().get("aliasUris").get(0).asString()).isEqualTo("https://ext.example/" + name);

        Result conflict = api.post(
                "/external-vocabularies",
                admin,
                new Form()
                        .field("name", unique("Other"))
                        .field("namespaceUri", "https://ext.example/" + name + "#")
                        .file("vocabularyFile", "e.ttl", Turtle.bytes("")));
        assertThat(conflict.status()).isEqualTo(409);
        Result badAlias = api.post(
                "/external-vocabularies",
                admin,
                new Form()
                        .field("name", unique("Alias"))
                        .field("namespaceUri", "http://alias.example/x")
                        .field("aliasUris", "http://alias.example/x/")
                        .file("vocabularyFile", "e.ttl", Turtle.bytes("")));
        assertThat(badAlias.status()).isEqualTo(400);
        Result badTurtle = api.post(
                "/external-vocabularies",
                admin,
                new Form()
                        .field("name", unique("Bad"))
                        .field("namespaceUri", "http://bad.example/" + name)
                        .file("vocabularyFile", "e.ttl", Turtle.bytes("<a> <b>")));
        assertThat(badTurtle.status()).isEqualTo(400);
        assertThat(badTurtle.json().get("issues")).hasSize(1);

        assertThat(api.get("/external-vocabularies/" + name, viewer)
                        .json()
                        .get("sourceUri")
                        .asString())
                .isEqualTo("https://ext.example/source.ttl");
        assertThat(api.get("/external-vocabularies?pageSize=100", viewer).status())
                .isEqualTo(200);
        assertThat(api.get("/external-vocabularies?sort=name", viewer).status()).isEqualTo(400);
        assertThat(api.get("/external-vocabularies/" + name + "/content", viewer)
                        .text())
                .contains("<http://a>");
        Result byUri = api.postJson("/external-vocabularies/content", viewer, "{\"namespaceUri\":\"" + ns + "\"}");
        assertThat(byUri.status()).isEqualTo(200);
        assertThat(byUri.header("Content-Disposition")).contains(name + ".ttl");
        assertThat(api.postJson("/external-vocabularies/content", viewer, "{\"namespaceUri\":\"http://none.example/\"}")
                        .status())
                .isEqualTo(404);

        Result search = api.postJson(
                "/external-vocabulary/searches",
                viewer,
                "{\"namespaceUris\":[\"" + ns + "\"],\"aliasUris\":[\"https://ext.example/" + name + "/\"]}");
        assertThat(search.json().get("total").asInt()).isEqualTo(1);
        assertThat(api.postJson(
                                "/external-vocabulary/searches",
                                viewer,
                                "{\"aliasUri\":\"https://ext.example/" + name + "\"}")
                        .json()
                        .get("total")
                        .asInt())
                .isEqualTo(1);
        assertThat(api.postJson("/external-vocabulary/searches", viewer, "{}").status())
                .isEqualTo(400);
        assertThat(api.postJson(
                                "/external-vocabulary/searches",
                                viewer,
                                "{\"namespaceUri\":\"http://a\",\"namespaceUris\":[\"http://b\"]}")
                        .status())
                .isEqualTo(400);
        assertThat(api.postJson("/external-vocabulary/searches", viewer, "{\"namespaceUris\":[]}")
                        .status())
                .isEqualTo(400);

        String user = unique("UsesExt");
        Result clean = create(user, Turtle.bytes("@prefix e: <" + ns + "> .\n" + new String(Turtle.valid(user))));
        assertThat(clean.json().get("validationWarnings")).isEmpty();

        assertThat(api.delete("/external-vocabularies/" + name, admin).status()).isEqualTo(204);
        Result gone = api.get("/external-vocabularies/" + name, viewer);
        assertThat(gone.status()).isEqualTo(404);
        assertThat(gone.json().get("detail").asString())
                .isEqualTo("External vocabulary with name '" + name + "' was not found.");
        assertThat(api.delete("/external-vocabularies/" + name, admin).status()).isEqualTo(404);
        assertThat(api.get("/content/" + user, viewer).status()).isEqualTo(200);
    }

    @Test
    void parallelVersionCreationsGetDistinctNumbers() throws Exception {
        String name = unique("Parallel");
        create(name, Turtle.valid(name));
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<Result>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 5; i++) {
                String description = "Parallel " + i;
                futures.add(pool.submit(() -> newVersion(name, description, Turtle.valid(name))));
            }
            List<String> locations = new ArrayList<>();
            for (Future<Result> f : futures) {
                Result r = f.get();
                assertThat(r.status()).isEqualTo(201);
                locations.add(r.header("Location"));
            }
            assertThat(locations).doesNotHaveDuplicates().hasSize(5);
        } finally {
            pool.shutdown();
        }
        JsonNode items =
                api.get("/vocabularies/" + name + "/versions", viewer).json().get("items");
        assertThat(items.get(0).get("version").asInt()).isEqualTo(6);
        assertThat(items.get(0).get("status").asString()).isEqualTo("ACTIVE");
        for (int i = 1; i < 6; i++) {
            assertThat(items.get(i).get("status").asString()).isEqualTo("DEPRECATED");
        }
    }

    @Test
    void securityHeadersAndCorrelationId() {
        Result r = api.get("/health", null);
        assertThat(r.header("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(r.header("X-Correlation-Id")).isNotBlank();
    }
}
