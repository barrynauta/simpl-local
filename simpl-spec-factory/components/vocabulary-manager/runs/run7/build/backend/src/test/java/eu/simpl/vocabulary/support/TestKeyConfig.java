package eu.simpl.vocabulary.support;

import eu.simpl.vocabulary.security.RealmKeySource;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the realm key source: test keys, and a switch that makes the identity provider unreachable. */
@TestConfiguration
public class TestKeyConfig {

    public static final AtomicBoolean IDP_DOWN = new AtomicBoolean(false);

    @Bean
    @Primary
    public RealmKeySource testRealmKeySource() {
        RealmKeySource source = new RealmKeySource(
                () -> {
                    if (IDP_DOWN.get()) {
                        throw new IOException("identity provider down");
                    }
                    return TestTokens.publicKeys();
                },
                Duration.ZERO,
                Clock.systemUTC());
        source.tryRefresh();
        return source;
    }
}
