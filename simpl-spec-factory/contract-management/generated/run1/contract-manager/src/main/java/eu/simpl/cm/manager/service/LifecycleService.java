package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.ApiException;
import eu.simpl.cm.common.web.Ids;
import eu.simpl.cm.common.web.JsonBody;
import eu.simpl.cm.common.web.Problems;
import eu.simpl.cm.common.web.Times;
import eu.simpl.cm.manager.CmSettings;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.LifecycleAction;
import eu.simpl.cm.manager.domain.PartyRole;
import eu.simpl.cm.manager.domain.ProcessedMessage;
import eu.simpl.cm.manager.domain.ProcessedMessageRepository;
import eu.simpl.cm.manager.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.simpl.cm.manager.service.ContractService.T_COUNTERPARTY;
import static eu.simpl.cm.manager.service.ContractService.T_USER;

/** D37 lifecycle transitions L1 to L7, lifecycle messages (D47, D48), consent status (D43). */
@Service
public class LifecycleService {
    private static final Logger log = LoggerFactory.getLogger(LifecycleService.class);
    public static final List<String> CONSENT_VALUES = List.of("PENDING", "GRANTED", "WITHDRAWN");

    private final ContractRepository contracts;
    private final ContractService records;
    private final ProcessedMessageRepository processed;
    private final EventPublisher events;
    private final CmSettings settings;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;

    public LifecycleService(ContractRepository contracts, ContractService records, ProcessedMessageRepository processed,
                            EventPublisher events, CmSettings settings, TransactionTemplate tx, ObjectMapper mapper) {
        this.contracts = contracts;
        this.records = records;
        this.processed = processed;
        this.events = events;
        this.settings = settings;
        this.tx = tx;
        this.mapper = mapper;
    }

    /** D39: required reasons are non-blank after trimming, at most 1000 characters. */
    public static String checkReason(LifecycleAction action, String reason) {
        String trimmed = reason == null ? null : reason.trim();
        if (action.reasonRequired && (trimmed == null || trimmed.isEmpty())) {
            throw ApiException.validation("reason is required for " + action);
        }
        if (reason != null && reason.length() > 1000) {
            throw ApiException.validation("reason must be at most 1000 characters");
        }
        return trimmed == null || trimmed.isEmpty() ? null : reason;
    }

    public void act(String nid, JsonNode body) {
        Ids.pathId("contractNegotiationId", nid);
        String actionName = JsonBody.text(body, "action");
        LifecycleAction action = LifecycleAction.parse(actionName);
        if (action == null) {
            throw ApiException.validation("action must be one of SUSPEND, REQUEST_REACTIVATION, REACTIVATE, "
                    + "REQUEST_REVOCATION, ACCEPT_REVOCATION, DENY_REVOCATION, TERMINATE");
        }
        String reason = checkReason(action, JsonBody.text(body, "reason"));
        String actor = JsonBody.text(body, "actor");
        tx.executeWithoutResult(s -> {
            ContractRecord r = contracts.findByContractNegotiationId(nid)
                    .orElseThrow(() -> ApiException.notFound("No contract for contractNegotiationId " + nid));
            if (!action.allowed(r.partyRole, r.status, r.revocationRequestedBy)) {
                throw new ApiException(409, Problems.INVALID_TRANSITION,
                        "Action " + action + " is not allowed for the " + r.partyRole + " in status " + r.status);
            }
            apply(r, action, r.partyRole, T_USER, actor, reason);
            publishMessage(r, action, reason, actor);
        });
    }

    private void apply(ContractRecord r, LifecycleAction action, PartyRole actingParty, String trigger, String actor, String reason) {
        if (action == LifecycleAction.REQUEST_REVOCATION) {
            r.revocationRequestedBy = actingParty;
        } else if (action == LifecycleAction.ACCEPT_REVOCATION || action == LifecycleAction.DENY_REVOCATION) {
            r.revocationRequestedBy = null;
        }
        records.transition(r, action.to, trigger, actor, null, reason);
    }

    private void publishMessage(ContractRecord r, LifecycleAction action, String reason, String actor) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("messageId", UUID.randomUUID().toString());
        m.put("messageType", action.messageType);
        m.put("contractNegotiationId", r.contractNegotiationId);
        m.put("contractAgreementId", r.contractAgreementId);
        m.put("senderParticipantId", settings.participantId);
        m.put("senderRole", r.partyRole.name());
        m.put("recipientParticipantId", r.counterpartyId());
        m.put("reason", reason);
        m.put("actor", actor);
        m.put("occurredAt", Times.rfc3339(Times.now()));
        Map<String, String> headers = records.headers(r.contractNegotiationId);
        AfterCommit.run(() -> events.publish(EventPublisher.LIFECYCLE_TOPIC, r.contractNegotiationId, m, headers));
    }

    /** D47, D48: apply a counterparty lifecycle message. */
    public void onCounterpartyMessage(String json) {
        JsonNode m;
        try {
            m = mapper.readTree(json);
        } catch (Exception e) {
            log.warn("Ignoring a lifecycle message that is not JSON");
            return;
        }
        String recipient = m.path("recipientParticipantId").asText(null);
        String sender = m.path("senderParticipantId").asText(null);
        String messageId = m.path("messageId").asText(null);
        if (!settings.participantId.equals(recipient) || settings.participantId.equals(sender) || messageId == null) {
            return;
        }
        String nid = m.path("contractNegotiationId").asText(null);
        tx.executeWithoutResult(s -> {
            if (processed.existsById(messageId)) {
                return;
            }
            ProcessedMessage pm = new ProcessedMessage();
            pm.messageId = messageId;
            pm.processedAt = Times.now();
            processed.save(pm);
            ContractRecord r = nid == null ? null : contracts.findByContractNegotiationId(nid).orElse(null);
            if (r == null) {
                log.warn("Lifecycle message {} for unknown contract {}", messageId, nid);
                return;
            }
            LifecycleAction action = LifecycleAction.byMessageType(m.path("messageType").asText());
            PartyRole senderRole;
            try {
                senderRole = PartyRole.valueOf(m.path("senderRole").asText());
            } catch (IllegalArgumentException e) {
                senderRole = null;
            }
            String reason = m.hasNonNull("reason") ? m.get("reason").asText() : null;
            boolean senderIsCounterparty = senderRole != null && senderRole == r.partyRole.other()
                    && r.counterpartyId() != null && r.counterpartyId().equals(sender);
            PartyRole requestedBy = r.revocationRequestedBy;
            boolean fits = action != null && senderIsCounterparty && action.actors.contains(senderRole)
                    && action.from.contains(r.status)
                    && (!action.respondsToRevocation || (requestedBy != null && requestedBy != senderRole));
            if (!fits) {
                records.note(r, "COUNTERPARTY_MESSAGE_REJECTED", T_COUNTERPARTY, null, null,
                        "Message " + m.path("messageType").asText() + " from " + sender + " does not fit status " + r.status);
                return;
            }
            apply(r, action, senderRole, T_COUNTERPARTY, null, reason);
        });
    }

    /** A.5 consent status, Provider records only. */
    public void consent(String nid, JsonNode body) {
        Ids.pathId("contractNegotiationId", nid);
        String value = JsonBody.text(body, "consentStatus");
        if (value == null || !CONSENT_VALUES.contains(value)) {
            throw ApiException.validation("consentStatus must be PENDING, GRANTED or WITHDRAWN");
        }
        tx.executeWithoutResult(s -> {
            ContractRecord r = contracts.findByContractNegotiationId(nid)
                    .orElseThrow(() -> ApiException.notFound("No contract for contractNegotiationId " + nid));
            if (r.partyRole != PartyRole.PROVIDER || r.status.isTerminal() || "NOT_DEFINED".equals(r.consentStatus)
                    || r.consentStatus == null) {
                throw new ApiException(409, Problems.INVALID_TRANSITION,
                        "Consent status cannot be changed for this contract");
            }
            String old = r.consentStatus;
            r.consentStatus = value;
            records.save(r);
            records.note(r, "CONSENT_STATUS_CHANGED", ContractService.T_SYSTEM, null, null,
                    "Consent status " + old + " -> " + value);
            records.statusEvent(r, r.status, ContractService.T_SYSTEM, null, "Consent status " + value);
        });
    }

    /** allowedActions for GET /contracts/{nid} (D37). */
    public static List<String> allowedActions(ContractRecord r) {
        List<String> out = new java.util.ArrayList<>();
        if (r.partyRole == PartyRole.CONSUMER && r.status == ContractStatus.REQUESTED) {
            out.add("ACCEPT");
            out.add("REJECT");
        }
        for (LifecycleAction a : LifecycleAction.values()) {
            if (a.allowed(r.partyRole, r.status, r.revocationRequestedBy)) {
                out.add(a.name());
            }
        }
        if (r.partyRole == PartyRole.PROVIDER && r.consentStatus != null && !"NOT_DEFINED".equals(r.consentStatus)
                && r.status.isOpen()) {
            out.add("UPDATE_CONSENT");
        }
        return out;
    }
}
