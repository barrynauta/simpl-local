package eu.simpl.cm.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.manager.client.HttpResult;
import eu.simpl.cm.manager.client.StubClient;
import eu.simpl.cm.manager.client.UibeClient;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.events.EventPublisher;
import eu.simpl.cm.manager.service.LifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CM at the Provider, with the stub, UIBE and Kafka mocked. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"cm.participant-id=provider-01",
        "spring.datasource.url=jdbc:h2:mem:cmprovider;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
class ProviderApiTest {

    static final String KEY = "test-key";

    @Autowired
    MockMvc mvc;
    @Autowired
    ContractRepository contracts;
    @Autowired
    LifecycleService lifecycle;
    @MockitoBean
    StubClient stub;
    @MockitoBean
    UibeClient uibe;
    @MockitoBean
    EventPublisher events;

    @BeforeEach
    void setUp() {
        StubMocks.defaults(stub, uibe);
        when(events.notification(anyString(), any())).thenReturn(true);
    }

    static String uuid() {
        return UUID.randomUUID().toString();
    }

    private MockHttpServletRequestBuilder key(MockHttpServletRequestBuilder b) {
        return b.header("X-Api-Key", KEY);
    }

    private JsonNode json(MvcResult r) throws Exception {
        return StubMocks.M.readTree(r.getResponse().getContentAsString());
    }

    private String statusOf(String nid) throws Exception {
        return json(mvc.perform(key(get("/contracts/" + nid))).andReturn()).path("status").asText();
    }

    // ---------------------------------------------------------------- PSO scenarios and D84 order

    @Test
    void psoHealthNeedsKey() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unauthorized"));
        mvc.perform(key(get("/health"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void psoUnauthorisedComesFirst() throws Exception {
        String aid = uuid();
        for (String path : new String[]{"/credentials/agreements/" + aid + "/definitions/test-def/issue",
                "/credentials/agreements/" + aid + "/definitions/test-def/create",
                "/agreements/test-negotiation/search"}) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Unauthorized"))
                    .andExpect(jsonPath("$.detail").isString());
        }
        mvc.perform(post("/credentials/agreements/" + aid + "/definitions/d/issue").header("X-Api-Key", "wrong")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void psoUnknownIdsGive404() throws Exception {
        String aid = uuid();
        mvc.perform(key(get("/agreements/" + aid))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/agreements/" + aid));
        mvc.perform(key(get("/agreements/" + aid + "/file"))).andExpect(status().isNotFound());
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/" + uuid() + "/issue"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Test
    void pathFormatBeforeRecordAndMediaType() throws Exception {
        mvc.perform(key(get("/agreements/not-a-uuid"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:argumentTypeMismatch"));
        mvc.perform(key(post("/agreements/n1/search")).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(key(post("/agreements/n1/search")).contentType(MediaType.APPLICATION_JSON).content("{x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:validationFailure"));
        mvc.perform(key(get("/agreements/search"))).andExpect(status().isMethodNotAllowed());
        mvc.perform(key(get("/agreements/" + uuid())).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void createRunsAtTheConsumerOnly() throws Exception {
        mvc.perform(key(post("/credentials/agreements/" + uuid() + "/definitions/d1/create"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"contractNegotiationId":"n-x","assetId":"a1","providerId":"provider-01",
                                 "consumerId":"consumer-01","contractOfferId":"o1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:wrongParty"));
    }

    // ---------------------------------------------------------------- existence check

    private String search(String nid, String aid, String asset, String consumer) throws Exception {
        return mvc.perform(key(post("/agreements/" + nid + "/search")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractAgreementId\":\"" + aid + "\",\"contractDefinitionId\":\"def-1\","
                                + "\"assetId\":\"" + asset + "\",\"providerId\":\"provider-01\",\"consumerId\":\""
                                + consumer + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void existenceCheckCreatesTheProviderRecordOnce() throws Exception {
        String nid = uuid();
        String aid = uuid();
        String asset = "consent-" + uuid();
        JsonNode r = StubMocks.M.readTree(search(nid, aid, asset, "consumer-01"));
        assertThat(r.path("exists").asBoolean()).isFalse();
        assertThat(r.path("contractAgreementId").asText()).isEqualTo(aid);
        assertThat(r.path("contractSearchTO").path("assetId").asText()).isEqualTo(asset);
        verify(stub, timeout(5000)).callback("isPresent", nid, false);
        search(nid, aid, asset, "consumer-01");
        assertThat(contracts.findAll().stream().filter(c -> c.contractNegotiationId.equals(nid)).count()).isEqualTo(1);
        JsonNode d = json(mvc.perform(key(get("/contracts/" + nid))).andReturn());
        assertThat(d.path("status").asText()).isEqualTo("REQUESTED");
        assertThat(d.path("partyRole").asText()).isEqualTo("PROVIDER");
        assertThat(d.path("consentStatus").asText()).isEqualTo("PENDING");
        assertThat(d.path("resourceName").asText()).isEqualTo("Resource " + asset);

        String nid2 = uuid();
        JsonNode r2 = StubMocks.M.readTree(search(nid2, uuid(), asset, "consumer-01"));
        assertThat(r2.path("exists").asBoolean()).isTrue();
        assertThat(r2.path("contractAgreementId").asText()).isEqualTo(aid);
        verify(stub, timeout(5000)).callback("isPresent", nid2, true);
        mvc.perform(key(get("/contracts/" + nid2))).andExpect(status().isNotFound());

        mvc.perform(key(post("/agreements/" + uuid() + "/search")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"consumerId\":\"c\"}")).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- validate, sign, finalise

    private ObjectNode consumerVc(String nid, String aid, String asset) {
        String now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ObjectNode s = StubMocks.M.createObjectNode();
        s.put("id", "urn:simpl:contract:" + nid);
        s.put("contractNegotiationId", nid);
        s.put("contractAgreementId", aid);
        s.put("contractDefinitionId", "def-1");
        s.put("assetId", asset);
        s.put("resourceDescriptionId", "rd-" + asset);
        s.put("providerId", "provider-01");
        s.put("consumerId", "consumer-01");
        s.put("usageContractHash", "hash-1");
        s.put("signerRole", "CONSUMER");
        s.put("signedAt", now);
        return StubMocks.vc(s, StubMocks.CONSUMER_DID, Instant.now().minusSeconds(5).toString(),
                Instant.now().plusSeconds(3600).toString());
    }

    @Test
    void providerValidatesSignsAndFinalises() throws Exception {
        String nid = uuid();
        String aid = uuid();
        String asset = "asset-" + uuid();
        search(nid, aid, asset, "consumer-01");
        when(stub.vcExchange(nid)).thenReturn(new HttpResult(200, consumerVc(nid, aid, asset)));
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/issue"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.contractAgreementId").value(aid));
        StubMocks.await(() -> {
            try {
                return statusOf(nid).equals("SIGNED");
            } catch (Exception e) {
                return false;
            }
        });
        verify(stub, timeout(5000)).callback("isValid", nid, true);
        verify(stub, timeout(5000)).callback("signed", nid, true);
        verify(stub, atLeastOnce()).walletPut(eq(nid), anyString(), any());
        JsonNode d = json(mvc.perform(key(get("/contracts/" + nid))).andReturn());
        assertThat(d.path("validationResult").asText()).isEqualTo("SUCCESS");
        assertThat(d.path("consumerSignatureDate").isNull()).isFalse();
        assertThat(d.path("providerSignatureDate").isNull()).isFalse();
        assertThat(d.path("ownCredentialId").asText()).startsWith("urn:uuid:");
        mvc.perform(key(get("/verifications/" + nid + "/vc"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialSubject.signerRole").value("PROVIDER"));
        mvc.perform(key(get("/agreements/" + aid))).andExpect(jsonPath("$.status").value("FINALIZING"));

        // duplicate trigger: ignored, result unchanged (D28, C1)
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/issue"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        JsonNode d2 = json(mvc.perform(key(get("/contracts/" + nid))).andReturn());
        assertThat(d2.path("validationResult").asText()).isEqualTo("SUCCESS");
        assertThat(d2.path("history").toString()).contains("DUPLICATE_OR_REPLAYED_SUBMISSION");

        mvc.perform(key(patch("/agreements/" + aid + "/definitions/def-1/status"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FINALIZED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZED"));
        assertThat(statusOf(nid)).isEqualTo("ACTIVE");
        mvc.perform(key(patch("/agreements/" + aid + "/definitions/def-1/status"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FINALIZED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:invalidTransition"));
        String file = mvc.perform(key(get("/agreements/" + aid + "/file"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(file.split("\n")).hasSize(7);
        assertThat(file).startsWith("CONTRACT AGREEMENT: " + aid + "\n").contains("ASSET: " + asset);
        mvc.perform(key(post("/agreements/search")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"" + asset + "\"}"))
                .andExpect(jsonPath("$.items[0].contractAgreementId").value(aid));

        // lifecycle: suspend, wrong party, counterparty message, reactivate
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SUSPEND\"}")).andExpect(status().isBadRequest());
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SUSPEND\",\"reason\":\"Breach\",\"actor\":\"provider-admin\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        verify(events, atLeastOnce()).lifecycleMessage(any());
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REQUEST_REACTIVATION\",\"reason\":\"Please\"}"))
                .andExpect(status().isConflict());
        String messageId = uuid();
        lifecycle.onCounterpartyMessage(StubMocks.M.valueToTree(Map.of("messageId", messageId,
                "messageType", "REACTIVATION_REQUESTED", "contractNegotiationId", nid,
                "senderParticipantId", "consumer-01", "senderRole", "CONSUMER",
                "recipientParticipantId", "provider-01", "reason", "We fixed it")));
        assertThat(statusOf(nid)).isEqualTo("REACTIVATION_REQUESTED");
        lifecycle.onCounterpartyMessage(StubMocks.M.valueToTree(Map.of("messageId", uuid(),
                "messageType", "TERMINATED", "contractNegotiationId", nid,
                "senderParticipantId", "consumer-01", "senderRole", "CONSUMER",
                "recipientParticipantId", "provider-01")));
        assertThat(statusOf(nid)).isEqualTo("REACTIVATION_REQUESTED");
        assertThat(json(mvc.perform(key(get("/contracts/" + nid))).andReturn()).path("history").toString())
                .contains("COUNTERPARTY_MESSAGE_REJECTED");
        mvc.perform(key(post("/contracts/" + nid + "/lifecycleActions")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"REACTIVATE\"}")).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(key(get("/audit/contractStatusCounts"))).andExpect(jsonPath("$.counts.ACTIVE").isNumber())
                .andExpect(jsonPath("$.counts.REVOKED").isNumber()).andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void rejectedConsumerVcFailsTheProviderAndNotifies() throws Exception {
        String nid = uuid();
        String aid = uuid();
        String asset = "asset-" + uuid();
        search(nid, aid, asset, "consumer-01");
        ObjectNode vc = consumerVc(nid, aid, asset);
        ((ObjectNode) vc.get("credentialSubject")).put("assetId", "another");
        when(stub.vcExchange(nid)).thenReturn(new HttpResult(200, vc));
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/issue"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        verify(stub, timeout(5000)).callback("isValid", nid, false);
        JsonNode d = json(mvc.perform(key(get("/contracts/" + nid))).andReturn());
        assertThat(d.path("status").asText()).isEqualTo("FAILED");
        assertThat(d.path("reasonCode").asText()).isEqualTo("PAYLOAD_INTEGRITY_MISMATCH");
        verify(events, timeout(5000)).notification(eq(nid), any());
    }

    @Test
    void technicalErrorKeepsStatusAndAllowsRetry() throws Exception {
        String nid = uuid();
        String aid = uuid();
        String asset = "asset-" + uuid();
        search(nid, aid, asset, "consumer-01");
        when(stub.vcExchange(nid)).thenReturn(new HttpResult(502, null));
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/issue"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        StubMocks.await(() -> contracts.findByContractNegotiationId(nid).map(c -> "FAILED".equals(c.validationResult))
                .orElse(false));
        assertThat(statusOf(nid)).isEqualTo("REQUESTED");
        when(stub.vcExchange(nid)).thenReturn(new HttpResult(404, null));
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/issue"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        StubMocks.await(() -> contracts.findByContractNegotiationId(nid).map(c -> "FAILED".equals(c.status.name()))
                .orElse(false));
        assertThat(json(mvc.perform(key(get("/contracts/" + nid))).andReturn()).path("reasonCode").asText())
                .isEqualTo("MISSING_CONTRACT_DATA");
    }

    @Test
    void consentAndWrongParty() throws Exception {
        String nid = uuid();
        search(nid, uuid(), "asset-" + uuid(), "consumer-01");
        mvc.perform(key(put("/contracts/" + nid + "/consentStatus")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"consentStatus\":\"GRANTED\"}")).andExpect(status().isConflict());
        String nid2 = uuid();
        search(nid2, uuid(), "consent-" + uuid(), "consumer-01");
        mvc.perform(key(put("/contracts/" + nid2 + "/consentStatus")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentStatus\":\"GRANTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.consentStatus").value("GRANTED"));
        mvc.perform(key(put("/verifications/" + nid)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:wrongParty"));
        mvc.perform(key(patch("/contractOffers/" + nid + "/accept"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:wrongParty"));
    }

    @Test
    void clauseUpload() throws Exception {
        MvcResult r = mvc.perform(key(multipart("/contractClauseDocuments")
                        .file(new MockMultipartFile("file", "c.txt", "text/plain", "hello".getBytes()))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode j = json(r);
        assertThat(j.path("hash").asText())
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThat(j.path("hashAlgorithm").asText()).isEqualTo("SHA-256");
        assertThat(j.has("content")).isFalse();
        JsonNode again = json(mvc.perform(key(multipart("/contractClauseDocuments")
                .file(new MockMultipartFile("file", "c.txt", "text/plain", "hello".getBytes())))).andReturn());
        assertThat(again.path("documentId").asText()).isNotEqualTo(j.path("documentId").asText());
        mvc.perform(key(multipart("/contractClauseDocuments")
                        .file(new MockMultipartFile("file", "c.png", "image/png", "x".getBytes()))))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(key(multipart("/contractClauseDocuments")
                        .file(new MockMultipartFile("file", "big.txt", "text/plain", new byte[2048]))))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(key(multipart("/contractClauseDocuments")
                        .file(new MockMultipartFile("file", "e.txt", "text/plain", new byte[0]))))
                .andExpect(status().isBadRequest());
    }
}
