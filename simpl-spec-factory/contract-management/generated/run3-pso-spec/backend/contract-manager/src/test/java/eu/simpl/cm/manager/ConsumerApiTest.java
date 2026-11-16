package eu.simpl.cm.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.manager.client.HttpResult;
import eu.simpl.cm.manager.client.StubClient;
import eu.simpl.cm.manager.client.UibeClient;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CM at the Consumer, with the stub, UIBE and Kafka mocked. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"cm.participant-id=consumer-01", "cm.ui-public-url=http://localhost:19203",
        "spring.datasource.url=jdbc:h2:mem:cmconsumer;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
class ConsumerApiTest {

    @Autowired
    MockMvc mvc;
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

    private MockHttpServletRequestBuilder key(MockHttpServletRequestBuilder b) {
        return b.header("X-Api-Key", "test-key");
    }

    private JsonNode details(String nid) throws Exception {
        return StubMocks.M.readTree(mvc.perform(key(get("/contracts/" + nid))).andReturn().getResponse()
                .getContentAsString());
    }

    private String create(String nid, String aid, String asset) throws Exception {
        return mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractNegotiationId\":\"" + nid + "\",\"assetId\":\"" + asset
                                + "\",\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\","
                                + "\"contractOfferId\":\"offer-1\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void createPreparesTheOffer() throws Exception {
        String nid = UUID.randomUUID().toString();
        String aid = UUID.randomUUID().toString();
        String asset = "asset-" + UUID.randomUUID();
        JsonNode r = StubMocks.M.readTree(create(nid, aid, asset));
        assertThat(r.path("status").asText()).isEqualTo("CREATED");
        assertThat(r.path("contractStatus").asText()).isEqualTo("REQUESTED");
        assertThat(r.path("offerUrl").asText()).isEqualTo("http://localhost:19203/contractOffers/" + nid);
        assertThat(r.path("usageContractHash").asText()).hasSize(64);
        assertThat(r.path("providerName").asText()).isEqualTo("Provider One");
        verify(uibe).postOffer(eq(nid), anyString(), eq(r.path("usageContractHash").asText()));
        verify(events).notification(eq(nid), any());
        assertThat(details(nid).path("humanReadable").asText()).isEqualTo(r.path("usageContractHash").isMissingNode()
                ? null : r.path("usageContract").asText());

        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contractNegotiationId\":\"" + nid
                                + "\",\"assetId\":\"" + asset + "\",\"providerId\":\"provider-01\","
                                + "\"consumerId\":\"consumer-01\",\"contractOfferId\":\"offer-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:alreadyExists"));
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contractNegotiationId\":\"x-1\","
                                + "\"assetId\":\"missing-1\",\"providerId\":\"provider-01\","
                                + "\"consumerId\":\"consumer-01\",\"contractOfferId\":\"offer-1\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(key(post("/credentials/agreements/" + aid + "/definitions/def-1/create"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contractNegotiationId\":\"x-2\","
                                + "\"assetId\":\"asset-2\",\"providerId\":\"provider-99\","
                                + "\"consumerId\":\"consumer-01\",\"contractOfferId\":\"offer-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uibeFailureKeepsNoRecord() throws Exception {
        String nid = UUID.randomUUID().toString();
        when(uibe.postOffer(eq(nid), anyString(), anyString())).thenReturn(new HttpResult(500, null));
        create(nid, UUID.randomUUID().toString(), "asset-1");
        mvc.perform(key(get("/contracts/" + nid))).andExpect(status().isNotFound());
    }

    @Test
    void acceptSignsAndVerificationActivates() throws Exception {
        String nid = UUID.randomUUID().toString();
        String aid = UUID.randomUUID().toString();
        String asset = "asset-" + UUID.randomUUID();
        create(nid, aid, asset);
        mvc.perform(key(patch("/contractOffers/" + nid + "/accept"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIATED"));
        verify(stub, timeout(5000)).callback("signed", nid, true);
        JsonNode d = details(nid);
        assertThat(d.path("status").asText()).isEqualTo("SIGNED");
        assertThat(d.path("consumerSignatureDate").isNull()).isFalse();
        mvc.perform(key(patch("/contractOffers/" + nid + "/accept"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:offerNotPending"));
        JsonNode ownVc = StubMocks.M.readTree(mvc.perform(key(get("/verifications/" + nid + "/vc")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        ObjectNode subject = ((ObjectNode) ownVc.get("credentialSubject")).deepCopy();
        subject.put("signerRole", "PROVIDER");
        subject.put("signedAt", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        ObjectNode providerVc = StubMocks.vc(subject, StubMocks.PROVIDER_DID, Instant.now().minusSeconds(5).toString(),
                Instant.now().plusSeconds(3600).toString());
        mvc.perform(key(put("/verifications/" + nid)).contentType(MediaType.APPLICATION_JSON)
                .content(providerVc.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIATED"));
        verify(stub, timeout(5000)).callback("isValid", nid, true);
        assertThat(details(nid).path("status").asText()).isEqualTo("ACTIVE");
        assertThat(details(nid).path("providerSignatureDate").isNull()).isFalse();
    }

    @Test
    void rejectExpiryAndWalletFailure() throws Exception {
        String n1 = UUID.randomUUID().toString();
        create(n1, UUID.randomUUID().toString(), "asset-1");
        mvc.perform(key(patch("/contractOffers/" + n1 + "/reject"))).andExpect(status().isOk());
        assertThat(details(n1).path("status").asText()).isEqualTo("REJECTED");
        verify(stub, timeout(5000)).callback("signed", n1, false);

        String n2 = UUID.randomUUID().toString();
        create(n2, UUID.randomUUID().toString(), "asset-1");
        mvc.perform(key(patch("/contractOffers/" + n2 + "/reject?reason=EXPIRED"))).andExpect(status().isOk());
        JsonNode d2 = details(n2);
        assertThat(d2.path("status").asText()).isEqualTo("TERMINATED");
        assertThat(d2.path("reasonCode").asText()).isEqualTo("OFFER_EXPIRED");

        String n3 = UUID.randomUUID().toString();
        create(n3, UUID.randomUUID().toString(), "asset-1");
        when(stub.walletPut(eq(n3), anyString(), any())).thenReturn(new HttpResult(507, null));
        mvc.perform(key(patch("/contractOffers/" + n3 + "/accept"))).andExpect(status().isOk());
        verify(stub, timeout(5000)).callback("signed", n3, false);
        JsonNode d3 = details(n3);
        assertThat(d3.path("status").asText()).isEqualTo("FAILED");
        assertThat(d3.path("reasonCode").asText()).isEqualTo("WALLET_FULL");
        assertThat(d3.path("history").toString()).contains("Secure storage location is full");

        String n4 = UUID.randomUUID().toString();
        create(n4, UUID.randomUUID().toString(), "asset-1");
        mvc.perform(key(put("/contractOffers/" + n4 + "/update")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SIGNED\"}")).andExpect(status().isBadRequest());
        mvc.perform(key(put("/contractOffers/" + n4 + "/update")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\",\"additionalInfo\":\"Provider failed\"}"))
                .andExpect(status().isOk());
        assertThat(details(n4).path("reasonCode").asText()).isEqualTo("PROVIDER_REJECTED");
        mvc.perform(key(put("/verifications/" + n4)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
