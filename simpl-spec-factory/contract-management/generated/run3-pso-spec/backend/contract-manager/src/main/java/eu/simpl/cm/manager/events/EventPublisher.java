package eu.simpl.cm.manager.events;

import java.util.Map;

/** Outbound Kafka messages of Appendix D. */
public interface EventPublisher {

    /** D.2 status event. Published after the surrounding transaction commits. */
    void statusEvent(Map<String, Object> event);

    /** D.1 lifecycle message to the counterparty. Published after commit. */
    void lifecycleMessage(Map<String, Object> message);

    /** D.3 email notification. Returns true when the broker accepted it. */
    boolean notification(String contractNegotiationId, Map<String, Object> notification);
}
