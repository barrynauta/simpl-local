package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stub contract (Appendix C): issue, verify, faults, wallet, directory. */
@SpringBootTest
@AutoConfigureMockMvc
class StubIT {
    static final ObjectMapper M = new ObjectMapper();
    @Autowired
    MockMvc mvc;

    JsonNode issue(String participant, String nid) throws Exception {
        String body = "{\"participantId\":\"" + participant + "\",\"credentialSubject\":{\"contractNegotiationId\":\"" + nid + "\",\"assetId\":\"a\"}}";
        String res = mvc.perform(post("/vc-issuer/v1/credentials/issue").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return M.readTree(res);
    }

    String verify(JsonNode vc) throws Exception {
        String res = mvc.perform(post("/vc-issuer/v1/credentials/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":" + vc + "}")).andReturn().getResponse().getContentAsString();
        JsonNode j = M.readTree(res);
        return j.path("problems").isEmpty() ? "OK" : j.path("problems").get(0).asText();
    }

    @Test
    void issueAndVerify() throws Exception {
        JsonNode vc = issue("consumer-01", "n1");
        assertThat(vc.path("issuer").asText()).isEqualTo("did:key:z6MkqGC3nWZhYieEVTVDKW5v588CiGfsDSmRVG9ZwwWTvLSK");
        assertThat(vc.at("/proof/cryptosuite").asText()).isEqualTo("eddsa-jcs-2022");
        assertThat(verify(vc)).isEqualTo("OK");
        ObjectNode tampered = vc.deepCopy();
        ((ObjectNode) tampered.get("credentialSubject")).put("assetId", "b");
        assertThat(verify(tampered)).isEqualTo("INVALID_SIGNATURE");
        ObjectNode noProof = vc.deepCopy();
        noProof.remove("proof");
        assertThat(verify(noProof)).isEqualTo("MALFORMED");
        assertThat(verify(issue("rogue-01", "n1"))).isEqualTo("UNKNOWN_ISSUER");
        mvc.perform(post("/_control/faults").contentType(MediaType.APPLICATION_JSON)
                .content("{\"contractNegotiationId\":\"n2\",\"service\":\"VERIFIER\",\"mode\":\"REVOKED\"}")).andExpect(status().isCreated());
        assertThat(verify(issue("provider-01", "n2"))).isEqualTo("REVOKED_ISSUER");
    }

    @Test
    void faultsAndWallet() throws Exception {
        mvc.perform(post("/_control/faults").contentType(MediaType.APPLICATION_JSON)
                .content("{\"contractNegotiationId\":\"n3\",\"service\":\"WALLET\",\"mode\":\"FULL\"}")).andExpect(status().isCreated());
        JsonNode vc = issue("consumer-01", "n3");
        mvc.perform(put("/wallet/v1/consumer-01/credentials/x").header("X-Contract-Negotiation-Id", "n3")
                .contentType(MediaType.APPLICATION_JSON).content(vc.toString())).andExpect(status().is(507));
        mvc.perform(put("/wallet/v1/consumer-01/credentials/x").header("X-Contract-Negotiation-Id", "n4")
                .contentType(MediaType.APPLICATION_JSON).content(issue("consumer-01", "n4").toString())).andExpect(status().isCreated());
        mvc.perform(get("/wallet/v1/consumer-01/credentials").param("contractNegotiationId", "n4"))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/edc/consumer-01/signed/n4/true")).andExpect(status().isUnauthorized());
        mvc.perform(post("/edc/consumer-01/signed/n4/true").header("X-Api-Key", "test-edc-key")).andExpect(status().isNoContent());
        mvc.perform(get("/directory/v1/participants/rogue-01")).andExpect(jsonPath("$.contactEmail").isEmpty());
        mvc.perform(get("/catalogue/v1/assets/asset-seed-01")).andExpect(jsonPath("$.consentRequired").value(true));
        mvc.perform(get("/_control/calls").param("contractNegotiationId", "n4")).andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
