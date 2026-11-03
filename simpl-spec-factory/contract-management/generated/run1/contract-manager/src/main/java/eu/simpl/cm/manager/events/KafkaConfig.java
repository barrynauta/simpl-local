package eu.simpl.cm.manager.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** D46: the three topics, created at startup if absent, one partition each. */
@Configuration
@ConditionalOnProperty(name = "cm.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {
    @Bean
    NewTopic lifecycleTopic() {
        return TopicBuilder.name(EventPublisher.LIFECYCLE_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic statusTopic() {
        return TopicBuilder.name(EventPublisher.STATUS_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic notificationsTopic() {
        return TopicBuilder.name(EventPublisher.NOTIFICATIONS_TOPIC).partitions(1).replicas(1).build();
    }
}
