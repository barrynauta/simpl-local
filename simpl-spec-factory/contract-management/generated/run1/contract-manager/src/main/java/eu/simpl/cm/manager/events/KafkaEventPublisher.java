package eu.simpl.cm.manager.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "cm.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventPublisher implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private final KafkaTemplate<String, String> template;
    private final ObjectMapper mapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> template, ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    private ProducerRecord<String, String> record(String topic, String key, Map<String, Object> payload, Map<String, String> headers) {
        try {
            ProducerRecord<String, String> r = new ProducerRecord<>(topic, key, mapper.writeValueAsString(payload));
            headers.forEach((k, v) -> {
                if (v != null) {
                    r.headers().add(k, v.getBytes(StandardCharsets.UTF_8));
                }
            });
            return r;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void publish(String topic, String key, Map<String, Object> payload, Map<String, String> headers) {
        try {
            template.send(record(topic, key, payload, headers)).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("Publish to {} for {} failed: {}", topic, key, ex.toString());
                }
            });
        } catch (Exception e) {
            log.error("Publish to {} for {} failed: {}", topic, key, e.toString());
        }
    }

    @Override
    public boolean publishAndWait(String topic, String key, Map<String, Object> payload, Map<String, String> headers) {
        try {
            template.send(record(topic, key, payload, headers)).get(10, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Publish to {} for {} failed: {}", topic, key, e.toString());
            return false;
        }
    }
}
