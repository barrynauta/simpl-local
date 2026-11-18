package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryHelpersTest {

    private static Vocabulary vocabulary(String name, VocabularyStatus status, String created, String updated) {
        return new Vocabulary(name, status, "u", Instant.parse(created), Instant.parse(updated), 1, "d");
    }

    private static VocabularyVersion version(int number, VocabularyStatus status, String created) {
        Instant at = Instant.parse(created);
        return new VocabularyVersion("V", number, "d", "c", status, "u", at, at);
    }

    @Test
    void dateFilterMatchesWholeUtcDayOrExactSecond() {
        Instant value = Instant.parse("2026-03-01T10:15:30Z");
        assertThat(DateFilter.parse("2026-03-01"))
                .get()
                .satisfies(f -> assertThat(f.matches(value)).isTrue());
        assertThat(DateFilter.parse("2026-03-02"))
                .get()
                .satisfies(f -> assertThat(f.matches(value)).isFalse());
        assertThat(DateFilter.parse("2026-03-01T10:15:30Z"))
                .get()
                .satisfies(f -> assertThat(f.matches(value)).isTrue());
        assertThat(DateFilter.parse("2026-03-01T10:15:30.900Z"))
                .get()
                .satisfies(f -> assertThat(f.matches(value)).isTrue());
        assertThat(DateFilter.parse("2026-03-01T11:15:30 01:00"))
                .get()
                .satisfies(f -> assertThat(f.matches(value)).isTrue());
        assertThat(DateFilter.parse("2026-03-01T10:15:30"))
                .get()
                .satisfies(f -> assertThat(f.matches(value)).isTrue());
        assertThat(DateFilter.parse("2026-03-01T10:15:31Z"))
                .get()
                .satisfies(f -> assertThat(f.matches(value)).isFalse());
        assertThat(DateFilter.parse("2026-02-30")).isEmpty();
        assertThat(DateFilter.parse("2026-02-01Tnope")).isEmpty();
        assertThat(DateFilter.parse("yesterday")).isEmpty();
        assertThat(DateFilter.parse("2026-03-01").get().matches(null)).isFalse();
    }

    @Test
    void vocabularySortBreaksTiesByNameAscending() {
        Vocabulary b = vocabulary("Bravo", VocabularyStatus.ACTIVE, "2026-01-01T00:00:00Z", "2026-01-03T00:00:00Z");
        Vocabulary a = vocabulary("Alpha", VocabularyStatus.DEPRECATED, "2026-01-02T00:00:00Z", "2026-01-03T00:00:00Z");
        Vocabulary c = vocabulary("Charlie", VocabularyStatus.ACTIVE, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
        List<Vocabulary> all = List.of(b, a, c);
        assertThat(all.stream()
                        .sorted(SortOrder.vocabularies(null).orElseThrow())
                        .map(Vocabulary::name))
                .containsExactly("Alpha", "Bravo", "Charlie");
        assertThat(all.stream()
                        .sorted(SortOrder.vocabularies("-name").orElseThrow())
                        .map(Vocabulary::name))
                .containsExactly("Charlie", "Bravo", "Alpha");
        assertThat(all.stream()
                        .sorted(SortOrder.vocabularies("status").orElseThrow())
                        .map(Vocabulary::name))
                .containsExactly("Bravo", "Charlie", "Alpha");
        assertThat(all.stream()
                        .sorted(SortOrder.vocabularies("-status").orElseThrow())
                        .map(Vocabulary::name))
                .containsExactly("Alpha", "Bravo", "Charlie");
        assertThat(all.stream()
                        .sorted(SortOrder.vocabularies("creationDate").orElseThrow())
                        .map(Vocabulary::name))
                .containsExactly("Bravo", "Charlie", "Alpha");
        assertThat(all.stream()
                        .sorted(SortOrder.vocabularies("-lastUpdated").orElseThrow())
                        .map(Vocabulary::name))
                .containsExactly("Alpha", "Bravo", "Charlie");
        assertThat(SortOrder.vocabularies("version")).isEmpty();
    }

    @Test
    void versionSortDefaultsToNewestFirst() {
        List<VocabularyVersion> all = List.of(
                version(1, VocabularyStatus.DEPRECATED, "2026-01-01T00:00:00Z"),
                version(3, VocabularyStatus.ACTIVE, "2026-01-01T00:00:00Z"),
                version(2, VocabularyStatus.DEPRECATED, "2026-01-02T00:00:00Z"));
        assertThat(all.stream().sorted(SortOrder.versions(null).orElseThrow()).map(VocabularyVersion::version))
                .containsExactly(3, 2, 1);
        assertThat(all.stream()
                        .sorted(SortOrder.versions("creationDate").orElseThrow())
                        .map(VocabularyVersion::version))
                .containsExactly(3, 1, 2);
        assertThat(all.stream()
                        .sorted(SortOrder.versions("-status").orElseThrow())
                        .map(VocabularyVersion::version))
                .containsExactly(2, 1, 3);
        assertThat(all.stream().sorted(SortOrder.versions("name").orElseThrow()).map(VocabularyVersion::version))
                .containsExactly(3, 2, 1);
        assertThat(SortOrder.versions("lastUpdated")).isEmpty();
    }

    @Test
    void pageCutsAndCountsPages() {
        List<Integer> all = List.of(1, 2, 3, 4, 5);
        Page<Integer> second = Page.of(all, new PageRequest(2, 2));
        assertThat(second.items()).containsExactly(3, 4);
        assertThat(second.totalPages()).isEqualTo(3);
        assertThat(Page.of(all, new PageRequest(9, 2)).items()).isEmpty();
        assertThat(Page.of(List.of(), new PageRequest(1, 20)).totalPages()).isZero();
    }

    @Test
    void filterCombinesNameStatusAndDates() {
        Vocabulary v =
                vocabulary("HealthShacl", VocabularyStatus.DEPRECATED, "2026-01-01T00:00:00Z", "2026-01-05T00:00:00Z");
        assertThat(VocabularyFilter.NONE.test(v)).isTrue();
        assertThat(new VocabularyFilter("shacl", null, null, null).test(v)).isTrue();
        assertThat(new VocabularyFilter("other", null, null, null).test(v)).isFalse();
        assertThat(new VocabularyFilter(null, VocabularyStatus.ACTIVE, null, null).test(v))
                .isFalse();
        assertThat(new VocabularyFilter(
                                null, null, DateFilter.parse("2026-01-01").orElseThrow(), null)
                        .test(v))
                .isTrue();
        assertThat(new VocabularyFilter(
                                null, null, null, DateFilter.parse("2026-01-01").orElseThrow())
                        .test(v))
                .isFalse();
    }
}
