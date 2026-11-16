package eu.simpl.cm.manager.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Used when Kafka is switched off (cm.kafka-enabled=false): logs only. */
@Component
@ConditionalOnProperty(name = "cm.kafka-enabled", havingValue = "false")
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void statusEvent(Map<String, Object> event) {
        LOG.info("status event {}", event);
    }

    @Override
    public void lifecycleMessage(Map<String, Object> message) {
        LOG.info("lifecycle message {}", message);
    }

    @Override
    public boolean notification(String nid, Map<String, Object> notification) {
        LOG.info("notification {}", notification);
        return true;
    }
}
