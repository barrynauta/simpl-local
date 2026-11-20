package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import eu.simpl.vocabularymanager.support.TestIdentityProvider;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Access rules (D16, D17, D84), early rejections (D67) and the order of checks on writes (D51, D63). */
class AccessAndOrderApiTest extends IntegrationTestBase {

    @Test
    void noTokenGives401WithProblemBody() {
        ApiClient.Response response = api().get("/v1/vocabularies", null);
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.header("Content-Type")).isEqualTo("application/problem+json");
        assertThat(response.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:unauthorized");
        assertThat(response.json().get("title").asString()).isEqualTo("Unauthorized");
        assertThat(response.header("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void invalidTokensGive401() throws Exception {
        assertThat(api().get("/v1/vocabularies", "not-a-jwt").status()).isEqualTo(401);
        String wrongAudience = IDP.sign(
                IDP.claims("s", "e@x", List.of("GA_VOCABULARY_ADMIN"))
                        .audience("other")
                        .build(),
                "test-key",
                IDP.key());
        assertThat(api().get("/v1/vocabularies", wrongAudience).status()).isEqualTo(401);
        String wrongIssuer = IDP.sign(
                IDP.claims("s", "e@x", List.of("GA_VOCABULARY_ADMIN"))
                        .issuer("http://other/realms/simpl-other")
                        .build(),
                "test-key",
                IDP.key());
        assertThat(api().get("/v1/vocabularies", wrongIssuer).status()).isEqualTo(401);
        RSAKey otherKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        String badSignature =
                IDP.sign(IDP.claims("s", "e@x", List.of("GA_VOCABULARY_ADMIN")).build(), "test-key", otherKey);
        assertThat(api().get("/v1/vocabularies", badSignature).status()).isEqualTo(401);
        String expired = IDP.sign(
                IDP.claims("s", "e@x", List.of("GA_VOCABULARY_ADMIN"))
                        .expirationTime(new java.util.Date(System.currentTimeMillis() - 120_000))
                        .build(),
                "test-key",
                IDP.key());
        assertThat(api().get("/v1/vocabularies", expired).status()).isEqualTo(401);
    }

    @Test
    void rolesDecideAccess() {
        assertThat(api().get("/v1/vocabularies", IDP.noRole()).status()).isEqualTo(403);
        assertThat(api().get("/v1/vocabularies", IDP.noRole())
                        .json()
                        .get("type")
                        .asString())
                .isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(api().get("/v1/vocabularies", IDP.viewer()).status()).isEqualTo(200);
        String name = uniqueName("Acc");
        ApiClient.Response viewerCreate =
                api().multipart("/v1/vocabularies", IDP.viewer(), ApiClient.createParts(name, "D", simpleTurtle(name)));
        assertThat(viewerCreate.status()).isEqualTo(403);
        assertThat(api().json("POST", "/v1/vocabularies/searches", IDP.viewer(), "{\"items\":[{\"name\":\"Abc\"}]}")
                        .status())
                .isEqualTo(200);
        assertThat(api().json("PATCH", "/v1/vocabularies/" + name, IDP.viewer(), "{\"status\":\"ACTIVE\"}")
                        .status())
                .isEqualTo(403);
        assertThat(api().delete("/v1/external-vocabularies/Abc", IDP.viewer()).status())
                .isEqualTo(403);
        assertThat(api().get("/v1/vocabularies?name=" + name, IDP.admin())
                        .json()
                        .get("total")
                        .asInt())
                .isZero();
    }

    @Test
    void healthNeedsNoToken() {
        ApiClient.Response health = api().get("/v1/health", null);
        assertThat(health.status()).isEqualTo(200);
        assertThat(health.json().get("status").asString()).isIn("UP", "DEGRADED");
        assertThat(api().get("/v1/health", "garbage").status()).isEqualTo(200);
    }

    @Test
    void unknownPathsGive401Then404() {
        assertThat(api().get("/v1/nothing", null).status()).isEqualTo(401);
        assertThat(api().get("/v1/nothing", IDP.noRole()).status()).isEqualTo(404);
        assertThat(api().get("/actuator/health", null).status()).isEqualTo(401);
        assertThat(api().get("/actuator/health", IDP.admin()).status()).isEqualTo(404);
        assertThat(api().get("/v1/vocabularies/", IDP.admin()).status()).isEqualTo(404);
        assertThat(api().get("/error", IDP.admin()).status()).isEqualTo(404);
    }

    @Test
    void undeclaredMethodsGive405BeforeTheToken() {
        ApiClient.Response put = api().method("PUT", "/v1/vocabularies", null);
        assertThat(put.status()).isEqualTo(405);
        assertThat(put.header("Allow")).isEqualTo("GET, POST");
        assertThat(put.header("Content-Type")).isEqualTo("application/problem+json");
        assertThat(api().method("QUERY", "/v1/vocabularies", IDP.admin()).status())
                .isEqualTo(405);
        assertThat(api().method("HEAD", "/v1/vocabularies", IDP.admin()).status())
                .isEqualTo(405);
        assertThat(api().get("/v1/vocabularies/searches", IDP.admin()).status()).isEqualTo(405);
        assertThat(api().get("/v1/external-vocabularies/content", IDP.admin()).status())
                .isEqualTo(405);
        assertThat(api().method("DELETE", "/v1/vocabularies/Abc", IDP.admin()).status())
                .isEqualTo(405);
    }

    @Test
    void firewallRejectsEncodedSlash() {
        ApiClient.Response response = api().get("/v1/vocabularies/Ab%2Fc", IDP.admin());
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.json().get("type").asString()).isEqualTo("urn:problem-type:belgif:badRequest");
    }

    @Test
    void acceptIsCheckedAfterRolesAndBeforeMediaType() {
        ApiClient.Response xml = api().get("/v1/vocabularies", IDP.viewer(), "application/xml");
        assertThat(xml.status()).isEqualTo(406);
        assertThat(xml.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:notAcceptable");
        assertThat(api().get("/v1/vocabularies", IDP.noRole(), "application/xml")
                        .status())
                .isEqualTo(403);
        assertThat(api().get("/v1/vocabularies", IDP.viewer(), "application/problem+json")
                        .status())
                .isEqualTo(200);
        assertThat(api().get("/v1/vocabularies", IDP.viewer(), "*/*").status()).isEqualTo(200);
    }

    @Test
    void unsupportedMediaTypeGives415BeforeSizeAndShape() {
        ApiClient.Response response = api().raw(
                        "POST",
                        "/v1/vocabularies?x=1",
                        IDP.admin(),
                        "application/json",
                        "{}".getBytes(StandardCharsets.UTF_8));
        assertThat(response.status()).isEqualTo(415);
        assertThat(response.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:unsupportedMediaType");
        assertThat(api().raw("PATCH", "/v1/vocabularies/Abc", IDP.admin(), "text/plain", new byte[0])
                        .status())
                .isEqualTo(415);
    }

    @Test
    void fileAbove5MbGives413AndRequestAbove6MbToo() {
        String name = uniqueName("Large");
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        java.util.Arrays.fill(big, (byte) ' ');
        List<ApiClient.Part> parts =
                List.of(ApiClient.Part.text("name", "bad name"), ApiClient.Part.file("vocabularyFile", big));
        ApiClient.Response response = api().multipart("/v1/vocabularies", IDP.admin(), parts);
        assertThat(response.status()).isEqualTo(413);
        assertThat(response.json().get("type").asString()).isEqualTo("urn:problem-type:simpl:payloadTooLarge");
        assertThat(response.json().get("issues").get(0).get("name").asString()).isEqualTo("vocabularyFile");

        byte[] exactly = new byte[5 * 1024 * 1024];
        java.util.Arrays.fill(exactly, (byte) ' ');
        ApiClient.Response atLimit = api().multipart(
                        "/v1/vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", name),
                                ApiClient.Part.text("description", "d"),
                                ApiClient.Part.text("changelog", "c"),
                                ApiClient.Part.file("vocabularyFile", exactly)));
        assertThat(atLimit.status()).isEqualTo(400);
        assertThat(atLimit.json().get("issues").get(0).get("detail").asString())
                .isEqualTo("Vocabulary contains no statements");

        byte[] huge = new byte[7 * 1024 * 1024];
        ApiClient.Response request = api().multipart(
                        "/v1/vocabularies",
                        IDP.admin(),
                        List.of(ApiClient.Part.text("description", new String(huge, StandardCharsets.ISO_8859_1))));
        assertThat(request.status()).isEqualTo(413);
    }

    @Test
    void shapeErrorsAre400BeforeNotFound() {
        ApiClient.Response response = api().multipart(
                        "/v1/vocabularies/bad/versions",
                        IDP.admin(),
                        List.of(ApiClient.Part.text("unknownField", "x"), ApiClient.Part.text("", "y")));
        assertThat(response.status()).isEqualTo(400);
        List<String> names = response.json()
                .get("issues")
                .valueStream()
                .map(issue -> issue.get("name").asString())
                .toList();
        assertThat(names).contains("vocabularyName", "unknownField", "description", "changelog", "vocabularyFile");
        ApiClient.Response query = api().multipart(
                        "/v1/vocabularies/Missing" + uniqueName("Q") + "/versions?debug=1",
                        IDP.admin(),
                        ApiClient.versionParts("d", "c", simpleTurtle("x")));
        assertThat(query.status()).isEqualTo(400);
        assertThat(query.json().get("issues").get(0).get("in").asString()).isEqualTo("query");
    }

    @Test
    void invalidPathValuesGive400() {
        ApiClient.Response name = api().get("/v1/vocabularies/lowercase", IDP.admin());
        assertThat(name.status()).isEqualTo(400);
        assertThat(name.json().get("issues").get(0).get("in").asString()).isEqualTo("path");
        assertThat(name.json().get("issues").get(0).get("type").asString())
                .isEqualTo("urn:problem-type:belgif:input-validation:schemaViolation");
        for (String version : List.of("0", "2.0", "1e1", "%2B2", "%202", "2147483648", "abc")) {
            assertThat(api().get("/v1/vocabularies/Abcd/versions/" + version, IDP.admin())
                            .status())
                    .as(version)
                    .isEqualTo(400);
        }
        assertThat(api().get("/v1/vocabularies/Abcd/versions/2147483647", IDP.admin())
                        .status())
                .isEqualTo(404);
        assertThat(api().get("/v1/external-vocabularies/ab", IDP.admin())
                        .json()
                        .get("issues")
                        .get(0)
                        .get("name")
                        .asString())
                .isEqualTo("name");
    }

    @Test
    void identityProviderKeysAreCached() {
        int before = IDP.fetchCount();
        api().get("/v1/vocabularies", IDP.viewer());
        api().get("/v1/vocabularies", IDP.viewer());
        assertThat(IDP.fetchCount() - before).isLessThanOrEqualTo(1);
        assertThat(TestIdentityProvider.AUDIENCE).isEqualTo("vocabulary-manager-be");
    }
}
