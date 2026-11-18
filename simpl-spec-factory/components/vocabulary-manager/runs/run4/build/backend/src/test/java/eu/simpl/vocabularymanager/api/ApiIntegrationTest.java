package eu.simpl.vocabularymanager.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

import eu.simpl.vocabularymanager.store.SparqlClient;
import eu.simpl.vocabularymanager.support.InMemorySparqlClient;
import eu.simpl.vocabularymanager.support.TestTokens;
import eu.simpl.vocabularymanager.support.Turtle;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String PROBLEM = "application/problem+json";

    @TestConfiguration
    static class StoreConfig {
        @Bean
        @Primary
        InMemorySparqlClient inMemorySparqlClient() {
            return new InMemorySparqlClient();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("vocabulary.security.jwks-url", TestTokens::jwksUrl);
        registry.add("vocabulary.security.issuer", () -> TestTokens.ISSUER);
        registry.add("vocabulary.content-base-url", () -> Turtle.BASE);
        registry.add("logging.structured.format.console", () -> "");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SparqlClient store;

    @BeforeEach
    void clean() {
        ((InMemorySparqlClient) store).clear();
        ((InMemorySparqlClient) store).setDown(false);
    }

    @AfterEach
    void up() {
        ((InMemorySparqlClient) store).setDown(false);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static MockMultipartHttpServletRequestBuilder form(String path, Map<String, String> fields, byte[] file) {
        MockMultipartHttpServletRequestBuilder builder = multipart(HttpMethod.POST, path);
        if (file != null) {
            builder.part(new MockPart("vocabularyFile", "vocabulary.ttl", file));
        }
        fields.forEach((name, value) -> builder.part(new MockPart(name, value.getBytes(StandardCharsets.UTF_8))));
        return builder;
    }

    private ResultActions createVocabulary(String name) throws Exception {
        return mvc.perform(form(
                        "/vocabularies",
                        Map.of("name", name, "description", "About " + name, "changelog", "Initial version."),
                        Turtle.simple(name))
                .header("Authorization", bearer(TestTokens.admin())));
    }

    private ResultActions createVersion(String name, String label) throws Exception {
        return mvc.perform(form(
                        "/vocabularies/" + name + "/versions",
                        Map.of("description", "About " + label, "changelog", "Changed " + label),
                        Turtle.simple(label))
                .header("Authorization", bearer(TestTokens.admin())));
    }

    @Test
    void healthNeedsNoTokenAndReportsStoreDown() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        ((InMemorySparqlClient) store).setDown(true);
        mvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void noTokenGives401AndInvalidTokenGives401() throws Exception {
        mvc.perform(get("/vocabularies"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unauthorized"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(header().exists("WWW-Authenticate"));
        mvc.perform(get("/vocabularies").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/vocabularies").header("Authorization", "Basic Zm9vOmJhcg=="))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/vocabularies")).andExpect(status().isUnauthorized());
    }

    @Test
    void rolesDecideAccess() throws Exception {
        mvc.perform(get("/vocabularies").header("Authorization", bearer(TestTokens.noRole())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:forbidden"))
                .andExpect(jsonPath("$.title").value("Forbidden"));
        mvc.perform(get("/vocabularies").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isOk());
        mvc.perform(form("/vocabularies", Map.of("name", "Viewer"), null)
                        .header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/vocabularies/Any")
                        .header("Authorization", bearer(TestTokens.viewer()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/external-vocabularies/Any").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/vocabularies/searches")
                        .header("Authorization", bearer(TestTokens.viewer()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"Nothing\"}]}"))
                .andExpect(status().isOk());
    }

    @Test
    void createReturns201WithLocationWarningsAndCreator() throws Exception {
        createVocabulary("Healthcare")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/vocabularies/Healthcare/versions/1"))
                .andExpect(jsonPath("$.href").value("/vocabularies/Healthcare/versions/1"))
                .andExpect(jsonPath("$.contentHref").value(Turtle.BASE + "/content/Healthcare/versions/1"))
                .andExpect(jsonPath("$.latestVersionHref").value("/vocabularies/Healthcare/versions/1"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.changelog").value("Initial version."))
                .andExpect(jsonPath("$.createdBy").value("ga-admin@example.com"))
                .andExpect(jsonPath("$.validationWarnings", hasSize(0)));
        mvc.perform(get("/vocabularies/Healthcare").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.href").value("/vocabularies/Healthcare"))
                .andExpect(jsonPath("$.contentHref").value(Turtle.BASE + "/content/Healthcare"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.description").value("About Healthcare"))
                .andExpect(jsonPath("$.creationDate", startsWith("20")));
    }

    @Test
    void createRejectsBadFieldsDuplicatesTooLargeAndWrongMediaType() throws Exception {
        mvc.perform(form("/vocabularies", Map.of("name", "lower", "description", " "), Turtle.simple("x"))
                        .header("Authorization", bearer(TestTokens.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.issues[*].name", contains("name", "description", "changelog")))
                .andExpect(jsonPath("$.issues[*].in", everyItem(is("body"))));
        mvc.perform(form(
                                "/vocabularies",
                                Map.of("name", "Fine", "description", "d", "changelog", "c", "extra", "x"),
                                Turtle.simple("x"))
                        .header("Authorization", bearer(TestTokens.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("extra"));
        createVocabulary("Taken").andExpect(status().isCreated());
        createVocabulary("TAKEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:conflict"))
                .andExpect(jsonPath("$.detail").value("Vocabulary with name 'TAKEN' already exists."));
        byte[] large = new byte[5 * 1024 * 1024 + 1];
        mvc.perform(form("/vocabularies", Map.of("name", "Large", "description", "d", "changelog", "c"), large)
                        .header("Authorization", bearer(TestTokens.admin())))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:payloadTooLarge"))
                .andExpect(jsonPath("$.detail", startsWith("Uploaded vocabulary file is too large")));
        mvc.perform(post("/vocabularies")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unsupportedMediaType"));
    }

    @Test
    void validationErrorsAreReportedAndNothingIsStored() throws Exception {
        mvc.perform(form(
                                "/vocabularies",
                                Map.of("name", "Broken", "description", "d", "changelog", "c"),
                                Turtle.bytes("@prefix ex: <http://e/> .\nex:a ex:b"))
                        .header("Authorization", bearer(TestTokens.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues", hasSize(1)))
                .andExpect(jsonPath("$.issues[0].type").value("urn:problem-type:belgif:input-validation:invalidInput"))
                .andExpect(jsonPath("$.issues[0].name").value("vocabularyFile"))
                .andExpect(jsonPath("$.issues[0].detail", startsWith("Encountered invalid Turtle syntax")))
                .andExpect(jsonPath("$.issues[0].location", startsWith("line 2, column")));
        mvc.perform(get("/vocabularies/Broken").header("Authorization", bearer(TestTokens.admin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:belgif:resourceNotFound"))
                .andExpect(jsonPath("$.detail").value("Vocabulary with name 'Broken' was not found."))
                .andExpect(jsonPath("$.issues[0].in").value("path"))
                .andExpect(jsonPath("$.issues[0].name").value("vocabularyName"));
    }

    @Test
    void newVersionFlowDownloadAndHistory() throws Exception {
        createVocabulary("Flow").andExpect(status().isCreated());
        createVersion("Flow", "two")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/vocabularies/Flow/versions/2"))
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(form(
                                "/vocabularies/Flow/versions",
                                Map.of("description", "d", "changelog", "c", "version", "7"),
                                Turtle.simple("v"))
                        .header("Authorization", bearer(TestTokens.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].in").value("body"))
                .andExpect(jsonPath("$.issues[0].name").value("version"));
        createVersion("Flow", "two")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Version 2 of vocabulary 'Flow' already has this content and description."));
        createVersion("Unknown", "x").andExpect(status().isNotFound());
        mvc.perform(get("/vocabularies/Flow/versions").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[*].version", contains(2, 1)))
                .andExpect(jsonPath("$.items[*].status", contains("ACTIVE", "DEPRECATED")))
                .andExpect(jsonPath("$.items[1].latestVersionHref").value("/vocabularies/Flow/versions/2"))
                .andExpect(jsonPath("$.self").value("/vocabularies/Flow/versions?page=1&pageSize=20"));
        mvc.perform(get("/vocabularies/Flow/versions/1").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPRECATED"))
                .andExpect(jsonPath("$.createdBy").value("ga-admin@example.com"));
        mvc.perform(get("/vocabularies/Flow/versions/9").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isNotFound());
        mvc.perform(get("/vocabularies/Flow/versions/zero").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/content/Flow").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/turtle"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Flow.ttl\""))
                .andExpect(content().bytes(Turtle.simple("two")));
        mvc.perform(get("/content/Flow/versions/1").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Flow-v1.ttl\""))
                .andExpect(content().bytes(Turtle.simple("Flow")));
        mvc.perform(get("/content/Flow")
                        .header("Authorization", bearer(TestTokens.viewer()))
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void statusPatchesValidateAndConflict() throws Exception {
        createVocabulary("Status").andExpect(status().isCreated());
        mvc.perform(patch("/vocabularies/Status")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPRECATED"))
                .andExpect(jsonPath("$.href").value("/vocabularies/Status"));
        mvc.perform(patch("/vocabularies/Status")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vocabulary is already in status DEPRECATED"));
        mvc.perform(patch("/vocabularies/Status")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RETIRED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("status"));
        mvc.perform(patch("/vocabularies/Status")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/vocabularies/Status")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(patch("/vocabularies/Missing")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/vocabularies/Status/versions/1")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPRECATED"))
                .andExpect(jsonPath("$.latestVersionHref").value("/vocabularies/Status/versions/1"));
        mvc.perform(patch("/vocabularies/Status/versions/1")
                        .header("Authorization", bearer(TestTokens.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vocabulary version is already in status DEPRECATED"));
    }

    @Test
    void listFiltersSortsPagesAndRejectsUnknownParameters() throws Exception {
        createVocabulary("AlphaShacl");
        createVocabulary("Bravo");
        createVocabulary("Charlie");
        String token = bearer(TestTokens.viewer());
        mvc.perform(get("/vocabularies?name=shacl").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("AlphaShacl")));
        mvc.perform(get("/vocabularies?sort=-name&page=1&pageSize=2").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("Charlie", "Bravo")))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.self").value("/vocabularies?page=1&pageSize=2&sort=-name"))
                .andExpect(jsonPath("$.next").value("/vocabularies?page=2&pageSize=2&sort=-name"))
                .andExpect(jsonPath("$.last").value("/vocabularies?page=2&pageSize=2&sort=-name"))
                .andExpect(jsonPath("$.prev").doesNotExist());
        mvc.perform(get("/vocabularies?page=5").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/vocabularies?status=DEPRECATED").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.last").doesNotExist());
        for (String bad : new String[] {
            "unknown=1",
            "page=0",
            "pageSize=101",
            "pageSize=x",
            "sort=version",
            "status=active",
            "name=a-b",
            "creationDate=2026-02-30",
            "lastUpdated=today",
            "page=1&page=2"
        }) {
            mvc.perform(get("/vocabularies?" + bad).header("Authorization", token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.issues[0].in").value("query"));
        }
        mvc.perform(get("/vocabularies/AlphaShacl?x=1").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/vocabularies/lowercase").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].in").value("path"));
    }

    @Test
    void searchesValidateTheirBodies() throws Exception {
        createVocabulary("Searched");
        String token = bearer(TestTokens.viewer());
        mvc.perform(
                        post("/vocabularies/searches")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"items\":[{\"name\":\"Searched\"},{\"name\":\"Searched\",\"version\":1},{\"name\":\"Gone\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].latest").value(true))
                .andExpect(jsonPath("$.items[0].href").value("/vocabularies/Searched"))
                .andExpect(jsonPath("$.items[1].href").value("/vocabularies/Searched/versions/1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(100))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.self").doesNotExist());
        for (String bad : new String[] {
            "{}",
            "{\"items\":[]}",
            "{\"items\":[{\"name\":\"ok\"}]}",
            "{\"items\":[{\"name\":\"Okay\",\"version\":0}]}",
            "{\"items\":[{\"name\":\"Okay\",\"extra\":1}]}",
            "{\"items\":[1]}",
            "[]"
        }) {
            mvc.perform(post("/vocabularies/searches")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bad))
                    .andExpect(status().isBadRequest());
        }
        StringBuilder many = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 101; i++) {
            many.append(i == 0 ? "" : ",").append("{\"name\":\"Name").append(i).append("\"}");
        }
        mvc.perform(post("/vocabularies/searches")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(many.append("]}").toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("items"));
        mvc.perform(post("/vocabularies/searches?page=1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"Searched\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void externalVocabularyLifecycle() throws Exception {
        String admin = bearer(TestTokens.admin());
        mvc.perform(form(
                                "/external-vocabularies",
                                Map.of(
                                        "name", "DublinCoreTerms",
                                        "namespaceUri", "http://purl.org/dc/terms/",
                                        "aliasUris", "https://purl.org/dc/terms/",
                                        "description", "Cached copy",
                                        "sourceUri", "https://www.dublincore.org/dc.ttl"),
                                Turtle.simple("dc"))
                        .header("Authorization", admin))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/external-vocabularies/DublinCoreTerms"))
                .andExpect(jsonPath("$.href").value("/external-vocabularies/DublinCoreTerms"))
                .andExpect(jsonPath("$.aliasUris[0]").value("https://purl.org/dc/terms/"))
                .andExpect(jsonPath("$.createdBy").value("ga-admin@example.com"));
        mvc.perform(form(
                                "/external-vocabularies",
                                Map.of("name", "Again", "namespaceUri", "https://purl.org/dc/terms"),
                                Turtle.simple("dc"))
                        .header("Authorization", admin))
                .andExpect(status().isConflict());
        mvc.perform(form(
                                "/external-vocabularies",
                                Map.of("name", "BadUri", "namespaceUri", "not a uri"),
                                Turtle.simple("dc"))
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(form(
                                "/external-vocabularies",
                                Map.of("name", "BadTurtle", "namespaceUri", "http://bad.example/"),
                                Turtle.bytes("bad"))
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues", hasSize(1)))
                .andExpect(jsonPath("$.issues[0].name").value("vocabularyFile"));
        String viewer = bearer(TestTokens.viewer());
        mvc.perform(get("/external-vocabularies").header("Authorization", viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("DublinCoreTerms")));
        mvc.perform(get("/external-vocabularies?sort=name").header("Authorization", viewer))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/external-vocabularies/DublinCoreTerms").header("Authorization", viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUri").value("https://www.dublincore.org/dc.ttl"));
        mvc.perform(get("/external-vocabularies/DublinCoreTerms/content").header("Authorization", viewer))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"DublinCoreTerms.ttl\""));
        mvc.perform(post("/external-vocabularies/content")
                        .header("Authorization", viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespaceUri\":\"https://purl.org/dc/terms#\"}"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(Turtle.simple("dc")));
        mvc.perform(post("/external-vocabulary/searches")
                        .header("Authorization", viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespaceUris\":[\"http://none.example\",\"http://purl.org/dc/terms\"],"
                                + "\"aliasUris\":[\"https://purl.org/dc/terms/\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(1));
        for (String bad : new String[] {
            "{}",
            "{\"namespaceUri\":\"http://a\",\"aliasUri\":\"http://b\"}",
            "{\"namespaceUri\":\"http://a\",\"namespaceUris\":[\"http://b\"]}",
            "{\"namespaceUris\":[]}",
            "{\"namespaceUri\":\"no uri\"}",
            "{\"other\":1}"
        }) {
            mvc.perform(post("/external-vocabulary/searches")
                            .header("Authorization", viewer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bad))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/external-vocabularies/DublinCoreTerms").header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/external-vocabularies/DublinCoreTerms").header("Authorization", viewer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("External vocabulary with name 'DublinCoreTerms' was not found."))
                .andExpect(jsonPath("$.issues[0].name").value("name"));
        mvc.perform(post("/external-vocabularies/content")
                        .header("Authorization", viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespaceUri\":\"https://purl.org/dc/terms#\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void storeDownGives503WithRetryAfter() throws Exception {
        ((InMemorySparqlClient) store).setDown(true);
        mvc.perform(get("/vocabularies").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:belgif:serviceUnavailable"))
                .andExpect(jsonPath("$.detail").value("The vocabulary store is not available. Retry later."));
        createVocabulary("Down").andExpect(status().isServiceUnavailable());
    }

    @Test
    void unknownPathsAndMethods() throws Exception {
        mvc.perform(get("/nothing").header("Authorization", bearer(TestTokens.viewer())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
        mvc.perform(delete("/vocabularies/Something").header("Authorization", bearer(TestTokens.admin())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
    }
}
