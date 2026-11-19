package eu.simpl.vocabulary.api.filter;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabulary.api.RouteTable;
import eu.simpl.vocabulary.api.RouteTable.BodyKind;
import eu.simpl.vocabulary.api.RouteTable.ResponseKind;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Accept (D63), Content-Type (D15) and routing (D67, D77) rules. */
class FilterRulesTest {

    @Test
    void acceptRules() {
        assertThat(RequestGateFilter.acceptable(List.of(), ResponseKind.JSON)).isTrue();
        assertThat(RequestGateFilter.acceptable(List.of("application/json"), ResponseKind.JSON))
                .isTrue();
        assertThat(RequestGateFilter.acceptable(List.of("application/problem+json"), ResponseKind.JSON))
                .isTrue();
        assertThat(RequestGateFilter.acceptable(List.of("text/html, */*;q=0.1"), ResponseKind.JSON))
                .isTrue();
        assertThat(RequestGateFilter.acceptable(List.of("text/html"), ResponseKind.JSON))
                .isFalse();
        assertThat(RequestGateFilter.acceptable(List.of("application/*"), ResponseKind.JSON))
                .isFalse();
        assertThat(RequestGateFilter.acceptable(List.of("application/json;q=0"), ResponseKind.JSON))
                .isFalse();
        assertThat(RequestGateFilter.acceptable(List.of("not a type///"), ResponseKind.JSON))
                .isFalse();
        assertThat(RequestGateFilter.acceptable(List.of("text/turtle"), ResponseKind.TURTLE))
                .isTrue();
        assertThat(RequestGateFilter.acceptable(List.of("application/json"), ResponseKind.TURTLE))
                .isFalse();
        assertThat(RequestGateFilter.acceptable(List.of(""), ResponseKind.JSON)).isTrue();
    }

    @Test
    void contentTypeRules() {
        assertThat(RequestGateFilter.supportedContentType(null, BodyKind.NONE)).isTrue();
        assertThat(RequestGateFilter.supportedContentType("application/json; charset=utf-8", BodyKind.JSON))
                .isTrue();
        assertThat(RequestGateFilter.supportedContentType("multipart/form-data; boundary=x", BodyKind.MULTIPART))
                .isTrue();
        assertThat(RequestGateFilter.supportedContentType("application/json", BodyKind.MULTIPART))
                .isFalse();
        assertThat(RequestGateFilter.supportedContentType(null, BodyKind.JSON)).isFalse();
        assertThat(RequestGateFilter.supportedContentType("bad", BodyKind.JSON)).isFalse();
    }

    @Test
    void routeTable() {
        assertThat(RouteTable.match("/v1/vocabularies/searches").orElseThrow().template())
                .isEqualTo("/vocabularies/searches");
        assertThat(RouteTable.match("/v1/vocabularies/Health").orElseThrow().template())
                .isEqualTo("/vocabularies/{vocabularyName}");
        assertThat(RouteTable.match("/v1/external-vocabularies/content")
                        .orElseThrow()
                        .allowHeader())
                .isEqualTo("POST");
        assertThat(RouteTable.match("/v1/external-vocabularies/X/content")
                        .orElseThrow()
                        .operation("GET"))
                .isPresent();
        assertThat(RouteTable.match("/v1/vocabularies/")).isEmpty();
        assertThat(RouteTable.match("/v1/unknown")).isEmpty();
        assertThat(RouteTable.match("/other")).isEmpty();
        assertThat(RouteTable.match(null)).isEmpty();
        assertThat(RouteTable.match("/v1/vocabularies/%zz")).isEmpty();
        assertThat(RouteTable.operations()).hasSize(19);
        assertThat(EntryFilter.causedBy(new RuntimeException(new IllegalStateException()), IllegalStateException.class))
                .isTrue();
        assertThat(EntryFilter.causedBy(new RuntimeException(), IllegalStateException.class))
                .isFalse();
    }
}
