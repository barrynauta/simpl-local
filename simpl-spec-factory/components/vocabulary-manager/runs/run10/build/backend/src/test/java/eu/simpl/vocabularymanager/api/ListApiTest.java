package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.JsonNode;

/** GET /vocabularies filters, sort, paging and links (D19 to D23, D58, D59, D78, D110, D111). */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListApiTest extends IntegrationTestBase {

    private final String tag = uniqueName("L").substring(1, 9).toLowerCase();
    private final String a = "AZ" + tag;
    private final String b = "Ab" + tag;
    private final String c = "Cc" + tag;

    @BeforeAll
    void createThree() {
        for (String name : List.of(c, a, b)) {
            ApiClient.Response response = api().multipart(
                            "/v1/vocabularies",
                            IDP.admin(),
                            ApiClient.createParts(name, "d " + name, simpleTurtle(name)));
            assertThat(response.status()).isEqualTo(201);
        }
        api().json("PATCH", "/v1/vocabularies/" + b, IDP.admin(), "{\"status\":\"DEPRECATED\"}");
    }

    @Test
    void nameFilterIsCaseInsensitiveContainsAndDefaultOrderIsByCodePoint() {
        JsonNode page = api().get("/v1/vocabularies?name=" + tag.toUpperCase(), IDP.viewer())
                .json();
        assertThat(page.get("total").asInt()).isEqualTo(3);
        assertThat(names(page)).containsExactly(a, b, c);
        assertThat(page.get("self").asString()).isEqualTo("/vocabularies?page=1&pageSize=20&name=" + tag.toUpperCase());
        assertThat(page.has("prev")).isFalse();
        assertThat(page.has("next")).isFalse();
        assertThat(page.get("last").asString()).isEqualTo("/vocabularies?page=1&pageSize=20&name=" + tag.toUpperCase());
    }

    @Test
    void statusFilterAndSorts() {
        JsonNode deprecated = api().get("/v1/vocabularies?name=" + tag + "&status=DEPRECATED", IDP.viewer())
                .json();
        assertThat(names(deprecated)).containsExactly(b);
        assertThat(names(api().get("/v1/vocabularies?name=" + tag + "&sort=-name", IDP.viewer())
                        .json()))
                .containsExactly(c, b, a);
        assertThat(names(api().get("/v1/vocabularies?name=" + tag + "&sort=status", IDP.viewer())
                        .json()))
                .containsExactly(a, c, b);
        assertThat(names(api().get("/v1/vocabularies?name=" + tag + "&sort=-status", IDP.viewer())
                        .json()))
                .containsExactly(b, a, c);
        assertThat(names(api().get("/v1/vocabularies?name=" + tag + "&sort=-lastUpdated", IDP.viewer())
                        .json()))
                .hasSize(3);
        assertThat(api().get("/v1/vocabularies?name=" + tag + "&sort=creationDate", IDP.viewer())
                        .status())
                .isEqualTo(200);
    }

    @Test
    void dateFilters() {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertThat(api().get("/v1/vocabularies?name=" + tag + "&creationDate=" + today, IDP.viewer())
                        .json()
                        .get("total")
                        .asInt())
                .isEqualTo(3);
        assertThat(api().get("/v1/vocabularies?name=" + tag + "&creationDate=2000-01-01", IDP.viewer())
                        .json()
                        .get("total")
                        .asInt())
                .isZero();
        String created = api().get("/v1/vocabularies/" + a, IDP.viewer())
                .json()
                .get("creationDate")
                .asString();
        assertThat(api().get("/v1/vocabularies?name=" + tag + "&creationDate=" + created, IDP.viewer())
                        .json()
                        .get("items")
                        .valueStream()
                        .map(item -> item.get("name").asString()))
                .contains(a);
        String noOffset = created.substring(0, created.length() - 1);
        assertThat(api().get("/v1/vocabularies?name=" + tag + "&lastUpdated=" + noOffset, IDP.viewer())
                        .status())
                .isEqualTo(200);
        assertThat(api().get("/v1/vocabularies?creationDate=2024-02-30", IDP.viewer())
                        .status())
                .isEqualTo(400);
        assertThat(api().get("/v1/vocabularies?creationDate=yesterday", IDP.viewer())
                        .status())
                .isEqualTo(400);
    }

    @Test
    void pagingLinksKeepParametersAndPrevPointsToLastBeyondTheEnd() {
        JsonNode page2 = api().get("/v1/vocabularies?sort=name&name=" + tag + "&pageSize=2&page=2", IDP.viewer())
                .json();
        assertThat(names(page2)).containsExactly(c);
        assertThat(page2.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page2.get("self").asString()).isEqualTo("/vocabularies?page=2&pageSize=2&sort=name&name=" + tag);
        assertThat(page2.get("prev").asString()).isEqualTo("/vocabularies?page=1&pageSize=2&sort=name&name=" + tag);
        JsonNode beyond = api().get("/v1/vocabularies?name=" + tag + "&pageSize=2&page=9", IDP.viewer())
                .json();
        assertThat(beyond.get("items")).isEmpty();
        assertThat(beyond.get("total").asInt()).isEqualTo(3);
        assertThat(beyond.get("prev").asString()).isEqualTo("/vocabularies?page=2&pageSize=2&name=" + tag);
        JsonNode empty =
                api().get("/v1/vocabularies?name=Zz" + tag + "zz", IDP.viewer()).json();
        assertThat(empty.get("totalPages").asInt()).isZero();
        assertThat(empty.has("last")).isFalse();
        assertThat(empty.has("prev")).isFalse();
    }

    @Test
    void invalidParametersGive400() {
        for (String query : List.of(
                "page=0",
                "pageSize=101",
                "pageSize=0",
                "page=1.5",
                "sort=size",
                "status=OLD",
                "name=a-b",
                "foo=1",
                "page=1&page=2",
                "name=")) {
            ApiClient.Response response = api().get("/v1/vocabularies?" + query, IDP.viewer());
            assertThat(response.status()).as(query).isEqualTo(400);
            assertThat(response.json().get("issues").get(0).get("in").asString())
                    .isEqualTo("query");
        }
        assertThat(api().get("/v1/vocabularies?pageSize=100", IDP.viewer()).status())
                .isEqualTo(200);
        assertThat(api().get("/v1/vocabularies/" + a + "/versions?sort=lastUpdated", IDP.viewer())
                        .status())
                .isEqualTo(400);
        assertThat(api().get("/v1/vocabularies/" + a + "?x=1", IDP.viewer()).status())
                .isEqualTo(400);
        assertThat(api().get("/v1/health?x=1", null).status()).isEqualTo(400);
    }

    @Test
    void versionListPagingAndSort() {
        JsonNode versions = api().get("/v1/vocabularies/" + a + "/versions?sort=-creationDate", IDP.viewer())
                .json();
        assertThat(versions.get("total").asInt()).isEqualTo(1);
        assertThat(versions.get("self").asString())
                .isEqualTo("/vocabularies/" + a + "/versions?page=1&pageSize=20&sort=-creationDate");
        assertThat(api().get("/v1/vocabularies/Zzz" + tag + "/versions", IDP.viewer())
                        .status())
                .isEqualTo(404);
        assertThat(api().get("/v1/vocabularies/" + a + "/versions?page=5", IDP.viewer())
                        .json()
                        .get("items"))
                .isEmpty();
    }

    private static List<String> names(JsonNode page) {
        return page.get("items")
                .valueStream()
                .map(item -> item.get("name").asString())
                .toList();
    }
}
