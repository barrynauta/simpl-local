package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.Api;
import eu.simpl.vocabularymanager.support.Api.Part;
import eu.simpl.vocabularymanager.support.Api.Result;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import eu.simpl.vocabularymanager.support.TestEnvironment;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Access rules (D16, D17, D44, D84, E5, E7) and the checks before an operation runs (D51, D52, D63, D67, D76). */
class SecurityAndShapeApiTest extends IntegrationTestBase {

    @Test
    void healthNeedsNoTokenAndReportsUp() {
        Result health = api.get("/v1/health", null);
        assertThat(health.status()).isEqualTo(200);
        assertThat(health.json().path("status").asString()).isEqualTo("UP");
        assertThat(api.get("/v1/health?x=1", null).status()).isEqualTo(400);
    }

    @Test
    void missingOrInvalidTokensGive401() {
        Result none = api.get("/v1/vocabularies", null);
        assertThat(none.status()).isEqualTo(401);
        assertThat(none.contentType()).startsWith("application/problem+json");
        assertThat(none.json().path("type").asString()).isEqualTo("urn:problem-type:simpl:unauthorized");
        assertThat(none.json().path("title").asString()).isEqualTo("Unauthorized");
        assertThat(none.header("WWW-Authenticate")).startsWith("Bearer");

        assertThat(api.get("/v1/vocabularies", "not-a-jwt").status()).isEqualTo(401);
        assertThat(api.send("GET", "/v1/vocabularies", null, null, null, "Authorization", "Basic abc").status())
                .isEqualTo(401);
        assertThat(api.send("GET", "/v1/vocabularies", null, null, null, "Authorization", "Bearer").status())
                .isEqualTo(401);
        String wrongAudience = ENV.token("ga-admin", "ga-admin@example.com", TestEnvironment.ISSUER,
                "other-client", Instant.now().plusSeconds(300), ENV.key(), TestEnvironment.ADMIN_ROLE);
        assertThat(api.get("/v1/vocabularies", wrongAudience).status()).isEqualTo(401);
        String wrongIssuer = ENV.token("ga-admin", "ga-admin@example.com", "http://idp.test/realms/simpl-other",
                TestEnvironment.AUDIENCE, Instant.now().plusSeconds(300), ENV.key(), TestEnvironment.ADMIN_ROLE);
        assertThat(api.get("/v1/vocabularies", wrongIssuer).status()).isEqualTo(401);
        String expired = ENV.token("ga-admin", "ga-admin@example.com", TestEnvironment.ISSUER,
                TestEnvironment.AUDIENCE, Instant.now().minusSeconds(600), ENV.key(), TestEnvironment.ADMIN_ROLE);
        assertThat(api.get("/v1/vocabularies", expired).status()).isEqualTo(401);
        assertThat(api.get("/v1/vocabularies", ENV.tokenWithUnknownKey()).status()).isEqualTo(401);
        // 401 comes before body checks (D17)
        assertThat(api.multipart("/v1/vocabularies", null, Part.field("x", "y")).status()).isEqualTo(401);
    }

    @Test
    void rolesDecideReadAndWrite() {
        Result noRole = api.get("/v1/vocabularies", ENV.noRole());
        assertThat(noRole.status()).isEqualTo(403);
        assertThat(noRole.json().path("type").asString()).isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(noRole.json().path("title").asString()).isEqualTo("Forbidden");
        assertThat(api.get("/v1/external-vocabularies", ENV.noRole()).status()).isEqualTo(403);
        assertThat(api.json("POST", "/v1/vocabularies/searches", ENV.noRole(), "{}").status()).isEqualTo(403);

        String id = Api.unique();
        Result viewerCreate = api.multipart("/v1/vocabularies", ENV.viewer(), Part.field("name", "Viewer" + id));
        assertThat(viewerCreate.status()).isEqualTo(403);
        assertThat(api.json("PATCH", "/v1/vocabularies/Abc", ENV.viewer(), "{\"status\":\"ACTIVE\"}").status())
                .isEqualTo(403);
        assertThat(api.json("PATCH", "/v1/vocabularies/Abc/versions/1", ENV.viewer(), "{}").status())
                .isEqualTo(403);
        assertThat(api.delete("/v1/external-vocabularies/Abc", ENV.viewer()).status()).isEqualTo(403);
        assertThat(api.multipart("/v1/external-vocabularies", ENV.viewer(), Part.field("name", "Abc")).status())
                .isEqualTo(403);
        assertThat(api.json("POST", "/v1/external-vocabulary/searches", ENV.viewer(),
                        "{\"namespaceUri\":\"https://x.example/\"}").status())
                .isEqualTo(200);
        // A token without an email claim: the creator is preferred_username (D27).
        String noEmail = ENV.token("nomail-admin", null, TestEnvironment.ISSUER, TestEnvironment.AUDIENCE,
                Instant.now().plusSeconds(300), ENV.key(), TestEnvironment.ADMIN_ROLE);
        Result created = api.multipart("/v1/vocabularies", noEmail, Part.field("name", "NoMail" + id),
                Part.field("description", "d"), Part.field("changelog", "c"), Part.file(simpleTurtle(id)));
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.json().path("createdBy").asString()).isEqualTo("nomail-admin");
    }

    @Test
    void identityProviderDownFailsClosedForUnknownKeys() {
        String token = ENV.admin();
        ENV.identityProviderUp(false);
        try {
            // Cached key keeps working (D44).
            assertThat(api.get("/v1/vocabularies", token).status()).isEqualTo(200);
            Result unknownKey = api.get("/v1/vocabularies", ENV.tokenWithUnknownKey());
            assertThat(unknownKey.status()).isEqualTo(503);
            assertThat(unknownKey.json().path("detail").asString())
                    .isEqualTo("The identity provider is not available. Retry later.");
            assertThat(unknownKey.header("Retry-After")).isEqualTo("5");
            assertThat(unknownKey.json().path("type").asString())
                    .isEqualTo("urn:problem-type:belgif:serviceUnavailable");
        } finally {
            ENV.identityProviderUp(true);
        }
    }

    @Test
    void unknownPathsAndMethods() {
        assertThat(api.get("/v1/nothing", null).status()).isEqualTo(401);
        Result unknown = api.get("/v1/nothing", ENV.admin());
        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.contentType()).startsWith("application/problem+json");
        assertThat(api.get("/actuator/health", ENV.admin()).status()).isEqualTo(404);
        assertThat(api.get("/actuator/health", null).status()).isEqualTo(401);
        Result put = api.send("PUT", "/v1/vocabularies", ENV.admin(), null, null);
        assertThat(put.status()).isEqualTo(405);
        assertThat(put.contentType()).startsWith("application/problem+json");
        assertThat(put.header("Allow")).contains("GET").contains("POST");
        assertThat(api.get("/v1/vocabularies/searches", null).status()).isEqualTo(405);
        assertThat(api.delete("/v1/vocabularies/Abc", ENV.admin()).status()).isEqualTo(405);
        Result query = api.send("QUERY", "/v1/vocabularies", null, null, null);
        assertThat(query.status()).isEqualTo(405);
        assertThat(query.header("Allow")).contains("GET");
        assertThat(api.send("QUERY", "/v1/nothing", null, null, null).status()).isEqualTo(405);
        Result slash = api.get("/v1/vocabularies/a%2Fb", null);
        assertThat(slash.status()).isEqualTo(400);
        assertThat(slash.json().path("type").asString()).isEqualTo("urn:problem-type:belgif:badRequest");
        assertThat(slash.json().has("issues")).isFalse();
    }

    @Test
    void tomcatLevelErrorsHaveProblemBodies() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            OutputStream out = socket.getOutputStream();
            out.write("GET /v1/voc\u0001abularies HTTP/1.1\r\nHost: x\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            InputStream in = socket.getInputStream();
            String response = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(response).startsWith("HTTP/1.1 400");
            assertThat(response).contains("application/problem+json").contains("urn:problem-type:belgif:badRequest");
        }
    }

    @Test
    void mediaTypesAndAcceptHeader() {
        Result notAcceptable = api.get("/v1/vocabularies", ENV.viewer(), "Accept", "text/html");
        assertThat(notAcceptable.status()).isEqualTo(406);
        assertThat(notAcceptable.json().path("type").asString()).isEqualTo("urn:problem-type:simpl:notAcceptable");
        assertThat(notAcceptable.json().path("title").asString()).isEqualTo("Not Acceptable");
        for (String accept : List.of("application/json", "application/problem+json", "*/*")) {
            assertThat(api.get("/v1/vocabularies", ENV.viewer(), "Accept", accept).status()).isEqualTo(200);
        }
        assertThat(api.get("/v1/vocabularies", ENV.viewer(), "Accept", "nonsense").status()).isEqualTo(406);
        // 406 before 415 (D63)
        assertThat(api.send("POST", "/v1/vocabularies", ENV.admin(), "text/plain", new byte[] {1}, "Accept",
                        "text/html").status())
                .isEqualTo(406);
        Result unsupported = api.json("POST", "/v1/vocabularies", ENV.admin(), "{}");
        assertThat(unsupported.status()).isEqualTo(415);
        assertThat(unsupported.json().path("type").asString())
                .isEqualTo("urn:problem-type:simpl:unsupportedMediaType");
        assertThat(api.send("POST", "/v1/vocabularies", ENV.admin(), null, new byte[] {1}).status()).isEqualTo(415);
        assertThat(api.send("PATCH", "/v1/vocabularies/Abc", ENV.admin(), "text/plain", new byte[] {1}).status())
                .isEqualTo(415);
        assertThat(api.send("POST", "/v1/vocabularies/searches", ENV.admin(), "bad/", new byte[] {1}).status())
                .isEqualTo(415);
        assertThat(api.send("POST", "/v1/vocabularies", ENV.admin(), "multipart/form-data", new byte[] {1})
                        .status())
                .isEqualTo(400);
        assertThat(api.send("POST", "/v1/vocabularies", ENV.admin(), "multipart/form-data; boundary=zz",
                        "garbage".getBytes(StandardCharsets.UTF_8)).status())
                .isEqualTo(400);
    }

    @Test
    void uploadLimits() {
        String id = Api.unique();
        byte[] atLimit = new byte[5 * 1024 * 1024];
        byte[] header = ("@prefix ex: <https://example.org/" + id + "#> . ex:a ex:b ex:c .\n")
                .getBytes(StandardCharsets.UTF_8);
        java.util.Arrays.fill(atLimit, (byte) ' ');
        System.arraycopy(header, 0, atLimit, 0, header.length);
        Result ok = api.multipart("/v1/vocabularies", ENV.admin(), Part.field("name", "Big" + id),
                Part.field("description", "d"), Part.field("changelog", "c"), Part.file(atLimit));
        assertThat(ok.status()).as(ok.text()).isEqualTo(201);

        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        java.util.Arrays.fill(tooLarge, (byte) ' ');
        Result large = api.multipart("/v1/vocabularies", ENV.admin(), Part.field("name", "Bigger" + id),
                Part.field("description", "d"), Part.field("changelog", "c"), Part.file(tooLarge));
        assertThat(large.status()).isEqualTo(413);
        assertThat(large.json().path("type").asString()).isEqualTo("urn:problem-type:simpl:payloadTooLarge");
        // 413 before field checks (D51)
        Result largeWithBadFields = api.multipart("/v1/vocabularies", ENV.admin(), Part.field("name", "x"),
                Part.file(tooLarge));
        assertThat(largeWithBadFields.status()).isEqualTo(413);
        // Whole request over 6 MB (D76)
        Result request = api.multipart("/v1/vocabularies", ENV.admin(), Part.field("name", "Req" + id),
                Part.field("description", "x".repeat(1024 * 1024)), Part.field("changelog", "c"),
                Part.file(atLimit));
        assertThat(request.status()).isEqualTo(413);
        assertThat(api.multipart("/v1/vocabularies/Big" + id + "/versions", ENV.admin(), Part.file(tooLarge))
                        .status())
                .isEqualTo(413);
        StringBuilder json = new StringBuilder("{\"items\":[");
        json.append("{\"name\":\"").append("A".repeat(2 * 1024 * 1024)).append("\"}]}");
        assertThat(api.json("POST", "/v1/vocabularies/searches", ENV.viewer(), json.toString()).status())
                .isIn(400, 413);
    }

    @Test
    void correlationHeadersAreReturned() {
        Result result = api.get("/v1/health", null, "X-Correlation-Id", "abc-123",
                "traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
        assertThat(result.header("X-Correlation-Id")).isEqualTo("abc-123");
        assertThat(result.header("traceparent")).contains("0af7651916cd43dd8448eb211c80319c");
        Result generated = api.get("/v1/health", null);
        assertThat(generated.header("X-Correlation-Id")).isNotBlank();
        Result invalid = api.get("/v1/health", null, "X-Correlation-Id", "x".repeat(300), "traceparent", "junk");
        assertThat(invalid.header("X-Correlation-Id")).isNotEqualTo("x".repeat(300));
    }

    @Test
    void managementPortServesHealthAndMetricsWithoutToken() {
        Api management = new Api(ENV.managementPort());
        assertThat(management.get("/actuator/health/readiness", null).status()).isEqualTo(200);
        api.get("/v1/vocabularies", ENV.viewer());
        Result metrics = management.get("/actuator/prometheus", null);
        assertThat(metrics.status()).isEqualTo(200);
        assertThat(metrics.text()).contains("http_server_requests");
    }
}
