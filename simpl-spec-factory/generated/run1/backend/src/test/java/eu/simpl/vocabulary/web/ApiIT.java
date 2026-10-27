package eu.simpl.vocabulary.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** End-to-end API tests: real controllers, services, validators and SPARQL, against an in-memory dataset. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestTokens.class)
class ApiIT {

    private static final String BASE = "https://vocabulary.simpl.local/vocabularies";
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    MockMvc mvc;

    static String ttl(String extra) {
        return """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix ex: <https://example.org/test#> .
                ex:Ontology a owl:Ontology .
                ex:Person a owl:Class .
                """ + extra;
    }

    static String uniqueName(String prefix) {
        return prefix + SEQ.incrementAndGet() + "X" + System.nanoTime() % 100000;
    }

    static MockMultipartFile file(String content) {
        return new MockMultipartFile("vocabularyFile", "vocab.ttl", "text/turtle",
                content.getBytes(StandardCharsets.UTF_8));
    }

    ResultActions create(String name, String content, String token) throws Exception {
        return mvc.perform(multipart("/v1/vocabularies").file(file(content))
                .param("name", name).param("description", "Desc of " + name).param("changelog", "Initial version")
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    ResultActions newVersion(String name, String content) throws Exception {
        return mvc.perform(multipart("/v1/vocabularies/" + name + "/versions").file(file(content))
                .param("description", "Second").param("changelog", "Changed")
                .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN));
    }

    // ------------------------------------------------------------ access control (SIMPL-18196, D3, D4)

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void noTokenIs401ProblemDetails() throws Exception {
        mvc.perform(get("/v1/vocabularies"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")));
    }

    @Test
    void invalidTokenIs401() throws Exception {
        mvc.perform(get("/v1/vocabularies").header(HttpHeaders.AUTHORIZATION, TestTokens.BAD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:problem-type:belgif:invalidAccessToken"));
    }

    @Test
    void noRoleIs403OnReadAndWrite() throws Exception {
        mvc.perform(get("/v1/vocabularies").header(HttpHeaders.AUTHORIZATION, TestTokens.NOROLE))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        create(uniqueName("Norole"), ttl(""), TestTokens.NOROLE).andExpect(status().isForbidden());
    }

    @Test
    void viewerCanReadAndSearchButNotWrite() throws Exception {
        mvc.perform(get("/v1/vocabularies").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/vocabularies/searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"Missing\"}]}")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk());
        create(uniqueName("Viewer"), ttl(""), TestTokens.VIEWER).andExpect(status().isForbidden());
        mvc.perform(patch("/v1/vocabularies/Anything").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ create (SIMPL-18170, 18186, 18187, 18194)

    @Test
    void createReturns201WithVersionOneActive() throws Exception {
        String name = uniqueName("Create");
        create(name, ttl(""), TestTokens.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/vocabularies/" + name + "/versions/1"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.changelog").value("Initial version"))
                .andExpect(jsonPath("$.contentHref").value("http://localhost:8096/v1/content/" + name + "/versions/1"))
                .andExpect(jsonPath("$.validationWarnings", hasSize(0)));
        mvc.perform(get("/v1/vocabularies/" + name).header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.createdBy").value("ga-admin"))
                .andExpect(jsonPath("$.description").value("Desc of " + name));
    }

    @Test
    void duplicateNameIs409() throws Exception { // D13
        String name = uniqueName("Dup");
        create(name, ttl(""), TestTokens.ADMIN).andExpect(status().isCreated());
        create(name, ttl(""), TestTokens.ADMIN).andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:conflict"));
    }

    @Test
    void missingFieldsAndBadNameAre400WithAllIssues() throws Exception {
        mvc.perform(multipart("/v1/vocabularies").param("name", "lower")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.issues", hasSize(4))); // name, description, changelog, vocabularyFile
    }

    @Test
    void versionFieldIsRejected() throws Exception { // D8
        String name = uniqueName("Manual");
        create(name, ttl(""), TestTokens.ADMIN).andExpect(status().isCreated());
        mvc.perform(multipart("/v1/vocabularies/" + name + "/versions").file(file(ttl("")))
                        .param("description", "d").param("changelog", "c").param("version", "5")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("version"));
        mvc.perform(get("/v1/vocabularies/" + name).header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void syntaxErrorsAre400WithLocations() throws Exception { // SIMPL-18181, 18194
        String name = uniqueName("Syntax");
        create(name, ttl("ex:a ex:p undefined:x .\nex:b ex:p other:y .\n"), TestTokens.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues", hasSize(2)))
                .andExpect(jsonPath("$.issues[0].name").value("vocabularyFile"))
                .andExpect(jsonPath("$.issues[0].location").value(startsWith("line 5,")))
                .andExpect(jsonPath("$.issues[1].location").value(startsWith("line 6,")));
        mvc.perform(get("/v1/vocabularies/" + name).header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void structureErrorIs400() throws Exception { // D16
        create(uniqueName("Struct"), "<https://e.org/a> <https://e.org/p> \"x\" .", TestTokens.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].location").value("document"));
    }

    @Test
    void nonTurtleIs415() throws Exception { // D15
        mvc.perform(multipart("/v1/vocabularies")
                        .file(new MockMultipartFile("vocabularyFile", "v.json", "application/json", "{}".getBytes()))
                        .param("name", uniqueName("Json")).param("description", "d").param("changelog", "c")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/v1/vocabularies").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void tooLargeIs413() throws Exception { // D30 (limit lowered to 20000 bytes in the test profile)
        String big = ttl("# " + "x".repeat(25000) + "\n");
        create(uniqueName("Big"), big, TestTokens.ADMIN).andExpect(status().isPayloadTooLarge());
    }

    // ------------------------------------------------------------ versions (SIMPL-18173, 18190, 18191, 18168)

    @Test
    void newVersionDeprecatesPreviousAndHistoryIsListed() throws Exception {
        String name = uniqueName("Ver");
        create(name, ttl(""), TestTokens.ADMIN).andExpect(status().isCreated());
        newVersion(name, ttl("ex:Address a owl:Class .\n"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(header().string("Location", "/vocabularies/" + name + "/versions/2"));
        mvc.perform(get("/v1/vocabularies/" + name + "/versions").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].version").value(2))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[1].version").value(1))
                .andExpect(jsonPath("$.items[1].status").value("DEPRECATED"))
                .andExpect(jsonPath("$.items[1].latestVersionHref").value("/vocabularies/" + name + "/versions/2"));
        mvc.perform(get("/v1/vocabularies/" + name + "/versions/1").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEPRECATED"));
        mvc.perform(get("/v1/vocabularies/" + name + "/versions/9").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isNotFound());
    }

    @Test
    void newVersionOfUnknownVocabularyIs404() throws Exception {
        newVersion("DoesNotExist", ttl("")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:belgif:resourceNotFound"));
    }

    @Test
    void newVersionOfDeprecatedVocabularyIs409() throws Exception { // D11
        String name = uniqueName("DepVer");
        create(name, ttl(""), TestTokens.ADMIN).andExpect(status().isCreated());
        patchStatus(name, "DEPRECATED").andExpect(status().isOk());
        newVersion(name, ttl("")).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------ status (SIMPL-18175, D10)

    ResultActions patchStatus(String name, String status) throws Exception {
        return mvc.perform(patch("/v1/vocabularies/" + name).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}").header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN));
    }

    @Test
    void statusChangeRoundTrip() throws Exception {
        String name = uniqueName("Status");
        create(name, ttl(""), TestTokens.ADMIN);
        newVersion(name, ttl(""));
        patchStatus(name, "DEPRECATED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEPRECATED"));
        mvc.perform(get("/v1/vocabularies/" + name + "/versions/2").header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(jsonPath("$.status").value("DEPRECATED"));
        patchStatus(name, "DEPRECATED").andExpect(status().isConflict());
        patchStatus(name, "ACTIVE").andExpect(status().isOk());
        mvc.perform(get("/v1/vocabularies/" + name + "/versions/2").header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/v1/vocabularies/" + name + "/versions/1").header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(jsonPath("$.status").value("DEPRECATED"));
        patchStatus(name, "RETIRED").andExpect(status().isBadRequest());
        patchStatus("NotThere", "ACTIVE").andExpect(status().isNotFound());
    }

    @Test
    void versionStatusRules() throws Exception {
        String name = uniqueName("VStatus");
        create(name, ttl(""), TestTokens.ADMIN);
        newVersion(name, ttl(""));
        mvc.perform(patch("/v1/vocabularies/" + name + "/versions/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}").header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isConflict());
        mvc.perform(patch("/v1/vocabularies/" + name + "/versions/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}").header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEPRECATED"));
    }

    // ------------------------------------------------------------ list, filter, sort (SIMPL-18162, 18164)

    @Test
    void listFilterSortAndPaging() throws Exception {
        String tag = "Zq" + (System.nanoTime() % 100000);
        create(tag + "Beta", ttl(""), TestTokens.ADMIN);
        create(tag + "Alpha", ttl(""), TestTokens.ADMIN);
        patchStatus(tag + "Beta", "DEPRECATED");
        String lower = tag.substring(0, 1) + tag.substring(1).toLowerCase(); // D23: case-insensitive contains
        mvc.perform(get("/v1/vocabularies").param("name", lower).param("sort", "-name")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].name").value(tag + "Beta"));
        mvc.perform(get("/v1/vocabularies").param("name", tag).param("status", "DEPRECATED")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].status").value("DEPRECATED"));
        mvc.perform(get("/v1/vocabularies").param("name", tag).param("pageSize", "1").param("page", "2")
                        .param("sort", "name").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(jsonPath("$.items[0].name").value(tag + "Beta"))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.prev").value(containsString("page=1")));
        mvc.perform(get("/v1/vocabularies").param("name", "NoMatchAtAll123")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void invalidListParametersAre400() throws Exception {
        for (String[] p : new String[][] {{"sort", "creator"}, {"status", "OLD"}, {"page", "0"}, {"pageSize", "101"},
                {"pageSize", "abc"}, {"creationDate", "yesterday"}, {"creator", "x"}, {"name", "a"}}) {
            mvc.perform(get("/v1/vocabularies").param(p[0], p[1]).header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.issues[0].name").value(p[0]));
        }
    }

    // ------------------------------------------------------------ download (SIMPL-18165, 18452)

    @Test
    void downloadReturnsUploadedTurtle() throws Exception {
        String name = uniqueName("Down");
        create(name, ttl("# version one\n"), TestTokens.ADMIN);
        newVersion(name, ttl("# version two\n"));
        mvc.perform(get("/v1/content/" + name).header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/turtle"))
                .andExpect(header().string("Content-Disposition", containsString(name + ".ttl")))
                .andExpect(content().string(containsString("# version two")));
        mvc.perform(get("/v1/content/" + name + "/versions/1").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(name + "-v1.ttl")))
                .andExpect(content().string(containsString("# version one")));
        mvc.perform(get("/v1/content/" + name + "/versions/3").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/v1/content/" + name).accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isNotAcceptable());
    }

    // ------------------------------------------------------------ references (SIMPL-18183, 18184, D17, D18, D19)

    @Test
    void deprecatedReferenceBlocksAndActiveReferencePasses() throws Exception {
        String dep = uniqueName("Ref");
        create(dep, ttl(""), TestTokens.ADMIN);
        newVersion(dep, ttl("ex:Address a owl:Class .\n"));
        String user = uniqueName("User");
        create(user, ttl("@prefix r: <" + BASE + "/" + dep + "/2#> .\nex:Home a r:Address .\n"), TestTokens.ADMIN)
                .andExpect(status().isCreated());
        newVersion(user, ttl("@prefix r: <" + BASE + "/" + dep + "/1#> .\nex:Home a r:Person .\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].detail").value(containsString("is DEPRECATED")))
                .andExpect(jsonPath("$.issues[0].detail").value(containsString("latest ACTIVE version 2")));
        newVersion(user, ttl("ex:Home a <" + BASE + "/Ghost/1#Thing> .\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].detail").value(containsString("'Ghost' does not exist")));
        mvc.perform(get("/v1/vocabularies/" + user).header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void externalNamespaceGivesWarningOn201() throws Exception { // D20
        create(uniqueName("Warn"), ttl("@prefix dcterms: <http://purl.org/dc/terms/> .\nex:Ontology dcterms:title \"t\" .\n"),
                TestTokens.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validationWarnings[0].code").value("externalReferenceSkipped"));
    }

    // ------------------------------------------------------------ search (OpenAPI only)

    @Test
    void searchReturnsLatestAndVersionedItems() throws Exception {
        String name = uniqueName("Search");
        create(name, ttl(""), TestTokens.ADMIN);
        newVersion(name, ttl(""));
        mvc.perform(post("/v1/vocabularies/searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"" + name + "\"},{\"name\":\"" + name + "\",\"version\":1},"
                                + "{\"name\":\"Unknown\"}]}")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].latest").value(true))
                .andExpect(jsonPath("$.items[0].version").value(2))
                .andExpect(jsonPath("$.items[1].latest").value(false))
                .andExpect(jsonPath("$.items[1].status").value("DEPRECATED"));
        mvc.perform(post("/v1/vocabularies/searches").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ external vocabularies (OpenAPI only)

    @Test
    void externalVocabularyLifecycle() throws Exception {
        String name = uniqueName("Ext");
        String ns = "http://purl.example/" + name + "/";
        MockMultipartHttpServletRequestBuilder req = multipart("/v1/external-vocabularies");
        req.file(new MockMultipartFile("vocabularyFile", "x.ttl", "text/turtle",
                ("<" + ns + "title> a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .").getBytes()));
        mvc.perform(req.param("name", name).param("namespaceUri", ns).param("aliasUris", "https://alias.example/" + name)
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.href").value("/external-vocabularies/" + name));
        mvc.perform(post("/v1/external-vocabulary/searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliasUri\":\"https://alias.example/" + name + "/\"}")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
        mvc.perform(post("/v1/external-vocabulary/searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aliasUri\":\"https://a.example\",\"namespaceUri\":\"https://b.example\"}")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/external-vocabularies/content").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespaceUri\":\"" + ns + "\"}").header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isOk()).andExpect(content().string(containsString("title")));
        mvc.perform(get("/v1/external-vocabularies/" + name + "/content").header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isOk());
        mvc.perform(delete("/v1/external-vocabularies/" + name).header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/v1/external-vocabularies/" + name).header(HttpHeaders.AUTHORIZATION, TestTokens.ADMIN))
                .andExpect(status().isNoContent());
        mvc.perform(get("/v1/external-vocabularies/" + name).header(HttpHeaders.AUTHORIZATION, TestTokens.VIEWER))
                .andExpect(status().isNotFound());
    }
}
