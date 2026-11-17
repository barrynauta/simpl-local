package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.web.ApiException;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.common.web.Ids;
import eu.simpl.cm.common.web.JsonBody;
import eu.simpl.cm.common.web.Problems;
import eu.simpl.cm.common.web.Times;
import eu.simpl.cm.manager.CmSettings;
import eu.simpl.cm.manager.clients.UibeClient;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.PartyRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static eu.simpl.cm.manager.service.ContractService.T_CONNECTOR;
import static eu.simpl.cm.manager.service.ContractService.T_SCHEDULER;
import static eu.simpl.cm.manager.service.ContractService.T_USER;

/** Synchronous part of the establishment operations (BP07, D19 to D33). */
@Service
public class EstablishmentService {
    private final ContractRepository contracts;
    private final ContractService records;
    private final Enricher enricher;
    private final UibeClient uibe;
    private final CmSettings settings;
    private final TransactionTemplate tx;
    private final Async async;
    private final SigningService signing;
    private final ObjectMapper mapper;

    public EstablishmentService(ContractRepository contracts, ContractService records, Enricher enricher, UibeClient uibe,
                                CmSettings settings, TransactionTemplate tx, Async async, SigningService signing,
                                ObjectMapper mapper) {
        this.contracts = contracts;
        this.records = records;
        this.enricher = enricher;
        this.uibe = uibe;
        this.settings = settings;
        this.tx = tx;
        this.async = async;
        this.signing = signing;
        this.mapper = mapper;
    }

    private ContractRecord byNidOr400(String nid) {
        return contracts.findByContractNegotiationId(nid)
                .orElseThrow(() -> ApiException.validation("Unknown contractNegotiationId"));
    }

    private static ApiException wrongParty(String detail) {
        return new ApiException(400, Problems.WRONG_PARTY, detail);
    }

    // ---------------------------------------------------------------- D19 existence check
    public Map<String, Object> search(String nid, JsonNode body) {
        Ids.pathId("contractNegotiationId", nid);
        String assetId = JsonBody.requiredId(body, "assetId");
        String consumerId = JsonBody.requiredId(body, "consumerId");
        String providerId = JsonBody.optionalId(body, "providerId");
        if (providerId == null) {
            providerId = settings.participantId;
        }
        String aid = JsonBody.text(body, "contractAgreementId");
        if (aid != null && !Ids.isUuid(aid)) {
            throw ApiException.validation("contractAgreementId must be a UUID");
        }
        String did = JsonBody.optionalId(body, "contractDefinitionId");
        boolean providerSide = providerId.equals(settings.participantId);
        if (!providerSide && !consumerId.equals(settings.participantId)) {
            throw wrongParty("Neither providerId nor consumerId is this participant");
        }
        final String fProviderId = providerId;
        final String fAid = aid;
        Object[] result = tx.execute(status -> {
            List<ContractRecord> open = contracts.findByAssetIdAndProviderIdAndConsumerIdAndStatusIn(assetId, fProviderId,
                    consumerId, ContractStatus.OPEN).stream().filter(r -> !r.contractNegotiationId.equals(nid)).toList();
            ContractRecord own = null;
            if (open.isEmpty() && providerSide && contracts.findByContractNegotiationId(nid).isEmpty()) {
                ContractRecord r = new ContractRecord();
                r.contractNegotiationId = nid;
                r.contractAgreementId = fAid;
                r.contractDefinitionId = did;
                r.assetId = assetId;
                r.providerId = fProviderId;
                r.consumerId = consumerId;
                r.partyRole = PartyRole.PROVIDER;
                r.status = ContractStatus.REQUESTED;
                enricher.enrich(r);
                own = records.create(r, T_CONNECTOR, null);
            } else {
                own = contracts.findByContractNegotiationId(nid).orElse(null);
            }
            return new Object[]{open.isEmpty() ? null : open.get(0), own};
        });
        ContractRecord existing = (ContractRecord) result[0];
        ContractRecord own = (ContractRecord) result[1];
        boolean exists = existing != null;
        Map<String, Object> to = new LinkedHashMap<>();
        body.fields().forEachRemaining(e -> to.put(e.getKey(), e.getValue()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractAgreementId", exists ? existing.contractAgreementId : fAid);
        out.put("contractDefinitionId", exists ? existing.contractDefinitionId : did);
        out.put("exists", exists);
        out.put("contractSearchTO", to);
        String ownNid = own == null ? null : own.contractNegotiationId;
        async.submit("isPresent " + nid, () -> {
            ContractRecord r = ownNid == null ? null : contracts.findByContractNegotiationId(ownNid).orElse(null);
            records.callback(r, nid, "isPresent", exists);
        });
        return out;
    }

    // ---------------------------------------------------------------- D20 create offer data at the consumer
    public Map<String, Object> create(String aidRaw, String did, JsonNode body) {
        String aid = Ids.pathUuid("contractAgreementId", aidRaw);
        Ids.pathId("contractDefinitionId", did);
        String nid = JsonBody.requiredId(body, "contractNegotiationId");
        String assetId = JsonBody.requiredId(body, "assetId");
        String providerId = JsonBody.requiredId(body, "providerId");
        String consumerId = JsonBody.requiredId(body, "consumerId");
        String offerId = JsonBody.requiredId(body, "contractOfferId");
        if (!consumerId.equals(settings.participantId)) {
            throw wrongParty("/create runs at the Consumer only");
        }
        if (contracts.findByContractNegotiationId(nid).isPresent()) {
            throw new ApiException(400, Problems.ALREADY_EXISTS, "A contract with this contractNegotiationId already exists");
        }
        JsonNode asset = enricher.asset(nid, assetId);
        if (asset == null) {
            throw ApiException.notFound("Asset not found: " + assetId);
        }
        if (!providerId.equals(asset.path("providerId").asText())) {
            throw new ApiException(403, Problems.FORBIDDEN, "The asset is not owned by the provider");
        }
        String providerName = enricher.participantName(nid, providerId);
        String consumerName = enricher.participantName(nid, consumerId);
        String html = OfferHtml.build(asset, providerName != null ? providerName : asset.path("providerName").asText(null), consumerName);
        String hash = OfferHtml.hash(html);

        ContractRecord r = new ContractRecord();
        r.contractNegotiationId = nid;
        r.contractAgreementId = aid;
        r.contractDefinitionId = did;
        r.contractOfferId = offerId;
        r.assetId = assetId;
        r.providerId = providerId;
        r.consumerId = consumerId;
        r.partyRole = PartyRole.CONSUMER;
        r.status = ContractStatus.REQUESTED;
        Enricher.applyAsset(r, asset);
        if (providerName != null) {
            r.providerName = providerName;
        }
        r.consumerName = consumerName;
        r.humanReadable = html;
        r.usageContractHash = hash;

        HttpJson.Response ui = uibe.postOffer(nid, html, hash);
        if (!ui.ok()) {
            throw new ApiException(500, Problems.INTERNAL_SERVER_ERROR, "The contract UI backend did not accept the offer");
        }
        String offerUrl = offerUrl(nid);
        ContractRecord saved;
        try {
            saved = tx.execute(s -> records.create(r, T_CONNECTOR, null));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ApiException(400, Problems.ALREADY_EXISTS, "A contract with this contractNegotiationId already exists");
        }
        async.submit("offer email " + nid, () -> {
            ContractRecord rec = contracts.findByContractNegotiationId(nid).orElseThrow();
            records.email(rec, consumerId, "Usage contract offer available for review: " + nid,
                    "A usage contract offer for " + (rec.resourceName == null ? assetId : rec.resourceName)
                            + " is available for your review: " + offerUrl);
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractAgreementId", saved.contractAgreementId);
        out.put("contractDefinitionId", saved.contractDefinitionId);
        out.put("status", "CREATED");
        out.put("contractNegotiationId", nid);
        out.put("assetId", assetId);
        out.put("resourceDescriptionId", saved.resourceDescriptionId);
        out.put("resourceDescriptionName", saved.resourceName);
        out.put("providerId", providerId);
        out.put("providerName", saved.providerName);
        out.put("consumerId", consumerId);
        out.put("consumerName", saved.consumerName);
        out.put("contractOfferId", offerId);
        out.put("usageContract", html);
        out.put("usageContractHash", hash);
        out.put("contractStatus", "REQUESTED");
        out.put("offerUrl", offerUrl);
        return out;
    }

    public String offerUrl(String nid) {
        return settings.uiPublicUrl + "/contractOffers/" + nid;
    }

    // ---------------------------------------------------------------- D23, D44 accept or reject at the consumer
    public Map<String, Object> offerDecision(String nid, String operation, String reason, String actor) {
        if (!"accept".equals(operation) && !"reject".equals(operation)) {
            throw ApiException.typeMismatch("operation must be accept or reject");
        }
        if (reason != null && !"EXPIRED".equals(reason)) {
            throw ApiException.typeMismatch("reason must be EXPIRED");
        }
        if (reason != null && "accept".equals(operation)) {
            throw ApiException.typeMismatch("reason is only allowed with reject");
        }
        Ids.pathId("contractNegotiationId", nid);
        tx.executeWithoutResult(s -> {
            ContractRecord r = byNidOr400(nid);
            if (r.partyRole != PartyRole.CONSUMER) {
                throw wrongParty("Offer decisions run at the Consumer only");
            }
            if (r.status != ContractStatus.REQUESTED) {
                throw new ApiException(400, Problems.OFFER_NOT_PENDING, "The offer is not pending, status " + r.status);
            }
            if ("accept".equals(operation)) {
                records.transition(r, ContractStatus.ACCEPTED, T_USER, actor, null, null);
                async.submit("consumer signing " + nid, () -> signing.consumerSign(nid));
            } else if ("EXPIRED".equals(reason)) {
                records.transition(r, ContractStatus.TERMINATED, T_SCHEDULER, null, "OFFER_EXPIRED", "Offer expired");
                async.submit("signed false " + nid, () -> signing.callback(nid, "signed", false));
            } else {
                records.transition(r, ContractStatus.REJECTED, T_USER, actor, null, null);
                async.submit("signed false " + nid, () -> signing.callback(nid, "signed", false));
            }
        });
        return initiated(nid);
    }

    private static Map<String, Object> initiated(String nid) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractNegotiationId", nid);
        out.put("status", "INITIATED");
        return out;
    }

    // ---------------------------------------------------------------- D33 CM POST /contractOffers/{nid}
    public Map<String, Object> transferOffer(String nid, JsonNode body) {
        Ids.pathId("contractNegotiationId", nid);
        String html = JsonBody.text(body, "humanReadable");
        String hash = JsonBody.text(body, "hash");
        if (html == null || html.isBlank() || hash == null || hash.isBlank()) {
            throw ApiException.validation("humanReadable and hash are required");
        }
        ContractRecord r = byNidOr400(nid);
        if (r.partyRole != PartyRole.CONSUMER) {
            throw wrongParty("Offer presentations run at the Consumer only");
        }
        if (r.status != ContractStatus.REQUESTED) {
            throw new ApiException(400, Problems.OFFER_NOT_PENDING, "The offer is not pending, status " + r.status);
        }
        HttpJson.Response ui = uibe.postOffer(nid, html, hash);
        if (!ui.ok()) {
            throw new ApiException(500, Problems.INTERNAL_SERVER_ERROR, "The contract UI backend did not accept the offer");
        }
        tx.executeWithoutResult(s -> {
            ContractRecord rec = byNidOr400(nid);
            rec.humanReadable = html;
            rec.usageContractHash = hash;
            records.save(rec);
            records.note(rec, "OFFER_UPDATE", T_CONNECTOR, null, null, "Offer presentation replaced");
        });
        return initiated(nid);
    }

    // ---------------------------------------------------------------- D33 PUT /contractOffers/{nid}/update
    public Map<String, Object> updateOffer(String nid, JsonNode body) {
        Ids.pathId("contractNegotiationId", nid);
        String status = JsonBody.text(body, "status");
        if (status == null || !List.of("CREATED", "APPROVED", "REJECTED").contains(status)) {
            throw ApiException.validation("status must be CREATED, APPROVED or REJECTED");
        }
        String info = JsonBody.text(body, "additionalInfo");
        tx.executeWithoutResult(s -> {
            ContractRecord r = byNidOr400(nid);
            if (r.partyRole != PartyRole.CONSUMER) {
                throw wrongParty("Offer status updates run at the Consumer only");
            }
            if ("REJECTED".equals(status) && (r.status == ContractStatus.REQUESTED || r.status == ContractStatus.ACCEPTED
                    || r.status == ContractStatus.SIGNED)) {
                records.transition(r, ContractStatus.FAILED, T_CONNECTOR, null, "PROVIDER_REJECTED", info);
            } else {
                records.note(r, "OFFER_UPDATE", T_CONNECTOR, null, null,
                        "Offer status update " + status + (info == null ? "" : ": " + info));
            }
        });
        return initiated(nid);
    }

    // ---------------------------------------------------------------- D25 provider validate and sign trigger
    public Map<String, Object> issue(String aidRaw, String did, JsonNode body) {
        String aid = Ids.pathUuid("contractAgreementId", aidRaw);
        Ids.pathId("contractDefinitionId", did);
        String nid;
        String offerId = null;
        if (body != null) {
            nid = JsonBody.requiredId(body, "contractNegotiationId");
            String providerId = JsonBody.optionalId(body, "providerId");
            String consumerId = JsonBody.optionalId(body, "consumerId");
            String assetId = JsonBody.optionalId(body, "assetId");
            offerId = JsonBody.optionalId(body, "contractOfferId");
            if (!settings.participantId.equals(providerId)) {
                throw wrongParty("/issue runs at the Provider only");
            }
            Optional<ContractRecord> existing = contracts.findByContractNegotiationId(nid);
            if (existing.isEmpty()) {
                if (assetId == null || consumerId == null) {
                    throw ApiException.validation("assetId and consumerId are required to create the record");
                }
                ContractRecord r = new ContractRecord();
                r.contractNegotiationId = nid;
                r.contractAgreementId = aid;
                r.contractDefinitionId = did;
                r.contractOfferId = offerId;
                r.assetId = assetId;
                r.providerId = providerId;
                r.consumerId = consumerId;
                r.partyRole = PartyRole.PROVIDER;
                r.status = ContractStatus.REQUESTED;
                enricher.enrich(r);
                try {
                    tx.executeWithoutResult(s -> records.create(r, T_CONNECTOR, null));
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // created concurrently, continue with the stored record
                }
            }
        } else {
            nid = contracts.findByContractAgreementId(aid).stream().filter(r -> r.partyRole == PartyRole.PROVIDER)
                    .map(r -> r.contractNegotiationId).findFirst()
                    .orElseThrow(() -> ApiException.validation("Unknown contractAgreementId"));
        }
        final String fOfferId = offerId;
        boolean start = Boolean.TRUE.equals(tx.execute(s -> {
            ContractRecord r = byNidOr400(nid);
            if (r.partyRole != PartyRole.PROVIDER) {
                throw wrongParty("/issue runs at the Provider only");
            }
            if (signing.isDuplicate(r)) {
                signing.recordDuplicate(r, T_CONNECTOR);
                return false;
            }
            if (r.status != ContractStatus.REQUESTED) {
                throw new ApiException(400, Problems.INVALID_TRANSITION, "The contract is not in status REQUESTED, status " + r.status);
            }
            r.contractAgreementId = aid;
            r.contractDefinitionId = did;
            if (fOfferId != null) {
                r.contractOfferId = fOfferId;
            }
            records.save(r);
            return true;
        }));
        if (start && signing.claim(nid)) {
            async.submit("provider validation " + nid, () -> signing.providerValidateAndSign(nid));
        } else if (start) {
            tx.executeWithoutResult(s -> signing.recordDuplicate(byNidOr400(nid), T_CONNECTOR));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractAgreementId", aid);
        out.put("contractDefinitionId", did);
        out.put("status", "INITIATED");
        return out;
    }

    // ---------------------------------------------------------------- D31 consumer verification
    public Map<String, Object> verify(String nid, JsonNode vc) {
        Ids.pathId("contractNegotiationId", nid);
        if (vc == null || !vc.isObject() || vc.isEmpty()) {
            throw ApiException.validation("A verifiable credential is required");
        }
        boolean start = Boolean.TRUE.equals(tx.execute(s -> {
            ContractRecord r = byNidOr400(nid);
            if (r.partyRole != PartyRole.CONSUMER) {
                throw wrongParty("Verification runs at the Consumer only");
            }
            if (signing.isDuplicate(r)) {
                signing.recordDuplicate(r, T_CONNECTOR);
                return false;
            }
            if (r.status != ContractStatus.SIGNED) {
                throw ApiException.validation("The contract is not in status SIGNED, status " + r.status);
            }
            return true;
        }));
        if (start && signing.claim(nid)) {
            JsonNode copy = vc.deepCopy();
            async.submit("consumer verification " + nid, () -> signing.consumerVerify(nid, copy));
        } else if (start) {
            tx.executeWithoutResult(s -> signing.recordDuplicate(byNidOr400(nid), T_CONNECTOR));
        }
        return initiated(nid);
    }

    // ---------------------------------------------------------------- D32 finalisation at the provider
    public Map<String, Object> agreementStatus(String aidRaw, String did, JsonNode body) {
        String aid = Ids.pathUuid("contractAgreementId", aidRaw);
        Ids.pathId("contractDefinitionId", did);
        String status = JsonBody.text(body, "status");
        if (!"FINALIZED".equals(status) && !"TERMINATED".equals(status)) {
            throw ApiException.validation("status must be FINALIZED or TERMINATED");
        }
        return tx.execute(s -> {
            List<ContractRecord> found = contracts.findByContractAgreementId(aid);
            if (found.isEmpty()) {
                throw ApiException.validation("Unknown contractAgreementId");
            }
            ContractRecord r = found.stream().filter(x -> x.partyRole == PartyRole.PROVIDER).findFirst()
                    .orElseThrow(() -> wrongParty("Agreement status updates run at the Provider only"));
            if ("FINALIZED".equals(status) && r.status == ContractStatus.SIGNED) {
                r = records.transition(r, ContractStatus.ACTIVE, T_CONNECTOR, null, null, null);
            } else if ("TERMINATED".equals(status)
                    && (r.status == ContractStatus.REQUESTED || r.status == ContractStatus.SIGNED)) {
                r = records.transition(r, ContractStatus.TERMINATED, T_CONNECTOR, null, "COUNTERPARTY_TERMINATED", null);
            } else {
                throw new ApiException(400, Problems.INVALID_TRANSITION,
                        "Status " + status + " is not allowed from " + r.status);
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("contractAgreementId", r.contractAgreementId);
            out.put("contractDefinitionId", r.contractDefinitionId);
            out.put("status", r.status.technical());
            return out;
        });
    }

    public JsonNode ownVc(String nid) {
        Ids.pathId("contractNegotiationId", nid);
        ContractRecord r = contracts.findByContractNegotiationId(nid)
                .orElseThrow(() -> ApiException.notFound("No contract for contractNegotiationId " + nid));
        if (r.ownCredential == null) {
            throw ApiException.notFound("The contract is not signed yet");
        }
        try {
            return mapper.readTree(r.ownCredential);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String nowText() {
        return Times.rfc3339(Times.now());
    }
}
