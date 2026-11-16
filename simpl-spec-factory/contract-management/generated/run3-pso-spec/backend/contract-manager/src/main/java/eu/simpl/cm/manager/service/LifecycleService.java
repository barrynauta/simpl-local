package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.ApiException;
import eu.simpl.cm.manager.CmProperties;
import eu.simpl.cm.manager.domain.ConsentStatus;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.EntryType;
import eu.simpl.cm.manager.domain.LifecycleAction;
import eu.simpl.cm.manager.domain.PartyRole;
import eu.simpl.cm.manager.domain.ProcessedMessage;
import eu.simpl.cm.manager.domain.ProcessedMessageRepository;
import eu.simpl.cm.manager.domain.Trigger;
import eu.simpl.cm.manager.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Lifecycle after establishment (L1 to L7 of D37, D39 to D43, D47, D48), reads and audit counts. */
@Service
public class LifecycleService {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleService.class);

    private final CmProperties props;
    private final ContractStore store;
    private final ContractRepository contracts;
    private final ProcessedMessageRepository processed;
    private final EventPublisher events;
    private final ContractViews views;

    public LifecycleService(CmProperties props, ContractStore store, ContractRepository contracts,
                            ProcessedMessageRepository processed, EventPublisher events, ContractViews views) {
        this.props = props;
        this.store = store;
        this.contracts = contracts;
        this.processed = processed;
        this.events = events;
        this.views = views;
    }

    // ------------------------------------------------------------------ reads (A.2, A.3, A.6)

    @Transactional(readOnly = true)
    public Map<String, Object> list(String nid, String resourceName, String providerName, String consumerName,
                                    String status) {
        Set<ContractStatus> statuses = parseStatuses(status);
        List<Map<String, Object>> items = contracts.findAllByOrderByCreatedDateDesc().stream()
                .filter(r -> nid == null || nid.equals(r.contractNegotiationId))
                .filter(r -> contains(r.resourceName, resourceName))
                .filter(r -> contains(r.providerName, providerName))
                .filter(r -> contains(r.consumerName, consumerName))
                .filter(r -> statuses == null || statuses.contains(r.status))
                .map(views::item)
                .toList();
        return Map.of("items", items);
    }

    static Set<ContractStatus> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        Set<ContractStatus> out = EnumSet.noneOf(ContractStatus.class);
        for (String s : status.split(",")) {
            String t = s.trim();
            try {
                out.add(ContractStatus.valueOf(t));
            } catch (IllegalArgumentException e) {
                throw ApiException.argumentTypeMismatch("Unknown status " + t);
            }
        }
        return out;
    }

    private static boolean contains(String value, String filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        return value != null && value.toLowerCase().contains(filter.toLowerCase());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> details(String nid) {
        return views.details(require(nid));
    }

    private ContractRecord require(String nid) {
        return store.find(nid).orElseThrow(() -> ApiException.notFound("Unknown contractNegotiationId"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ContractStatus s : ContractStatus.values()) {
            counts.put(s.name(), 0L);
        }
        long total = 0;
        for (Object[] row : contracts.countByStatus()) {
            long n = ((Number) row[1]).longValue();
            counts.put(((ContractStatus) row[0]).name(), n);
            total += n;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("participantId", props.participantId());
        out.put("generatedAt", Instant.now().toString());
        out.put("total", total);
        out.put("counts", counts);
        return out;
    }

    // ------------------------------------------------------------------ user actions (A.4)

    public Map<String, Object> act(String nid, JsonNode body) {
        ContractRecord r = require(nid);
        Requests.requireObject(body);
        String actionName = Requests.text(body, "action");
        LifecycleAction action = LifecycleAction.parse(actionName)
                .orElseThrow(() -> ApiException.validation("Unknown action " + actionName + "; allowed: "
                        + Arrays.stream(LifecycleAction.values()).map(Enum::name).collect(Collectors.joining(", "))));
        String reason = reason(Requests.text(body, "reason"), action.reasonRequired());
        String actor = Requests.text(body, "actor");
        if (!action.allowed(r.status, r.partyRole, r.revocationRequestedBy)) {
            throw ApiException.invalidTransition(409, "Action " + action + " is not allowed for the "
                    + r.partyRole + " in status " + r.status);
        }
        ContractRecord after;
        try {
            after = store.transition(nid, EnumSet.of(r.status), action.to(), Trigger.USER, actor, null, reason,
                    rec -> applySideFields(rec, action, rec.partyRole));
        } catch (ContractStore.TransitionRefused | ObjectOptimisticLockingFailureException e) {
            throw ApiException.invalidTransition(409, "The contract changed concurrently");
        }
        publishLifecycle(after, action, reason, actor);
        return views.details(store.find(nid).orElseThrow());
    }

    static String reason(String raw, boolean required) {
        String t = raw == null ? null : raw.trim();
        if (t == null || t.isEmpty()) {
            if (required) {
                throw ApiException.validation("A reason is required for this action");
            }
            return null;
        }
        if (t.length() > 1000) {
            throw ApiException.validation("The reason must be at most 1000 characters");
        }
        return t;
    }

    private static void applySideFields(ContractRecord rec, LifecycleAction action, PartyRole requester) {
        if (action == LifecycleAction.REQUEST_REVOCATION) {
            rec.revocationRequestedBy = requester;
        } else if (action == LifecycleAction.ACCEPT_REVOCATION || action == LifecycleAction.DENY_REVOCATION) {
            rec.revocationRequestedBy = null;
        }
    }

    private void publishLifecycle(ContractRecord r, LifecycleAction action, String reason, String actor) {
        String recipient = r.partyRole == PartyRole.PROVIDER ? r.consumerId : r.providerId;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("messageId", UUID.randomUUID().toString());
        m.put("messageType", action.messageType());
        m.put("contractNegotiationId", r.contractNegotiationId);
        m.put("contractAgreementId", r.contractAgreementId);
        m.put("senderParticipantId", props.participantId());
        m.put("senderRole", r.partyRole.name());
        m.put("recipientParticipantId", recipient);
        m.put("reason", reason);
        m.put("actor", actor);
        m.put("occurredAt", Instant.now().toString());
        events.lifecycleMessage(m);
    }

    // ------------------------------------------------------------------ counterparty messages (D47, D48)

    public void onCounterpartyMessage(JsonNode msg) {
        String recipient = msg.path("recipientParticipantId").asText(null);
        String sender = msg.path("senderParticipantId").asText(null);
        if (!props.participantId().equals(recipient) || props.participantId().equals(sender)) {
            return;
        }
        String messageId = msg.path("messageId").asText(null);
        if (messageId == null || processed.existsById(messageId)) {
            return;
        }
        markProcessed(messageId);
        String nid = msg.path("contractNegotiationId").asText(null);
        ContractRecord r = nid == null ? null : store.find(nid).orElse(null);
        if (r == null) {
            LOG.info("Lifecycle message {} for unknown negotiation {} ignored", messageId, nid);
            return;
        }
        String type = msg.path("messageType").asText("");
        PartyRole senderRole;
        try {
            senderRole = PartyRole.valueOf(msg.path("senderRole").asText(""));
        } catch (IllegalArgumentException e) {
            senderRole = null;
        }
        LifecycleAction action = LifecycleAction.fromMessageType(type).orElse(null);
        String reason = msg.path("reason").isTextual() ? msg.get("reason").asText() : null;
        if (action == null || senderRole == null || senderRole == r.partyRole
                || !action.allowed(r.status, senderRole, r.revocationRequestedBy)) {
            store.note(nid, EntryType.COUNTERPARTY_MESSAGE_REJECTED, Trigger.COUNTERPARTY, null, type,
                    "Message " + type + " from " + sender + " does not fit status " + r.status);
            return;
        }
        final PartyRole requester = senderRole;
        try {
            store.transition(nid, EnumSet.of(r.status), action.to(), Trigger.COUNTERPARTY, null, null, reason,
                    rec -> applySideFields(rec, action, requester));
        } catch (ContractStore.TransitionRefused | ObjectOptimisticLockingFailureException e) {
            store.note(nid, EntryType.COUNTERPARTY_MESSAGE_REJECTED, Trigger.COUNTERPARTY, null, type,
                    "Message " + type + " from " + sender + " lost a race");
        }
    }

    @Transactional
    void markProcessed(String messageId) {
        ProcessedMessage p = new ProcessedMessage();
        p.messageId = messageId;
        p.processedAt = Instant.now();
        processed.save(p);
    }

    // ------------------------------------------------------------------ consent (A.5, D43)

    public Map<String, Object> consent(String nid, JsonNode body) {
        ContractRecord r = require(nid);
        Requests.requireObject(body);
        String value = Requests.text(body, "consentStatus");
        ConsentStatus cs;
        try {
            cs = value == null ? null : ConsentStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            cs = null;
        }
        if (cs == null || cs == ConsentStatus.NOT_DEFINED) {
            throw ApiException.validation("consentStatus must be PENDING, GRANTED or WITHDRAWN");
        }
        if (r.partyRole != PartyRole.PROVIDER || r.status.isTerminal() || r.consentStatus == ConsentStatus.NOT_DEFINED) {
            throw ApiException.invalidTransition(409, "Consent status cannot be changed for this contract");
        }
        final ConsentStatus target = cs;
        store.noteWithEvent(nid, rec -> rec.consentStatus = target, Trigger.SYSTEM, null, "CONSENT_STATUS_CHANGED",
                "Consent status " + r.consentStatus + " -> " + target);
        return views.details(store.find(nid).orElseThrow());
    }
}
