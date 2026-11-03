package eu.simpl.cm.manager.service;

import eu.simpl.cm.common.web.ApiException;
import eu.simpl.cm.common.web.Ids;
import eu.simpl.cm.common.web.Times;
import eu.simpl.cm.manager.CmSettings;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.HistoryEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Read operations: A.2, A.3, A.6 and the OpenAPI agreement reads (D34). */
@Service
public class QueryService {
    private final ContractRepository contracts;
    private final ContractService records;
    private final CmSettings settings;

    public QueryService(ContractRepository contracts, ContractService records, CmSettings settings) {
        this.contracts = contracts;
        this.records = records;
        this.settings = settings;
    }

    public static Set<ContractStatus> parseStatuses(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        Set<ContractStatus> out = EnumSet.noneOf(ContractStatus.class);
        for (String s : csv.split(",")) {
            try {
                out.add(ContractStatus.valueOf(s.trim()));
            } catch (IllegalArgumentException e) {
                throw ApiException.typeMismatch("Unknown status " + s.trim());
            }
        }
        return out;
    }

    private static boolean contains(String value, String needle) {
        if (needle == null || needle.isEmpty()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    public Map<String, Object> list(String nid, String resourceName, String providerName, String consumerName, String status) {
        Set<ContractStatus> statuses = parseStatuses(status);
        List<Map<String, Object>> items = contracts.findAll().stream()
                .filter(r -> nid == null || nid.isEmpty() || nid.equals(r.contractNegotiationId))
                .filter(r -> contains(r.resourceName, resourceName))
                .filter(r -> contains(r.providerName, providerName))
                .filter(r -> contains(r.consumerName, consumerName))
                .filter(r -> statuses == null || statuses.contains(r.status))
                .sorted(Comparator.comparing((ContractRecord r) -> r.createdDate).reversed()
                        .thenComparing(r -> r.id, Comparator.reverseOrder()))
                .map(QueryService::item)
                .toList();
        return Map.of("items", items);
    }

    public static Map<String, Object> item(ContractRecord r) {
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
        m.put("consentStatus", r.consentStatus);
        m.put("createdDate", Times.rfc3339(r.createdDate));
        m.put("lastUpdatedDate", Times.rfc3339(r.lastUpdatedDate));
        return m;
    }

    public Map<String, Object> details(String nid) {
        Ids.pathId("contractNegotiationId", nid);
        ContractRecord r = contracts.findByContractNegotiationId(nid)
                .orElseThrow(() -> ApiException.notFound("No contract for contractNegotiationId " + nid));
        return details(r);
    }

    public Map<String, Object> details(ContractRecord r) {
        Map<String, Object> m = item(r);
        m.put("contractOfferId", r.contractOfferId);
        m.put("revocationRequestedBy", r.revocationRequestedBy == null ? null : r.revocationRequestedBy.name());
        m.put("validationResult", r.validationResult);
        m.put("validationReasonCode", r.validationReasonCode);
        m.put("validatedAt", Times.rfc3339(r.validatedAt));
        m.put("consumerSignatureDate", Times.rfc3339(r.consumerSignatureDate));
        m.put("providerSignatureDate", Times.rfc3339(r.providerSignatureDate));
        m.put("humanReadable", r.partyRole == eu.simpl.cm.manager.domain.PartyRole.CONSUMER ? r.humanReadable : null);
        m.put("usageContractHash", r.usageContractHash);
        m.put("ownCredentialId", r.ownCredentialId);
        m.put("counterpartyCredentialId", r.counterpartyCredentialId);
        m.put("allowedActions", LifecycleService.allowedActions(r));
        List<Map<String, Object>> history = new ArrayList<>();
        for (HistoryEntry h : records.history(r.contractNegotiationId)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("at", Times.rfc3339(h.at));
            e.put("fromStatus", h.fromStatus == null ? null : h.fromStatus.name());
            e.put("toStatus", h.toStatus == null ? null : h.toStatus.name());
            e.put("entryType", h.entryType);
            e.put("trigger", h.triggerType);
            e.put("actor", h.actor);
            e.put("reasonCode", h.reasonCode);
            e.put("reason", h.reason);
            e.put("correlationId", h.correlationId);
            history.add(e);
        }
        m.put("history", history);
        return m;
    }

    public Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        long total = 0;
        for (ContractStatus s : ContractStatus.values()) {
            long c = contracts.countByStatus(s);
            counts.put(s.name(), c);
            total += c;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("participantId", settings.participantId);
        out.put("generatedAt", Times.rfc3339(Times.now()));
        out.put("total", total);
        out.put("counts", counts);
        return out;
    }

    // ---- D34
    private ContractRecord byAid(String aidRaw) {
        String aid = Ids.pathUuid("contractAgreementId", aidRaw);
        List<ContractRecord> found = contracts.findByContractAgreementId(aid);
        if (found.isEmpty()) {
            throw ApiException.notFound("ContractAgreement not found for contractAgreementId = " + aid);
        }
        return found.get(0);
    }

    public Map<String, Object> agreement(String aid) {
        ContractRecord r = byAid(aid);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractAgreementId", r.contractAgreementId);
        m.put("contractDefinitionId", r.contractDefinitionId);
        if (r.consumerSignatureDate != null) {
            m.put("consumerSignatureDate", Times.local(r.consumerSignatureDate));
        }
        if (r.providerSignatureDate != null) {
            m.put("providerSignatureDate", Times.local(r.providerSignatureDate));
        }
        m.put("status", r.status.technical());
        m.put("contractNegotiationId", r.contractNegotiationId);
        m.put("assetId", r.assetId);
        m.put("providerId", r.providerId);
        m.put("consumerId", r.consumerId);
        m.put("contractOfferId", r.contractOfferId);
        return m;
    }

    public String agreementFile(String aid) {
        ContractRecord r = byAid(aid);
        if (r.consumerSignatureDate == null || r.providerSignatureDate == null) {
            throw ApiException.notFound("The contract agreement is not signed by both parties yet");
        }
        return "CONTRACT AGREEMENT: " + r.contractAgreementId + "\n"
                + "CONTRACT DEFINITION: " + r.contractDefinitionId + "\n"
                + "ASSET: " + r.assetId + "\n"
                + "PROVIDER ID: " + r.providerId + "\n"
                + "PROVIDER SIGNATURE DATE: " + Times.file(r.providerSignatureDate) + "\n"
                + "CONSUMER: " + r.consumerId + "\n"
                + "CONSUMER SIGNATURE DATE: " + Times.file(r.consumerSignatureDate);
    }

    public Map<String, Object> searchEstablished(String did, String assetId, String providerId, String consumerId) {
        List<Map<String, Object>> items = contracts.findByStatusIn(ContractStatus.ESTABLISHED).stream()
                .filter(r -> did == null || did.equals(r.contractDefinitionId))
                .filter(r -> assetId == null || assetId.equals(r.assetId))
                .filter(r -> providerId == null || providerId.equals(r.providerId))
                .filter(r -> consumerId == null || consumerId.equals(r.consumerId))
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("contractAgreementId", r.contractAgreementId);
                    m.put("consumerId", r.consumerId);
                    return m;
                }).toList();
        return Map.of("items", items);
    }
}
