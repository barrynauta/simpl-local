package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Access rules (D16, D17, D84) and the order of the protocol checks (D51, D63, D67, D139). */
class AccessAndProtocolTest extends ApiTestBase {

    @Test
    void tokenAndRoles() throws Exception {
        assertThat(get("/v1/vocabularies", null).statusCode()).isEqualTo(401);
        HttpResponse<String> r = get("/v1/vocabularies", "not-a-jwt");
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(body(r).get("type").asString()).isEqualTo("urn:problem-type:simpl:unauthorized");
        assertThat(send("GET", "/v1/vocabularies", null, Map.of("Authorization", "Basic abc"), null)
                        .statusCode())
                .isEqualTo(401);
        HttpResponse<String> forbidden = get("/v1/vocabularies", norole);
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(body(forbidden).get("type").asString()).isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(get("/v1/vocabularies", viewer).statusCode()).isEqualTo(200);
        assertThat(multipart("/v1/vocabularies", viewer, fields("name", "Abc"), null)
                        .statusCode())
                .isEqualTo(403);
        assertThat(json("DELETE", "/v1/external-vocabularies/Abcd", viewer, "").statusCode())
                .isEqualTo(403);
        String wrongAudience = IDP.token(
                "ga-admin", TestIdp.ISSUER, "other", IDP.key(), Instant.now().plusSeconds(60), "GA_VOCABULARY_ADMIN");
        assertThat(get("/v1/vocabularies", wrongAudience).statusCode()).isEqualTo(401);
        String wrongIssuer = IDP.token(
                "ga-admin",
                "http://other/realms/x",
                TestIdp.AUDIENCE,
                IDP.key(),
                Instant.now().plusSeconds(60),
                "GA_VOCABULARY_ADMIN");
        assertThat(get("/v1/vocabularies", wrongIssuer).statusCode()).isEqualTo(401);
        String expired = IDP.token(
                "ga-admin",
                TestIdp.ISSUER,
                TestIdp.AUDIENCE,
                IDP.key(),
                Instant.now().minusSeconds(3600),
                "GA_VOCABULARY_ADMIN");
        assertThat(get("/v1/vocabularies", expired).statusCode()).isEqualTo(401);
        // role check before body checks (D17)
        assertThat(json("PATCH", "/v1/vocabularies/abc", viewer, "garbage").statusCode())
                .isEqualTo(403);
    }

    @Test
    void methodsAndUnknownPaths() throws Exception {
        HttpResponse<String> put =
                send("PUT", "/v1/vocabularies", null, Map.of(), HttpRequest.BodyPublishers.ofString("x"));
        assertThat(put.statusCode()).isEqualTo(405);
        assertThat(put.headers().firstValue("Allow")).contains("GET, POST");
        assertThat(send("QUERY", "/v1/vocabularies", admin, Map.of(), null).statusCode())
                .isEqualTo(405);
        assertThat(send("OPTIONS", "/v1/vocabularies", admin, Map.of(), null).statusCode())
                .isEqualTo(405);
        assertThat(send("DELETE", "/v1/vocabularies/Abc", admin, Map.of(), null).statusCode())
                .isEqualTo(405);
        HttpResponse<String> searches = get("/v1/vocabularies/searches", admin);
        assertThat(searches.statusCode()).isEqualTo(405);
        assertThat(searches.headers().firstValue("Allow")).contains("POST");
        assertThat(get("/v1/unknown", null).statusCode()).isEqualTo(401);
        assertThat(get("/v1/unknown", admin).statusCode()).isEqualTo(404);
        assertThat(get("/actuator/health", admin).statusCode()).isEqualTo(404);
        assertThat(get("/error", admin).statusCode()).isEqualTo(404);
        assertThat(send("PATCH", "/v1/nothing", admin, Map.of(), null).statusCode())
                .isEqualTo(404);
    }

    @Test
    void negotiationOrder() throws Exception {
        HttpResponse<String> notAcceptable =
                send("GET", "/v1/vocabularies", viewer, Map.of("Accept", "text/html"), null);
        assertThat(notAcceptable.statusCode()).isEqualTo(406);
        assertThat(body(notAcceptable).get("type").asString()).isEqualTo("urn:problem-type:simpl:notAcceptable");
        for (String a : new String[] {
            "application/json", "application/problem+json", "*/*", "application/*", "text/html, application/json;q=0.5"
        }) {
            assertThat(send("GET", "/v1/vocabularies?pageSize=1", viewer, Map.of("Accept", a), null)
                            .statusCode())
                    .as(a)
                    .isEqualTo(200);
        }
        assertThat(send("GET", "/v1/content/Abcd", viewer, Map.of("Accept", "application/json"), null)
                        .statusCode())
                .isEqualTo(406);
        assertThat(send("GET", "/v1/content/Abcd", viewer, Map.of("Accept", "text/*"), null)
                        .statusCode())
                .isEqualTo(404);
        assertThat(send("GET", "/v1/vocabularies", viewer, Map.of("Accept", "application/json;q=0"), null)
                        .statusCode())
                .isEqualTo(406);
        // 406 before 415 (D63)
        HttpResponse<String> both = send(
                "PATCH",
                "/v1/vocabularies/Abcd",
                admin,
                Map.of("Accept", "text/html", "Content-Type", "text/plain"),
                HttpRequest.BodyPublishers.ofString("x"));
        assertThat(both.statusCode()).isEqualTo(406);
        HttpResponse<String> unsupported = send(
                "PATCH",
                "/v1/vocabularies/Abcd",
                admin,
                Map.of("Content-Type", "text/plain"),
                HttpRequest.BodyPublishers.ofString("x"));
        assertThat(unsupported.statusCode()).isEqualTo(415);
        assertThat(send(
                                "POST",
                                "/v1/vocabularies",
                                admin,
                                Map.of("Content-Type", "application/json"),
                                HttpRequest.BodyPublishers.ofString("{}"))
                        .statusCode())
                .isEqualTo(415);
        assertThat(send("POST", "/v1/vocabularies/searches", admin, Map.of(), HttpRequest.BodyPublishers.ofString("{}"))
                        .statusCode())
                .isEqualTo(415);
        // 415 before 413 before 400 (D51)
        HttpResponse<String> large =
                json("PATCH", "/v1/vocabularies/abc", admin, "{\"a\":\"" + "x".repeat(1_100_000) + "\"}");
        assertThat(large.statusCode()).isEqualTo(413);
        assertThat(json("PATCH", "/v1/vocabularies/abc", admin, "{\"status\":\"ACTIVE\"}")
                        .statusCode())
                .isEqualTo(400);
    }

    @Test
    void healthNeedsNoToken() throws Exception {
        HttpResponse<String> h = get("/v1/health", null);
        assertThat(h.statusCode()).isEqualTo(200);
        assertThat(body(h).get("status").asString()).isIn("UP", "DEGRADED");
        assertThat(get("/v1/health?x=1", null).statusCode()).isEqualTo(400);
        assertThat(h.headers().firstValue("X-Correlation-Id")).isPresent();
        HttpResponse<String> c = send("GET", "/v1/health", null, Map.of("X-Correlation-Id", "abc-123"), null);
        assertThat(c.headers().firstValue("X-Correlation-Id")).contains("abc-123");
    }
}
