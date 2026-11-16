package eu.simpl.cm.uibe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UIBE with real signed JWTs (mocked realm key) and a fake CM over HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
class UibeApiTest {

    static final ObjectMapper M = new ObjectMapper();
    static final KeyPair KEYS;
    static final HttpServer CM;
    static final List<String> CM_CALLS = new CopyOnWriteArrayList<>();
    static final AtomicInteger CM_PATCH_STATUS = new AtomicInteger(200);

    static {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            KEYS = g.generateKeyPair();
            CM = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            CM.createContext("/", ex -> {
                String call = ex.getRequestMethod() + " " + ex.getRequestURI() + " actor=" + ex.getRequestHeaders()
                        .getFirst("X-Actor") + " key=" + ex.getRequestHeaders().getFirst("X-Api-Key");
                CM_CALLS.add(call);
                int code = 200;
                String body;
                String path = ex.getRequestURI().getPath();
                if (ex.getRequestMethod().equals("PATCH")) {
                    code = CM_PATCH_STATUS.get();
                    body = code == 200 ? "{\"contractNegotiationId\":\"x\",\"status\":\"INITIATED\"}"
                            : "{\"type\":\"urn:problem-type:simpl:contract-management:offerNotPending\","
                            + "\"title\":\"Bad Request\",\"status\":" + code + ",\"detail\":\"not pending\"}";
                } else if (path.matches(".*/contracts/[^/]+")) {
                    String nid = path.substring(path.lastIndexOf('/') + 1);
                    body = "{\"contractNegotiationId\":\"" + nid + "\",\"status\":\"ACTIVE\",\"partyRole\":\"PROVIDER\","
                            + "\"allowedActions\":[\"SUSPEND\",\"UPDATE_CONSENT\"],\"history\":[]}";
                } else {
                    body = "{\"items\":[]}";
                }
                byte[] b = body.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", code == 200 ? "application/json"
                        : "application/problem+json");
                ex.sendResponseHeaders(code, b.length);
                ex.getResponseBody().write(b);
                ex.close();
            });
            CM.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("uibe.cm-base-url", () -> "http://127.0.0.1:" + CM.getAddress().getPort() + "/contract/v1");
    }

    @AfterAll
    static void stop() {
        CM.stop(0);
    }

    @TestConfiguration
    static class Keys {
        @Bean
        @Primary
        JwtDecoder testDecoder() {
            NimbusJwtDecoder d = NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEYS.getPublic()).build();
            d.setJwtValidator(JwtValidators.createDefaultWithIssuer("http://localhost:19080/realms/consumer"));
            return d;
        }
    }

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() {
        CM_CALLS.clear();
        CM_PATCH_STATUS.set(200);
    }

    static String token(String issuer, String participant, String user, String... roles) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).subject(UUID.randomUUID().toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(600))).issueTime(new Date())
                .claim("participant_id", participant).claim("preferred_username", user)
                .claim("realm_access", Map.of("roles", List.of(roles))).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(KEYS.getPrivate()));
        return "Bearer " + jwt.serialize();
    }

    static String admin() throws Exception {
        return token("http://localhost:19080/realms/consumer", "consumer-01", "consumer-admin", "contract_manager");
    }

    private void offer(String nid) throws Exception {
        mvc.perform(post("/contractOffers/" + nid).header("x-api-key", "uibe-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"humanReadable\":\"<p>x</p>\",\"hash\":\"h\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.offerStatus").value("CREATED"))
                .andExpect(jsonPath("$.offerUrl").value("http://localhost:19203/contractOffers/" + nid));
    }

    @Test
    void healthIsOpen() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void tokensAndRoles() throws Exception {
        mvc.perform(get("/contractOffers")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:unauthorized"));
        mvc.perform(get("/contractOffers").header("x-api-key", "uibe-key")).andExpect(status().isUnauthorized());
        mvc.perform(get("/contractOffers").header("Authorization",
                token("http://localhost:19080/realms/provider", "consumer-01", "x", "contract_manager")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/contractOffers").header("Authorization",
                token("http://localhost:19080/realms/consumer", "consumer-01", "consumer-norole")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/contractOffers").header("Authorization",
                token("http://localhost:19080/realms/consumer", "provider-01", "x", "contract_manager")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/contractOffers").header("Authorization", admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void machineOperationsNeedTheKey() throws Exception {
        mvc.perform(post("/contractOffers/n1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"humanReadable\":\"x\",\"hash\":\"h\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/contractOffers/n1").header("x-api-key", "uibe-key").contentType(MediaType.APPLICATION_JSON)
                .content("{\"hash\":\"h\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/admin/offerExpiry/run").header("x-api-key", "wrong")).andExpect(status().isUnauthorized());
    }

    @Test
    void offerFieldsAndDecision() throws Exception {
        String nid = UUID.randomUUID().toString();
        offer(nid);
        mvc.perform(get("/contractOffers/" + nid).header("Authorization", admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.contractNegotiationId").value(nid))
                .andExpect(jsonPath("$.humanReadableData").value("<p>x</p>"))
                .andExpect(jsonPath("$.hashData").value("h"))
                .andExpect(jsonPath("$.contractData").isEmpty())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.createdDate").isString());
        CM_PATCH_STATUS.set(400);
        mvc.perform(patch("/contractOffers/" + nid + "/accept").header("Authorization", admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:simpl:contract-management:offerNotPending"));
        mvc.perform(get("/contractOffers/" + nid).header("Authorization", admin()))
                .andExpect(jsonPath("$.status").value("CREATED"));
        CM_PATCH_STATUS.set(200);
        mvc.perform(patch("/contractOffers/" + nid + "/accept").header("Authorization", admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        assertThat(CM_CALLS).anyMatch(c -> c.startsWith("PATCH /contract/v1/contractOffers/" + nid + "/accept")
                && c.contains("actor=consumer-admin") && c.contains("key=cm-key"));
        mvc.perform(patch("/contractOffers/" + nid + "/accept").header("Authorization", admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/contractOffers/" + nid + "/approve").header("Authorization", admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/contractOffers/unknown-1/accept").header("Authorization", admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailsFilterConsentAction() throws Exception {
        String withRole = token("http://localhost:19080/realms/consumer", "consumer-01", "a", "contract_manager",
                "consent_manager");
        JsonNode d = M.readTree(mvc.perform(get("/contracts/n-7").header("Authorization", withRole))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(d.path("allowedActions").toString()).contains("UPDATE_CONSENT");
        assertThat(d.path("consentManagerUrl").asText())
                .isEqualTo("http://localhost:19090/consent?contractNegotiationId=n-7");
        assertThat(d.has("offerStatus")).isTrue();
        JsonNode d2 = M.readTree(mvc.perform(get("/contracts/n-7").header("Authorization", admin()))
                .andReturn().getResponse().getContentAsString());
        assertThat(d2.path("allowedActions").toString()).doesNotContain("UPDATE_CONSENT");
        assertThat(d2.has("consentManagerUrl")).isFalse();
        mvc.perform(get("/contracts").param("counterpartyName", "Prov").header("Authorization", admin()))
                .andExpect(status().isOk());
        assertThat(CM_CALLS).anyMatch(c -> c.contains("/contract/v1/contracts?providerName=Prov"));
    }

    @Test
    void expiryRun() throws Exception {
        String old = UUID.randomUUID().toString();
        offer(old);
        String asOf = Instant.now().plusSeconds(7 * 86400 + 60).toString();
        String r = mvc.perform(post("/admin/offerExpiry/run").header("x-api-key", "uibe-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asOf\":\"" + asOf + "\",\"contractNegotiationIds\":[\"" + old + "\",\"nope\"]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode j = M.readTree(r);
        assertThat(j.path("expired").toString()).contains(old);
        assertThat(j.path("skipped").toString()).contains("nope");
        assertThat(CM_CALLS).anyMatch(c -> c.startsWith("PATCH /contract/v1/contractOffers/" + old
                + "/reject?reason=EXPIRED"));
        mvc.perform(get("/contractOffers/" + old).header("Authorization", admin()))
                .andExpect(jsonPath("$.status").value("TERMINATED"));
        String recent = UUID.randomUUID().toString();
        offer(recent);
        JsonNode j2 = M.readTree(mvc.perform(post("/admin/offerExpiry/run").header("x-api-key", "uibe-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractNegotiationIds\":[\"" + recent + "\"]}"))
                .andReturn().getResponse().getContentAsString());
        assertThat(j2.path("skipped").toString()).contains(recent);
        CM_PATCH_STATUS.set(500);
        String failing = UUID.randomUUID().toString();
        offer(failing);
        JsonNode j3 = M.readTree(mvc.perform(post("/admin/offerExpiry/run").header("x-api-key", "uibe-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asOf\":\"" + asOf + "\",\"contractNegotiationIds\":[\"" + failing + "\"]}"))
                .andReturn().getResponse().getContentAsString());
        assertThat(j3.path("failed").toString()).contains(failing);
        mvc.perform(get("/contractOffers/" + failing).header("Authorization", admin()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }
}
