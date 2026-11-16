package eu.simpl.cm.manager.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.CorrelationContext;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Kafka implementation (D46, D49, D50, Appendix D). Key = nid, header correlationId. */
@Component
@ConditionalOnProperty(name = "cm.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    private ProducerRecord<String, String> record(String topic, String nid, Map<String, Object> payload)
            throws Exception {
        ProducerRecord<String, String> r = new ProducerRecord<>(topic, nid, mapper.writeValueAsString(payload));
        String cid = CorrelationContext.get();
        if (cid != null) {
            r.headers().add("correlationId", cid.getBytes(StandardCharsets.UTF_8));
        }
        if (nid != null) {
            r.headers().add("contractNegotiationId", nid.getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    private void sendAfterCommit(String topic, Map<String, Object> payload) {
        String nid = (String) payload.get("contractNegotiationId");
        String cid = CorrelationContext.get();
        AfterCommit.run(CorrelationContext.wrap(cid, () -> {
            try {
                kafka.send(record(topic, nid, payload));
            } catch (Exception e) {
                LOG.error("Could not publish to {} for {}", topic, nid, e);
            }
        }));
    }

    @Override
    public void statusEvent(Map<String, Object> event) {
        sendAfterCommit(Topics.STATUS, event);
    }

    @Override
    public void lifecycleMessage(Map<String, Object> message) {
        sendAfterCommit(Topics.LIFECYCLE, message);
    }

    @Override
    public boolean notification(String nid, Map<String, Object> notification) {
        try {
            kafka.send(record(Topics.NOTIFICATIONS, nid, notification)).get(10, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            LOG.error("Could not publish notification for {}", nid, e);
            return false;
        }
    }
}
