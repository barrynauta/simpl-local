package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.domain.Status;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyNames;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Paging, links, filters and form checks (D19 to D23, D37, D52, D58, D59). */
class WebHelpersTest {

    @Test
    void pagingDefaultsAndLimits() {
        List<Issue> issues = new ArrayList<>();
        assertThat(Paging.read(Map.of(), issues)).isEqualTo(new Paging(1, 20));
        Paging.read(Map.of("page", List.of("0"), "pageSize", List.of("101")), issues);
        Paging.read(Map.of("page", List.of("x"), "pageSize", List.of("1.5")), issues);
        assertThat(issues).extracting(Issue::name).containsExactly("page", "pageSize", "page", "pageSize");
    }

    @Test
    void pageLinks() {
        List<Integer> items = IntStream.rangeClosed(1, 45).boxed().toList();
        CollectionPage<Integer> second = CollectionPage.of(items, 2, 20, "/vocabularies", "sort=name");
        assertThat(second.items()).hasSize(20).first().isEqualTo(21);
        assertThat(second.self()).isEqualTo("/vocabularies?page=2&pageSize=20&sort=name");
        assertThat(second.prev()).isEqualTo("/vocabularies?page=1&pageSize=20&sort=name");
        assertThat(second.next()).isEqualTo("/vocabularies?page=3&pageSize=20&sort=name");
        assertThat(second.last()).isEqualTo("/vocabularies?page=3&pageSize=20&sort=name");
        assertThat(second.totalPages()).isEqualTo(3);

        CollectionPage<Integer> beyond = CollectionPage.of(items, 9, 20, "/vocabularies", "");
        assertThat(beyond.items()).isEmpty();
        assertThat(beyond.prev()).isEqualTo("/vocabularies?page=3&pageSize=20");
        assertThat(beyond.next()).isNull();

        CollectionPage<Integer> empty = CollectionPage.of(List.of(), 1, 20, "/vocabularies", "");
        assertThat(empty.totalPages()).isZero();
        assertThat(empty.last()).isNull();
        assertThat(empty.prev()).isNull();
        assertThat(CollectionPage.of(List.of(), 2, 20, "/x", "").prev()).isNull();

        CollectionPage<Integer> search = CollectionPage.unpaged(List.of());
        assertThat(search.totalPages()).isZero();
        assertThat(search.self()).isNull();
        assertThat(CollectionPage.unpaged(List.of(1)).totalPages()).isEqualTo(1);
    }

    @Test
    void dateFilters() {
        assertThat(DateFilter.parse("2026-11-18").matches("2026-11-18T23:59:59Z"))
                .isTrue();
        assertThat(DateFilter.parse("2026-11-18").matches("2026-11-19T00:00:00Z"))
                .isFalse();
        assertThat(DateFilter.parse("2026-11-18T09:27:00Z").matches("2026-11-18T09:27:00Z"))
                .isTrue();
        assertThat(DateFilter.parse("2026-11-18T10:27:00+01:00").matches("2026-11-18T09:27:00Z"))
                .isTrue();
        assertThat(DateFilter.parse("2026-11-18T09:27:00").matches("2026-11-18T09:27:00Z"))
                .isTrue();
        assertThat(DateFilter.parse("2026-11-18T09:27:01Z").matches("2026-11-18T09:27:00Z"))
                .isFalse();
        assertThat(DateFilter.parse("2026-11-18").matches(null)).isFalse();
        assertThat(DateFilter.parse("2026-02-30")).isNull();
        assertThat(DateFilter.parse("2026-11-18Tnope")).isNull();
        assertThat(DateFilter.parse("18-11-2026")).isNull();
    }

    @Test
    void namesAndVersions() {
        assertThat(VocabularyNames.isValidName("Abc")).isTrue();
        assertThat(VocabularyNames.isValidName("Ab")).isFalse();
        assertThat(VocabularyNames.isValidName("abc")).isFalse();
        assertThat(VocabularyNames.isValidName("A" + "b".repeat(63))).isTrue();
        assertThat(VocabularyNames.isValidName("A" + "b".repeat(64))).isFalse();
        assertThat(VocabularyNames.isValidName(null)).isFalse();
        assertThat(VocabularyNames.isValidVersion("1")).isTrue();
        assertThat(VocabularyNames.isValidVersion("0")).isFalse();
        assertThat(VocabularyNames.isValidVersion("-1")).isFalse();
        assertThat(VocabularyNames.isValidVersion("99999999999")).isFalse();
        assertThat(Uris.isAbsoluteUri("http://purl.org/dc/terms/")).isTrue();
        assertThat(Uris.isAbsoluteUri("relative/path")).isFalse();
        assertThat(Uris.isAbsoluteUri("http://bad uri")).isFalse();
    }

    @Test
    void formFields() {
        List<FormPart> parts = List.of(
                new FormPart("name", null, "lower".getBytes()),
                new FormPart("description", null, "  ".getBytes()),
                new FormPart("changelog", null, "x".repeat(1001).getBytes()),
                new FormPart("version", null, "2".getBytes()),
                new FormPart("extra", null, "1".getBytes()),
                new FormPart("extra", null, "2".getBytes()));
        FormFields fields =
                new FormFields(parts, Set.of("vocabularyFile", "name", "description", "changelog"), Set.of());
        assertThat(fields.requiredFile("vocabularyFile")).isNull();
        assertThat(fields.requiredName("name")).isNull();
        assertThat(fields.requiredText("description")).isNull();
        assertThat(fields.requiredText("changelog")).isNull();
        assertThat(fields.issues())
                .extracting(Issue::name)
                .containsExactly("version", "extra", "vocabularyFile", "name", "description", "changelog");

        FormFields twice = new FormFields(
                List.of(new FormPart("name", null, "Abc".getBytes()), new FormPart("name", null, "Abd".getBytes())),
                Set.of("name"),
                Set.of());
        assertThat(twice.issues()).hasSize(1);
        assertThat(new FormFields(List.of(), Set.of("d"), Set.of()).optionalText("d"))
                .isNull();
        FormFields ok = new FormFields(List.of(new FormPart("d", null, " text ".getBytes())), Set.of("d"), Set.of());
        assertThat(ok.optionalText("d")).isEqualTo("text");
        assertThat(ok.requiredText("d")).isEqualTo("text");
    }

    @Test
    void vocabularyOrderBreaksTiesByNameAscending() {
        Vocabulary b =
                new Vocabulary("Bbb", Status.ACTIVE, "u", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z", 1, "d");
        Vocabulary a =
                new Vocabulary("Aaa", Status.ACTIVE, "u", "2026-01-01T00:00:00Z", "2026-01-03T00:00:00Z", 1, "d");
        Vocabulary z =
                new Vocabulary("AZz", Status.DEPRECATED, "u", "2025-01-01T00:00:00Z", "2026-01-01T00:00:00Z", 1, "d");
        Vocabulary lower =
                new Vocabulary("Abz", Status.ACTIVE, "u", "2027-01-01T00:00:00Z", "2026-01-01T00:00:00Z", 1, "d");
        List<Vocabulary> all = List.of(b, a, z, lower);
        assertThat(sorted(all, null)).containsExactly("AZz", "Aaa", "Abz", "Bbb");
        assertThat(sorted(all, "-name")).containsExactly("Bbb", "Abz", "Aaa", "AZz");
        assertThat(sorted(all, "-status")).containsExactly("AZz", "Aaa", "Abz", "Bbb");
        assertThat(sorted(all, "creationDate")).containsExactly("AZz", "Aaa", "Bbb", "Abz");
        assertThat(sorted(all, "-creationDate")).containsExactly("Abz", "Aaa", "Bbb", "AZz");
        assertThat(sorted(all, "lastUpdated")).containsExactly("AZz", "Abz", "Bbb", "Aaa");
    }

    private static List<String> sorted(List<Vocabulary> all, String sort) {
        return all.stream()
                .sorted(VocabularyController.vocabularyOrder(sort))
                .map(Vocabulary::name)
                .toList();
    }

    @Test
    void versionOrderDefaultsToNewestFirst() {
        List<VocabularyVersion> versions = List.of(
                new VocabularyVersion("V", 1, Status.DEPRECATED, "d", "c", "u", "2026-01-01T00:00:00Z", "t"),
                new VocabularyVersion("V", 3, Status.ACTIVE, "d", "c", "u", "2026-01-03T00:00:00Z", "t"),
                new VocabularyVersion("V", 2, Status.DEPRECATED, "d", "c", "u", "2026-01-02T00:00:00Z", "t"));
        assertThat(versions.stream()
                        .sorted(VocabularyController.versionOrder(null))
                        .map(VocabularyVersion::version))
                .containsExactly(3, 2, 1);
        assertThat(versions.stream()
                        .sorted(VocabularyController.versionOrder("creationDate"))
                        .map(VocabularyVersion::version))
                .containsExactly(1, 2, 3);
        assertThat(versions.stream()
                        .sorted(VocabularyController.versionOrder("status"))
                        .map(VocabularyVersion::version))
                .containsExactly(3, 2, 1);
        assertThat(versions.stream()
                        .sorted(VocabularyController.versionOrder("-name"))
                        .map(VocabularyVersion::version))
                .containsExactly(3, 2, 1);
    }
}
