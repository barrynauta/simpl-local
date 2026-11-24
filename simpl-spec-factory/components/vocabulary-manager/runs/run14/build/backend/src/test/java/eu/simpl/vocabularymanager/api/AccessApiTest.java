package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Access rules and protocol answers (SIMPL-18196, D16, D17, D49, D51, D63, D67, D84, D126, D139, D162, D172). */
class AccessApiTest extends ApiTestBase {

    @Test
    void tokenAndRoleChecks() {
        assertThat(get("/v1/vocabularies", null).status()).isEqualTo(401);
        Reply unauthorized = get("/v1/vocabularies", null);
        assertThat(unauthorized.type()).isEqualTo("urn:problem-type:simpl:unauthorized");
        assertThat(unauthorized.header("Content-Type")).isEqualTo("application/problem+json");
        assertThat(unauthorized.header("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(get("/v1/vocabularies", "garbage").status()).isEqualTo(401);
        assertThat(send("GET", "/v1/vocabularies", null, null, null, "Authorization", "Basic abc")
                        .status())
                .isEqualTo(401);
        assertThat(get(
                                "/v1/vocabularies",
                                IDP.token(
                                        "http://evil/realms/simpl",
                                        "vocabulary-manager-be",
                                        Instant.now().plusSeconds(60),
                                        "GA_VOCABULARY_ADMIN"))
                        .status())
                .isEqualTo(401);
        assertThat(get(
                                "/v1/vocabularies",
                                IDP.token(
                                        TestIdp.ISSUER, "other", Instant.now().plusSeconds(60), "GA_VOCABULARY_ADMIN"))
                        .status())
                .isEqualTo(401);
        assertThat(get(
                                "/v1/vocabularies",
                                IDP.token(
                                        TestIdp.ISSUER,
                                        "vocabulary-manager-be",
                                        Instant.now().minusSeconds(5),
                                        "GA_VOCABULARY_ADMIN"))
                        .status())
                .isEqualTo(401);
        Reply forbidden = get("/v1/vocabularies", norole);
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.type()).isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(json("PATCH", "/v1/vocabularies/Abc", viewer, "{}").status()).isEqualTo(403);
        assertThat(send("DELETE", "/v1/external-vocabularies/Abc", viewer, null, null)
                        .status())
                .isEqualTo(403);
        assertThat(get("/v1/vocabularies", admin).status()).isEqualTo(200);
    }

    @Test
    void routingAnswers() {
        Reply put = send("PUT", "/v1/vocabularies", null, null, null);
        assertThat(put.status()).isEqualTo(405);
        assertThat(put.header("Allow")).isEqualTo("GET, POST");
        assertThat(send("HEAD", "/v1/vocabularies/Abc", null, null, null).header("Allow"))
                .isEqualTo("GET, PATCH");
        assertThat(send("OPTIONS", "/v1/vocabularies/searches", null, null, null)
                        .header("Allow"))
                .isEqualTo("POST");
        assertThat(get("/v1/external-vocabularies/content", viewer).status()).isEqualTo(405);
        Reply query = send("QUERY", "/v1/nothing", null, null, null);
        assertThat(query.status()).isEqualTo(405);
        assertThat(query.header("Allow")).isNull();
        assertThat(send("TRACE", "/v1/nothing", null, null, null).status()).isEqualTo(405);
        assertThat(get("/v1/nothing", null).status()).isEqualTo(401);
        assertThat(get("/v1/nothing", viewer).status()).isEqualTo(404);
        assertThat(send("HEAD", "/v1/nothing", viewer, null, null).status()).isEqualTo(404);
        assertThat(get("/actuator/health", viewer).status()).isEqualTo(404);
        assertThat(get("/v1/vocabularies/", viewer).status()).isEqualTo(404);
        assertThat(get("/v1/vocabularies/A%2Fb", viewer).status()).isEqualTo(400);
        assertThat(get("/v1/vocabularies/Abc;x=1", viewer).status()).isEqualTo(400);
        assertThat(get("/v1/vocabularies/abc", viewer)
                        .json()
                        .path("issues")
                        .get(0)
                        .path("in")
                        .asString())
                .isEqualTo("path");
        assertThat(get("/v1/vocabularies/Abc/versions/x", viewer)
                        .json()
                        .path("issues")
                        .get(0)
                        .path("name")
                        .asString())
                .isEqualTo("version");
        assertThat(get("/v1/vocabularies/Abc/versions/99999999999", viewer).status())
                .isEqualTo(400);
        Reply notFound = get("/v1/vocabularies/Nowhere" + uniqueName("X"), viewer);
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.json().path("issues").get(0).path("name").asString())
                .isEqualTo("vocabularyName");
    }

    @Test
    void negotiationAndOrder() {
        assertThat(get("/v1/vocabularies", viewer, "Accept", "text/html").status())
                .isEqualTo(406);
        assertThat(get("/v1/vocabularies", viewer, "Accept", "application/problem+json")
                        .status())
                .isEqualTo(200);
        assertThat(get("/v1/vocabularies", viewer, "Accept", "application/*").header("Content-Type"))
                .isEqualTo("application/json");
        assertThat(get("/v1/vocabularies", viewer, "Accept", "text/html, */*;q=0.1")
                        .status())
                .isEqualTo(200);
        assertThat(get("/v1/vocabularies", viewer, "Accept", "application/json;q=0")
                        .status())
                .isEqualTo(406);
        assertThat(get("/v1/content/Abc", viewer, "Accept", "application/json").status())
                .isEqualTo(406);
        assertThat(get("/v1/content/abc", norole, "Accept", "text/html").status())
                .isEqualTo(403);
        assertThat(get("/v1/content/abc", viewer, "Accept", "text/html").status())
                .isEqualTo(406);
        assertThat(json("PATCH", "/v1/vocabularies/abc", viewer, "{}").status()).isEqualTo(403);
        assertThat(send("PATCH", "/v1/vocabularies/abc", admin, "text/plain", "x".getBytes())
                        .status())
                .isEqualTo(415);
        assertThat(send("PATCH", "/v1/vocabularies/abc", admin, null, "x".getBytes())
                        .status())
                .isEqualTo(415);
        assertThat(send("POST", "/v1/vocabularies", admin, "multipart/mixed; boundary=x", "x".getBytes())
                        .status())
                .isEqualTo(415);
        assertThat(send("POST", "/v1/vocabularies", admin, "multipart/form-data", "x".getBytes())
                        .status())
                .isEqualTo(400);
        assertThat(json("PATCH", "/v1/vocabularies/abc", admin, "{\"status\":\"x\"}")
                        .json()
                        .path("issues")
                        .get(0)
                        .path("in")
                        .asString())
                .isEqualTo("path");
        assertThat(json(
                                "PATCH",
                                "/v1/vocabularies/Abc",
                                admin,
                                "{\"status\":\"ACTIVE\"," + "\"pad\":\"" + "x".repeat(1_100_000) + "\"}")
                        .status())
                .isEqualTo(413);
        assertThat(send("DELETE", "/v1/external-vocabularies/Abc?x=1", admin, null, null)
                        .status())
                .isEqualTo(400);
    }

    @Test
    void healthNeedsNoTokenAndKeepsItsRules() {
        Reply health = get("/v1/health", null);
        assertThat(health.status()).isEqualTo(200);
        assertThat(health.json().path("status").asString()).isIn("UP", "DEGRADED");
        assertThat(get("/v1/health?x=1", null).status()).isEqualTo(400);
        assertThat(health.header("X-Correlation-Id")).isNotBlank();
        assertThat(get("/v1/health", null, "X-Correlation-Id", "abc-123").header("X-Correlation-Id"))
                .isEqualTo("abc-123");
    }

    @org.springframework.beans.factory.annotation.Autowired
    private eu.simpl.vocabularymanager.service.UploadSlots uploadSlots;

    @Test
    void fifthUploadGets503BeforeTheBodyIsRead() {
        int taken = 0;
        while (uploadSlots.tryAcquire()) {
            taken++;
        }
        try {
            assertThat(taken).isEqualTo(4);
            Reply busy = post("/v1/vocabularies", admin, new Form().field("name", uniqueName("Busy")));
            assertThat(busy.status()).isEqualTo(503);
            assertThat(busy.header("Retry-After")).isEqualTo("5");
            assertThat(busy.json().path("detail").asString()).isEqualTo("Too many uploads in progress. Retry later.");
            assertThat(get("/v1/vocabularies", viewer).status()).isEqualTo(200);
        } finally {
            for (int i = 0; i < taken; i++) {
                uploadSlots.release();
            }
        }
        assertThat(uploadSlots.available()).isEqualTo(4);
    }

    @Test
    void unknownSigningKeyIsRejected() {
        assertThat(get("/v1/vocabularies", IDP.tokenWithUnknownKey("GA_VOCABULARY_ADMIN"))
                        .status())
                .isEqualTo(401);
    }

    /** Constants of the test identity provider. */
    private static final class TestIdp {
        static final String ISSUER = eu.simpl.vocabularymanager.support.TestIdentityProvider.ISSUER;
    }
}
