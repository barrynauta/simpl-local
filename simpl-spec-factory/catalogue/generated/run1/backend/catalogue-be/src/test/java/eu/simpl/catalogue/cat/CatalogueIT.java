package eu.simpl.catalogue.cat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.common.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Publication pipeline, quality gate, search and lifecycle of catalogue-be against H2 and a mocked quality stub. */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogueIT {
    static final MockWebServer QUALITY = new MockWebServer();
    static final Map<String, Double> OVERALL = new ConcurrentHashMap<>();
    static final String PROFILE = ShaclAndQualityTest.PROFILE;

    static {
        QUALITY.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                String path = r.getPath();
                if (path.startsWith("/v1/profiles/active")) {
                    if (path.contains("NoProfileShape")) return new MockResponse().setResponseCode(404);
                    return new MockResponse().setHeader("Content-Type", "application/ld+json").setBody(PROFILE);
                }
                if (path.startsWith("/v1/score-validation")) {
                    JsonNode vc = Json.parse(r.getBody().readUtf8());
                    String id = vc.path("credentialSubject").path("@id").asText();
                    double overall = OVERALL.getOrDefault(id, 100.0);
                    String qar = """
                        {"@graph": [
                          {"@id": "%1$s", "@type": "simpl:DataOffering"},
                          {"@id": "urn:d:completeness", "@type": "QualityDimension", "name": "Completeness"},
                          {"@id": "s1", "@type": "SelfDescriptionScore", "computedOn": "%1$s", "score": %2$s, "maxScore": 100},
                          {"@id": "d1", "@type": "DimensionScore", "dimension": "urn:d:completeness", "score": 95, "maxScore": 100},
                          {"@id": "e1", "@type": "RuleEvaluation", "result": "passed"},
                          {"@id": "q1", "@type": "QualityAssessmentRecord", "computedOn": "%1$s"}]}""".formatted(id, overall);
                    return new MockResponse().setHeader("Content-Type", "application/ld+json").setBody(qar);
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        try {
            QUALITY.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:h2:mem:cat;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        r.add("spring.datasource.username", () -> "sa");
        r.add("spring.datasource.password", () -> "");
        r.add("simpl.cat.quality-stub-url", () -> "http://localhost:" + QUALITY.getPort());
        r.add("simpl.cat.seed-dirs", () -> "../../env/seed/schemas,../../env/seed/catalogue-only-schemas");
        r.add("simpl.cat.key-registry", () -> "../../env/keys/participant-keys-a.json");
    }

    @AfterAll
    static void stop() throws IOException {
        QUALITY.shutdown();
    }

    @Autowired
    MockMvc mvc;

    static RequestPostProcessor user(String participant, List<String> roles, List<String> attrs) {
        return jwt().jwt(j -> j.claim("iss", "http://localhost:18080/realms/dataspace-a")
                .claim("realm_access", Map.of("roles", roles))
                .claim("participant_id", participant)
                .claim("identity_attributes", attrs));
    }

    static final RequestPostProcessor PROVIDER = user("provider-a",
            List.of("DATA_PROVIDER_PUBLISHER", "APP_PROVIDER_PUBLISHER", "INFRA_PROVIDER_PUBLISHER", "CATALOGUE_SEARCHER"), List.of("PROVIDER"));
    static final RequestPostProcessor CONSUMER = user("consumer-a", List.of("CATALOGUE_SEARCHER"), List.of("CONSUMER", "RESEARCHER"));
    static final RequestPostProcessor NOBODY = user("nobody-a", List.of(), List.of("CONSUMER"));

    static String token() {
        return "t" + Long.toHexString(System.nanoTime()).substring(0, 10);
    }

    static String enc(String id) {
        return URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    JsonNode publish(JsonNode vc, int expected) throws Exception {
        String body = mvc.perform(post("/self-descriptions").with(PROVIDER).contentType(MediaType.APPLICATION_JSON).content(Json.write(vc)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return Json.parse(body);
    }

    @Test
    void publishSearchAndReadAnRd() throws Exception {
        String t = token();
        ObjectNode vc = TestVc.signed("provider-a", "provider-a", t, "CONSUMER");
        String id = TestVc.id(vc);
        mvc.perform(post("/self-descriptions/resource").with(PROVIDER).contentType(MediaType.APPLICATION_JSON).content(Json.write(vc)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/self-descriptions/" + enc(id)))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.resourceKey").value(id))
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andExpect(jsonPath("$.validatorDids").isEmpty())
                .andExpect(jsonPath("$.qualityAssessment.overall.score").value(100.0))
                .andExpect(jsonPath("$.qualityAssessment.overall.classification").value("Green"));

        mvc.perform(get(java.net.URI.create("/self-descriptions/" + enc(id))).with(CONSUMER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.credentialSubject['@id']").value(id));

        mvc.perform(get("/selfDescriptions/quickSearch").param("searchTerm", t).with(CONSUMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].n.claimsGraphUri[0]").value(id))
                .andExpect(jsonPath("$.items[0].n.name").value(t + " dataset"))
                .andExpect(jsonPath("$.items[0].n.serviceAccessPoint").value("https://provider.example.org/data"))
                .andExpect(jsonPath("$.items[0].n.overallScore").value(100.0));
        // The provider's own attributes (PROVIDER) do not satisfy the policy: owners get no exception (D30).
        mvc.perform(get("/selfDescriptions/quickSearch").param("searchTerm", t).with(PROVIDER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(0));
        // A User-Attributes header replaces the token attributes (D69).
        mvc.perform(get("/selfDescriptions/quickSearch").param("searchTerm", t).header("User-Attributes", "CONSUMER").with(PROVIDER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(1));

        String adv = "{\"simpl:GeneralServiceProperties\": {\"name\": \"" + t + "\"}, "
                + "\"simpl:OfferingPrice\": {\"price\": {\"op1\": \">=\", \"value1\": 50, \"op2\": \"<\", \"value2\": 150}}}";
        mvc.perform(post("/selfDescriptions/advancedSearch").with(CONSUMER).contentType(MediaType.APPLICATION_JSON).content(adv))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(1));
        mvc.perform(post("/selfDescriptions/advancedSearch").with(CONSUMER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"simpl:ContractTemplate\": {}}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"));

        mvc.perform(get("/selfDescriptions/quickSearch").param("searchTerm", t).param("minimumClassification", "Yellow").with(CONSUMER))
                .andExpect(jsonPath("$.totalCount").value(1));
        mvc.perform(get("/selfDescriptions/quickSearch").param("searchTerm", t).param("minimumClassification", "Purple").with(CONSUMER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("minimumClassification is not a supported quality classification value."));
    }

    @Test
    void pipelineErrorsInTheDigestOrder() throws Exception {
        String t = token();
        ObjectNode vc = TestVc.signed("provider-a", "provider-a", t, "CONSUMER");
        mvc.perform(post("/self-descriptions").contentType(MediaType.APPLICATION_JSON).content(Json.write(vc)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/self-descriptions").with(CONSUMER).contentType(MediaType.APPLICATION_JSON).content(Json.write(vc)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/self-descriptions").with(PROVIDER).contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("urn:problem-type:simpl:badRequest"));

        ObjectNode tampered = vc.deepCopy();
        tampered.put("issuanceDate", "2026-10-29T10:00:00Z");
        assertThat(publish(tampered, 422).path("type").asText()).isEqualTo("urn:problem-type:simpl:catalogue:invalidSignature");

        assertThat(publish(TestVc.signed("unregistered", "provider-a", t, "CONSUMER"), 403).path("type").asText())
                .isEqualTo("urn:problem-type:simpl:catalogue:providerMismatch");
        assertThat(publish(TestVc.signed("provider-a", "provider-b", t, "CONSUMER"), 403).path("type").asText())
                .isEqualTo("urn:problem-type:simpl:catalogue:providerMismatch");

        ObjectNode badSchema = TestVc.unsigned("provider-a", "provider-a", t, "CONSUMER");
        ((ObjectNode) badSchema.get("credentialSubject").get("dct:conformsTo")).put("dct:hasVersion", "7");
        assertThat(publish(TestVc.sign(badSchema, "provider-a"), 422).path("type").asText())
                .isEqualTo("urn:problem-type:simpl:catalogue:schemaNotSupported");

        ObjectNode invalid = TestVc.unsigned("provider-a", "provider-a", t, "CONSUMER");
        ((ObjectNode) invalid.get("credentialSubject").get("simpl:generalServiceProperties")).remove("simpl:name");
        JsonNode p = publish(TestVc.sign(invalid, "provider-a"), 422);
        assertThat(p.path("type").asText()).isEqualTo("urn:problem-type:simpl:catalogue:shaclViolation");
        assertThat(p.path("detail").asText()).contains("simpl:generalServiceProperties/simpl:name");

        publish(vc, 201);
        assertThat(publish(vc, 409).path("type").asText()).isEqualTo("urn:problem-type:simpl:catalogue:alreadyExists");
    }

    @Test
    void qualityGateRejectsBelowThresholdAndStoresNothing() throws Exception {
        String t = token();
        ObjectNode vc = TestVc.signed("provider-a", "provider-a", t, "CONSUMER");
        OVERALL.put(TestVc.id(vc), 65.0);
        JsonNode p = publish(vc, 422);
        assertThat(p.path("type").asText()).isEqualTo("urn:problem-type:simpl:catalogue:qualityBelowThreshold");
        assertThat(p.path("detail").asText()).isEqualTo("Quality Score 65 is below treshold 70.");
        mvc.perform(get(java.net.URI.create("/self-descriptions/" + enc(TestVc.id(vc)))).with(CONSUMER)).andExpect(status().isNotFound());

        ObjectNode noProfile = TestVc.unsigned("provider-a", "provider-a", t, "CONSUMER");
        ((ObjectNode) noProfile.get("credentialSubject").get("dct:conformsTo")).put("dct:schemaName", "NoProfileShape");
        assertThat(publish(TestVc.sign(noProfile, "provider-a"), 500).path("type").asText())
                .isEqualTo("urn:problem-type:simpl:catalogue:qualityConfigurationError");
    }

    @Test
    void newVersionRevokesThePreviousOneAndLifecycleIsOwnerOnly() throws Exception {
        String t = token();
        ObjectNode v1 = TestVc.signed("provider-a", "provider-a", t, "CONSUMER");
        JsonNode m1 = publish(v1, 201);
        ObjectNode v2 = TestVc.unsigned("provider-a", "provider-a", t, "CONSUMER");
        ((ObjectNode) v2.get("credentialSubject")).put("simpl:version", 2);
        ((ObjectNode) v2.get("credentialSubject")).putObject("simpl:previousVersion").put("@id", TestVc.id(v1));
        JsonNode m2 = publish(TestVc.sign(v2, "provider-a"), 201);
        assertThat(m2.path("resourceKey").asText()).isEqualTo(TestVc.id(v1));
        assertThat(m2.path("currentVersion").asInt()).isEqualTo(2);
        assertThat(publish(TestVc.sign(v2.deepCopy().put("issuanceDate", "2026-10-30T00:00:00Z"), "provider-a"), 409)
                .path("type").asText()).isIn("urn:problem-type:simpl:catalogue:alreadyExists");

        mvc.perform(get("/self-descriptions").param("ids", TestVc.id(v1)).param("statuses", "revoked").with(CONSUMER))
                .andExpect(jsonPath("$.totalCount").value(1)).andExpect(jsonPath("$.items[0].meta.status").value("revoked"));
        mvc.perform(get(java.net.URI.create("/self-descriptions/" + enc(TestVc.id(v2)) + "/versions")).with(CONSUMER))
                .andExpect(jsonPath("$.totalCount").value(1)).andExpect(jsonPath("$.items[0].n.version").value(1));
        mvc.perform(get("/selfDescriptions/quickSearch").param("searchTerm", t).with(CONSUMER))
                .andExpect(jsonPath("$.totalCount").value(1)).andExpect(jsonPath("$.items[0].n.claimsGraphUri[0]").value(TestVc.id(v2)));

        RequestPostProcessor other = user("provider-a2", List.of("DATA_PROVIDER_PUBLISHER"), List.of("PROVIDER"));
        mvc.perform(post(java.net.URI.create("/self-descriptions/" + enc(TestVc.id(v2)) + "/revoke")).with(other)).andExpect(status().isForbidden());
        mvc.perform(post(java.net.URI.create("/self-descriptions/" + enc(TestVc.id(v2)) + "/revoke")).with(PROVIDER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("revoked"));
        mvc.perform(post(java.net.URI.create("/self-descriptions/" + enc(TestVc.id(v2)) + "/revoke")).with(PROVIDER)).andExpect(status().isOk());
        mvc.perform(delete(java.net.URI.create("/self-descriptions/" + m1.path("sdHash").asText())).with(other)).andExpect(status().isForbidden());
        mvc.perform(delete(java.net.URI.create("/self-descriptions/" + m1.path("sdHash").asText())).with(PROVIDER)).andExpect(status().isOk());
        mvc.perform(delete(java.net.URI.create("/self-descriptions/" + m1.path("sdHash").asText())).with(PROVIDER)).andExpect(status().isNotFound());
    }

    @Test
    void verificationSchemasWebhookAndOutOfScope() throws Exception {
        ObjectNode vc = TestVc.signed("provider-a", "provider-a", token(), "CONSUMER");
        mvc.perform(post("/verification").with(NOBODY).contentType(MediaType.APPLICATION_JSON).content(Json.write(vc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleStatus").value("active"))
                .andExpect(jsonPath("$.issuer").value(TestVc.did("provider-a")))
                .andExpect(jsonPath("$.issuedDateTime").value("2026-10-28T10:00:00Z"));
        mvc.perform(get("/schemas").with(CONSUMER)).andExpect(status().isForbidden());
        RequestPostProcessor viewer = user("schema-viewer-a", List.of("GA_SCHEMA_VIEWER"), List.of());
        mvc.perform(get("/schemas").with(viewer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.shapes[?(@ == 'DataOfferingShape')]").exists());
        mvc.perform(post("/webhooks/schema-events").contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"e1\",\"eventType\":\"SchemaRevoked\",\"timestamp\":\"2026-10-28T10:00:00Z\","
                        + "\"data\":{\"schema\":{\"name\":\"UnknownLaterShape\",\"uri\":\"u\",\"title\":\"t\",\"status\":\"REVOKED\"}}}"))
                .andExpect(status().isOk());
        mvc.perform(post("/webhooks/schema-events").contentType(MediaType.APPLICATION_JSON).content("{\"eventId\":\"e1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/query")).andExpect(status().isNotImplemented());
        mvc.perform(post("/schemas/DataOfferingShape/recalcQualityScore").with(NOBODY).contentType(MediaType.APPLICATION_JSON)
                .content("{\"profileVersion\":\"1.0.0\"}")).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(post("/schemas/NoProfileShape/recalcQualityScore").with(NOBODY).contentType(MediaType.APPLICATION_JSON)
                .content("{\"profileVersion\":\"1.0.0\"}")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:catalogue:activeQualityProfileNotFound"));
    }
}
