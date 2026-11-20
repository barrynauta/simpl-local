package eu.simpl.vocabularymanager.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Field rules, query parameters, JSON bodies and paging links (D19, D21, D22, D37, D52, D59, D73, D78, D110 to D112). */
class RequestRulesTest {

    @Test
    void names() {
        assertThat(FieldRules.isValidName("Abc")).isTrue();
        assertThat(FieldRules.isValidName("A" + "b".repeat(63))).isTrue();
        assertThat(FieldRules.isValidName("A" + "b".repeat(64))).isFalse();
        assertThat(FieldRules.isValidName("Ab")).isFalse();
        assertThat(FieldRules.isValidName("abc")).isFalse();
        assertThat(FieldRules.isValidName("Ab-c")).isFalse();
        assertThat(FieldRules.isValidName(null)).isFalse();
        List<Issue> issues = new ArrayList<>();
        assertThat(FieldRules.name(null, "body", "name", issues)).isNull();
        assertThat(FieldRules.name("bad", "body", "name", issues)).isNull();
        assertThat(issues).hasSize(2);
    }

    @Test
    void textFieldsAreTrimmedAndLimited() {
        List<Issue> issues = new ArrayList<>();
        assertThat(FieldRules.text("  x  ", "description", true, issues)).isEqualTo("x");
        assertThat(FieldRules.text("a".repeat(1000), "description", true, issues))
                .hasSize(1000);
        assertThat(issues).isEmpty();
        assertThat(FieldRules.text("   ", "description", true, issues)).isNull();
        assertThat(FieldRules.text("a".repeat(1001), "description", true, issues))
                .isNull();
        assertThat(FieldRules.text(null, "changelog", true, issues)).isNull();
        assertThat(issues).hasSize(3);
        assertThat(FieldRules.text(null, "description", false, issues)).isNull();
        assertThat(FieldRules.text(" ", "description", false, issues)).isNull();
        assertThat(issues).hasSize(3);
    }

    @Test
    void integers() {
        List<Issue> issues = new ArrayList<>();
        assertThat(FieldRules.positiveInt("2147483647", "path", "version", Integer.MAX_VALUE, issues))
                .isEqualTo(Integer.MAX_VALUE);
        for (String bad : List.of("0", "2147483648", "2.0", "1e1", "+2", " 2", "", "99999999999")) {
            assertThat(FieldRules.positiveInt(bad, "path", "version", Integer.MAX_VALUE, issues))
                    .as(bad)
                    .isNull();
        }
        assertThat(FieldRules.positiveInt(null, "query", "page", 100, issues)).isNull();
        assertThat(issues).hasSize(9);
    }

    @Test
    void uris() {
        assertThat(FieldRules.isAbsoluteUri("http://purl.org/dc/terms/")).isTrue();
        assertThat(FieldRules.isAbsoluteUri("urn:isbn:1")).isTrue();
        assertThat(FieldRules.isAbsoluteUri("relative/path")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("http://a b")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("http://a/é")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("")).isFalse();
        assertThat(FieldRules.isAbsoluteUri("http://[bad")).isFalse();
        List<Issue> issues = new ArrayList<>();
        assertThat(FieldRules.uri(null, "body", "sourceUri", false, issues)).isNull();
        assertThat(FieldRules.uri(null, "body", "namespaceUri", true, issues)).isNull();
        assertThat(FieldRules.uri("x", "body", "namespaceUri", true, issues)).isNull();
        assertThat(issues).hasSize(2);
    }

    @Test
    void queryParameters() {
        QueryParameters ok = QueryParameters.parse("name=Ab%20c&page=2", Set.of("name", "page"));
        assertThat(ok.issues()).isEmpty();
        assertThat(ok.get("name")).isEqualTo("Ab c");
        assertThat(ok.inOrder().keySet()).containsExactly("name", "page");
        QueryParameters bad = QueryParameters.parse("x=1&page=1&page=2&bad=%zz&=v&flag", Set.of("page"));
        assertThat(bad.issues()).extracting(Issue::name).containsExactly("x", "page", "bad", "", "flag");
        assertThat(QueryParameters.parse(null, Set.of()).issues()).isEmpty();
    }

    @Test
    void pagingLinks() {
        QueryParameters parameters =
                QueryParameters.parse("sort=-name&pageSize=2&page=3", Set.of("sort", "page", "pageSize"));
        Paging paging = Paging.from(parameters, new ArrayList<>());
        assertThat(paging.offset()).isEqualTo(4);
        Paging.Links links = paging.links("/vocabularies", 5, parameters);
        assertThat(links.self()).isEqualTo("/vocabularies?page=3&pageSize=2&sort=-name");
        assertThat(links.first()).isEqualTo("/vocabularies?page=1&pageSize=2&sort=-name");
        assertThat(links.prev()).isEqualTo("/vocabularies?page=2&pageSize=2&sort=-name");
        assertThat(links.next()).isNull();
        assertThat(links.last()).isEqualTo("/vocabularies?page=3&pageSize=2&sort=-name");
        Paging beyond = new Paging(9, 2);
        assertThat(beyond.links("/x", 5, QueryParameters.parse("", Set.of())).prev())
                .isEqualTo("/x?page=3&pageSize=2");
        Paging.Links empty = new Paging(2, 20).links("/x", 0, QueryParameters.parse("", Set.of()));
        assertThat(empty.prev()).isNull();
        assertThat(empty.last()).isNull();
        assertThat(new Paging(1, 20)
                        .links("/x", 41, QueryParameters.parse("", Set.of()))
                        .next())
                .isEqualTo("/x?page=2&pageSize=20");
    }

    @Test
    void jsonBodies() {
        Map<String, Object> body = JsonBody.parse("{\"a\":1,\"b\":2.0,\"c\":1e1}".getBytes(StandardCharsets.UTF_8));
        assertThat(JsonBody.isInteger(body.get("a"))).isTrue();
        assertThat(JsonBody.isInteger(body.get("b"))).isFalse();
        assertThat(JsonBody.isInteger(body.get("c"))).isFalse();
        assertThat(JsonBody.isInteger(new java.math.BigInteger("99999999999999999999")))
                .isTrue();
        for (String bad : List.of("", "[1]", "{\"a\":1,\"a\":2}", "{\"a\":1} x", "{bad", "null")) {
            assertThatThrownBy(() -> JsonBody.parse(bad.getBytes(StandardCharsets.UTF_8)))
                    .as(bad)
                    .isInstanceOfSatisfying(
                            ProblemException.class, e -> assertThat(e.status()).isEqualTo(400));
        }
    }
}
