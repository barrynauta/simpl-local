package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.HttpJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UIBE with mocked JWTs (spring-security-test) and a mocked CM. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"UIBE_PARTICIPANT_ID=consumer-01", "UIBE_API_KEY=ui-key"})
class UibeIT {
    static final ObjectMapper M = new ObjectMapper();
    @Autowired
    MockMvc mvc;
    @MockitoBean
    CmClient cm;
    String nid;

    static RequestPostProcessor user(String participant, String... roles) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j
                .claim("participant_id", participant)
                .claim("preferred_username", "consumer-admin")
                .claim("realm_access", Map.of("roles", List.of(roles))));
    }

    static HttpJson.Response json(int status, Object body) throws Exception {
        return new HttpJson.Response(status, M.writeValueAsString(body), "application/json");
    }

    @BeforeEach
    void setUp() throws Exception {
        nid = UUID.randomUUID().toString();
    }

    void createOffer() throws Exception {
        mvc.perform(post("/contractOffers/" + nid).header("x-api-key", "ui-key").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humanReadable\":\"<p>x</p>\",\"hash\":\"h\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerStatus").value("CREATED"))
                .andExpect(jsonPath("$.offerUrl").value("http://localhost:19103/contractOffers/" + nid));
    }

    @Test
    void authentication() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/contracts")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unauthorized"));
        mvc.perform(get("/contracts").header("x-api-key", "ui-key")).andExpect(status().isUnauthorized());
        mvc.perform(get("/contracts").with(user("consumer-01"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:forbidden"));
        mvc.perform(get("/contracts").with(user("provider-01", "contract_manager"))).andExpect(status().isForbidden());
        mvc.perform(post("/contractOffers/" + nid).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/offerExpiry/run").header("x-api-key", "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void offersAcceptAndCmFailure() throws Exception {
        createOffer();
        mvc.perform(get("/contractOffers").with(user("consumer-01", "contract_manager"))).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.contractNegotiationId == '" + nid + "')].humanReadableData").value("<p>x</p>"));
        mvc.perform(get("/contractOffers/" + nid).with(user("consumer-01", "contract_manager")))
                .andExpect(jsonPath("$.hashData").value("h")).andExpect(jsonPath("$.contractData").isEmpty())
                .andExpect(jsonPath("$.status").value("CREATED"));
        // CM refuses: UIBE answers with the same status and keeps CREATED (D23)
        when(cm.call(eq("PATCH"), eq("/contractOffers/" + nid + "/accept"), isNull(), anyString()))
                .thenReturn(json(400, Map.of("type", "urn:problem-type:simpl:contract-management:offerNotPending", "status", 400)));
        mvc.perform(patch("/contractOffers/" + nid + "/accept").with(user("consumer-01", "contract_manager")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:offerNotPending"));
        mvc.perform(get("/contractOffers/" + nid).with(user("consumer-01", "contract_manager")))
                .andExpect(jsonPath("$.status").value("CREATED"));
        when(cm.call(eq("PATCH"), eq("/contractOffers/" + nid + "/accept"), isNull(), anyString()))
                .thenReturn(json(200, Map.of("contractNegotiationId", nid, "status", "INITIATED")));
        mvc.perform(patch("/contractOffers/" + nid + "/accept").with(user("consumer-01", "contract_manager")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.humanReadableData").value("<p>x</p>"));
        mvc.perform(patch("/contractOffers/" + nid + "/accept").with(user("consumer-01", "contract_manager")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:offerNotPending"));
        mvc.perform(get("/contractOffers/unknown-" + nid).with(user("consumer-01", "contract_manager")))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailsFilterConsentForUsersWithoutTheRole() throws Exception {
        when(cm.call(eq("GET"), eq("/contracts/" + nid), any(), any())).thenReturn(json(200, Map.of(
                "contractNegotiationId", nid, "partyRole", "PROVIDER", "status", "ACTIVE",
                "allowedActions", List.of("SUSPEND", "UPDATE_CONSENT"))));
        mvc.perform(get("/contracts/" + nid).with(user("consumer-01", "contract_manager")))
                .andExpect(jsonPath("$.allowedActions.length()").value(1))
                .andExpect(jsonPath("$.consentManagerUrl").doesNotExist())
                .andExpect(jsonPath("$.offerStatus").isEmpty());
        mvc.perform(get("/contracts/" + nid).with(user("consumer-01", "contract_manager", "consent_manager")))
                .andExpect(jsonPath("$.allowedActions.length()").value(2))
                .andExpect(jsonPath("$.consentManagerUrl").value("http://localhost:19090/consent?contractNegotiationId=" + nid));
    }

    @Test
    void counterpartyFilterAndLifecyclePassThrough() throws Exception {
        when(cm.call(eq("GET"), eq("/contracts"), any(), any())).thenReturn(json(200, Map.of("items", List.of(
                Map.of("contractNegotiationId", "a", "partyRole", "CONSUMER", "providerName", "Provider One", "consumerName", "Consumer One"),
                Map.of("contractNegotiationId", "b", "partyRole", "CONSUMER", "providerName", "Other", "consumerName", "Consumer One")))));
        mvc.perform(get("/contracts").param("counterpartyName", "provider o").with(user("consumer-01", "contract_manager")))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].contractNegotiationId").value("a"));
        when(cm.call(eq("POST"), eq("/contracts/" + nid + "/lifecycleActions"), any(), eq("consumer-admin")))
                .thenReturn(json(409, Map.of("type", "urn:problem-type:simpl:contract-management:invalidTransition", "status", 409)));
        mvc.perform(post("/contracts/" + nid + "/lifecycleActions").with(user("consumer-01", "contract_manager"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"SUSPEND\",\"reason\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void expiryRun() throws Exception {
        createOffer();
        when(cm.call(eq("PATCH"), eq("/contractOffers/" + nid + "/reject?reason=EXPIRED"), isNull(), isNull()))
                .thenReturn(json(200, Map.of("contractNegotiationId", nid, "status", "INITIATED")));
        // not old enough
        mvc.perform(post("/admin/offerExpiry/run").header("x-api-key", "ui-key").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractNegotiationIds\":[\"" + nid + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.skipped[0]").value(nid));
        String asOf = java.time.Instant.now().plus(java.time.Duration.ofDays(7)).plusSeconds(60).toString();
        mvc.perform(post("/admin/offerExpiry/run").header("x-api-key", "ui-key").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asOf\":\"" + asOf + "\",\"contractNegotiationIds\":[\"" + nid + "\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expired[0]").value(nid));
        mvc.perform(get("/contractOffers/" + nid).with(user("consumer-01", "contract_manager")))
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }
}
