package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/** Route table, default deny check and paging links (D77, D84, D111). */
class RoutesTest {

    @Test
    void fixedSegmentsWinOverParameters() {
        assertThat(ApiRoutes.find("POST", "/vocabularies/searches")
                        .orElseThrow()
                        .operationId())
                .isEqualTo("searchVocabularies");
        assertThat(ApiRoutes.find("GET", "/vocabularies/searches")).isEmpty();
        assertThat(ApiRoutes.allow(ApiRoutes.matching("/external-vocabularies/content")))
                .isEqualTo("POST");
        assertThat(ApiRoutes.allow(ApiRoutes.matching("/external-vocabularies/Abc")))
                .isEqualTo("GET, DELETE");
        assertThat(ApiRoutes.relative("/other")).isEmpty();
        assertThat(ApiRoutes.all()).hasSize(19);
    }

    @Test
    void handlerWithoutRuleIsReported() {
        RequestMappingInfo stray = RequestMappingInfo.paths("/v1/stray")
                .methods(org.springframework.web.bind.annotation.RequestMethod.GET)
                .build();
        assertThat(RouteRuleCheck.check(Set.of(stray)))
                .contains("handler without rule: GET /v1/stray")
                .anyMatch(p -> p.startsWith("rule without handler"));
        RequestMappingInfo noMethod = RequestMappingInfo.paths("/v1/x").build();
        assertThat(RouteRuleCheck.check(Set.of(noMethod))).contains("handler without method: /v1/x");
    }

    @Test
    void linksPutPagingFirstAndEncodeValues() {
        assertThat(Representations.link("/vocabularies", Map.of("creationDate", "2026-01-01T00:00:00+01:00"), 2, 20))
                .isEqualTo("/vocabularies?page=2&pageSize=20&creationDate=2026-01-01T00%3A00%3A00%2B01%3A00");
    }
}
