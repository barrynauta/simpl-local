package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.store.InstantRange;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** Decisions D19, D20, D22, D58 and D59. */
class ParametersTest {

    private static MultiValueMap<String, String> params(String... nameValues) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (int i = 0; i < nameValues.length; i += 2) {
            map.add(nameValues[i], nameValues[i + 1]);
        }
        return map;
    }

    @Test
    void dateMatchesTheWholeUtcDayAndDateTimeTheSecond() {
        assertThat(ListParameters.parseDate("2026-11-18"))
                .isEqualTo(
                        new InstantRange(Instant.parse("2026-11-18T00:00:00Z"), Instant.parse("2026-11-19T00:00:00Z")));
        assertThat(ListParameters.parseDate("2026-11-18T10:00:00.900+01:00"))
                .isEqualTo(
                        new InstantRange(Instant.parse("2026-11-18T09:00:00Z"), Instant.parse("2026-11-18T09:00:01Z")));
        assertThat(ListParameters.parseDate("2026-11-18T09:00:00")).isNotNull();
        assertThat(ListParameters.parseDate("2026-02-30")).isNull();
        assertThat(ListParameters.parseDate("2026-11-18Tgarbage")).isNull();
    }

    @Test
    void filtersReportInvalidValues() {
        List<Issue> issues = new ArrayList<>();
        assertThat(ListParameters.nameFilter(params("name", "sh-acl"), issues)).isNull();
        assertThat(ListParameters.statusFilter(params("status", "active"), issues))
                .isNull();
        assertThat(ListParameters.dateFilter(params("creationDate", "yesterday"), "creationDate", issues))
                .isNull();
        assertThat(issues).extracting(Issue::name).containsExactly("name", "status", "creationDate");
        assertThat(ListParameters.nameFilter(params("name", "shacl"), issues)).isEqualTo("shacl");
    }

    @Test
    void pagingDefaultsAndLimits() {
        List<Issue> issues = new ArrayList<>();
        Paging paging = Paging.from(params(), issues);
        assertThat(paging).isEqualTo(new Paging(BigInteger.ONE, 20));
        Paging.from(params("page", "0", "pageSize", "101"), issues);
        Paging.from(params("page", "-1", "pageSize", "1.5"), issues);
        assertThat(issues).hasSize(4);
        assertThat(new Paging(new BigInteger("99999999999999999999"), 100).offset())
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void linksIncludePrevNextAndLast() {
        PageDto<String> middle = new Paging(BigInteger.TWO, 1)
                .response("/vocabularies", java.util.Map.of("sort", "-name"), 3, List.of("b"));
        assertThat(middle.self()).isEqualTo("/vocabularies?page=2&pageSize=1&sort=-name");
        assertThat(middle.prev()).isEqualTo("/vocabularies?page=1&pageSize=1&sort=-name");
        assertThat(middle.next()).isEqualTo("/vocabularies?page=3&pageSize=1&sort=-name");
        assertThat(middle.last()).isEqualTo("/vocabularies?page=3&pageSize=1&sort=-name");
        PageDto<String> beyond = new Paging(BigInteger.TEN, 1).response("/x", java.util.Map.of(), 3, List.of());
        assertThat(beyond.prev()).isEqualTo("/x?page=3&pageSize=1");
        assertThat(beyond.next()).isNull();
        PageDto<String> empty = new Paging(BigInteger.TWO, 20).response("/x", java.util.Map.of(), 0, List.of());
        assertThat(empty.totalPages()).isZero();
        assertThat(empty.last()).isNull();
        assertThat(empty.prev()).isNull();
        assertThat(PageDto.unpaged(List.of()).totalPages()).isZero();
    }

    @Test
    void pathAndTextRules() {
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkPathVersion("0", issues);
        RequestChecks.checkPathVersion("1a", issues);
        RequestChecks.checkPathVersion("007", issues);
        RequestChecks.checkPathName("vocabularyName", "Ab", issues);
        assertThat(issues).hasSize(3);
        assertThat(RequestChecks.requiredText("description", "  x  ", issues)).isEqualTo("x");
        assertThat(RequestChecks.requiredText("description", " ", issues)).isNull();
        assertThat(RequestChecks.requiredText("description", null, issues)).isNull();
        assertThat(RequestChecks.requiredText("description", "y".repeat(1001), issues))
                .isNull();
        assertThat(UriValues.isAbsoluteUri("relative/path")).isFalse();
        assertThat(UriValues.isAbsoluteUri("http://a b")).isFalse();
        assertThat(UriValues.isAbsoluteUri("https://purl.org/dc/terms/")).isTrue();
    }
}
