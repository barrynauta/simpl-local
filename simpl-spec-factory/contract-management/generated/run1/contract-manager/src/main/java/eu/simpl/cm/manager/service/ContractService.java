package eu.simpl.cm.manager.service;

import eu.simpl.cm.common.web.CorrelationIdFilter;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.common.web.Times;
import eu.simpl.cm.manager.CmSettings;
import eu.simpl.cm.manager.clients.StubClient;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.HistoryEntry;
import eu.simpl.cm.manager.domain.HistoryRepository;
import eu.simpl.cm.manager.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Record changes with history (D61), status events (D49) and connector callbacks. */
@Service
public class ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    public static final String T_USER = "USER";
    public static final String T_CONNECTOR = "CONNECTOR";
    public static final String T_COUNTERPARTY = "COUNTERPARTY";
    public static final String T_SCHEDULER = "SCHEDULER";
    public static final String T_SYSTEM = "SYSTEM";

    private final ContractRepository contracts;
    private final HistoryRepository history;
    private final EventPublisher events;
    private final StubClient stub;
    private final CmSettings settings;

    public ContractService(ContractRepository contracts, HistoryRepository history, EventPublisher events,
                           StubClient stub, CmSettings settings) {
        this.contracts = contracts;
        this.history = history;
        this.events = events;
        this.stub = stub;
        this.settings = settings;
    }

    public ContractRecord create(ContractRecord r, String trigger, String actor) {
        Instant now = Times.now();
        r.createdDate = now;
        r.lastUpdatedDate = now;
        ContractRecord saved = contracts.saveAndFlush(r);
        writeHistory(saved, null, saved.status, "TRANSITION", trigger, actor, saved.reasonCode, null);
        statusEvent(saved, null, trigger, actor, null);
        log.info("Contract {} created in {} (correlationId {})", saved.contractNegotiationId, saved.status, CorrelationIdFilter.current());
        return saved;
    }

    public ContractRecord transition(ContractRecord r, ContractStatus to, String trigger, String actor, String reasonCode,
                                     String reason) {
        ContractStatus from = r.status;
        r.status = to;
        if (reasonCode != null) {
            r.reasonCode = reasonCode;
        }
        r.lastUpdatedDate = Times.now();
        ContractRecord saved = contracts.saveAndFlush(r);
        writeHistory(saved, from, to, "TRANSITION", trigger, actor, reasonCode, reason);
        statusEvent(saved, from, trigger, actor, reason, reasonCode);
        log.info("Contract {} {} -> {} trigger {} reason {} (correlationId {})", saved.contractNegotiationId, from, to,
                trigger, reasonCode, CorrelationIdFilter.current());
        return saved;
    }

    public ContractRecord save(ContractRecord r) {
        r.lastUpdatedDate = Times.now();
        return contracts.saveAndFlush(r);
    }

    public void note(ContractRecord r, String entryType, String trigger, String actor, String reasonCode, String reason) {
        writeHistory(r, r.status, r.status, entryType, trigger, actor, reasonCode, reason);
    }

    public void writeHistory(ContractRecord r, ContractStatus from, ContractStatus to, String entryType, String trigger,
                             String actor, String reasonCode, String reason) {
        HistoryEntry h = new HistoryEntry();
        h.contractNegotiationId = r.contractNegotiationId;
        h.at = Times.now();
        h.fromStatus = from;
        h.toStatus = to;
        h.entryType = entryType;
        h.triggerType = trigger;
        h.actor = actor;
        h.reasonCode = reasonCode;
        h.reason = reason == null ? null : reason.length() > 2000 ? reason.substring(0, 2000) : reason;
        h.correlationId = CorrelationIdFilter.current();
        history.save(h);
    }

    public List<HistoryEntry> history(String nid) {
        return history.findByContractNegotiationIdOrderByIdAsc(nid);
    }

    public void statusEvent(ContractRecord r, ContractStatus previous, String trigger, String actor, String reason) {
        statusEvent(r, previous, trigger, actor, reason, r.reasonCode);
    }

    private void statusEvent(ContractRecord r, ContractStatus previous, String trigger, String actor, String reason,
                             String reasonCode) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("eventId", UUID.randomUUID().toString());
        e.put("participantId", settings.participantId);
        e.put("partyRole", r.partyRole.name());
        e.put("contractNegotiationId", r.contractNegotiationId);
        e.put("contractAgreementId", r.contractAgreementId);
        e.put("previousStatus", previous == null ? null : previous.name());
        e.put("newStatus", r.status.name());
        e.put("reasonCode", reasonCode);
        e.put("reason", reason);
        e.put("trigger", trigger);
        e.put("actor", actor);
        e.put("occurredAt", Times.rfc3339(Times.now()));
        Map<String, String> headers = headers(r.contractNegotiationId);
        AfterCommit.run(() -> events.publish(EventPublisher.STATUS_TOPIC, r.contractNegotiationId, e, headers));
    }

    public Map<String, String> headers(String nid) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("correlationId", CorrelationIdFilter.current());
        h.put("contractNegotiationId", nid);
        return h;
    }

    /** Calls a connector callback on the stub and records it in the history when a record exists. */
    public void callback(ContractRecord r, String nid, String endpoint, boolean value) {
        HttpJson.Response res = stub.callback(endpoint, nid, value);
        String text = "/" + endpoint + "/" + nid + "/" + value + " -> " + (res.status() < 0 ? "unreachable" : String.valueOf(res.status()));
        if (!res.ok()) {
            log.warn("Connector callback failed: {}", text);
        }
        if (r != null) {
            writeHistory(r, r.status, r.status, "CONNECTOR_CALLBACK", T_SYSTEM, null, null, text);
        }
    }

    /** D22 and D30: email to the participant's directory contact. */
    public void email(ContractRecord r, String participantId, String subject, String message) {
        HttpJson.Response p = stub.participant(r.contractNegotiationId, participantId);
        String to = null;
        if (p.status() == 200) {
            var json = stub.json(p);
            if (json != null && json.path("contactEmail").isTextual() && !json.get("contactEmail").asText().isBlank()) {
                to = json.get("contactEmail").asText();
            }
        }
        if (to == null) {
            note(r, "NOTIFICATION_SKIPPED", T_SYSTEM, null, null, "No contact email for " + participantId);
            return;
        }
        Map<String, Object> mail = new LinkedHashMap<>();
        mail.put("channel", "EMAIL");
        mail.put("to", List.of(to));
        mail.put("cc", List.of());
        mail.put("subject", subject);
        mail.put("message", message);
        boolean ok = events.publishAndWait(EventPublisher.NOTIFICATIONS_TOPIC, r.contractNegotiationId, mail, headers(r.contractNegotiationId));
        note(r, ok ? "NOTIFICATION_SENT" : "NOTIFICATION_FAILED", T_SYSTEM, null, null, subject);
    }
}
