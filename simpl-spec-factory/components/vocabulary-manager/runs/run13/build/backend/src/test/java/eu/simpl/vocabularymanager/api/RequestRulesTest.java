package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.store.VocabularyQuery.TimeRange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestRulesTest {

    private final List<ProblemIssue> issues = new ArrayList<>();

    @Test
    void namesFollowTheDecidedPattern() {
        assertThat(RequestRules.name("Abc", "body", "name", issues)).isEqualTo("Abc");
        assertThat(RequestRules.name("A".repeat(64), "body", "name", issues)).isNotNull();
        assertThat(issues).isEmpty();
        assertThat(RequestRules.name("Ab", "body", "name", issues)).isNull();
        assertThat(RequestRules.name("A".repeat(65), "body", "name", issues)).isNull();
        assertThat(RequestRules.name("abc", "path", "vocabularyName", issues)).isNull();
        assertThat(RequestRules.name("Ab-c", "body", "name", issues)).isNull();
        assertThat(RequestRules.name(null, "body", "name", issues)).isNull();
        assertThat(issues).hasSize(5);
        assertThat(issues.get(2).in()).isEqualTo("path");
        assertThat(issues.get(2).name()).isEqualTo("vocabularyName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2.0", "1e1", "+2", " 2", "", "abc", "2147483648"})
    void invalidIntegers(String value) {
        assertThat(RequestRules.positiveInt(value, "query", "page", RequestRules.INT32_MAX, issues))
                .isNull();
        assertThat(issues)
                .singleElement()
                .satisfies(issue -> assertThat(issue.name()).isEqualTo("page"));
    }

    @Test
    void validIntegersAndUpperBounds() {
        assertThat(RequestRules.positiveInt("2147483647", "query", "page", RequestRules.INT32_MAX, issues))
                .isEqualTo(Integer.MAX_VALUE);
        assertThat(RequestRules.positiveInt("100", "query", "pageSize", 100, issues))
                .isEqualTo(100);
        assertThat(RequestRules.positiveInt("101", "query", "pageSize", 100, issues))
                .isNull();
        assertThat(issues).hasSize(1);
    }

    @Test
    void textIsTrimmedAndLimited() {
        assertThat(RequestRules.text("  Hello ", "description", true, issues)).isEqualTo("Hello");
        assertThat(RequestRules.text("x".repeat(1000), "description", true, issues))
                .hasSize(1000);
        assertThat(issues).isEmpty();
        assertThat(RequestRules.text("x".repeat(1001), "description", true, issues))
                .isNull();
        assertThat(RequestRules.text("   ", "changelog", true, issues)).isNull();
        assertThat(RequestRules.text(null, "changelog", true, issues)).isNull();
        assertThat(RequestRules.text("   ", "description", false, issues)).isNull();
        assertThat(RequestRules.text(null, "description", false, issues)).isNull();
        assertThat(issues).hasSize(3);
    }

    @Test
    void nameFilterIsRelaxed() {
        assertThat(RequestRules.nameFilter("a1", issues)).isEqualTo("a1");
        assertThat(RequestRules.nameFilter("", issues)).isNull();
        assertThat(RequestRules.nameFilter("a b", issues)).isNull();
        assertThat(RequestRules.nameFilter("a".repeat(65), issues)).isNull();
        assertThat(issues).hasSize(3);
    }

    @Test
    void dateFilters() {
        TimeRange day = RequestRules.dateFilter("2026-11-18", "creationDate", issues);
        assertThat(day.from()).isEqualTo(Instant.parse("2026-11-18T00:00:00Z"));
        assertThat(day.toExclusive()).isEqualTo(Instant.parse("2026-11-19T00:00:00Z"));
        TimeRange second = RequestRules.dateFilter("2026-11-18T09:27:00.700Z", "creationDate", issues);
        assertThat(second.from()).isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(second.toExclusive()).isEqualTo(Instant.parse("2026-11-18T09:27:01Z"));
        TimeRange local = RequestRules.dateFilter("2026-11-18T09:27:00", "lastUpdated", issues);
        assertThat(local.from()).isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        TimeRange offset = RequestRules.dateFilter("2026-11-18T10:27:00+01:00", "lastUpdated", issues);
        assertThat(offset.from()).isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(issues).isEmpty();
        for (String bad : List.of(
                "2026-02-30",
                "2026-13-01",
                "2026-11-18T25:00:00Z",
                "2026-11-18T10:00:00+19:00",
                "18-11-2026",
                "2026-11-18Tx")) {
            assertThat(RequestRules.dateFilter(bad, "creationDate", issues))
                    .as(bad)
                    .isNull();
        }
        assertThat(issues).hasSize(6);
    }

    @Test
    void absoluteUris() {
        assertThat(RequestRules.absoluteUri("http://purl.org/dc/terms/")).isTrue();
        assertThat(RequestRules.absoluteUri("urn:example:x")).isTrue();
        assertThat(RequestRules.absoluteUri("relative/path")).isFalse();
        assertThat(RequestRules.absoluteUri("a,b")).isFalse();
        assertThat(RequestRules.absoluteUri("http://exa mple.org")).isFalse();
        assertThat(RequestRules.absoluteUri("")).isFalse();
        assertThat(RequestRules.absoluteUri(null)).isFalse();
    }
}
