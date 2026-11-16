package eu.simpl.cm.manager.service;

import eu.simpl.cm.manager.domain.EntryType;
import eu.simpl.cm.manager.domain.Trigger;
import eu.simpl.cm.manager.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** EmailNotification to the Notification Service topic (D22, D30, Appendix D.3). */
@Component
public class Notifier {

    private static final Logger LOG = LoggerFactory.getLogger(Notifier.class);

    private final Directory directory;
    private final EventPublisher events;
    private final ContractStore store;

    public Notifier(Directory directory, EventPublisher events, ContractStore store) {
        this.directory = directory;
        this.events = events;
        this.store = store;
    }

    /** Sends one email to the participant's directory contact, recording the outcome in the history. */
    public void email(String nid, String recipientParticipantId, String subject, String message, Trigger trigger) {
        Directory.Participant p = directory.participant(nid, recipientParticipantId);
        if (p == null || p.contactEmail() == null || p.contactEmail().isBlank()) {
            LOG.warn("No contact email for {} (negotiation {}): notification skipped", recipientParticipantId, nid);
            store.note(nid, EntryType.NOTIFICATION_SKIPPED, trigger, null, "NO_CONTACT_EMAIL",
                    "No contact email for " + recipientParticipantId);
            return;
        }
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("channel", "EMAIL");
        n.put("to", List.of(p.contactEmail()));
        n.put("cc", List.of());
        n.put("subject", subject);
        n.put("message", message);
        if (events.notification(nid, n)) {
            store.note(nid, EntryType.NOTIFICATION_SENT, trigger, null, null, subject);
        } else {
            store.note(nid, EntryType.NOTIFICATION_FAILED, trigger, null, "PUBLISH_FAILED", subject);
        }
    }
}
