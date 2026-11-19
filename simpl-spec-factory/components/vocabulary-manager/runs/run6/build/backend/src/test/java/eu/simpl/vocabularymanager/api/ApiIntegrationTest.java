package eu.simpl.vocabularymanager.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.simpl.vocabularymanager.security.SecurityConfig;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * API behaviour through the whole Spring stack with the real store adapter on an embedded Fuseki (decision E3):
 * main flow, access rules (D16, D17), order of checks (D51), limits and error bodies.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final FusekiServer FUSEKI = FusekiServer.create()
            .port(0)
            .add("/vocabulary", DatasetGraphFactory.createTxnMem())
            .build()
            .start();

    @DynamicPropertySource
    static void store(DynamicPropertyRegistry registry) {
        registry.add("vm.store.dataset-url", () -> "http://localhost:" + FUSEKI.getPort() + "/vocabulary");
    }

    @AfterAll
    static void stop() {
        FUSEKI.stop();
    }

    @Autowired
    private MockMvc mvc;

    private static final String TTL = "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
            + "@prefix ex: <https://example.eu/it#> .\nex:s skos:prefLabel \"%s\"@en .\n";

    private static JwtRequestPostProcessor admin() {
        return jwt().jwt(j -> j.claim("email", "ga-admin@example.com"))
                .authorities(new SimpleGrantedAuthority(SecurityConfig.ADMIN));
    }

    private static JwtRequestPostProcessor viewer() {
        return jwt().authorities(new SimpleGrantedAuthority(SecurityConfig.VIEWER));
    }

    private static JwtRequestPostProcessor noRole() {
        return jwt();
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static MockPart file(String label) {
        return filePart("v.ttl", String.format(TTL, label).getBytes(StandardCharsets.UTF_8));
    }

    private static MockPart filePart(String filename, byte[] bytes) {
        MockPart part = new MockPart("vocabularyFile", filename, bytes);
        part.getHeaders().setContentType(MediaType.parseMediaType("text/turtle"));
        return part;
    }

    private static void fields(MockMultipartHttpServletRequestBuilder builder, String... nameValues) {
        for (int i = 0; i < nameValues.length; i += 2) {
            builder.part(new MockPart(nameValues[i], nameValues[i + 1].getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static MockMultipartHttpServletRequestBuilder createRequest(String name, String label) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/v1/vocabularies");
        builder.part(file(label));
        fields(builder, "name", name, "description", "Description " + label, "changelog", "Initial version.");
        return builder;
    }

    @Test
    void mainFlow() throws Exception {
        String name = unique("Flow");
        mvc.perform(createRequest(name, "one").with(admin()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/vocabularies/" + name + "/versions/1"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdBy").value("ga-admin@example.com"))
                .andExpect(jsonPath("$.validationWarnings", hasSize(0)));
        MockMultipartHttpServletRequestBuilder version = multipart("/v1/vocabularies/" + name + "/versions");
        version.part(file("two"));
        fields(version, "description", "Second", "changelog", "More");
        mvc.perform(version.with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/v1/vocabularies")
                        .queryParam("name", name.toLowerCase())
                        .with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].version").value(2))
                .andExpect(jsonPath("$.items[0].description").value("Second"))
                .andExpect(jsonPath("$.items[0].createdBy").value("ga-admin@example.com"))
                .andExpect(jsonPath("$.prev").doesNotExist());
        mvc.perform(get("/v1/vocabularies/" + name + "/versions").with(viewer()))
                .andExpect(jsonPath("$.items[0].version").value(2))
                .andExpect(jsonPath("$.items[1].status").value("DEPRECATED"))
                .andExpect(jsonPath("$.items[1].latestVersionHref").value("/vocabularies/" + name + "/versions/2"));
        mvc.perform(get("/v1/content/" + name + "/versions/1").with(viewer()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + name + "-v1.ttl\""))
                .andExpect(content().string(String.format(TTL, "one")));
        mvc.perform(get("/v1/content/" + name).with(viewer()))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + name + ".ttl\""));
        mvc.perform(post("/v1/vocabularies/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"" + name + "\"},{\"name\":\"" + name + "\",\"version\":1}]}")
                        .with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.pageSize").value(100))
                .andExpect(jsonPath("$.self").doesNotExist())
                .andExpect(jsonPath("$.items[0].latest").value(true))
                .andExpect(jsonPath("$.items[1].status").value("DEPRECATED"));
        mvc.perform(patch("/v1/vocabularies/" + name)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEPRECATED\"}")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPRECATED"));
        mvc.perform(patch("/v1/vocabularies/" + name + "/versions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}")
                        .with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:conflict"));
    }

    @Test
    void accessRules() throws Exception {
        mvc.perform(get("/v1/vocabularies"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unauthorized"));
        mvc.perform(get("/v1/vocabularies").with(noRole()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:forbidden"));
        mvc.perform(createRequest(unique("Viewer"), "x").with(viewer())).andExpect(status().isForbidden());
        mvc.perform(post("/v1/vocabularies/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"Abc\"}]}")
                        .with(viewer()))
                .andExpect(status().isOk());
        mvc.perform(delete("/v1/external-vocabularies/Abc").with(viewer())).andExpect(status().isForbidden());
        mvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void orderOfChecksOnWrites() throws Exception {
        String name = unique("Order");
        mvc.perform(createRequest(name, "one").with(admin())).andExpect(status().isCreated());
        // 415 before 400
        mvc.perform(post("/v1/vocabularies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(admin()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unsupportedMediaType"));
        // 413 before 400
        MockMultipartHttpServletRequestBuilder big = multipart("/v1/vocabularies");
        big.part(filePart("big.ttl", new byte[5_242_881]));
        mvc.perform(big.with(admin()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.issues[0].name").value("vocabularyFile"));
        // exactly at the limit is not 413
        MockMultipartHttpServletRequestBuilder limit = multipart("/v1/vocabularies");
        limit.part(filePart("l.ttl", new byte[5_242_880]));
        mvc.perform(limit.with(admin())).andExpect(status().isBadRequest());
        // 400 before 404: malformed request to an unknown name
        MockMultipartHttpServletRequestBuilder malformed = multipart("/v1/vocabularies/Unknown" + name + "/versions");
        malformed.part(file("x"));
        fields(malformed, "description", "d");
        mvc.perform(malformed.with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("changelog"));
        // 409 before content validation
        MockMultipartHttpServletRequestBuilder duplicate = multipart("/v1/vocabularies");
        duplicate.part(filePart("b.ttl", "bad".getBytes()));
        fields(duplicate, "name", name.toUpperCase(), "description", "d", "changelog", "c");
        mvc.perform(duplicate.with(admin())).andExpect(status().isConflict());
        // version field and unknown fields
        MockMultipartHttpServletRequestBuilder versionField = multipart("/v1/vocabularies/" + name + "/versions");
        versionField.part(file("v"));
        fields(versionField, "description", "d", "changelog", "c", "version", "5");
        mvc.perform(versionField.with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("version"))
                .andExpect(jsonPath("$.issues[0].in").value("body"));
        MockMultipartHttpServletRequestBuilder unknown = createRequest(unique("Unknown"), "u");
        fields(unknown, "colour", "red");
        mvc.perform(unknown.with(admin()))
                .andExpect(jsonPath("$.issues[0].name").value("colour"));
        // query parameter on a write
        mvc.perform(createRequest(unique("Query"), "q").queryParam("x", "1").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].in").value("query"));
    }

    @Test
    void validationReport() throws Exception {
        MockMultipartHttpServletRequestBuilder bad = multipart("/v1/vocabularies");
        bad.part(filePart("b.ttl", "@prefix x: <http://e/> .\nx:a x:b .\n".getBytes()));
        fields(bad, "name", unique("Syntax"), "description", "d", "changelog", "c");
        mvc.perform(bad.with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.issues", hasSize(1)))
                .andExpect(jsonPath("$.issues[0].detail", containsString("Encountered invalid Turtle syntax")))
                .andExpect(jsonPath("$.issues[0].location", containsString("line 2")));
    }

    @Test
    void readValidation() throws Exception {
        mvc.perform(get("/v1/vocabularies").queryParam("pageSize", "101").with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/vocabularies")
                        .queryParam("sort", "bogus")
                        .queryParam("page", "x")
                        .with(viewer()))
                .andExpect(jsonPath("$.issues", hasSize(2)));
        mvc.perform(get("/v1/vocabularies")
                        .queryParam("creationDate", "2026-02-30")
                        .with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/vocabularies?status=ACTIVE&status=ACTIVE").with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/vocabularies")
                        .queryParam("page", "99999999999999999999999")
                        .with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/v1/vocabularies/lowercase").with(viewer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].in").value("path"));
        mvc.perform(get("/v1/vocabularies/Missing1234/versions/0").with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/vocabularies/Missing1234").with(viewer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Vocabulary with name 'Missing1234' was not found."))
                .andExpect(jsonPath("$.issues[0].name").value("vocabularyName"));
        mvc.perform(get("/v1/vocabularies").with(viewer()).accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotAcceptable());
        mvc.perform(get("/v1/vocabularies")
                        .queryParam("lastUpdated", "2026-11-18T10:00:00+01:00")
                        .with(viewer()))
                .andExpect(status().isOk());
    }

    @Test
    void jsonBodies() throws Exception {
        mvc.perform(post("/v1/vocabularies/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}")
                        .with(viewer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("items"));
        mvc.perform(post("/v1/vocabularies/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"Abc\",\"extra\":1}]}")
                        .with(viewer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].type").value("urn:problem-type:belgif:input-validation:unknownInput"));
        mvc.perform(post("/v1/vocabularies/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json")
                        .with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/v1/vocabularies/Abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RETIRED\"}")
                        .with(admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/v1/vocabularies/Abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}")
                        .with(admin()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/v1/external-vocabulary/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespaceUri\":\"http://a\",\"aliasUri\":\"http://b\"}")
                        .with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/external-vocabulary/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/external-vocabulary/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespaceUris\":[\"http://nothing.example\"]}")
                        .with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void externalVocabularies() throws Exception {
        String name = unique("Ext");
        String ns = "http://example.org/" + name + "/";
        MockMultipartHttpServletRequestBuilder register = multipart("/v1/external-vocabularies");
        register.part(filePart("e.ttl", ("@prefix e: <" + ns + "> .\ne:a e:b e:c .\n").getBytes()));
        fields(register, "name", name, "namespaceUri", ns, "aliasUris", ns + "alias#");
        mvc.perform(register.with(admin()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/external-vocabularies/" + name))
                .andExpect(jsonPath("$.aliasUris[0]").value(ns + "alias#"))
                .andExpect(jsonPath("$.description").doesNotExist());
        mvc.perform(register.with(admin())).andExpect(status().isConflict());
        mvc.perform(get("/v1/external-vocabularies/" + name).with(viewer())).andExpect(status().isOk());
        mvc.perform(get("/v1/external-vocabularies/" + name + "/content").with(viewer()))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + name + ".ttl\""));
        mvc.perform(post("/v1/external-vocabularies/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namespaceUri\":\"" + ns + "alias\"}")
                        .with(viewer()))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/external-vocabularies").with(viewer())).andExpect(status().isOk());
        mvc.perform(delete("/v1/external-vocabularies/" + name).with(admin())).andExpect(status().isNoContent());
        mvc.perform(get("/v1/external-vocabularies/" + name).with(viewer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("External vocabulary with name '" + name + "' was not found."));
        MockMultipartHttpServletRequestBuilder sameAlias = multipart("/v1/external-vocabularies");
        sameAlias.part(file("x"));
        fields(sameAlias, "name", unique("Dup"), "namespaceUri", ns, "aliasUris", ns);
        mvc.perform(sameAlias.with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.issues[0].name").value("aliasUris"));
    }

    @Test
    void undeclaredMethodsAndPaths() throws Exception {
        mvc.perform(put("/v1/vocabularies").with(admin()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET, POST"));
        mvc.perform(request(HttpMethod.OPTIONS, "/v1/vocabularies/Abc").with(admin()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(request(HttpMethod.HEAD, "/v1/health")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/v1/vocabularies/searches").with(admin())).andExpect(status().isMethodNotAllowed());
        // Methods outside the firewall's standard set (QUERY, invented ones) also give 405, not 400.
        mvc.perform(request(HttpMethod.valueOf("QUERY"), "/v1/vocabularies").with(admin()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET, POST"));
        mvc.perform(request(HttpMethod.valueOf("FOO"), "/v1/nowhere")).andExpect(status().isNotFound());
        mvc.perform(get("/v1/unknown").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
