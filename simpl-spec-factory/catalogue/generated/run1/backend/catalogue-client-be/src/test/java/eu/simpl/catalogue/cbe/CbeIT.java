package eu.simpl.catalogue.cbe;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Grouped search, partial failure, detail flattening and the schema proxies of cbe, with qma and stubs mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class CbeIT {
    static final MockWebServer DOWN = new MockWebServer();
    static final List<RecordedRequest> CALLS = new CopyOnWriteArrayList<>();
    static final String PAGE = "{\"totalCount\":%1$d,\"items\":[{\"n\":{\"claimsGraphUri\":[\"%2$s\"],\"name\":\"n\",\"offeringType\":\"data\","
            + "\"overallScore\":%3$s,\"overallClassification\":%4$s}}],\"page\":1,\"pageSize\":20,\"total\":%1$d,\"totalPages\":%5$d,\"next\":null}";

    static MockResponse json(int status, String body) {
        return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body);
    }

    static {
        DOWN.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                CALLS.add(r);
                String p = r.getPath();
                if (p.startsWith("/qma/selfDescriptions?")) return json(200, PAGE.formatted(41, "rd-a", "87.5", "\"Green\"", 3));
                if (p.startsWith("/qma/selfDescriptions/advancedSearch")) return json(200, PAGE.formatted(1, "rd-a", "87.5", "\"Green\"", 1));
                if (p.contains("catalogueIds=catalogue-b") || (p.startsWith("/qma/governanceAuthority/selfDescriptions/advancedSearch"))) {
                    return json(200, PAGE.formatted(1, "rd-b", "null", "null", 1));
                }
                if (p.contains("catalogueIds=catalogue-c")) {
                    return json(502, "{\"type\":\"urn:problem-type:simpl:catalogue:federationUnauthenticated\",\"status\":502,\"detail\":\"no\"}");
                }
                if (p.contains("catalogueIds=catalogue-s")) {
                    return json(504, "{\"type\":\"urn:problem-type:simpl:catalogue:federationTimeout\",\"status\":504,\"detail\":\"slow\"}");
                }
                if (p.startsWith("/qma/governanceAuthority/resourceDescriptions/rd-a")) {
                    return json(200, "{\"catalogueId\":\"catalogue-a\",\"resourceDescription\":{\"credentialSubject\":{\"@id\":\"rd-a\"}},"
                            + "\"qualityAssessment\":{\"overall\":{\"score\":87.5}}}");
                }
                if (p.startsWith("/qma/governanceAuthority/resourceDescriptions/")) {
                    return json(404, "{\"type\":\"urn:problem-type:simpl:notFound\",\"status\":404,\"title\":\"Not Found\"}");
                }
                if (p.startsWith("/quality/v1/profiles/active")) {
                    return json(200, "{\"@graph\":[{\"@type\":\"ScoreRange\",\"minScore\":0,\"maxScore\":60,\"rangeClassification\":{\"@id\":\"urn:c:Red\"}},"
                            + "{\"@type\":\"ScoreRange\",\"minScore\":60,\"maxScore\":100,\"rangeClassification\":{\"@id\":\"urn:c:Green\"}}]}");
                }
                if (p.startsWith("/schema/schemas")) return json(200, "{\"schemas\":[{\"schemaId\":\"FederatedSearchShape\",\"schemaType\":\"SEARCH\"}]}");
                return json(404, "{}");
            }
        });
        try {
            DOWN.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws IOException {
        String base = "http://localhost:" + DOWN.getPort();
        Path f = Files.createTempFile("federation", ".json");
        Files.writeString(f, """
            {"gaId": "ga-a", "ownCatalogue": {"id": "catalogue-a", "name": "Data space A catalogue"},
             "federatedCatalogues": [
               {"id": "catalogue-b", "name": "B", "gaId": "ga-b", "authorityURL": "http://b/v1", "active": true},
               {"id": "catalogue-c", "name": "C", "gaId": "ga-c", "authorityURL": "http://c/v1", "active": true},
               {"id": "catalogue-s", "name": "S", "gaId": "ga-s", "authorityURL": "http://s/v1", "active": true}]}""");
        r.add("simpl.cbe.federation-file", f::toString);
        r.add("simpl.cbe.qma-url", () -> base + "/qma");
        r.add("simpl.cbe.cat-url", () -> base + "/cat");
        r.add("simpl.cbe.schema-stub-url", () -> base + "/schema");
        r.add("simpl.cbe.quality-stub-url", () -> base + "/quality");
    }

    @AfterAll
    static void stop() throws IOException {
        DOWN.shutdown();
    }

    @Autowired
    MockMvc mvc;

    static RequestPostProcessor user(List<String> roles) {
        return jwt().jwt(j -> j.claim("iss", "http://localhost:18080/realms/dataspace-a").claim("realm_access", Map.of("roles", roles)));
    }

    static final RequestPostProcessor CONSUMER = user(List.of("CATALOGUE_SEARCHER"));

    @Test
    void groupedSearchKeepsCataloguesApartAndReportsFailedOnes() throws Exception {
        mvc.perform(get("/v1/selfDescriptions").param("q", "climate").param("catalogueIds", "catalogue-c", "local", "catalogue-b", "catalogue-s")
                        .param("minimumScore", "50").with(CONSUMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(42))
                .andExpect(jsonPath("$.groups[0].catalogueId").value("catalogue-a"))
                .andExpect(jsonPath("$.groups[0].local").value(true))
                .andExpect(jsonPath("$.groups[0].catalogueName").value("Data space A catalogue"))
                .andExpect(jsonPath("$.groups[0].governanceAuthorityId").value("ga-a"))
                .andExpect(jsonPath("$.groups[0].totalPages").value(3))
                .andExpect(jsonPath("$.groups[0].items[0].i.qualityScore").value(87.5))
                .andExpect(jsonPath("$.groups[0].items[0].i.catalogueId").value("catalogue-a"))
                .andExpect(jsonPath("$.groups[1].catalogueId").value("catalogue-c"))
                .andExpect(jsonPath("$.groups[1].status").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.groups[1].items").isEmpty())
                .andExpect(jsonPath("$.groups[1].error.type").value("urn:problem-type:simpl:catalogue:federationUnauthenticated"))
                .andExpect(jsonPath("$.groups[2].catalogueId").value("catalogue-b"))
                .andExpect(jsonPath("$.groups[2].status").value("OK"))
                .andExpect(jsonPath("$.groups[2].items[0].i.qualityScore").isEmpty())
                .andExpect(jsonPath("$.groups[3].status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.items.length()").value(2));
        assertThat(CALLS.stream().map(RecordedRequest::getPath).filter(p -> p.startsWith("/qma/selfDescriptions?")).findFirst().orElseThrow())
                .contains("minimumScore=50");
        assertThat(CALLS.stream().map(RecordedRequest::getPath).filter(p -> p.contains("catalogueIds=catalogue-b")).findFirst().orElseThrow())
                .doesNotContain("minimumScore");
    }

    @Test
    void validationAndRoles() throws Exception {
        mvc.perform(get("/v1/selfDescriptions").param("q", "x").param("catalogueIds", "zzz").with(CONSUMER)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/selfDescriptions").with(CONSUMER)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/selfDescriptions").param("q", "x").param("minimumScore", "101").with(CONSUMER)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/selfDescriptions").param("q", "x").param("minimumClassification", "Blue").with(CONSUMER)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/selfDescriptions").param("q", "x").param("minimumClassification", "green").with(CONSUMER)).andExpect(status().isOk());
        mvc.perform(get("/v1/selfDescriptions").param("q", "x").with(user(List.of()))).andExpect(status().isForbidden());
        mvc.perform(get("/v1/selfDescriptions").param("q", "x")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/contractClauses/documents/x").with(CONSUMER)).andExpect(status().isNotImplemented());
    }

    @Test
    void advancedSearchSendsQualityOnlyToTheOwnCatalogue() throws Exception {
        CALLS.clear();
        mvc.perform(post("/v1/selfDescriptions/advanced").with(CONSUMER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogueIds\": [\"local\", \"catalogue-b\"], \"minimumScore\": 70, \"simpl:GeneralServiceProperties\": {\"name\": \"x\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups.length()").value(2))
                .andExpect(jsonPath("$.groups[1].items[0].i.catalogueId").value("catalogue-b"));
        String own = CALLS.stream().filter(r -> r.getPath().startsWith("/qma/selfDescriptions/advancedSearch")).findFirst().orElseThrow().getBody().readUtf8();
        String fed = CALLS.stream().filter(r -> r.getPath().startsWith("/qma/governanceAuthority")).findFirst().orElseThrow().getBody().readUtf8();
        assertThat(own).contains("\"minimumScore\":70").contains("\"catalogueIds\":[\"catalogue-a\"]");
        assertThat(fed).doesNotContain("minimumScore").contains("\"catalogueIds\":[\"catalogue-b\"]");
        mvc.perform(post("/v1/selfDescriptions/advanced").with(CONSUMER).contentType(MediaType.APPLICATION_JSON).content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailIsTheVcPlusQualityAndErrorsPassThrough() throws Exception {
        mvc.perform(get("/v2/selfDescriptions/rd-a").param("catalogueId", "catalogue-a").with(CONSUMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialSubject['@id']").value("rd-a"))
                .andExpect(jsonPath("$.qualityAssessment.overall.score").value(87.5));
        mvc.perform(get("/v2/selfDescriptions/rd-x").param("catalogueId", "catalogue-b").with(CONSUMER)).andExpect(status().isNotFound());
        mvc.perform(get("/v2/selfDescriptions/rd-x").with(CONSUMER)).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/selfDescriptions/rd-a").with(CONSUMER)).andExpect(status().isOk());
    }

    @Test
    void cataloguesAndSchemas() throws Exception {
        mvc.perform(get("/v1/catalogues").with(CONSUMER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.items[0].id").value("catalogue-b"));
        mvc.perform(get("/v2/schemas").param("schemaType", "SEARCH").with(CONSUMER)).andExpect(status().isOk());
        mvc.perform(get("/v2/schemas").param("schemaType", "OTHER").with(CONSUMER)).andExpect(status().isBadRequest());
        mvc.perform(get("/v2/schemas/lower/qualityProfiles/active").with(CONSUMER)).andExpect(status().isBadRequest());
    }
}
