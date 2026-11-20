package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.domain.VocabularyFilter;
import eu.simpl.vocabularymanager.domain.VocabularyFilter.SortField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Query parsing, Accept and Content-Type checks, paging links (D19 to D23, D52, D59, D63, D78, D91). */
class RequestParsingTest {

    @Test
    void parsesFilters() {
        List<Issue> issues = new ArrayList<>();
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("name", "abc");
        parameters.put("status", "ACTIVE");
        parameters.put("creationDate", "2026-11-18");
        parameters.put("lastUpdated", "2026-11-18T09:27:00.5+01:00");
        parameters.put("sort", "-lastUpdated");
        VocabularyFilter filter = QueryParsers.vocabularyFilter(parameters, issues);
        assertThat(issues).isEmpty();
        assertThat(filter.creationFrom()).hasToString("2026-11-18T00:00:00Z");
        assertThat(filter.creationTo()).hasToString("2026-11-19T00:00:00Z");
        assertThat(filter.updatedFrom()).hasToString("2026-11-18T08:27:00Z");
        assertThat(filter.sort()).isEqualTo(SortField.LAST_UPDATED);
        assertThat(filter.descending()).isTrue();
        parameters.put("lastUpdated", "2026-11-18T09:27:00");
        assertThat(QueryParsers.vocabularyFilter(parameters, issues).updatedFrom())
                .hasToString("2026-11-18T09:27:00Z");
        for (String sort : List.of("status", "creationDate", "name")) {
            parameters.put("sort", sort);
            assertThat(QueryParsers.vocabularyFilter(parameters, issues).descending())
                    .isFalse();
        }
        assertThat(issues).isEmpty();
    }

    @Test
    void rejectsInvalidValues() {
        List<Issue> issues = new ArrayList<>();
        Map<String, String> parameters = Map.of(
                "name",
                "",
                "status",
                "x",
                "creationDate",
                "2026-13-01",
                "sort",
                "-version",
                "page",
                "a",
                "pageSize",
                "0");
        QueryParsers.vocabularyFilter(parameters, issues);
        QueryParsers.page(parameters, issues);
        assertThat(issues)
                .extracting(Issue::name)
                .containsExactlyInAnyOrder("name", "status", "creationDate", "sort", "page", "pageSize");
        List<Issue> more = new ArrayList<>();
        QueryParsers.versionSort("lastUpdated", more);
        assertThat(more).hasSize(1);
        assertThat(QueryParsers.versionSort(null, more).field()).isEqualTo(SortField.VERSION);
    }

    @Test
    void parsesQueryStrings() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("a=1&&b=x%20y&c");
        assertThat(RequestChecks.query(request, Set.of("a", "b", "c")))
                .containsEntry("a", "1")
                .containsEntry("b", "x y")
                .containsEntry("c", "");
        request.setQueryString("a=%zz");
        assertThatThrownBy(() -> RequestChecks.query(request, Set.of("a"))).isInstanceOf(ProblemException.class);
        request.setQueryString("=x");
        assertThatThrownBy(() -> RequestChecks.query(request, Set.of("a"))).isInstanceOf(ProblemException.class);
    }

    @Test
    void checksAcceptAndContentType() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestChecks.acceptJson(request);
        for (String accept : List.of(
                "application/json", "application/problem+json", "*/*", "application/*", "text/html, */*;q=0.1")) {
            request.removeHeader("Accept");
            request.addHeader("Accept", accept);
            RequestChecks.acceptJson(request);
        }
        for (String accept : List.of("text/html", "application/xml", "application/json;q=0", "not a type///")) {
            request.removeHeader("Accept");
            request.addHeader("Accept", accept);
            assertThatThrownBy(() -> RequestChecks.acceptJson(request))
                    .isInstanceOfSatisfying(
                            ProblemException.class,
                            ex -> assertThat(ex.problem().status()).isEqualTo(406));
        }
        request.removeHeader("Accept");
        request.addHeader("Accept", "text/turtle");
        RequestChecks.acceptTurtle(request);
        request.setContentType("application/json; charset=utf-8");
        RequestChecks.contentTypeJson(request);
        assertThatThrownBy(() -> RequestChecks.contentTypeMultipart(request)).isInstanceOf(ProblemException.class);
        request.setContentType("garbage");
        assertThatThrownBy(() -> RequestChecks.contentTypeJson(request)).isInstanceOf(ProblemException.class);
        request.setContentType(null);
        assertThatThrownBy(() -> RequestChecks.contentTypeJson(request)).isInstanceOf(ProblemException.class);
    }

    @Test
    void pathValues() {
        assertThat(RequestChecks.pathName("Health", "vocabularyName")).isEqualTo("Health");
        assertThatThrownBy(() -> RequestChecks.pathName("he", "vocabularyName")).isInstanceOf(ProblemException.class);
        assertThat(RequestChecks.pathVersion("2147483647")).isEqualTo(Integer.MAX_VALUE);
        assertThatThrownBy(() -> RequestChecks.pathVersion("2147483648")).isInstanceOf(ProblemException.class);
        assertThat(RequestChecks.positiveInt("-1")).isNull();
        assertThat(RequestChecks.positiveInt(null)).isNull();
        assertThat(RequestChecks.nameIssue("A".repeat(65), "body", "name")).isNotNull();
    }

    @Test
    void pagingLinks() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("name", "a b");
        parameters.put("page", "9");
        Map<String, Object> page = Representations.page("/vocabularies", parameters, 9, 2, 5, List.of());
        assertThat(page.get("self")).isEqualTo("/vocabularies?page=9&pageSize=2&name=a+b");
        assertThat(page.get("prev")).isEqualTo("/vocabularies?page=3&pageSize=2&name=a+b");
        assertThat(page.get("last")).isEqualTo("/vocabularies?page=3&pageSize=2&name=a+b");
        assertThat(page).doesNotContainKey("next");
        Map<String, Object> middle = Representations.page("/x", Map.of(), 2, 2, 5, List.of());
        assertThat(middle.get("prev")).isEqualTo("/x?page=1&pageSize=2");
        assertThat(middle.get("next")).isEqualTo("/x?page=3&pageSize=2");
        Map<String, Object> empty = Representations.page("/x", Map.of(), 1, 20, 0, List.of());
        assertThat(empty).doesNotContainKeys("prev", "next", "last");
        assertThat(empty.get("totalPages")).isEqualTo(0L);
        assertThat(Representations.searchPage(List.of()).get("totalPages")).isEqualTo(0);
        assertThat(Representations.time(null)).isNull();
    }

    @Test
    void problemsHaveDecidedTypes() {
        assertThat(Problems.generic(404, "x").type()).isEqualTo("urn:problem-type:belgif:resourceNotFound");
        assertThat(Problems.generic(405, "x").status()).isEqualTo(405);
        assertThat(Problems.generic(502, "x").status()).isEqualTo(500);
        assertThat(Problems.generic(400, "x").type()).isEqualTo("urn:problem-type:belgif:badRequest");
        assertThat(Problems.generic(411, "Length Required").type()).isEqualTo("urn:problem-type:simpl:httpError");
        assertThat(Problems.serviceUnavailable("d").headers()).containsEntry("Retry-After", "5");
        assertThat(Problems.toMap(Problems.payloadTooLarge())).containsKey("issues");
        assertThat(Problems.toMap(Problems.unauthorized())).doesNotContainKey("issues");
    }
}
