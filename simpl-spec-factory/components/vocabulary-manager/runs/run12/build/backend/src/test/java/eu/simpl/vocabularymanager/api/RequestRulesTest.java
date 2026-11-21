package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Field, value and query rules (D19 to D22, D37, D49, D52, D73, D78, D91, D111, D112, D130). */
class RequestRulesTest {

    @Test
    void names() {
        assertThat(Validation.validName("Abc")).isTrue();
        assertThat(Validation.validName("Ab")).isFalse();
        assertThat(Validation.validName("abc")).isFalse();
        assertThat(Validation.validName("A".repeat(65))).isFalse();
        assertThat(Validation.validName("Ab-c")).isFalse();
        assertThat(Validation.validName(null)).isFalse();
        assertThatThrownBy(() -> Validation.pathName("vocabularyName", "x")).isInstanceOf(ApiException.class);
    }

    @Test
    void integers() {
        assertThat(Validation.positiveInt("1")).isEqualTo(1);
        assertThat(Validation.positiveInt("2147483647")).isEqualTo(Integer.MAX_VALUE);
        for (String bad : new String[] {"0", "2147483648", "2.0", "1e1", "+2", " 2", "", "-1", "12345678901"}) {
            assertThat(Validation.positiveInt(bad)).as(bad).isNull();
        }
        assertThat(Validation.pathVersion("3")).isEqualTo(3);
        assertThatThrownBy(() -> Validation.pathVersion("0")).isInstanceOf(ApiException.class);
    }

    @Test
    void texts() {
        assertThat(Validation.validText(" a ")).isTrue();
        assertThat(Validation.validText("   ")).isFalse();
        assertThat(Validation.validText(null)).isFalse();
        assertThat(Validation.validText("x".repeat(1000))).isTrue();
        assertThat(Validation.validText("x".repeat(1001))).isFalse();
    }

    @Test
    void uris() {
        assertThat(Validation.absoluteUri("http://purl.org/dc/terms/")).isTrue();
        assertThat(Validation.absoluteUri("urn:x:y")).isTrue();
        assertThat(Validation.absoluteUri("relative/path")).isFalse();
        assertThat(Validation.absoluteUri("http://a b")).isFalse();
        assertThat(Validation.absoluteUri("http:")).isFalse();
        assertThat(Validation.absoluteUri("")).isFalse();
        assertThat(Validation.absoluteUri(null)).isFalse();
    }

    @Test
    void dates() {
        Instant[] day = Validation.dateRange("2026-11-18");
        assertThat(day[0]).isEqualTo(Instant.parse("2026-11-18T00:00:00Z"));
        assertThat(day[1]).isEqualTo(Instant.parse("2026-11-19T00:00:00Z"));
        assertThat(Validation.dateRange("2026-11-18T09:27:00Z")[0]).isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(Validation.dateRange("2026-11-18T09:27:00")[0]).isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(Validation.dateRange("2026-11-18T10:27:00.9+01:00")[0])
                .isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(Validation.dateRange("2026-11-18T10:27:00-18:00")).isNotNull();
        for (String bad : new String[] {
            "2026-02-30",
            "2026-11-18T",
            "2026-11-18T25:00:00Z",
            "2026-11-18T10:00:00+19:00",
            "18-11-2026",
            "2026-11-18T10:00:00+18:30",
            null
        }) {
            assertThat(Validation.dateRange(bad)).as(String.valueOf(bad)).isNull();
        }
    }

    @Test
    void queryParams() {
        QueryParams q = QueryParams.parse("name=A%20b&page=2&x", Set.of("name", "page", "x"));
        assertThat(q.get("name")).isEqualTo("A b");
        assertThat(q.get("x")).isEmpty();
        assertThat(q.all().keySet()).containsExactly("name", "page", "x");
        assertThat(QueryParams.parse(null, Set.of()).all()).isEmpty();
        assertThat(QueryParams.parse("&&", Set.of()).all()).isEmpty();
        assertThatThrownBy(() -> QueryParams.parse("foo=1&bar=2", Set.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).issues()).hasSize(2));
        assertThatThrownBy(() -> QueryParams.parse("page=1&page=2&page=3", Set.of("page")))
                .satisfies(e -> assertThat(((ApiException) e).issues()).hasSize(1));
        assertThatThrownBy(() -> QueryParams.parse("a=%zz", Set.of("a")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).issues()).isEmpty());
        List<Issue> issues = new ArrayList<>();
        QueryParams p = QueryParams.parse("pageSize=101&page=x", Set.of("pageSize", "page"));
        assertThat(p.positiveInt("pageSize", 20, 100, issues)).isEqualTo(20);
        assertThat(p.positiveInt("page", 1, Integer.MAX_VALUE, issues)).isEqualTo(1);
        assertThat(p.positiveInt("other", 7, 100, issues)).isEqualTo(7);
        assertThat(issues).hasSize(2);
    }

    @Test
    void pagingLinks() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("sort", "-name");
        params.put("page", "3");
        params.put("name", "a b");
        Dto.PageDto<String> p = Dto.PageDto.paged("/vocabularies", params, 3, 2, 5, List.of("x"));
        assertThat(p.self()).isEqualTo("/vocabularies?page=3&pageSize=2&sort=-name&name=a%20b");
        assertThat(p.prev()).isEqualTo("/vocabularies?page=2&pageSize=2&sort=-name&name=a%20b");
        assertThat(p.next()).isNull();
        assertThat(p.totalPages()).isEqualTo(3);
        Dto.PageDto<String> beyond = Dto.PageDto.paged("/v", Map.of(), 9, 2, 5, List.of());
        assertThat(beyond.prev()).isEqualTo("/v?page=3&pageSize=2");
        Dto.PageDto<String> empty = Dto.PageDto.paged("/v", Map.of(), 1, 20, 0, List.of());
        assertThat(empty.last()).isNull();
        assertThat(empty.totalPages()).isZero();
        assertThat(Dto.PageDto.search(List.of()).totalPages()).isZero();
        assertThat(Dto.PageDto.search(List.of("a")).totalPages()).isEqualTo(1);
    }

    @Test
    void acceptRules() {
        assertThat(NegotiationFilter.acceptable(null, false)).isTrue();
        assertThat(NegotiationFilter.acceptable("application/json", false)).isTrue();
        assertThat(NegotiationFilter.acceptable("application/problem+json", false))
                .isTrue();
        assertThat(NegotiationFilter.acceptable("application/*", true)).isTrue();
        assertThat(NegotiationFilter.acceptable("text/turtle", true)).isTrue();
        assertThat(NegotiationFilter.acceptable("text/*", true)).isTrue();
        assertThat(NegotiationFilter.acceptable("text/turtle", false)).isFalse();
        assertThat(NegotiationFilter.acceptable("application/json", true)).isFalse();
        assertThat(NegotiationFilter.acceptable("application/xml", false)).isFalse();
        assertThat(NegotiationFilter.acceptable("*/*;q=0", false)).isFalse();
        assertThat(NegotiationFilter.acceptable("not a media type", false)).isFalse();
    }

    @Test
    void routes() {
        Routes.Match m = Routes.match("GET", "/v1/vocabularies/searches").orElseThrow();
        assertThat(m.route()).isNull();
        assertThat(m.allowed()).containsExactly("POST");
        Routes.Match v =
                Routes.match("PATCH", "/v1/vocabularies/Abc/versions/2").orElseThrow();
        assertThat(v.route().operationId()).isEqualTo("updateVocabularyVersionStatus");
        assertThat(v.variables()).containsEntry("version", "2");
        assertThat(Routes.match("GET", "/v1/nothing")).isEmpty();
        assertThat(Routes.match("POST", "/v1/external-vocabularies/content")
                        .orElseThrow()
                        .route()
                        .turtle())
                .isTrue();
    }
}
