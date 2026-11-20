package eu.simpl.vocabularymanager.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.validation.DateFilters;
import eu.simpl.vocabularymanager.validation.Names;
import eu.simpl.vocabularymanager.validation.QueryParameters;
import eu.simpl.vocabularymanager.validation.UriRules;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Field rules, integers, dates, URIs and query strings (D19, D20, D21, D37, D52, D73, D91, D112, D130, D138). */
class HelpersTest {

    @Test
    void names() {
        assertThat(Names.isVocabularyName("Abc")).isTrue();
        assertThat(Names.isVocabularyName("Ab")).isFalse();
        assertThat(Names.isVocabularyName("A".repeat(65))).isFalse();
        assertThat(Names.isVocabularyName("abc")).isFalse();
        assertThat(Names.isVocabularyName("Ab-c")).isFalse();
        assertThat(Names.isNameFilter("shacl")).isTrue();
        assertThat(Names.isNameFilter("")).isFalse();
        assertThat(Names.trimmedLength("  ab  ")).isEqualTo(2);
    }

    @Test
    void integers() {
        assertThat(Names.positiveInt("1", 100)).isEqualTo(1);
        assertThat(Names.positiveInt("100", 100)).isEqualTo(100);
        assertThat(Names.positiveInt("101", 100)).isEqualTo(-1);
        assertThat(Names.positiveInt("2147483648", Integer.MAX_VALUE)).isEqualTo(-1);
        for (String bad : new String[] {"0", "2.0", "1e1", "+2", " 2", "", "9999999999999"}) {
            assertThat(Names.positiveInt(bad, Integer.MAX_VALUE)).isEqualTo(-1);
        }
    }

    @Test
    void dates() {
        var day = DateFilters.parse("2026-11-18").orElseThrow();
        assertThat(day.from()).isEqualTo(Instant.parse("2026-11-18T00:00:00Z"));
        assertThat(day.to()).isEqualTo(Instant.parse("2026-11-19T00:00:00Z"));
        assertThat(DateFilters.parse("2026-11-18T09:27:00.5Z").orElseThrow().from())
                .isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(DateFilters.parse("2026-11-18T10:27:00+01:00").orElseThrow().from())
                .isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(DateFilters.parse("2026-11-18T09:27:00").orElseThrow().from())
                .isEqualTo(Instant.parse("2026-11-18T09:27:00Z"));
        assertThat(DateFilters.parse("2026-02-30")).isEmpty();
        assertThat(DateFilters.parse("2026-11-18T09:27:00+19:00")).isEmpty();
        assertThat(DateFilters.parse("yesterday")).isEmpty();
        assertThat(DateFilters.parse(null)).isEmpty();
    }

    @Test
    void uris() {
        assertThat(UriRules.isAbsoluteUri("http://purl.org/dc/terms/")).isTrue();
        assertThat(UriRules.isAbsoluteUri("urn:x:y")).isTrue();
        assertThat(UriRules.isAbsoluteUri("relative/path")).isFalse();
        assertThat(UriRules.isAbsoluteUri("http://a b")).isFalse();
        assertThat(UriRules.isAbsoluteUri("")).isFalse();
        assertThat(ExternalVocabularyRepository.normalise("http://x/#")).isEqualTo("http://x/");
        assertThat(ExternalVocabularyRepository.normalise("http://x")).isEqualTo("http://x");
    }

    @Test
    void queryStrings() {
        QueryParameters ok = QueryParameters.parse("b=2&a=%41&&c", Set.of("a", "b", "c"));
        assertThat(ok.get("a")).isEqualTo("A");
        assertThat(ok.inOrder().keySet()).containsExactly("b", "a", "c");
        assertThatThrownBy(() -> QueryParameters.parse("x=1&x=2&a=1&a=2&a=3", Set.of("a")))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.issues()).hasSize(2));
        assertThatThrownBy(() -> QueryParameters.parse("a=%zz", Set.of("a"))).isInstanceOf(ApiException.class);
        assertThat(QueryParameters.parse(null, Set.of()).inOrder()).isEmpty();
    }
}
