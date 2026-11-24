package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.api.PageResponse;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests of routing, query parsing, field rules and paging (D19 to D22, D37, D49, D52, D59, D77, D78, D91, D111, D112, D168). */
class WebRulesTest {

    private final RouteMatcher matcher = new RouteMatcher();

    @Test
    void fixedSegmentsWinOverParameters() {
        assertThat(matcher.match("POST", "/v1/vocabularies/searches").operation())
                .isEqualTo(Operation.SEARCH_VOCABULARIES);
        RouteMatcher.Match get = matcher.match("GET", "/v1/vocabularies/searches");
        assertThat(get.operation()).isNull();
        assertThat(get.allowedMethods()).containsExactly("POST");
        assertThat(matcher.match("GET", "/v1/vocabularies/Abc").pathValues()).containsEntry("vocabularyName", "Abc");
        assertThat(matcher.match("GET", "/v1/external-vocabularies/Abc/content").operation())
                .isEqualTo(Operation.GET_EXTERNAL_VOCABULARY_CONTENT_BY_NAME);
        assertThat(matcher.match("POST", "/v1/external-vocabularies/content").operation())
                .isEqualTo(Operation.DOWNLOAD_EXTERNAL_VOCABULARY_CONTENT);
        assertThat(matcher.match("GET", "/v1/vocabularies/").pathDeclared()).isFalse();
        assertThat(matcher.match("GET", "/v1/vocabularies//versions").pathDeclared())
                .isFalse();
        assertThat(matcher.match("GET", "v1").pathDeclared()).isFalse();
        assertThat(matcher.declaredPaths()).hasSize(13);
        assertThat(Operation.CREATE_VOCABULARY.isWrite()).isTrue();
        assertThat(Operation.SEARCH_VOCABULARIES.isWrite()).isFalse();
    }

    @Test
    void queryStringIsStrict() {
        QueryParameters query = QueryParameters.parse("a=1&b=x+y&c&&d=%2B01%3A00");
        assertThat(query.get("b")).isEqualTo("x+y");
        assertThat(query.get("c")).isEmpty();
        assertThat(query.get("d")).isEqualTo("+01:00");
        assertThat(query.raw("d")).isEqualTo("%2B01%3A00");
        assertThat(query.names()).containsExactly("a", "b", "c", "d");
        assertThat(QueryParameters.parse(null).isEmpty()).isTrue();
        assertThatThrownBy(() -> QueryParameters.parse("a=1&a=2")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> QueryParameters.parse("a=%zz")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> QueryParameters.parse("a=%2")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> QueryParameters.parse("a=%C3%28")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> query.requireOnly(Set.of("a")))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.issues()).hasSize(3));
        assertThat(QueryParameters.decode("%C3%A9")).isEqualTo("é");
    }

    @Test
    void fieldAndParameterRules() {
        assertThat(RequestRules.version("12")).isEqualTo(12);
        for (String bad : new String[] {"0", "-1", "1.0", "1e1", "+2", " 2", "2147483648", "x", "9999999999999"}) {
            assertThatThrownBy(() -> RequestRules.version(bad)).as(bad).isInstanceOf(ApiException.class);
        }
        assertThat(RequestRules.queryInteger("page", null, 1, 5)).isEqualTo(1);
        assertThatThrownBy(() -> RequestRules.queryInteger("pageSize", "101", 20, 100))
                .isInstanceOf(ApiException.class);
        assertThat(RequestRules.vocabularyName("Health")).isEqualTo("Health");
        assertThatThrownBy(() -> RequestRules.externalName("he")).isInstanceOf(ApiException.class);
        assertThat(RequestRules.nameFilter("hEa1")).isEqualTo("hEa1");
        assertThat(RequestRules.nameFilter(null)).isNull();
        assertThatThrownBy(() -> RequestRules.nameFilter("a b")).isInstanceOf(ApiException.class);
        assertThat(RequestRules.dateFilter("creationDate", "2026-11-18")[1].toString())
                .isEqualTo("2026-11-19T00:00:00Z");
        assertThat(RequestRules.dateFilter("creationDate", "2026-11-18T10:00:00+01:00")[0].toString())
                .isEqualTo("2026-11-18T09:00:00Z");
        assertThat(RequestRules.dateFilter("creationDate", "2026-11-18T10:00:00.9")[0].toString())
                .isEqualTo("2026-11-18T10:00:00Z");
        assertThat(RequestRules.dateFilter("creationDate", null)).isNull();
        for (String bad :
                new String[] {"2026-13-01", "2026-11-18T25:00", "2026-11-18Tx", "18-11-2026", "2026-11-18T10:00+18:30"
                }) {
            assertThatThrownBy(() -> RequestRules.dateFilter("creationDate", bad))
                    .as(bad)
                    .isInstanceOf(ApiException.class);
        }
        List<Issue> issues = new ArrayList<>();
        assertThat(RequestRules.requiredText("description", "  ok  ", issues)).isEqualTo("ok");
        assertThat(RequestRules.requiredText("description", " ", issues)).isNull();
        assertThat(RequestRules.requiredText("description", null, issues)).isNull();
        assertThat(RequestRules.requiredText("description", "é".repeat(1000), issues))
                .hasSize(1000);
        assertThat(RequestRules.optionalText("description", "  ", issues)).isNull();
        assertThat(RequestRules.optionalText("description", null, issues)).isNull();
        assertThat(RequestRules.optionalText("description", "x".repeat(1001), issues))
                .isNull();
        assertThat(RequestRules.nameField("ab", issues)).isNull();
        assertThat(RequestRules.nameField(null, issues)).isNull();
        assertThat(issues).hasSize(5);
        assertThat(RequestRules.isAbsoluteUri("http://x/y#")).isTrue();
        assertThat(RequestRules.isAbsoluteUri("urn:isbn:1")).isTrue();
        assertThat(RequestRules.isAbsoluteUri("relative/path")).isFalse();
        assertThat(RequestRules.isAbsoluteUri("http://bad uri")).isFalse();
        assertThat(RequestRules.isAbsoluteUri("")).isFalse();
        assertThat(RequestRules.isAbsoluteUri(null)).isFalse();
    }

    @Test
    void pagingLinks() {
        QueryParameters query = QueryParameters.parse("sort=-name&page=3&name=ab&pageSize=2");
        PageResponse<String> page = Paging.page("/vocabularies", query, 3, 2, 5, List.of("x"));
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.self()).isEqualTo("/vocabularies?page=3&pageSize=2&sort=-name&name=ab");
        assertThat(page.prev()).isEqualTo("/vocabularies?page=2&pageSize=2&sort=-name&name=ab");
        assertThat(page.next()).isNull();
        assertThat(page.last()).isEqualTo("/vocabularies?page=3&pageSize=2&sort=-name&name=ab");
        PageResponse<String> beyond = Paging.page("/x", QueryParameters.parse(null), 9, 2, 5, List.of());
        assertThat(beyond.prev()).isEqualTo("/x?page=3&pageSize=2");
        PageResponse<String> empty = Paging.page("/x", QueryParameters.parse(null), 2, 20, 0, List.of());
        assertThat(empty.prev()).isNull();
        assertThat(empty.last()).isNull();
        assertThat(empty.totalPages()).isZero();
        assertThat(Paging.searchPage(List.of()).totalPages()).isZero();
        assertThat(Paging.searchPage(List.of("a")).totalPages()).isEqualTo(1);
    }
}
