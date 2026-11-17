package eu.simpl.cm.manager.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics are created at startup when absent, one partition each (D46). */
@Configuration
@ConditionalOnProperty(name = "cm.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Bean
    NewTopic lifecycleTopic() {
        return TopicBuilder.name(Topics.LIFECYCLE).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic statusTopic() {
        return TopicBuilder.name(Topics.STATUS).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic notificationsTopic() {
        return TopicBuilder.name(Topics.NOTIFICATIONS).partitions(1).replicas(1).build();
    }
}
