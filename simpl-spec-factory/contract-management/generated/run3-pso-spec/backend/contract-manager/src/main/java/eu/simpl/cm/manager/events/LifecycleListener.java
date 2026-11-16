package eu.simpl.cm.manager.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.CorrelationContext;
import eu.simpl.cm.manager.service.LifecycleService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Receives lifecycle messages from the counterparty (D47). Consumer group cm-<participantId> (D50). */
@Component
@ConditionalOnProperty(name = "cm.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class LifecycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleListener.class);

    private final LifecycleService lifecycle;
    private final ObjectMapper mapper;

    public LifecycleListener(LifecycleService lifecycle, ObjectMapper mapper) {
        this.lifecycle = lifecycle;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.LIFECYCLE, groupId = "cm-${cm.participant-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        Header h = record.headers().lastHeader("correlationId");
        String cid = h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
        CorrelationContext.set(cid != null ? cid : record.key());
        try {
            JsonNode msg = mapper.readTree(record.value());
            lifecycle.onCounterpartyMessage(msg);
        } catch (Exception e) {
            LOG.error("Could not handle lifecycle message at offset {}", record.offset(), e);
        } finally {
            CorrelationContext.clear();
        }
    }
}
