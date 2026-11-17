package eu.simpl.catalogue.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagingAndQualityParamsTest {
    @Test
    void pagingDefaultsCapAndRejects() {
        assertThat(Paging.parse(null, null)).isEqualTo(new Paging(1, 20));
        assertThat(Paging.parse("2", "500")).isEqualTo(new Paging(2, 100));
        assertThat(Paging.parse("3", "10").offset()).isEqualTo(20);
        assertThat(new Paging(1, 20).totalPages(41)).isEqualTo(3);
        assertThatThrownBy(() -> Paging.parse("0", null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Paging.parse("1", "x")).isInstanceOf(ApiException.class);
    }

    @Test
    void qualityParams() {
        assertThat(QualityParams.parse("", " ", null).isSet()).isFalse();
        assertThat(QualityParams.parse("70", "Green", 100.0)).isEqualTo(new QualityParams(70.0, "Green"));
        assertThatThrownBy(() -> QualityParams.parse("-1", null, null)).hasMessage(QualityParams.SCORE_DETAIL);
        assertThatThrownBy(() -> QualityParams.parse("101", null, 100.0)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> QualityParams.parse("abc", null, null)).isInstanceOf(ApiException.class);
    }
}
