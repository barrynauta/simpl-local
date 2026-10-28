package eu.simpl.schemamanager.api;

import static eu.simpl.schemamanager.Fixtures.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import eu.simpl.schemamanager.Fixtures;
import eu.simpl.schemamanager.validation.VocabularyClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import eu.simpl.schemamanager.security.SecurityConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests against a real PostgreSQL (Testcontainers), with mocked JWTs,
 * a mocked Vocabulary Manager client and a mocked mail sender.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    VocabularyClient vocabularyClient;

    @MockitoBean
    JavaMailSender mailSender;

    @BeforeEach
    void resetMocks() {
        reset(mailSender, vocabularyClient);
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    static RequestPostProcessor as(String user, String... roles) {
        return jwt().jwt(j -> j.claim("preferred_username", user).claim("roles", List.of(roles)))
                .authorities(SecurityConfig::roles);
    }

    static RequestPostProcessor admin() {
        return as("ga-admin", "GA_SCHEMA_ADMIN");
    }

    static RequestPostProcessor viewer() {
        return as("ga-viewer", "GA_SCHEMA_VIEWER");
    }

    static RequestPostProcessor norole() {
        return as("ga-norole");
    }

    static MockMultipartHttpServletRequestBuilder createReq(String name, String type, String resourceType, String ttl) {
        MockMultipartHttpServletRequestBuilder b = multipart("/schemas")
                .file(new MockMultipartFile("schemaFile", "s.ttl", "text/turtle", bytes(ttl)));
        b.param("name", name).param("title", name + " title").param("description", "Desc of " + name);
        if (type != null) {
            b.param("schemaType", type);
        }
        if (resourceType != null) {
            b.param("resourceType", resourceType);
        }
        return b;
    }

    static MockMultipartHttpServletRequestBuilder versionReq(String name, String type, String changelog, String ttl) {
        MockMultipartHttpServletRequestBuilder b = multipart("/schemas/{n}/versions", name);
        b.file(new MockMultipartFile("schemaFile", "s.ttl", "text/turtle", bytes(ttl)));
        if (changelog != null) {
            b.param("changelog", changelog);
        }
        if (type != null) {
            b.param("schemaType", type);
        }
        return b;
    }

    JsonNode body(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsByteArray());
    }

    // -------------------------------------------------------------- access (D3, D5, D7)

    @Test
    void missingTokenGives401ProblemJson() throws Exception {
        mvc.perform(get("/schemas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/webhooks")).andExpect(status().isUnauthorized());
    }

    @Test
    void roleMatrix() throws Exception {
        mvc.perform(get("/schemas").with(viewer())).andExpect(status().isOk());
        mvc.perform(get("/schemas").with(norole()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:forbidden"));
        // A viewer sending a malformed create gets 403, not 400 (D5).
        mvc.perform(multipart("/schemas").with(viewer())).andExpect(status().isForbidden());
        mvc.perform(patch("/schemas/Abc").with(viewer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVOKED\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/webhooks").with(norole())).andExpect(status().isOk());
    }

    // -------------------------------------------------------------- main flow

    @Test
    void createVersionListAndRetrieve() throws Exception {
        String name = unique("T");
        String rt = unique("Rt");
        mvc.perform(createReq(name, null, rt, Fixtures.VALID).with(admin()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/schemas/" + name)))
                .andExpect(jsonPath("$.schemaName").value(name))
                .andExpect(jsonPath("$.schemaVersion").value("1"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.validationWarnings").isArray());

        String v2 = Fixtures.VALID + "\n# version two\n";
        mvc.perform(versionReq(name, null, "Second", v2).param("version", "7.0.0").with(admin()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/schemas/" + name + "/2")))
                .andExpect(jsonPath("$.version").value("2"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.changelog").value("Second"));

        mvc.perform(get("/schemas").param("resourceType", rt).with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.metadata[0].name").value(name))
                .andExpect(jsonPath("$.metadata[0].version").value("2"))
                .andExpect(jsonPath("$.metadata[0].schemaType").value("RESOURCE"))
                .andExpect(jsonPath("$.metadata[0].createdBy").value("ga-admin"));

        MvcResult latest = mvc.perform(get("/schemas/{n}", name).with(viewer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/turtle"))
                .andExpect(header().string("Content-Location", endsWith("/schemas/" + name + "/2")))
                .andReturn();
        assertThat(latest.getResponse().getContentAsByteArray()).isEqualTo(bytes(v2));

        MvcResult first = mvc.perform(get("/schemas/{n}/1", name).with(viewer()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Location", endsWith("/schemas/" + name + "/1")))
                .andReturn();
        assertThat(first.getResponse().getContentAsString(StandardCharsets.UTF_8)).isEqualTo(Fixtures.VALID);

        mvc.perform(get("/schemas/{n}/1", name).accept(MediaType.APPLICATION_JSON).with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1"))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.changelog").value("Initial version."));

        mvc.perform(get("/schemas/{n}/versions", name).with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata[0].version").value("1"))
                .andExpect(jsonPath("$.metadata[1].version").value("2"))
                .andExpect(jsonPath("$.metadata[1].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.metadata[1].graphName", endsWith("/schemas/" + name + "/2")));
        mvc.perform(get("/schemas/{n}/versions", name).param("page", "2").with(viewer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/schemas/{n}/9.9.9", name).with(viewer())).andExpect(status().isNotFound());
        mvc.perform(get("/schemas/{n}/3", name).with(viewer())).andExpect(status().isNotFound());
        mvc.perform(get("/schemas/{n}", name).param("schemaType", "SEARCH").with(viewer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/schemas/{n}", name).param("resourceType", "Other").with(viewer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void paginationEnvelope() throws Exception {
        String rt = unique("Rt");
        for (int i = 0; i < 3; i++) {
            mvc.perform(createReq(unique("T"), null, rt, Fixtures.VALID).with(admin())).andExpect(status().isCreated());
        }
        mvc.perform(get("/schemas").param("resourceType", rt).param("pageSize", "2").with(viewer()))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.metadata.length()").value(2))
                .andExpect(jsonPath("$.next", startsWith("/schemas?")));
        mvc.perform(get("/schemas").param("resourceType", rt).param("pageSize", "2").param("page", "2").with(viewer()))
                .andExpect(jsonPath("$.metadata.length()").value(1))
                .andExpect(jsonPath("$.next").doesNotExist());
        mvc.perform(get("/schemas").param("resourceType", rt).param("page", "5").with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.length()").value(0));
        mvc.perform(get("/schemas").param("pageSize", "101").with(viewer())).andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- slot rule (D16, D18)

    @Test
    void slotRuleForResource() throws Exception {
        String rt = unique("Rt");
        String a = unique("T");
        String b = unique("T");
        mvc.perform(createReq(a, null, rt, Fixtures.VALID).with(admin())).andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(createReq(b, null, rt, Fixtures.VALID).with(admin()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REVOKED"));

        mvc.perform(patch("/schemas/{n}", b).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Another schema is in PUBLISHED state for the resource type."));
        mvc.perform(patch("/schemas/{n}", a).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVOKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.name").value(a));
        mvc.perform(patch("/schemas/{n}", a).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVOKED\"}")).andExpect(status().isConflict());
        mvc.perform(patch("/schemas/{n}", b).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\"}")).andExpect(status().isOk());
        // A new version of a revoked concept whose slot is taken stays REVOKED.
        mvc.perform(versionReq(a, null, "v2", Fixtures.VALID).with(admin()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REVOKED"));
        mvc.perform(patch("/schemas/{n}", b).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ARCHIVED\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/schemas/{n}", b).with(admin()).contentType(MediaType.TEXT_PLAIN)
                .content("{\"status\":\"REVOKED\"}")).andExpect(status().isUnsupportedMediaType());
        mvc.perform(patch("/schemas/{n}", unique("T")).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVOKED\"}")).andExpect(status().isNotFound());
        mvc.perform(patch("/schemas/{n}", b).with(admin()).accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REVOKED\"}"))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void searchSchemasHaveOneGlobalSlot() throws Exception {
        String s1 = unique("T");
        String s2 = unique("T");
        mvc.perform(createReq(s1, "SEARCH", "Data", Fixtures.VALID).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(createReq(s1, "SEARCH", null, Fixtures.NO_VERSION).with(admin()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/schemas/" + s1 + "?schemaType=SEARCH")))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.resourceType").isEmpty());
        mvc.perform(createReq(s2, "SEARCH", null, Fixtures.VALID).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A published federated search schema already exists."));
        mvc.perform(createReq(s1, "SEARCH", null, Fixtures.VALID).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A schema with that name already exists."));
        // Same name may exist once as RESOURCE (D11).
        mvc.perform(createReq(s1, null, unique("Rt"), Fixtures.VALID).with(admin())).andExpect(status().isCreated());
        mvc.perform(get("/schemas").param("schemaType", "SEARCH").param("resourceType", "Data").with(viewer()))
                .andExpect(status().isBadRequest());
        mvc.perform(versionReq(s1, "SEARCH", "next", Fixtures.VALID).with(admin()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/schemas/" + s1 + "/2?schemaType=SEARCH")));
        mvc.perform(patch("/schemas/{n}", s1).param("schemaType", "SEARCH").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REVOKED\"}")).andExpect(status().isOk());
        mvc.perform(get("/schemas").param("schemaType", "SEARCH").param("status", "PUBLISHED").with(viewer()))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(createReq(s2, "SEARCH", null, Fixtures.VALID).with(admin()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(patch("/schemas/{n}", s1).param("schemaType", "SEARCH").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A published federated search schema already exists."));
        // No provider e-mail for SEARCH (D47). Only the RESOURCE twin of s1 may have mailed.
        Thread.sleep(1500);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.atMost(1)).send(captor.capture());
        assertThat(captor.getAllValues()).noneMatch(m -> m.getSubject().contains(s2));
    }

    // -------------------------------------------------------------- validation (D12, D20, D30 to D36)

    @Test
    void createValidation() throws Exception {
        String rt = unique("Rt");
        mvc.perform(createReq("lowerCase", null, rt, Fixtures.VALID).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(createReq(unique("T"), null, null, Fixtures.VALID).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(createReq(unique("T"), null, "data", Fixtures.VALID).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(createReq(unique("T"), "OTHER", rt, Fixtures.VALID).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/schemas").param("name", unique("T")).param("title", "x").param("description", "y")
                .param("resourceType", rt).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(post("/schemas").contentType(MediaType.APPLICATION_JSON).content("{}").with(admin()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unsupportedMediaType"));
        mvc.perform(createReq(unique("T"), null, rt, Fixtures.BROKEN).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", startsWith("Syntax Error: ")));
        mvc.perform(createReq(unique("T"), null, rt, Fixtures.NO_VERSION).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", startsWith("Default Properties Error: ")))
                .andExpect(jsonPath("$.issues[0].code").value("missingDefaultProperty"));
        when(vocabularyClient.fetch("Vmissing")).thenReturn(Optional.empty());
        mvc.perform(createReq(unique("T"), null, rt,
                        Fixtures.withLocal("https://vocabulary.simpl.test/content/", "Vmissing", "x")).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", startsWith("Semantic Validation Error: ")))
                .andExpect(jsonPath("$.issues[0].code").value("localReferenceUnavailable"));
        mvc.perform(get("/schemas").param("resourceType", rt).with(viewer())).andExpect(jsonPath("$.total").value(0));
        mvc.perform(createReq(unique("T"), null, rt, Fixtures.VALID).accept(MediaType.TEXT_HTML).with(admin()))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void newVersionValidation() throws Exception {
        String name = unique("T");
        String rt = unique("Rt");
        mvc.perform(createReq(name, null, rt, Fixtures.VALID).with(admin())).andExpect(status().isCreated());
        mvc.perform(versionReq(name, null, null, Fixtures.VALID).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(versionReq(name, "SEARCH", "x", Fixtures.VALID).with(admin())).andExpect(status().isNotFound());
        mvc.perform(versionReq(unique("T"), null, "x", Fixtures.VALID).with(admin())).andExpect(status().isNotFound());
        mvc.perform(versionReq("bad$name", null, "x", Fixtures.VALID).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(versionReq(name, null, "x", Fixtures.VALID).param("resourceType", "Different").with(admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(versionReq(name, null, "x", Fixtures.BROKEN).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", startsWith("Syntax Error: ")));
        mvc.perform(get("/schemas/{n}/versions", name).with(viewer())).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/schemas/{n}/versions", "Bad$").with(viewer())).andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- notifications (D39 to D44)

    @Test
    void providerMailOnPublishAndRevoke() throws Exception {
        String name = unique("T");
        String rt = unique("Rt");
        mvc.perform(createReq(name, null, rt, Fixtures.VALID).with(admin())).andExpect(status().isCreated());
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getSubject()).isEqualTo("Schema published: " + name + " (version 1)");
        assertThat(msg.getText()).contains("Name: " + name, "Resource type: " + rt, "Version: 1",
                "Change description: Initial version.", "Timestamp: ", "Instructions: ");

        reset(mailSender);
        mvc.perform(patch("/schemas/{n}", name).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVOKED\"}")).andExpect(status().isOk());
        verify(mailSender, timeout(5000)).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Schema revoked: " + name);

        reset(mailSender);
        mvc.perform(versionReq(name, null, "Change", Fixtures.VALID).with(admin()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        verify(mailSender, timeout(5000)).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Schema new version published: " + name + " (version 2)");

        reset(mailSender);
        mvc.perform(versionReq(name, null, "x", Fixtures.BROKEN).with(admin())).andExpect(status().isBadRequest());
        verify(mailSender, after(1000).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void webhookSubscriptionsAndDelivery() throws Exception {
        BlockingQueue<String[]> received = new LinkedBlockingQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            received.add(new String[] {ex.getRequestHeaders().getFirst("X-Simpl-Event"),
                    new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)});
            ex.sendResponseHeaders(204, -1);
            ex.close();
        });
        server.start();
        try {
            String target = "http://127.0.0.1:" + server.getAddress().getPort() + "/hooks/" + UUID.randomUUID();
            String req = "{\"targetUrl\":\"" + target + "\",\"events\":[\"SchemaPublished\",\"SchemaRevoked\"]}";
            MvcResult created = mvc.perform(post("/webhooks").with(norole()).contentType(MediaType.APPLICATION_JSON)
                            .content(req))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.webhookId").isNotEmpty())
                    .andReturn();
            String id = body(created).get("webhookId").asText();
            mvc.perform(post("/webhooks").with(norole()).contentType(MediaType.APPLICATION_JSON).content(req))
                    .andExpect(status().isConflict());
            mvc.perform(post("/webhooks").with(norole()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"targetUrl\":\"ftp://invalid.com\",\"events\":[\"SchemaPublished\"]}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/webhooks").with(norole()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"targetUrl\":\"https://a.example/x\",\"events\":[]}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/webhooks").with(norole()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetUrl\":\"https://a.example/x\",\"events\":[\"SchemaExploded\"]}"))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(jsonPath("$.detail").value(
                            "'SchemaExploded' is not a valid event type. Valid event types are: SchemaPublished, SchemaRevoked"));
            mvc.perform(get("/webhooks").with(viewer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.webhookId == '" + id + "')]").exists());

            String name = unique("T");
            String rt = unique("Rt");
            mvc.perform(createReq(name, null, rt, Fixtures.VALID).with(admin())).andExpect(status().isCreated());
            String[] first = received.poll(10, TimeUnit.SECONDS);
            assertThat(first).isNotNull();
            assertThat(first[0]).isEqualTo("SchemaPublished");
            JsonNode payload = mapper.readTree(first[1]);
            assertThat(payload.get("schemaName").asText()).isEqualTo(name);
            assertThat(payload.get("version").asText()).isEqualTo("1");
            assertThat(payload.get("status").asText()).isEqualTo("PUBLISHED");
            assertThat(payload.get("schemaUri").asText()).endsWith("/schemas/" + name + "/1");

            mvc.perform(patch("/schemas/{n}", name).with(admin()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"REVOKED\"}")).andExpect(status().isOk());
            String[] second = received.poll(10, TimeUnit.SECONDS);
            assertThat(second).isNotNull();
            assertThat(second[0]).isEqualTo("SchemaRevoked");

            mvc.perform(delete("/webhooks/{id}", id).with(norole())).andExpect(status().isNoContent());
            mvc.perform(delete("/webhooks/{id}", id).with(norole())).andExpect(status().isNotFound());
            mvc.perform(delete("/webhooks/{id}", "wh_999").with(norole())).andExpect(status().isNotFound());
        } finally {
            server.stop(0);
        }
        verify(mailSender, timeout(5000).atLeastOnce()).send(any(SimpleMailMessage.class));
    }

    @Test
    void outOfScopePathsAnswer404() throws Exception {
        mvc.perform(get("/vocabularies").with(admin())).andExpect(status().isNotFound());
        mvc.perform(patch("/metadataQualityProfiles/AbcProfile").with(admin())).andExpect(status().isNotFound());
        mvc.perform(get("/schemas/Abc/qualityProfiles/active").with(admin())).andExpect(status().isNotFound());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
