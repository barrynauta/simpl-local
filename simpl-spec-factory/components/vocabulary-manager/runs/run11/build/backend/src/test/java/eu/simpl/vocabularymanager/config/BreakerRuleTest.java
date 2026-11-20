package eu.simpl.vocabularymanager.config;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.store.StoreCallException;
import org.junit.jupiter.api.Test;

/** A store 4xx is no breaker failure; 5xx and timeouts are (D134). */
class BreakerRuleTest {

    @Test
    void storeClientErrorsDoNotCount() {
        assertThat(InfrastructureConfig.isFailure(
                        new StoreCallException(StoreCallException.Kind.CLIENT_ERROR, "400", null)))
                .isFalse();
        assertThat(InfrastructureConfig.isFailure(
                        new StoreCallException(StoreCallException.Kind.SERVER_ERROR, "500", null)))
                .isTrue();
        assertThat(InfrastructureConfig.isFailure(new RuntimeException())).isTrue();
    }
}
