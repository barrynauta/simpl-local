package eu.simpl.cm.manager.service;

import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.HistoryEntry;
import eu.simpl.cm.manager.domain.HistoryRepository;
import eu.simpl.cm.manager.domain.StateMachine;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON views of A.2 and A.3. */
@Component
public class ContractViews {

    private final HistoryRepository history;

    public ContractViews(HistoryRepository history) {
        this.history = history;
    }

    public Map<String, Object> item(ContractRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractNegotiationId", r.contractNegotiationId);
        m.put("contractAgreementId", r.contractAgreementId);
        m.put("contractDefinitionId", r.contractDefinitionId);
        m.put("assetId", r.assetId);
        m.put("resourceDescriptionId", r.resourceDescriptionId);
        m.put("resourceName", r.resourceName);
        m.put("providerId", r.providerId);
        m.put("providerName", r.providerName);
        m.put("consumerId", r.consumerId);
        m.put("consumerName", r.consumerName);
        m.put("partyRole", r.partyRole.name());
        m.put("status", r.status.name());
        m.put("reasonCode", r.reasonCode);
        m.put("consentStatus", r.consentStatus.name());
        m.put("createdDate", Dates.rfc3339(r.createdDate));
        m.put("lastUpdatedDate", Dates.rfc3339(r.lastUpdatedDate));
        return m;
    }

    public Map<String, Object> details(ContractRecord r) {
        Map<String, Object> m = item(r);
        m.put("contractOfferId", r.contractOfferId);
        m.put("revocationRequestedBy", r.revocationRequestedBy == null ? null : r.revocationRequestedBy.name());
        m.put("validationResult", r.validationResult);
        m.put("validationReasonCode", r.validationReasonCode);
        m.put("validatedAt", Dates.rfc3339(r.validatedAt));
        m.put("consumerSignatureDate", Dates.rfc3339(r.consumerSignatureDate));
        m.put("providerSignatureDate", Dates.rfc3339(r.providerSignatureDate));
        m.put("humanReadable", r.partyRole == eu.simpl.cm.manager.domain.PartyRole.CONSUMER ? r.humanReadable : null);
        m.put("usageContractHash", r.usageContractHash);
        m.put("ownCredentialId", r.ownCredentialId);
        m.put("counterpartyCredentialId", r.counterpartyCredentialId);
        m.put("allowedActions", StateMachine.allowedActions(r));
        List<Map<String, Object>> h = history.findByContractNegotiationIdOrderByIdAsc(r.contractNegotiationId)
                .stream().map(ContractViews::entry).toList();
        m.put("history", h);
        return m;
    }

    static Map<String, Object> entry(HistoryEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("at", Dates.rfc3339(e.at));
        m.put("fromStatus", e.fromStatus == null ? null : e.fromStatus.name());
        m.put("toStatus", e.toStatus == null ? null : e.toStatus.name());
        m.put("entryType", e.entryType.name());
        m.put("trigger", e.trigger.name());
        m.put("actor", e.actor);
        m.put("reasonCode", e.reasonCode);
        m.put("reason", e.reason);
        m.put("correlationId", e.correlationId);
        return m;
    }
}
