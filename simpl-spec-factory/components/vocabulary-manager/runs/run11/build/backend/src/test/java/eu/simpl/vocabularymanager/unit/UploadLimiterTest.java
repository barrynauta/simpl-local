package eu.simpl.vocabularymanager.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.service.UploadLimiter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** D92: at most 4 uploads at the same time per instance; the fifth gets 503 with Retry-After. */
class UploadLimiterTest {

    @Test
    void fifthUploadIsRefusedAndPermitsReturn() {
        UploadLimiter limiter =
                new UploadLimiter(new AppProperties("x", null, null, new AppProperties.Limits(1, 1, 1, 4, null, 1)));
        List<UploadLimiter.Permit> permits = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            permits.add(limiter.acquire());
        }
        assertThatThrownBy(limiter::acquire).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.problemType()).isEqualTo(ProblemType.SERVICE_UNAVAILABLE);
            assertThat(ex.detail()).isEqualTo("Too many uploads in progress. Retry later.");
            assertThat(ex.headers()).containsEntry("Retry-After", "5");
        });
        permits.get(0).close();
        assertThat(limiter.acquire()).isNotNull();
    }
}
