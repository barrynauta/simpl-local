package eu.simpl.vocabulary.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Query and field rules (D19 to D22, D37, D49, D52, D56, D73). */
class RequestRulesTest {

    @Test
    void parsesQueryInOrderAndFlagsUnknownRepeatedAndMalformed() {
        QueryParameters query = QueryParameters.parse("b=2&a=1&&b=3&c&%zz=1&d=%zz");
        assertThat(query.parameters())
                .extracting(QueryParameters.Parameter::name)
                .containsExactly("b", "a", "b", "c");
        assertThat(query.get("b")).contains("2");
        assertThat(query.get("c")).contains("");
        assertThat(query.names()).containsExactly("b", "a", "c");
        List<Issue> issues = query.allowOnly(Set.of("a", "b"));
        assertThat(issues).extracting(Issue::name).containsExactly("%zz", "d", "b", "c");
        assertThat(issues).allMatch(i -> Issue.IN_QUERY.equals(i.in()));
        assertThat(QueryParameters.parse(null).requireNone()).isEmpty();
        assertThat(QueryParameters.parse("").parameters()).isEmpty();
        assertThat(QueryParameters.parse("x=1").requireNone())
                .extracting(Issue::name)
                .containsExactly("x");
    }

    @Test
    void pagingRules() {
        List<Issue> issues = new ArrayList<>();
        assertThat(QueryRules.page(QueryParameters.parse(""), issues)).isEqualTo(1);
        assertThat(QueryRules.pageSize(QueryParameters.parse(""), issues)).isEqualTo(20);
        assertThat(QueryRules.page(QueryParameters.parse("page=007"), issues)).isEqualTo(7);
        assertThat(QueryRules.page(QueryParameters.parse("page=99999999999999999999999"), issues))
                .isPositive();
        assertThat(QueryRules.pageSize(QueryParameters.parse("pageSize=100"), issues))
                .isEqualTo(100);
        assertThat(issues).isEmpty();
        QueryRules.page(QueryParameters.parse("page=0"), issues);
        QueryRules.page(QueryParameters.parse("page=-1"), issues);
        QueryRules.page(QueryParameters.parse("page=1.5"), issues);
        QueryRules.pageSize(QueryParameters.parse("pageSize=101"), issues);
        QueryRules.pageSize(QueryParameters.parse("pageSize=0"), issues);
        QueryRules.pageSize(QueryParameters.parse("pageSize=x"), issues);
        assertThat(issues)
                .extracting(Issue::name)
                .containsExactly("page", "page", "page", "pageSize", "pageSize", "pageSize");
    }

    @Test
    void filterRules() {
        List<Issue> issues = new ArrayList<>();
        assertThat(QueryRules.nameFilter(QueryParameters.parse("name=abc1"), issues))
                .isEqualTo("abc1");
        assertThat(QueryRules.status(QueryParameters.parse("status=DEPRECATED"), issues))
                .isEqualTo(VocabularyStatus.DEPRECATED);
        assertThat(QueryRules.sort(QueryParameters.parse("sort=-lastUpdated"), QueryRules.VOCABULARY_SORTS, issues))
                .isEqualTo("-lastUpdated");
        assertThat(QueryRules.date(QueryParameters.parse("creationDate=2024-03-01"), "creationDate", issues))
                .isNotNull();
        assertThat(QueryRules.nameFilter(QueryParameters.parse(""), issues)).isNull();
        assertThat(QueryRules.status(QueryParameters.parse(""), issues)).isNull();
        assertThat(QueryRules.sort(QueryParameters.parse(""), QueryRules.VOCABULARY_SORTS, issues))
                .isNull();
        assertThat(QueryRules.date(QueryParameters.parse(""), "creationDate", issues))
                .isNull();
        assertThat(issues).isEmpty();
        QueryRules.nameFilter(QueryParameters.parse("name=a%20b"), issues);
        QueryRules.status(QueryParameters.parse("status=Active"), issues);
        QueryRules.sort(QueryParameters.parse("sort=lastUpdated"), QueryRules.VERSION_SORTS, issues);
        QueryRules.date(QueryParameters.parse("lastUpdated=2024-13-01"), "lastUpdated", issues);
        assertThat(issues).extracting(Issue::name).containsExactly("name", "status", "sort", "lastUpdated");
    }

    @Test
    void fieldRules() {
        assertThat(FieldRules.isValidName("Abc")).isTrue();
        assertThat(FieldRules.isValidName("Ab")).isFalse();
        assertThat(FieldRules.isValidName("abc")).isFalse();
        assertThat(FieldRules.isValidName("A" + "b".repeat(64))).isFalse();
        assertThat(FieldRules.isValidName("Ab-c")).isFalse();
        assertThat(FieldRules.isValidName(null)).isFalse();
        assertThat(FieldRules.checkName(Issue.IN_BODY, "name", "x")).isPresent();
        assertThat(FieldRules.checkName(Issue.IN_BODY, "name", "Xyz")).isEmpty();
        assertThat(FieldRules.checkRequiredText("description", null)).isPresent();
        assertThat(FieldRules.checkRequiredText("description", "  ")).isPresent();
        assertThat(FieldRules.checkRequiredText("description", "x".repeat(1000)))
                .isEmpty();
        assertThat(FieldRules.checkRequiredText("description", " " + "x".repeat(1000) + " "))
                .isEmpty();
        assertThat(FieldRules.checkRequiredText("description", "x".repeat(1001)))
                .isPresent();
        assertThat(FieldRules.checkOptionalText("description", null)).isEmpty();
        assertThat(FieldRules.checkOptionalText("description", "x".repeat(1001)))
                .isPresent();
        assertThat(FieldRules.trimmedLength(" 😀 ")).isEqualTo(1);
        assertThat(FieldRules.isAbsoluteUri("http://purl.org/dc/terms/")).isTrue();
        assertThat(FieldRules.isAbsoluteUri("urn:x:y")).isTrue();
        assertThat(FieldRules.isAbsoluteUri("relative/path")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("http://a b")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("http://a/<b>")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("http://[bad")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("")).isFalse();
        assertThat(FieldRules.isAbsoluteUri(null)).isFalse();
        assertThat(FieldRules.normaliseNamespace("http://a/#")).isEqualTo("http://a/");
        assertThat(FieldRules.normaliseNamespace("http://a/")).isEqualTo("http://a");
        assertThat(FieldRules.normaliseNamespace("http://a")).isEqualTo("http://a");
        assertThat(FieldRules.parseVersion("3")).contains(3L);
        assertThat(FieldRules.parseVersion("0")).isEmpty();
        assertThat(FieldRules.parseVersion("-1")).isEmpty();
        assertThat(FieldRules.parseVersion("+1")).isEmpty();
        assertThat(FieldRules.parseVersion("1a")).isEmpty();
        assertThat(FieldRules.parseVersion(null)).isEmpty();
        assertThat(FieldRules.parseVersion("123456789012345678901234567890")).contains(Long.MAX_VALUE);
        assertThat(FieldRules.required("x").name()).isEqualTo("x");
    }

    @Test
    void jsonBodyRules() {
        assertThat(JsonBody.parse("{\"a\":1}".getBytes()).isObject()).isTrue();
        for (String bad : new String[] {"", "{", "{\"a\":1,\"a\":2}", "{} {}"}) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> JsonBody.parse(bad.getBytes()))
                    .isInstanceOf(eu.simpl.vocabulary.api.problem.ApiException.class);
        }
    }

    @Test
    void multipartFormHelpers() {
        MultipartForm form = new MultipartForm(List.of(
                new MultipartForm.FormPart("a", "1".getBytes()),
                new MultipartForm.FormPart("a", "2".getBytes()),
                new MultipartForm.FormPart("b", "x".getBytes())));
        assertThat(form.all("a")).hasSize(2);
        assertThat(form.first("b").map(MultipartForm.FormPart::text)).contains("x");
        assertThat(form.has("c")).isFalse();
        assertThat(form.names()).containsExactly("a", "b");
        assertThat(form.unknownOrRepeated(Set.of("a"), Set.of()))
                .extracting(Issue::name)
                .containsExactly("a", "b");
        assertThat(form.unknownOrRepeated(Set.of("a", "b"), Set.of("a"))).isEmpty();
    }
}
