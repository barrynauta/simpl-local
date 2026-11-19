package eu.simpl.vocabularymanager.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application wiring that is not tied to one layer. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /**
     * The clock: UTC, so times are stored and returned in UTC (decision D58).
     *
     * @return the clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
