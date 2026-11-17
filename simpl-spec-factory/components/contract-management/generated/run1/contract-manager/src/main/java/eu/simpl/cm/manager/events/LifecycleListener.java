package eu.simpl.cm.manager.events;

import eu.simpl.cm.manager.service.LifecycleService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** D47: receives lifecycle messages from the counterparty. Consumer group cm-<participantId> (D50). */
@Component
@ConditionalOnProperty(name = "cm.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class LifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(LifecycleListener.class);
    private final LifecycleService lifecycle;

    public LifecycleListener(LifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @KafkaListener(topics = EventPublisher.LIFECYCLE_TOPIC, groupId = "cm-${CM_PARTICIPANT_ID:provider-01}")
    public void onMessage(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("correlationId");
        if (header != null) {
            MDC.put("correlationId", new String(header.value(), StandardCharsets.UTF_8));
        }
        try {
            lifecycle.onCounterpartyMessage(record.value());
        } catch (Exception e) {
            log.error("Lifecycle message for {} could not be handled: {}", record.key(), e.toString(), e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
