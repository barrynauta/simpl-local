package eu.simpl.catalogue.qma;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** qma as Trusted GA (local, GA search, detail) and as Trustor (inbound federated calls), against mocked peers. */
@SpringBootTest
@AutoConfigureMockMvc
class QmaIT {
    static final MockWebServer CAT = new MockWebServer();
    static final MockWebServer REMOTE = new MockWebServer();
    static final List<RecordedRequest> CAT_CALLS = new CopyOnWriteArrayList<>();
    static final List<RecordedRequest> REMOTE_CALLS = new CopyOnWriteArrayList<>();
    static final String ITEM = "{\"n\":{\"claimsGraphUri\":[\"%s\"],\"name\":\"n\",\"overallScore\":%s,\"overallClassification\":%s}}";
    static final String VC = "{\"credentialSubject\":{\"@id\":\"rd-1\",\"rdf:type\":{\"@id\":\"simpl:DataOffering\"},"
            + "\"simpl:providerInformation\":{\"simpl:providedBy\":\"consumer-a\"},\"simpl:servicePolicy\":{\"simpl:access-policy\":"
            + "\"{\\\"permission\\\":[{\\\"action\\\":[\\\"http://simpl.eu/odrl/actions/search\\\"],\\\"assignee\\\":{\\\"uid\\\":\\\"%s\\\"}}]}\"}}}";

    static MockResponse json(int status, String body) {
        return new MockResponse().setResponseCode(status).setHeader("Content-Type",
                status >= 400 ? "application/problem+json" : "application/json").setBody(body);
    }

    static {
        CAT.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                CAT_CALLS.add(r);
                String p = r.getPath();
                if (p.startsWith("/selfDescriptions/")) {
                    return json(200, "{\"totalCount\":25,\"items\":[" + ITEM.formatted("rd-1", "87.5", "\"Green\"") + "]}");
                }
                if (p.startsWith("/self-descriptions?")) {
                    String assignee = p.contains("ids=rd-1") ? "CONSUMER" : "OTHER";
                    String content = Json.write(Json.parse(VC.formatted(assignee)));
                    String item = Json.write(Json.object().<com.fasterxml.jackson.databind.node.ObjectNode>set("meta", Json.parse(
                            "{\"id\":\"" + (p.contains("ids=rd-1") ? "rd-1" : "rd-2") + "\",\"status\":\"active\",\"qualityAssessment\":{\"overall\":{\"score\":90}}}"))
                            .put("content", content));
                    return json(200, "{\"totalCount\":1,\"items\":[" + item + "]}");
                }
                return json(404, "{\"type\":\"urn:problem-type:simpl:resourceNotFound\",\"status\":404}");
            }
        });
        REMOTE.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                REMOTE_CALLS.add(r);
                String p = r.getPath();
                if (p.startsWith("/token")) return json(200, "{\"access_token\":\"fed-token\",\"expires_in\":600}");
                if (p.startsWith("/b/v1/federated/selfDescriptions")) {
                    return json(200, "{\"totalCount\":1,\"items\":[" + ITEM.formatted("rd-b", "null", "null")
                            + "],\"page\":1,\"pageSize\":20,\"total\":1,\"totalPages\":1,\"next\":null}");
                }
                if (p.startsWith("/b/v1/federated/resourceDescriptions/")) {
                    return json(200, "{\"resourceDescription\":{\"credentialSubject\":{\"@id\":\"rd-b\"}}}");
                }
                if (p.startsWith("/c/")) return json(403, "{\"type\":\"urn:problem-type:simpl:forbidden\",\"status\":403}");
                return json(404, "{}");
            }
        });
        try {
            CAT.start();
            REMOTE.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws IOException {
        String remote = "http://localhost:" + REMOTE.getPort();
        String cfg = """
            {"gaId": "ga-a", "ownCatalogue": {"id": "catalogue-a", "name": "A"},
             "outboundClient": {"tokenUrl": "%1$s/token", "clientId": "qma-a-federation", "clientSecret": "s"},
             "serviceClient": {"tokenUrl": "%1$s/token", "clientId": "svc", "clientSecret": "s"},
             "federatedCatalogues": [
               {"id": "catalogue-b", "name": "B", "gaId": "ga-b", "authorityURL": "%1$s/b/v1", "active": true},
               {"id": "catalogue-c", "name": "C", "gaId": "ga-c", "authorityURL": "%1$s/c/v1", "active": true},
               {"id": "catalogue-d", "name": "D", "gaId": "ga-d", "authorityURL": "%1$s/d/v1", "active": false}],
             "trustedAuthorities": [{"gaId": "ga-x", "issuer": "http://localhost:18080/realms/dataspace-x", "clientId": "qma-x-federation",
               "attributeMapping": [{"remoteAuthorityAttribute": "CONSUMER", "localAuthorityAttribute": "PARTNER_CONSUMER"}],
               "additionalIdentityAttributes": ["FEDERATED"]}]}""".formatted(remote);
        Path f = Files.createTempFile("federation", ".json");
        Files.writeString(f, cfg);
        r.add("simpl.qma.federation-file", f::toString);
        r.add("simpl.qma.cat-url", () -> "http://localhost:" + CAT.getPort());
    }

    @AfterAll
    static void stop() throws IOException {
        CAT.shutdown();
        REMOTE.shutdown();
    }

    @BeforeEach
    void clear() {
        CAT_CALLS.clear();
        REMOTE_CALLS.clear();
    }

    @Autowired
    MockMvc mvc;

    static RequestPostProcessor token(String iss, String azp, List<String> roles, List<String> attrs) {
        return jwt().jwt(j -> j.tokenValue("user-token").claim("iss", iss).claim("azp", azp)
                .claim("realm_access", Map.of("roles", roles)).claim("participant_id", "consumer-a").claim("identity_attributes", attrs));
    }

    static final RequestPostProcessor CONSUMER = token("http://localhost:18080/realms/dataspace-a", "test-client",
            List.of("CATALOGUE_SEARCHER"), List.of("CONSUMER", "RESEARCHER"));
    static final RequestPostProcessor TRUSTED_GA = token("http://localhost:18080/realms/dataspace-x", "qma-x-federation", List.of(), List.of());

    @Test
    void localQuickSearchForwardsTheCallerAttributesAndPages() throws Exception {
        mvc.perform(get("/v1/selfDescriptions").param("searchString", "climate data").param("pageSize", "10").with(CONSUMER)
                        .header("User-Attributes", "IGNORED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(25))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.next").value("/selfDescriptions?searchString=climate%20data&page=2&pageSize=10"));
        RecordedRequest r = CAT_CALLS.get(0);
        assertThat(r.getPath()).startsWith("/selfDescriptions/quickSearch?searchTerm=climate%20data");
        assertThat(r.getHeader("User-Attributes")).isEqualTo("CONSUMER,RESEARCHER");
        assertThat(r.getHeader("Authorization")).isEqualTo("Bearer user-token");

        mvc.perform(get("/v1/selfDescriptions").param("searchString", "a<b").with(CONSUMER)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/selfDescriptions").param("searchString", "x").with(CONSUMER).header("Accept", "text/html"))
                .andExpect(status().isNotAcceptable());
        mvc.perform(get("/v1/selfDescriptions").param("searchString", "x")).andExpect(status().isUnauthorized());
    }

    @Test
    void localAdvancedSearchOnlyWhenTheOwnCatalogueIsSelected() throws Exception {
        mvc.perform(post("/v1/selfDescriptions/advancedSearch").with(CONSUMER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogueIds\": [\"other\"], \"simpl:GeneralServiceProperties\": {\"name\": \"x\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        assertThat(CAT_CALLS).isEmpty();
        mvc.perform(post("/v1/selfDescriptions/advancedSearch").with(CONSUMER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogueIds\": [\"catalogue-a\"], \"minimumScore\": 10, \"simpl:GeneralServiceProperties\": {\"name\": \"x\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(25));
        JsonNode sent = Json.parse(CAT_CALLS.get(0).getBody().readUtf8());
        assertThat(sent.has("catalogueIds")).isFalse();
        assertThat(sent.path("minimumScore").asInt()).isEqualTo(10);
        mvc.perform(post("/v1/selfDescriptions/advancedSearch").with(CONSUMER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"simpl:GeneralServiceProperties\": {\"name\": \"x\"}}")).andExpect(status().isBadRequest());
        mvc.perform(post("/v1/selfDescriptions/advancedSearch").with(CONSUMER).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void governanceAuthoritySearchForwardsToTrustorsAndTranslatesErrors() throws Exception {
        mvc.perform(get("/v1/governanceAuthority/selfDescriptions").param("catalogueIds", "catalogue-b").param("searchString", "x").with(CONSUMER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].n.claimsGraphUri[0]").value("rd-b"))
                .andExpect(jsonPath("$.items[0].n.overallScore").isEmpty());
        RecordedRequest fwd = REMOTE_CALLS.stream().filter(r -> r.getPath().startsWith("/b/")).findFirst().orElseThrow();
        assertThat(fwd.getHeader("Authorization")).isEqualTo("Bearer fed-token");
        assertThat(fwd.getHeader("User-Attributes")).isEqualTo("CONSUMER,RESEARCHER");
        assertThat(fwd.getHeader("Correlation-Id")).matches("[0-9a-f-]{36}");
        assertThat(fwd.getPath()).contains("catalogueIds=catalogue-b");

        mvc.perform(get("/v1/governanceAuthority/selfDescriptions").param("catalogueIds", "catalogue-c").param("searchString", "x").with(CONSUMER))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:catalogue:federationUnauthenticated"));
        mvc.perform(get("/v1/governanceAuthority/selfDescriptions").param("catalogueIds", "catalogue-d").param("searchString", "x").with(CONSUMER))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:catalogue:federationUnavailable"));
        mvc.perform(get("/v1/governanceAuthority/selfDescriptions").param("catalogueIds", "nope").param("searchString", "x").with(CONSUMER))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/governanceAuthority/selfDescriptions").param("searchString", "x").with(CONSUMER))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/governanceAuthority/selfDescriptions").param("catalogueIds", "local", "catalogue-c")
                        .param("searchString", "x").with(CONSUMER))
                .andExpect(status().isOk())
                .andExpect(header().string("Simpl-Failed-Catalogues", "catalogue-c"))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.items[0].n.overallScore").value(87.5));
    }

    @Test
    void trustorSideChecksTrustMapsAttributesAndEchoesTheCorrelationId() throws Exception {
        mvc.perform(get("/v1/federated/selfDescriptions").param("catalogueIds", "catalogue-a").param("searchString", "x")
                        .header("User-Attributes", "CONSUMER,RESEARCHER").header("Correlation-Id", "cid-1").with(TRUSTED_GA))
                .andExpect(status().isOk())
                .andExpect(header().string("Correlation-Id", "cid-1"))
                .andExpect(jsonPath("$.items[0].n.overallScore").isEmpty());
        RecordedRequest r = CAT_CALLS.get(0);
        assertThat(r.getHeader("User-Attributes")).isEqualTo("PARTNER_CONSUMER,FEDERATED");
        assertThat(r.getHeader("Authorization")).isEqualTo("Bearer fed-token");

        mvc.perform(get("/v1/federated/selfDescriptions").param("catalogueIds", "catalogue-a").param("searchString", "x")
                        .header("User-Attributes", "CONSUMER").header("Correlation-Id", "cid-2").with(CONSUMER))
                .andExpect(status().isForbidden()).andExpect(header().string("Correlation-Id", "cid-2"));
        mvc.perform(get("/v1/federated/selfDescriptions").param("catalogueIds", "catalogue-a").param("searchString", "x")
                        .header("Correlation-Id", "cid-3").with(TRUSTED_GA))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/federated/selfDescriptions").param("catalogueIds", "catalogue-z").param("searchString", "x")
                        .header("User-Attributes", "").header("Correlation-Id", "cid-4").with(TRUSTED_GA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/v1/federated/resourceDescriptions/rd-2").header("User-Attributes", "CONSUMER")
                        .header("Correlation-Id", "cid-5").with(TRUSTED_GA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:catalogue:discoveryPolicyDenied"));
    }

    @Test
    void participantOperationsAreOwnerOnly() throws Exception {
        RequestPostProcessor owner = token("http://localhost:18080/realms/dataspace-a", "test-client",
                List.of("DATA_PROVIDER_PUBLISHER"), List.of("PROVIDER"));
        mvc.perform(get("/v1/participants/resourceDescriptions").with(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].n.claimsGraphUri[0]").value("rd-1"));
        assertThat(CAT_CALLS.get(0).getPath()).contains("providedBy=consumer-a").contains("statuses=active%2Crevoked");
        mvc.perform(get("/v1/participants/resourceDescriptions").param("orderBy", "name").with(owner)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/participants/resourceDescriptions/rd-1").with(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.credentialSubject['@id']").value("rd-1"));
        mvc.perform(get("/v1/participants/resourceDescriptions/rd-1").with(CONSUMER)).andExpect(status().isForbidden());
    }

    @Test
    void detailThroughTheGovernanceAuthority() throws Exception {
        mvc.perform(get("/v1/governanceAuthority/resourceDescriptions/rd-1").param("catalogueId", "local").with(CONSUMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogueId").value("catalogue-a"))
                .andExpect(jsonPath("$.resourceDescription.credentialSubject['@id']").value("rd-1"))
                .andExpect(jsonPath("$.qualityAssessment.overall.score").value(90));
        mvc.perform(get("/v1/governanceAuthority/resourceDescriptions/rd-2").param("catalogueId", "catalogue-a").with(CONSUMER))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/governanceAuthority/resourceDescriptions/rd-b").param("catalogueId", "catalogue-b").with(CONSUMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogueId").value("catalogue-b"))
                .andExpect(jsonPath("$.qualityAssessment").doesNotExist());
        mvc.perform(get("/v1/governanceAuthority/resourceDescriptions/rd-b").param("catalogueId", "bad id").with(CONSUMER))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/governanceAuthority/resourceDescriptions/rd-b").param("catalogueId", "catalogue-c").with(CONSUMER))
                .andExpect(status().isBadGateway());
    }
}
