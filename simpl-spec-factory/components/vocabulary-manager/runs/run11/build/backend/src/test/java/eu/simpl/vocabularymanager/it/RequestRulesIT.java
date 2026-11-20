package eu.simpl.vocabularymanager.it;

import static eu.simpl.vocabularymanager.it.TestEnvironment.admin;
import static eu.simpl.vocabularymanager.it.TestEnvironment.norole;
import static eu.simpl.vocabularymanager.it.TestEnvironment.viewer;
import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Access rules and the order of checks (SIMPL-18196; D16, D17, D21, D22, D49, D51, D52, D63, D67, D76, D139). */
class RequestRulesIT extends IntegrationTest {

    @Test
    void tokensAndRoles() throws Exception {
        assertThat(api.get("/v1/vocabularies", null).status()).isEqualTo(401);
        assertThat(api.get("/v1/vocabularies", null).type()).isEqualTo("urn:problem-type:simpl:unauthorized");
        assertThat(api.get("/v1/vocabularies", "garbage").status()).isEqualTo(401);
        assertThat(api.get("/v1/vocabularies", norole()).status()).isEqualTo(403);
        assertThat(api.get("/v1/vocabularies", norole()).type()).isEqualTo("urn:problem-type:simpl:forbidden");
        assertThat(api.get("/v1/vocabularies", viewer()).status()).isEqualTo(200);
        assertThat(create(uniqueName("Viewer"), "x").status()).isEqualTo(400);
        ApiClient.Result viewerWrite =
                api.post("/v1/vocabularies", viewer(), new ApiClient.Form().field("name", "Abc"));
        assertThat(viewerWrite.status()).isEqualTo(403);
        String otherIssuer = TestEnvironment.token(
                "ga-admin",
                List.of("GA_VOCABULARY_ADMIN"),
                "http://other/realms/x",
                TestEnvironment.AUDIENCE,
                TestEnvironment.KEY,
                Instant.now().plusSeconds(60));
        assertThat(api.get("/v1/vocabularies", otherIssuer).status()).isEqualTo(401);
        String noAudience = TestEnvironment.token(
                "ga-admin",
                List.of("GA_VOCABULARY_ADMIN"),
                TestEnvironment.ISSUER,
                "account",
                TestEnvironment.KEY,
                Instant.now().plusSeconds(60));
        assertThat(api.get("/v1/vocabularies", noAudience).status()).isEqualTo(401);
        String expired = TestEnvironment.token(
                "ga-admin",
                List.of("GA_VOCABULARY_ADMIN"),
                TestEnvironment.ISSUER,
                TestEnvironment.AUDIENCE,
                TestEnvironment.KEY,
                Instant.now().minusSeconds(600));
        assertThat(api.get("/v1/vocabularies", expired).status()).isEqualTo(401);
        String foreignKey = TestEnvironment.token(
                "ga-admin",
                List.of("GA_VOCABULARY_ADMIN"),
                TestEnvironment.ISSUER,
                TestEnvironment.AUDIENCE,
                new RSAKeyGenerator(2048).keyID("other-key").generate(),
                Instant.now().plusSeconds(60));
        assertThat(api.get("/v1/vocabularies", foreignKey).status()).isEqualTo(401);
        assertThat(api.get("/v1/health", null).json().get("status").asString()).isEqualTo("UP");
    }

    @Test
    void methodsAndUnknownPaths() {
        ApiClient.Result put = api.send("PUT", "/v1/vocabularies", null, null, null);
        assertThat(put.status()).isEqualTo(405);
        assertThat(put.header("Allow")).isEqualTo("GET, POST");
        assertThat(api.send("GET", "/v1/vocabularies/searches", admin(), null, null)
                        .status())
                .isEqualTo(405);
        assertThat(api.send("HEAD", "/v1/vocabularies", null, null, null).status())
                .isEqualTo(405);
        assertThat(api.send("OPTIONS", "/v1/vocabularies", null, null, null).status())
                .isEqualTo(405);
        assertThat(api.send("QUERY", "/v1/whatever", null, null, null).status()).isEqualTo(405);
        assertThat(api.get("/v1/nothing", null).status()).isEqualTo(401);
        assertThat(api.get("/v1/nothing", viewer()).status()).isEqualTo(404);
        assertThat(api.get("/actuator/health", viewer()).status()).isEqualTo(404);
        assertThat(api.get("/v1/vocabularies/a%2Fb", viewer()).status()).isEqualTo(400);
        assertThat(api.get("/error", viewer()).status()).isEqualTo(404);
    }

    @Test
    void negotiationOrder() {
        assertThat(api.get("/v1/vocabularies", viewer(), "Accept", "text/html").status())
                .isEqualTo(406);
        assertThat(api.get("/v1/vocabularies", viewer(), "Accept", "application/*")
                        .status())
                .isEqualTo(200);
        assertThat(api.get("/v1/vocabularies", viewer(), "Accept", "application/problem+json")
                        .header("Content-Type"))
                .startsWith("application/json");
        assertThat(api.get("/v1/content/Abc", viewer(), "Accept", "application/json")
                        .status())
                .isEqualTo(406);
        assertThat(api.get("/v1/vocabularies", norole(), "Accept", "text/html").status())
                .isEqualTo(403);
        ApiClient.Result json = api.send(
                "POST", "/v1/vocabularies", admin(), "application/json", "{}".getBytes(StandardCharsets.UTF_8));
        assertThat(json.status()).isEqualTo(415);
        assertThat(api.send("PATCH", "/v1/vocabularies/Abc", admin(), "text/plain", "x".getBytes())
                        .status())
                .isEqualTo(415);
        ApiClient.Result tooBig =
                api.send("POST", "/v1/vocabularies/searches", viewer(), "application/json", new byte[1024 * 1024 + 10]);
        assertThat(tooBig.status()).isEqualTo(413);
    }

    @Test
    void queryParameterRules() {
        assertThat(api.get("/v1/vocabularies?foo=1", viewer())
                        .json()
                        .get("issues")
                        .get(0)
                        .get("name")
                        .asString())
                .isEqualTo("foo");
        JsonNode repeated = api.get("/v1/vocabularies?page=1&page=2", viewer()).json();
        assertThat(repeated.get("issues")).hasSize(1);
        assertThat(api.get("/v1/vocabularies?pageSize=101", viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/vocabularies?page=0", viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/vocabularies?page=%2B2", viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/vocabularies?sort=bogus", viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/vocabularies?status=active", viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/vocabularies?creationDate=2026-02-30", viewer()).status())
                .isEqualTo(400);
        assertThat(api.get("/v1/vocabularies?creationDate=2026-02-03T10:00:00%2B19:00", viewer())
                        .status())
                .isEqualTo(400);
        assertThat(api.get("/v1/vocabularies?name=a-b", viewer()).status()).isEqualTo(400);
        assertThat(api.get("/v1/vocabularies/Abc?x=1", viewer()).status()).isEqualTo(400);
        assertThat(api.send("DELETE", "/v1/external-vocabularies/Abc?x=1", admin(), null, null)
                        .status())
                .isEqualTo(400);
        assertThat(api.get("/v1/health?x=1", null).status()).isEqualTo(400);
    }

    @Test
    void listFiltersSortsAndPages() {
        String prefix = uniqueName("List");
        for (String suffix : List.of("Bb", "Aa", "Cc")) {
            create(prefix + suffix, ttl(prefix + suffix));
        }
        api.patch("/v1/vocabularies/" + prefix + "Cc", admin(), "{\"status\":\"DEPRECATED\"}");
        JsonNode page = api.get("/v1/vocabularies?name=" + prefix.toLowerCase() + "&pageSize=2&sort=-name", viewer())
                .json();
        assertThat(page.get("total").asLong()).isEqualTo(3);
        assertThat(page.get("totalPages").asLong()).isEqualTo(2);
        assertThat(page.get("items").get(0).get("name").asString()).isEqualTo(prefix + "Cc");
        assertThat(page.get("self").asString())
                .isEqualTo("/vocabularies?page=1&pageSize=2&name=" + prefix.toLowerCase() + "&sort=-name");
        assertThat(page.get("next").asString()).contains("page=2");
        assertThat(page.has("prev")).isFalse();
        JsonNode beyond = api.get("/v1/vocabularies?name=" + prefix + "&pageSize=2&page=5", viewer())
                .json();
        assertThat(beyond.get("items")).isEmpty();
        assertThat(beyond.get("prev").asString()).startsWith("/vocabularies?page=2&pageSize=2");
        JsonNode deprecated = api.get("/v1/vocabularies?name=" + prefix + "&status=DEPRECATED", viewer())
                .json();
        assertThat(deprecated.get("total").asLong()).isEqualTo(1);
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        assertThat(api.get("/v1/vocabularies?name=" + prefix + "&creationDate=" + today, viewer())
                        .json()
                        .get("total")
                        .asLong())
                .isEqualTo(3);
        assertThat(api.get("/v1/vocabularies?name=" + prefix + "&lastUpdated=2001-01-01", viewer())
                        .json()
                        .get("total")
                        .asLong())
                .isZero();
        JsonNode byStatus = api.get("/v1/vocabularies?name=" + prefix + "&sort=-status", viewer())
                .json();
        assertThat(byStatus.get("items").get(0).get("status").asString()).isEqualTo("DEPRECATED");
        assertThat(api.get("/v1/vocabularies?name=" + prefix + "&sort=creationDate", viewer())
                        .status())
                .isEqualTo(200);
        assertThat(api.get("/v1/vocabularies?name=" + prefix + "&sort=-lastUpdated", viewer())
                        .status())
                .isEqualTo(200);
        JsonNode versions = api.get("/v1/vocabularies/" + prefix + "Aa/versions?sort=status&page=3", viewer())
                .json();
        assertThat(versions.get("items")).isEmpty();
        assertThat(api.get("/v1/vocabularies/" + prefix + "Aa/versions?sort=lastUpdated", viewer())
                        .status())
                .isEqualTo(400);
    }

    @Test
    void formFieldRules() {
        ApiClient.Result versionField = api.post(
                "/v1/vocabularies/Abc/versions",
                admin(),
                new ApiClient.Form()
                        .field("description", "d")
                        .field("changelog", "c")
                        .field("version", "3")
                        .file("vocabularyFile", "<a> <b> <c> ."));
        assertThat(versionField.status()).isEqualTo(400);
        assertThat(versionField.json().get("issues").get(0).get("name").asString())
                .isEqualTo("version");
        ApiClient.Result missing = api.post("/v1/vocabularies", admin(), new ApiClient.Form().field("other", "x"));
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.json().get("issues").size()).isEqualTo(5);
        ApiClient.Result longText = api.post(
                "/v1/vocabularies",
                admin(),
                new ApiClient.Form()
                        .field("name", "Abcd")
                        .field("description", "x".repeat(1001))
                        .field("changelog", "   ")
                        .file("vocabularyFile", "<a> <b> <c> ."));
        assertThat(longText.json().get("issues")).hasSize(2);
        ApiClient.Result tooLarge = api.post(
                "/v1/vocabularies",
                admin(),
                new ApiClient.Form().field("name", "Abcd").file("vocabularyFile", new byte[5 * 1024 * 1024 + 1]));
        assertThat(tooLarge.status()).isEqualTo(413);
        assertThat(tooLarge.type()).isEqualTo("urn:problem-type:simpl:payloadTooLarge");
        ApiClient.Result notMultipart =
                api.send("POST", "/v1/vocabularies", admin(), "multipart/form-data; boundary=x", "garbage".getBytes());
        assertThat(notMultipart.status()).isEqualTo(400);
    }
}
