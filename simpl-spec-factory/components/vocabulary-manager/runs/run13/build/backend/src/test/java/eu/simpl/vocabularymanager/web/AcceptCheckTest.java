package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.web.OperationCatalog.Produces;
import java.util.List;
import org.junit.jupiter.api.Test;

class AcceptCheckTest {

    @Test
    void jsonOperations() {
        assertThat(AcceptCheck.acceptable(List.of(), Produces.JSON)).isTrue();
        assertThat(AcceptCheck.acceptable(List.of(""), Produces.JSON)).isTrue();
        assertThat(AcceptCheck.acceptable(List.of("application/json"), Produces.JSON))
                .isTrue();
        assertThat(AcceptCheck.acceptable(List.of("application/problem+json"), Produces.JSON))
                .isTrue();
        assertThat(AcceptCheck.acceptable(List.of("*/*"), Produces.JSON)).isTrue();
        assertThat(AcceptCheck.acceptable(List.of("application/*"), Produces.JSON))
                .isTrue();
        assertThat(AcceptCheck.acceptable(List.of("text/html, application/json;q=0.5"), Produces.JSON))
                .isTrue();
        assertThat(AcceptCheck.acceptable(List.of("text/turtle"), Produces.JSON))
                .isFalse();
        assertThat(AcceptCheck.acceptable(List.of("application/json;q=0"), Produces.JSON))
                .isFalse();
        assertThat(AcceptCheck.acceptable(List.of("not a media type"), Produces.JSON))
                .isFalse();
        assertThat(AcceptCheck.acceptable(List.of("text/turtle"), Produces.NONE))
                .isFalse();
    }

    @Test
    void turtleOperations() {
        assertThat(AcceptCheck.acceptable(List.of("text/turtle"), Produces.TURTLE))
                .isTrue();
        assertThat(AcceptCheck.acceptable(List.of("text/*"), Produces.TURTLE)).isTrue();
        assertThat(AcceptCheck.acceptable(List.of("application/*"), Produces.TURTLE))
                .isTrue();
        assertThat(AcceptCheck.acceptable(List.of("application/json"), Produces.TURTLE))
                .isFalse();
    }
}
