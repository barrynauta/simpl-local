package eu.simpl.cm.manager.events;

import java.util.Map;

/** Kafka output of CM (Appendix D). */
public interface EventPublisher {
    String LIFECYCLE_TOPIC = "simpl.contract.lifecycle.v1";
    String STATUS_TOPIC = "simpl.contract.status.v1";
    String NOTIFICATIONS_TOPIC = "notifications";

    /** Publishes asynchronously; failures are logged. */
    void publish(String topic, String key, Map<String, Object> payload, Map<String, String> headers);

    /** Publishes and waits for the broker acknowledgement. Returns false on failure. */
    boolean publishAndWait(String topic, String key, Map<String, Object> payload, Map<String, String> headers);
}
