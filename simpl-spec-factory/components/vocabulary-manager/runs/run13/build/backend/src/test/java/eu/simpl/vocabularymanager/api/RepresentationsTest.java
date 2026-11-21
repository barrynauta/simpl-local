package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.api.dto.CollectionResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RepresentationsTest {

    @Test
    void middlePageHasAllLinksWithPagingFirst() {
        Map<String, String> others = new LinkedHashMap<>();
        others.put("sort", "-name");
        others.put("creationDate", "2026-11-18T10:00:00+01:00");
        CollectionResponse<String> page = Representations.page("/vocabularies", 2, 10, 35, others, List.of("x"));
        assertThat(page.totalPages()).isEqualTo(4);
        assertThat(page.self())
                .isEqualTo("/vocabularies?page=2&pageSize=10&sort=-name&creationDate=2026-11-18T10:00:00+01:00");
        assertThat(page.first()).startsWith("/vocabularies?page=1&pageSize=10");
        assertThat(page.prev()).startsWith("/vocabularies?page=1&");
        assertThat(page.next()).startsWith("/vocabularies?page=3&");
        assertThat(page.last()).startsWith("/vocabularies?page=4&");
    }

    @Test
    void beyondTheLastPagePrevPointsToTheLastPage() {
        CollectionResponse<String> page = Representations.page("/vocabularies", 9, 20, 25, Map.of(), List.of());
        assertThat(page.prev()).isEqualTo("/vocabularies?page=2&pageSize=20");
        assertThat(page.next()).isNull();
        assertThat(page.last()).isEqualTo("/vocabularies?page=2&pageSize=20");
    }

    @Test
    void emptyCollectionHasNoPrevNextLast() {
        CollectionResponse<String> page = Representations.page("/vocabularies", 3, 20, 0, Map.of(), List.of());
        assertThat(page.totalPages()).isZero();
        assertThat(page.prev()).isNull();
        assertThat(page.next()).isNull();
        assertThat(page.last()).isNull();
        assertThat(page.self()).isEqualTo("/vocabularies?page=3&pageSize=20");
    }

    @Test
    void searchResultsAreNotPaged() {
        CollectionResponse<String> none = Representations.unpaged(List.of());
        assertThat(none.totalPages()).isZero();
        assertThat(none.pageSize()).isEqualTo(100);
        assertThat(none.self()).isNull();
        assertThat(Representations.unpaged(List.of("a", "b")).totalPages()).isEqualTo(1);
    }
}
