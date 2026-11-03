package eu.simpl.cm.manager.it;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.manager.clients.StubClient;
import eu.simpl.cm.manager.clients.UibeClient;
import eu.simpl.cm.manager.events.EventPublisher;
import eu.simpl.cm.manager.service.LifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CM at the Consumer: create, accept, sign, verify, lifecycle message receipt, expiry. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"CM_PARTICIPANT_ID=consumer-01", "CM_API_KEY=key-c"})
class ConsumerFlowIT {
    @Autowired
    MockMvc mvc;
    @Autowired
    LifecycleService lifecycle;
    @MockitoBean
    StubClient stub;
    @MockitoBean
    UibeClient uibe;
    @MockitoBean
    EventPublisher events;

    String nid;
    String aid;

    @BeforeEach
    void setUp() {
        nid = UUID.randomUUID().toString();
        aid = UUID.randomUUID().toString();
        when(stub.callback(anyString(), anyString(), anyBoolean())).thenReturn(ItSupport.json(204, null));
        when(stub.asset(anyString(), eq("asset-1"))).thenReturn(ItSupport.json(200, Map.of("assetId", "asset-1",
                "resourceDescriptionId", "rd-1", "resourceName", "Air", "providerId", "provider-01", "description", "d",
                "providerName", "Provider One", "consentRequired", false, "policies", java.util.List.of(), "contractClauses", java.util.List.of())));
        when(stub.asset(anyString(), eq("missing"))).thenReturn(ItSupport.json(404, null));
        when(stub.participant(anyString(), eq("consumer-01"))).thenReturn(ItSupport.json(200, Map.of("participantId", "consumer-01",
                "name", "Consumer One", "did", ItSupport.did(0x22), "contactEmail", "consumer-01@example.test")));
        when(stub.participant(anyString(), eq("provider-01"))).thenReturn(ItSupport.json(200, Map.of("participantId", "provider-01",
                "name", "Provider One", "did", ItSupport.did(0x11), "contactEmail", "provider-01@example.test")));
        when(stub.json(any())).thenAnswer(inv -> ((eu.simpl.cm.common.web.HttpJson.Response) inv.getArgument(0)).json(ItSupport.M));
        when(stub.verify(anyString(), any())).thenReturn(ItSupport.verified(true, null));
        when(stub.storeInWallet(anyString(), anyString(), any())).thenReturn(ItSupport.json(201, Map.of()));
        when(stub.issue(anyString(), any(), anyString(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> subject = inv.getArgument(1);
            return ItSupport.json(201, ItSupport.signedVc(0x22, subject));
        });
        when(uibe.postOffer(anyString(), anyString(), anyString())).thenReturn(ItSupport.json(201, Map.of("offerStatus", "CREATED")));
        when(events.publishAndWait(anyString(), anyString(), any(), any())).thenReturn(true);
    }

    MockHttpServletRequestBuilder key(MockHttpServletRequestBuilder b) {
        return b.header("X-Api-Key", "key-c");
    }

    JsonNode details() throws Exception {
        return ItSupport.M.readTree(mvc.perform(key(get("/contracts/" + nid))).andReturn().getResponse().getContentAsString());
    }

    String cmStatus() throws Exception {
        return details().path("status").asText();
    }

    String createBody(String asset) {
        return "{\"contractNegotiationId\":\"" + nid + "\",\"assetId\":\"" + asset + "\",\"providerId\":\"provider-01\","
                + "\"consumerId\":\"consumer-01\",\"contractOfferId\":\"offer-1\"}";
    }

    void create() throws Exception {
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create")).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("asset-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.contractStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.providerName").value("Provider One"))
                .andExpect(jsonPath("$.offerUrl").value("http://localhost:19103/contractOffers/" + nid));
    }

    @Test
    void createAcceptSignVerifyAndCounterpartySuspension() throws Exception {
        create();
        JsonNode d = details();
        assertThat(d.path("status").asText()).isEqualTo("REQUESTED");
        assertThat(eu.simpl.cm.common.proof.DataIntegrity.sha256Hex(d.path("humanReadable").asText().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isEqualTo(d.path("usageContractHash").asText());
        assertThat(d.path("allowedActions").toString()).isEqualTo("[\"ACCEPT\",\"REJECT\"]");
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create")).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("asset-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:alreadyExists"));

        mvc.perform(key(patch("/contractOffers/" + nid + "/accept"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIATED"));
        ItSupport.await(() -> "SIGNED".equals(cmStatus()));
        ItSupport.await(() -> {
            verify(stub).callback("signed", nid, true);
            return true;
        });
        mvc.perform(key(patch("/contractOffers/" + nid + "/accept"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:offerNotPending"));

        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", "urn:simpl:contract:" + nid);
        s.put("contractNegotiationId", nid);
        s.put("contractAgreementId", aid);
        s.put("contractDefinitionId", "def-1");
        s.put("assetId", "asset-1");
        s.put("resourceDescriptionId", "rd-1");
        s.put("providerId", "provider-01");
        s.put("consumerId", "consumer-01");
        s.put("usageContractHash", d.path("usageContractHash").asText());
        s.put("signerRole", "PROVIDER");
        s.put("signedAt", "2026-10-29T11:00:00Z");
        String providerVc = ItSupport.signedVc(0x11, s).toString();
        mvc.perform(key(put("/verifications/" + nid)).contentType(MediaType.APPLICATION_JSON).content(providerVc))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INITIATED"));
        ItSupport.await(() -> "ACTIVE".equals(cmStatus()));
        verify(stub).callback("isValid", nid, true);
        assertThat(details().path("providerSignatureDate").asText()).isEqualTo("2026-10-29T11:00:00Z");

        // counterparty lifecycle message (D47) and its idempotency
        String msg = "{\"messageId\":\"m-" + nid + "\",\"messageType\":\"SUSPENDED\",\"contractNegotiationId\":\"" + nid
                + "\",\"senderParticipantId\":\"provider-01\",\"senderRole\":\"PROVIDER\",\"recipientParticipantId\":\"consumer-01\",\"reason\":\"Breach\"}";
        lifecycle.onCounterpartyMessage(msg);
        assertThat(cmStatus()).isEqualTo("SUSPENDED");
        lifecycle.onCounterpartyMessage(msg);
        assertThat(cmStatus()).isEqualTo("SUSPENDED");
        // a message that does not fit (D48)
        lifecycle.onCounterpartyMessage(msg.replace("m-" + nid, "m2-" + nid));
        boolean rejected = false;
        for (JsonNode h : details().path("history")) {
            rejected |= "COUNTERPARTY_MESSAGE_REJECTED".equals(h.path("entryType").asText());
        }
        assertThat(rejected).isTrue();
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REQUEST_REACTIVATION\",\"reason\":\"Fixed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REACTIVATION_REQUESTED"));
    }

    @Test
    void createErrors() throws Exception {
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create")).contentType(MediaType.APPLICATION_JSON)
                .content(createBody("missing"))).andExpect(status().isNotFound());
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contractNegotiationId\":\"" + nid + "\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:validationFailure"));
        org.mockito.Mockito.doReturn(ItSupport.json(500, null)).when(uibe).postOffer(anyString(), anyString(), anyString());
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create")).contentType(MediaType.APPLICATION_JSON)
                .content(createBody("asset-1"))).andExpect(status().isInternalServerError());
        mvc.perform(key(get("/contracts/" + nid))).andExpect(status().isNotFound());
    }

    @Test
    void signingFailureAndRejectAndExpiry() throws Exception {
        create();
        org.mockito.Mockito.doReturn(ItSupport.json(500, null)).when(stub).issue(anyString(), any(), anyString(), anyString());
        mvc.perform(key(patch("/contractOffers/" + nid + "/accept"))).andExpect(status().isOk());
        ItSupport.await(() -> "FAILED".equals(cmStatus()));
        assertThat(details().path("reasonCode").asText()).isEqualTo("SIGNING_FAILED");
        ItSupport.await(() -> {
            verify(stub).callback("signed", nid, false);
            return true;
        });

        nid = UUID.randomUUID().toString();
        create();
        mvc.perform(key(patch("/contractOffers/" + nid + "/reject"))).andExpect(status().isOk());
        assertThat(cmStatus()).isEqualTo("REJECTED");

        nid = UUID.randomUUID().toString();
        create();
        mvc.perform(key(patch("/contractOffers/" + nid + "/reject").param("reason", "EXPIRED"))).andExpect(status().isOk());
        assertThat(cmStatus()).isEqualTo("TERMINATED");
        assertThat(details().path("reasonCode").asText()).isEqualTo("OFFER_EXPIRED");

        nid = UUID.randomUUID().toString();
        create();
        mvc.perform(key(put("/contractOffers/" + nid + "/update")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SIGNED\"}")).andExpect(status().isBadRequest());
        mvc.perform(key(put("/contractOffers/" + nid + "/update")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\",\"additionalInfo\":\"Provider validation or signing failed\"}")).andExpect(status().isOk());
        assertThat(cmStatus()).isEqualTo("FAILED");
        assertThat(details().path("reasonCode").asText()).isEqualTo("PROVIDER_REJECTED");
    }
}
