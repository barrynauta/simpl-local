package eu.simpl.cm.manager.it;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.manager.clients.StubClient;
import eu.simpl.cm.manager.clients.UibeClient;
import eu.simpl.cm.manager.events.EventPublisher;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CM at the Provider with a mocked stub (Appendix C) and mocked Kafka. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"CM_PARTICIPANT_ID=provider-01", "CM_API_KEY=key-p"})
class ProviderFlowIT {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    StubClient stub;
    @MockitoBean
    UibeClient uibe;
    @MockitoBean
    EventPublisher events;

    String nid;
    String aid;
    String asset;

    @BeforeEach
    void setUp() {
        nid = UUID.randomUUID().toString();
        aid = UUID.randomUUID().toString();
        asset = "asset-" + UUID.randomUUID();
        when(stub.callback(anyString(), anyString(), anyBoolean())).thenReturn(ItSupport.json(204, null));
        when(stub.asset(anyString(), anyString())).thenReturn(ItSupport.json(200, Map.of("assetId", "asset-1",
                "resourceDescriptionId", "rd-1", "resourceName", "Air", "providerId", "provider-01",
                "providerName", "Provider One", "consentRequired", true)));
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
            return ItSupport.json(201, ItSupport.signedVc(0x11, subject));
        });
        when(events.publishAndWait(anyString(), anyString(), any(), any())).thenReturn(true);
    }

    MockHttpServletRequestBuilder key(MockHttpServletRequestBuilder b) {
        return b.header("X-Api-Key", "key-p");
    }

    Map<String, Object> consumerSubject() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", "urn:simpl:contract:" + nid);
        s.put("contractNegotiationId", nid);
        s.put("contractAgreementId", aid);
        s.put("contractDefinitionId", "def-1");
        s.put("assetId", asset);
        s.put("resourceDescriptionId", "rd-1");
        s.put("providerId", "provider-01");
        s.put("consumerId", "consumer-01");
        s.put("usageContractHash", "abc");
        s.put("signerRole", "CONSUMER");
        s.put("signedAt", "2026-10-29T10:00:00Z");
        return s;
    }

    String cmStatus() throws Exception {
        String body = mvc.perform(key(get("/contracts/" + nid))).andReturn().getResponse().getContentAsString();
        return ItSupport.M.readTree(body).path("status").asText();
    }

    JsonNode details() throws Exception {
        return ItSupport.M.readTree(mvc.perform(key(get("/contracts/" + nid))).andReturn().getResponse().getContentAsString());
    }

    void search() throws Exception {
        mvc.perform(key(post("/agreements/" + nid + "/search")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractAgreementId\":\"" + aid + "\",\"contractDefinitionId\":\"def-1\",\"assetId\":\"" + asset + "\","
                                + "\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.contractAgreementId").value(aid))
                .andExpect(jsonPath("$.contractSearchTO.assetId").value(asset));
    }

    void issue() throws Exception {
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/issue")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractNegotiationId\":\"" + nid + "\",\"assetId\":\"" + asset + "\",\"providerId\":\"provider-01\","
                                + "\"consumerId\":\"consumer-01\",\"contractOfferId\":\"offer-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    void healthAndApiKey() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/contracts")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unauthorized"));
        mvc.perform(get("/contracts").header("X-Api-Key", "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void wholeProviderSideToActiveThenLifecycle() throws Exception {
        search();
        assertThat(cmStatus()).isEqualTo("REQUESTED");
        assertThat(details().path("consentStatus").asText()).isEqualTo("PENDING");
        ItSupport.await(() -> {
            verify(stub, atLeastOnce()).callback("isPresent", nid, false);
            return true;
        });
        when(stub.vcExchange(nid)).thenReturn(ItSupport.json(200, ItSupport.signedVc(0x22, consumerSubject())));
        issue();
        ItSupport.await(() -> "SIGNED".equals(cmStatus()));
        verify(stub).callback("isValid", nid, true);
        ItSupport.await(() -> {
            verify(stub).callback("signed", nid, true);
            return true;
        });
        JsonNode d = details();
        assertThat(d.path("validationResult").asText()).isEqualTo("SUCCESS");
        assertThat(d.path("consumerSignatureDate").asText()).isNotEmpty();
        mvc.perform(key(get("/verifications/" + nid + "/vc"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialSubject.signerRole").value("PROVIDER"));
        mvc.perform(key(get("/agreements/" + aid))).andExpect(jsonPath("$.status").value("FINALIZING"));

        // duplicate /issue after SUCCESS is IGNORED (D28)
        String validatedAt = details().path("validatedAt").asText();
        issue();
        JsonNode dup = details();
        assertThat(dup.path("validationResult").asText()).isEqualTo("SUCCESS");
        assertThat(dup.path("validationReasonCode").isNull()).isTrue();
        assertThat(dup.path("validatedAt").asText()).isEqualTo(validatedAt);
        assertThat(cmStatus()).isEqualTo("SIGNED");
        boolean ignoredEntry = false;
        String signedTrigger = null;
        for (JsonNode h : dup.path("history")) {
            ignoredEntry |= "VALIDATION".equals(h.path("entryType").asText())
                    && "DUPLICATE_OR_REPLAYED_SUBMISSION".equals(h.path("reasonCode").asText())
                    && h.path("reason").asText().contains("IGNORED");
            if ("SIGNED".equals(h.path("toStatus").asText()) && "TRANSITION".equals(h.path("entryType").asText())) {
                signedTrigger = h.path("trigger").asText();
            }
        }
        assertThat(ignoredEntry).isTrue();
        assertThat(signedTrigger).isEqualTo("CONNECTOR");
        assertThat(dup.path("ownCredentialId").asText()).startsWith("urn:uuid:");
        assertThat(dup.path("counterpartyCredentialId").asText()).startsWith("urn:uuid:");

        // FINALIZED -> ACTIVE (D32)
        mvc.perform(key(patch("/agreements/" + aid + "/definitions/def-1/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINALIZED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZED"));
        assertThat(cmStatus()).isEqualTo("ACTIVE");
        mvc.perform(key(patch("/agreements/" + aid + "/definitions/def-1/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINALIZED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:invalidTransition"));
        mvc.perform(key(get("/agreements/" + aid + "/file"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("CONTRACT AGREEMENT: " + aid + "\nCONTRACT DEFINITION: def-1\n")));

        // lifecycle: suspend requires a reason, consumer-only action is 409, suspend publishes a message
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SUSPEND\",\"reason\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"REQUEST_REACTIVATION\",\"reason\":\"x\"}")).andExpect(status().isConflict());
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUSPEND\",\"reason\":\"Breach\",\"actor\":\"provider-admin\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        verify(events).publish(eq(EventPublisher.LIFECYCLE_TOPIC), eq(nid),
                org.mockito.ArgumentMatchers.argThat(m -> "SUSPENDED".equals(m.get("messageType"))
                        && "consumer-01".equals(m.get("recipientParticipantId")) && "PROVIDER".equals(m.get("senderRole"))), any());
        mvc.perform(key(post("/agreements/search")).contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":\"" + asset + "\"}"))
                .andExpect(jsonPath("$.items[?(@.contractAgreementId == '" + aid + "')]").exists());

        // consent (A.5)
        mvc.perform(key(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/contracts/" + nid + "/consentStatus"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"consentStatus\":\"GRANTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.consentStatus").value("GRANTED"));
    }

    @Test
    void incorrectlySignedConsumerVcIsRejectedAndNotified() throws Exception {
        search();
        when(stub.vcExchange(nid)).thenReturn(ItSupport.json(200, ItSupport.signedVc(0x22, consumerSubject())));
        when(stub.verify(eq(nid), any())).thenReturn(ItSupport.verified(false, "INVALID_SIGNATURE"));
        issue();
        ItSupport.await(() -> "FAILED".equals(cmStatus()));
        JsonNode d = details();
        assertThat(d.path("reasonCode").asText()).isEqualTo("INVALID_CONSUMER_SIGNATURE");
        assertThat(d.path("validationResult").asText()).isEqualTo("REJECTED");
        String failedTrigger = null;
        for (JsonNode h : d.path("history")) {
            if ("FAILED".equals(h.path("toStatus").asText()) && "TRANSITION".equals(h.path("entryType").asText())) {
                failedTrigger = h.path("trigger").asText();
            }
        }
        assertThat(failedTrigger).isEqualTo("CONNECTOR");
        assertThat(d.path("ownCredentialId").isNull()).isTrue();
        ItSupport.await(() -> {
            verify(events).publishAndWait(eq(EventPublisher.NOTIFICATIONS_TOPIC), eq(nid),
                    org.mockito.ArgumentMatchers.argThat(m -> ("Usage contract offer could not be validated: " + nid).equals(m.get("subject"))
                            && String.valueOf(m.get("message")).contains("INVALID_CONSUMER_SIGNATURE")), any());
            return true;
        });
        verify(stub).callback("isValid", nid, false);
    }

    @Test
    void missingVcAndTechnicalErrorAndWallet() throws Exception {
        search();
        when(stub.vcExchange(nid)).thenReturn(ItSupport.json(502, null));
        issue();
        ItSupport.await(() -> "FAILED".equals(details().path("validationResult").asText()));
        assertThat(cmStatus()).isEqualTo("REQUESTED");
        // retry after a technical failure runs a fresh validation, now with a wallet fault
        org.mockito.Mockito.doReturn(ItSupport.json(200, ItSupport.signedVc(0x22, consumerSubject()))).when(stub).vcExchange(nid);
        org.mockito.Mockito.doReturn(ItSupport.json(507, null)).when(stub).storeInWallet(anyString(), anyString(), any());
        issue();
        ItSupport.await(() -> "FAILED".equals(cmStatus()));
        assertThat(details().path("reasonCode").asText()).isEqualTo("WALLET_FULL");
        JsonNode history = details().path("history");
        boolean message = false;
        for (JsonNode h : history) {
            message |= "Secure storage location is full".equals(h.path("reason").asText());
        }
        assertThat(message).isTrue();
    }

    @Test
    void errorsFollowD11AndD12() throws Exception {
        mvc.perform(key(post("/credentials/agreements/not-a-uuid/definitions/def-1/issue")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("urn:problem-type:simpl:argumentTypeMismatch"));
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/issue")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("urn:problem-type:simpl:validationFailure"));
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractNegotiationId\":\"n\",\"assetId\":\"a\",\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\",\"contractOfferId\":\"o\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:wrongParty"));
        mvc.perform(key(post("/agreements/search")).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(key(get("/contracts")).accept(MediaType.TEXT_HTML)).andExpect(status().isNotAcceptable());
        mvc.perform(key(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/contracts")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(key(get("/contracts/" + nid))).andExpect(status().isNotFound());
        mvc.perform(key(get("/verifications/" + nid + "/vc"))).andExpect(status().isNotFound());
        mvc.perform(key(get("/audit/contractStatusCounts"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.participantId").value("provider-01"))
                .andExpect(jsonPath("$.counts.REVOCATION_REQUESTED").isNumber());
    }

    @Test
    void clauseUpload() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("file", "clauses.txt", "text/plain", "hello".getBytes());
        mvc.perform(key(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/contractClauseDocuments").file(file)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hash").value("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
                .andExpect(jsonPath("$.content").doesNotExist());
        var png = new org.springframework.mock.web.MockMultipartFile("file", "x.png", "image/png", "x".getBytes());
        mvc.perform(key(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/contractClauseDocuments").file(png)))
                .andExpect(status().isUnsupportedMediaType());
        var empty = new org.springframework.mock.web.MockMultipartFile("file", "e.txt", "text/plain", new byte[0]);
        mvc.perform(key(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/contractClauseDocuments").file(empty)))
                .andExpect(status().isBadRequest());
    }
}
