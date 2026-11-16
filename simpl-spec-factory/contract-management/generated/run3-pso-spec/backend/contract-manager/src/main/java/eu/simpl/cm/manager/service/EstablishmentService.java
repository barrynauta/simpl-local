package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.common.ApiException;
import eu.simpl.cm.manager.CmProperties;
import eu.simpl.cm.manager.client.HttpResult;
import eu.simpl.cm.manager.client.StubClient;
import eu.simpl.cm.manager.client.UibeClient;
import eu.simpl.cm.manager.domain.ConsentStatus;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.EntryType;
import eu.simpl.cm.manager.domain.PartyRole;
import eu.simpl.cm.manager.domain.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.simpl.cm.manager.domain.ContractStatus.ACCEPTED;
import static eu.simpl.cm.manager.domain.ContractStatus.ACTIVE;
import static eu.simpl.cm.manager.domain.ContractStatus.FAILED;
import static eu.simpl.cm.manager.domain.ContractStatus.REJECTED;
import static eu.simpl.cm.manager.domain.ContractStatus.REQUESTED;
import static eu.simpl.cm.manager.domain.ContractStatus.SIGNED;
import static eu.simpl.cm.manager.domain.ContractStatus.TERMINATED;

/** Contract establishment (BP07, D19 to D36, E and P transitions of D37). */
@Service
public class EstablishmentService {

    private static final Logger LOG = LoggerFactory.getLogger(EstablishmentService.class);

    private final CmProperties props;
    private final ContractStore store;
    private final ContractRepository contracts;
    private final StubClient stub;
    private final UibeClient uibe;
    private final Directory directory;
    private final Notifier notifier;
    private final CredentialValidator validator;
    private final Tasks tasks;
    private final ObjectMapper mapper;
    private final Set<String> validating = ConcurrentHashMap.newKeySet();

    public EstablishmentService(CmProperties props, ContractStore store, ContractRepository contracts,
                                StubClient stub, UibeClient uibe, Directory directory, Notifier notifier,
                                CredentialValidator validator, Tasks tasks, ObjectMapper mapper) {
        this.props = props;
        this.store = store;
        this.contracts = contracts;
        this.stub = stub;
        this.uibe = uibe;
        this.directory = directory;
        this.notifier = notifier;
        this.validator = validator;
        this.tasks = tasks;
        this.mapper = mapper;
    }

    private String own() {
        return props.participantId();
    }

    private static String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    // ------------------------------------------------------------------ existence check (D19, E1, E2)

    public Map<String, Object> search(String nid, JsonNode body) {
        Requests.requireObject(body);
        String aid = Requests.optionalUuid(body, "contractAgreementId");
        String did = Requests.optionalId(body, "contractDefinitionId");
        String assetId = Requests.requiredId(body, "assetId");
        String consumerId = Requests.requiredId(body, "consumerId");
        String providerId = Requests.optionalId(body, "providerId");
        if (providerId == null) {
            providerId = own();
        }
        List<ContractRecord> existing = contracts.findOpenDuplicates(assetId, providerId, consumerId, nid,
                ContractStatus.OPEN);
        boolean exists = !existing.isEmpty();
        Map<String, Object> out = new LinkedHashMap<>();
        if (exists) {
            out.put("contractAgreementId", existing.get(0).contractAgreementId);
            out.put("contractDefinitionId", existing.get(0).contractDefinitionId);
        } else {
            out.put("contractAgreementId", aid);
            out.put("contractDefinitionId", did);
        }
        out.put("exists", exists);
        out.put("contractSearchTO", body);

        boolean createRecord = false;
        if (!exists && own().equals(providerId) && store.find(nid).isEmpty()) {
            ContractRecord r = new ContractRecord();
            r.contractNegotiationId = nid;
            r.contractAgreementId = aid;
            r.contractDefinitionId = did;
            r.assetId = assetId;
            r.providerId = providerId;
            r.consumerId = consumerId;
            r.partyRole = PartyRole.PROVIDER;
            r.status = REQUESTED;
            enrich(r);
            try {
                store.create(r, Trigger.CONNECTOR, null);
                createRecord = true;
            } catch (DataIntegrityViolationException e) {
                LOG.info("Record for {} was created concurrently", nid);
            }
        }
        boolean recordExists = createRecord || store.find(nid).isPresent();
        tasks.submit("isPresent " + nid, () -> callback("isPresent", nid, exists, recordExists));
        return out;
    }

    /** Fills resource and party data from the catalogue and directory. Failures are ignored (D19). */
    private void enrich(ContractRecord r) {
        try {
            HttpResult a = stub.asset(r.contractNegotiationId, r.assetId);
            if (a.status() == 200 && a.body() != null) {
                r.resourceDescriptionId = Directory.text(a.body(), "resourceDescriptionId");
                r.resourceName = Directory.text(a.body(), "resourceName");
                r.consentStatus = a.body().path("consentRequired").asBoolean(false)
                        ? ConsentStatus.PENDING : ConsentStatus.NOT_DEFINED;
            }
            r.providerName = directory.name(r.contractNegotiationId, r.providerId);
            r.consumerName = directory.name(r.contractNegotiationId, r.consumerId);
        } catch (Exception e) {
            LOG.warn("Enrichment failed for {}: {}", r.contractNegotiationId, e.toString());
        }
    }

    /** Calls a connector callback and records it (C3). */
    void callback(String endpoint, String nid, boolean value, boolean recordExists) {
        HttpResult r = stub.callback(endpoint, nid, value);
        if (recordExists) {
            store.note(nid, EntryType.CONNECTOR_CALLBACK, Trigger.SYSTEM, null, null,
                    endpoint + "/" + nid + "/" + value + " -> " + r.status());
        }
        if (!r.ok()) {
            LOG.warn("Connector callback {} for {} answered {}", endpoint, nid, r.status());
        }
    }

    void callback(String endpoint, String nid, boolean value) {
        callback(endpoint, nid, value, true);
    }

    // ------------------------------------------------------------------ offer preparation (D20, E3)

    public Map<String, Object> create(String aid, String did, JsonNode body) {
        Requests.requireObject(body);
        String nid = Requests.requiredId(body, "contractNegotiationId");
        String assetId = Requests.requiredId(body, "assetId");
        String providerId = Requests.requiredId(body, "providerId");
        String consumerId = Requests.requiredId(body, "consumerId");
        String offerId = Requests.requiredId(body, "contractOfferId");
        if (!own().equals(consumerId)) {
            throw ApiException.wrongParty("Offer data is prepared at the Consumer side only");
        }
        if (store.find(nid).isPresent()) {
            throw ApiException.alreadyExists("Contract negotiation " + nid + " already exists");
        }
        HttpResult a = stub.asset(nid, assetId);
        if (a.status() == 404) {
            throw ApiException.notFound("Unknown assetId");
        }
        if (a.status() != 200 || a.body() == null) {
            throw ApiException.internal("Catalogue not available");
        }
        JsonNode asset = a.body();
        if (!providerId.equals(Directory.text(asset, "providerId"))) {
            throw ApiException.forbidden("Asset is not owned by the provider");
        }
        String providerName = directory.name(nid, providerId);
        String consumerName = directory.name(nid, consumerId);
        String html = OfferHtml.build(asset, providerName, consumerName);
        String hash = OfferHtml.sha256Hex(html);

        HttpResult u = uibe.postOffer(nid, html, hash);
        if (!u.ok()) {
            throw ApiException.internal("Contract UI backend did not accept the offer");
        }
        ContractRecord r = new ContractRecord();
        r.contractNegotiationId = nid;
        r.contractAgreementId = aid;
        r.contractDefinitionId = did;
        r.contractOfferId = offerId;
        r.assetId = assetId;
        r.resourceDescriptionId = Directory.text(asset, "resourceDescriptionId");
        r.resourceName = Directory.text(asset, "resourceName");
        r.providerId = providerId;
        r.providerName = providerName;
        r.consumerId = consumerId;
        r.consumerName = consumerName;
        r.partyRole = PartyRole.CONSUMER;
        r.status = REQUESTED;
        r.consentStatus = asset.path("consentRequired").asBoolean(false) ? ConsentStatus.PENDING
                : ConsentStatus.NOT_DEFINED;
        r.humanReadable = html;
        r.usageContractHash = hash;
        try {
            store.create(r, Trigger.CONNECTOR, null);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.alreadyExists("Contract negotiation " + nid + " already exists");
        }
        String offerUrl = props.uiPublicUrl() + "/contractOffers/" + nid;
        notifier.email(nid, consumerId, "Usage contract offer available for review: " + nid,
                "A usage contract offer for " + (r.resourceName == null ? assetId : r.resourceName)
                        + " is available for review: " + offerUrl, Trigger.CONNECTOR);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractAgreementId", aid);
        out.put("contractDefinitionId", did);
        out.put("status", "CREATED");
        out.put("contractNegotiationId", nid);
        out.put("assetId", assetId);
        out.put("resourceDescriptionId", r.resourceDescriptionId);
        out.put("resourceDescriptionName", r.resourceName);
        out.put("providerId", providerId);
        out.put("providerName", providerName);
        out.put("consumerId", consumerId);
        out.put("consumerName", consumerName);
        out.put("contractOfferId", offerId);
        out.put("usageContract", html);
        out.put("usageContractHash", hash);
        out.put("contractStatus", REQUESTED.name());
        out.put("offerUrl", offerUrl);
        return out;
    }

    // ------------------------------------------------------------------ helpers for the D84 order

    private ContractRecord requireRecord(String nid) {
        return store.find(nid).orElseThrow(() -> ApiException.notFound("Unknown contractNegotiationId"));
    }

    private ContractRecord requireByAgreement(String aid) {
        return store.findByAgreement(aid).orElseThrow(() -> ApiException.notFound("Unknown contractAgreementId"));
    }

    private static void requireSide(ContractRecord r, PartyRole role, String what) {
        if (r.partyRole != role) {
            throw ApiException.wrongParty(what + " runs at the " + role.name().charAt(0)
                    + role.name().substring(1).toLowerCase() + " side only");
        }
    }

    // ------------------------------------------------------------------ Consumer decision (D23, D44, E4, E7, E8)

    public Map<String, Object> offerDecision(String nid, String operation, String reason, String actor) {
        if (!"accept".equals(operation) && !"reject".equals(operation)) {
            throw ApiException.argumentTypeMismatch("Invalid request param operation");
        }
        if (reason != null && !"EXPIRED".equals(reason)) {
            throw ApiException.argumentTypeMismatch("Invalid request param reason");
        }
        if (reason != null && !"reject".equals(operation)) {
            throw ApiException.argumentTypeMismatch("reason is allowed only with reject");
        }
        ContractRecord r = requireRecord(nid);
        requireSide(r, PartyRole.CONSUMER, "The offer decision");
        if (r.status != REQUESTED) {
            throw ApiException.offerNotPending("The offer is not pending: status " + r.status);
        }
        try {
            if ("accept".equals(operation)) {
                store.transition(nid, EnumSet.of(REQUESTED), ACCEPTED, Trigger.USER, actor, null, null, null);
                tasks.submit("consumer signing " + nid, () -> consumerSign(nid));
            } else if ("EXPIRED".equals(reason)) {
                store.transition(nid, EnumSet.of(REQUESTED), TERMINATED, Trigger.SCHEDULER, null, "OFFER_EXPIRED",
                        "The offer expired before it was signed", null);
                tasks.submit("signed false " + nid, () -> callback("signed", nid, false));
            } else {
                store.transition(nid, EnumSet.of(REQUESTED), REJECTED, Trigger.USER, actor, null, null, null);
                tasks.submit("signed false " + nid, () -> callback("signed", nid, false));
            }
        } catch (ContractStore.TransitionRefused | org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw ApiException.offerNotPending("The offer is not pending");
        }
        return Map.of("contractNegotiationId", nid, "status", "INITIATED");
    }

    /** D24: Consumer signing after the accept. */
    void consumerSign(String nid) {
        ContractRecord r = store.find(nid).orElseThrow();
        String signedAt = now();
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("id", "urn:simpl:contract:" + nid);
        subject.put("contractNegotiationId", nid);
        subject.put("contractAgreementId", r.contractAgreementId);
        subject.put("contractDefinitionId", r.contractDefinitionId);
        subject.put("assetId", r.assetId);
        subject.put("resourceDescriptionId", r.resourceDescriptionId);
        subject.put("providerId", r.providerId);
        subject.put("consumerId", r.consumerId);
        subject.put("usageContractHash", r.usageContractHash);
        subject.put("signerRole", "CONSUMER");
        subject.put("signedAt", signedAt);
        HttpResult issued = stub.issue(nid, subject, signedAt, validUntil());
        if (issued.status() != 201 || issued.body() == null || !issued.body().isObject()) {
            fail(nid, EnumSet.of(ACCEPTED), "SIGNING_FAILED", "Signing or VC issuing failed", Trigger.SYSTEM);
            callback("signed", nid, false);
            return;
        }
        JsonNode vc = issued.body();
        String credentialId = vc.path("id").asText();
        String wallet = walletError(stub.walletPut(nid, credentialId, vc));
        if (wallet != null) {
            fail(nid, EnumSet.of(ACCEPTED), wallet, walletMessage(wallet), Trigger.SYSTEM);
            callback("signed", nid, false);
            return;
        }
        store.transition(nid, EnumSet.of(ACCEPTED), SIGNED, Trigger.SYSTEM, null, null, null, rec -> {
            rec.consumerSignatureDate = Instant.parse(signedAt);
            rec.ownCredentialId = credentialId;
            rec.ownCredential = vc.toString();
        });
        callback("signed", nid, true);
    }

    private String validUntil() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(props.contractVcValidity()).toString();
    }

    private void fail(String nid, Set<ContractStatus> from, String reasonCode, String reason, Trigger trigger) {
        try {
            store.transition(nid, from, FAILED, trigger, null, reasonCode, reason, null);
        } catch (ContractStore.TransitionRefused e) {
            LOG.warn("Could not move {} to FAILED from {}", nid, e.actual);
        }
    }

    /** D57: maps the wallet answer to a reason code, or null on success. */
    static String walletError(HttpResult r) {
        if (r.ok()) {
            return null;
        }
        return switch (r.status()) {
            case 507 -> "WALLET_FULL";
            case 500 -> "WALLET_CORRUPTED";
            default -> "WALLET_UNAVAILABLE";
        };
    }

    static String walletMessage(String code) {
        return switch (code) {
            case "WALLET_FULL" -> "Secure storage location is full";
            case "WALLET_CORRUPTED" -> "Secure storage location is corrupted";
            default -> "Secure storage location unavailable";
        };
    }

    // ------------------------------------------------------------------ Provider validate and sign (D25 to D30)

    public Map<String, Object> issue(String aid, String did, JsonNode body) {
        boolean noBody = body == null || body.isNull() || (body.isObject() && body.isEmpty());
        ContractRecord r;
        if (noBody) {
            r = requireByAgreement(aid);
            requireSide(r, PartyRole.PROVIDER, "Validation and signing");
        } else {
            Requests.requireObject(body);
            String nid = Requests.requiredId(body, "contractNegotiationId");
            String assetId = Requests.requiredId(body, "assetId");
            String providerId = Requests.requiredId(body, "providerId");
            String consumerId = Requests.requiredId(body, "consumerId");
            String offerId = Requests.optionalId(body, "contractOfferId");
            if (!own().equals(providerId)) {
                throw ApiException.wrongParty("Validation and signing runs at the Provider side only");
            }
            Optional<ContractRecord> found = store.find(nid);
            if (found.isPresent()) {
                r = found.get();
                requireSide(r, PartyRole.PROVIDER, "Validation and signing");
            } else {
                ContractRecord n = new ContractRecord();
                n.contractNegotiationId = nid;
                n.contractAgreementId = aid;
                n.contractDefinitionId = did;
                n.contractOfferId = offerId;
                n.assetId = assetId;
                n.providerId = providerId;
                n.consumerId = consumerId;
                n.partyRole = PartyRole.PROVIDER;
                n.status = REQUESTED;
                enrich(n);
                try {
                    r = store.create(n, Trigger.CONNECTOR, null);
                } catch (DataIntegrityViolationException e) {
                    r = store.find(nid).orElseThrow();
                }
            }
            if (offerId != null && r.contractOfferId == null) {
                store.update(r.contractNegotiationId, rec -> rec.contractOfferId = offerId);
            }
        }
        String nid = r.contractNegotiationId;
        store.update(nid, rec -> {
            rec.contractAgreementId = aid;
            rec.contractDefinitionId = did;
        });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractAgreementId", aid);
        out.put("contractDefinitionId", did);
        out.put("status", "INITIATED");
        if (isDuplicate(r)) {
            store.note(nid, EntryType.VALIDATION, Trigger.CONNECTOR, null, "DUPLICATE_OR_REPLAYED_SUBMISSION",
                    "Validation result " + CredentialValidator.IGNORED);
            return out;
        }
        if (r.status != REQUESTED) {
            throw ApiException.invalidTransition(400, "Validation is not possible in status " + r.status);
        }
        validating.add(nid);
        tasks.submit("provider validation " + nid, () -> {
            try {
                providerValidateAndSign(nid);
            } finally {
                validating.remove(nid);
            }
        });
        return out;
    }

    private boolean isDuplicate(ContractRecord r) {
        return CredentialValidator.SUCCESS.equals(r.validationResult)
                || CredentialValidator.REJECTED.equals(r.validationResult)
                || validating.contains(r.contractNegotiationId);
    }

    void providerValidateAndSign(String nid) {
        HttpResult x = stub.vcExchange(nid);
        ContractRecord r = store.find(nid).orElseThrow();
        CredentialValidator.Outcome outcome;
        JsonNode consumerVc = null;
        if (x.status() == 200) {
            consumerVc = x.body();
            outcome = validator.validate(r, consumerVc);
        } else if (x.status() == 404) {
            outcome = new CredentialValidator.Outcome(CredentialValidator.REJECTED, "MISSING_CONTRACT_DATA");
        } else {
            outcome = new CredentialValidator.Outcome(CredentialValidator.FAILED, "TECHNICAL_VALIDATION_ERROR");
        }
        recordValidation(nid, outcome);
        if (CredentialValidator.FAILED.equals(outcome.result())) {
            return; // D28: no transition, no callback, retry allowed
        }
        if (!outcome.success()) {
            fail(nid, EnumSet.of(REQUESTED), outcome.reasonCode(), "Consumer VC validation failed",
                    Trigger.CONNECTOR);
            callback("isValid", nid, false);
            notifier.email(nid, r.consumerId, "Usage contract offer could not be validated: " + nid,
                    "The signed usage contract offer " + nid + " could not be validated. Reason: "
                            + outcome.reasonCode() + ".", Trigger.CONNECTOR);
            return;
        }
        callback("isValid", nid, true);
        JsonNode cs = consumerVc.get("credentialSubject");
        String signedAt = now();
        Map<String, Object> subject = mapper.convertValue(cs, LinkedHashMap.class);
        subject.put("signerRole", "PROVIDER");
        subject.put("signedAt", signedAt);
        HttpResult issued = stub.issue(nid, subject, signedAt, validUntil());
        if (issued.status() != 201 || issued.body() == null || !issued.body().isObject()) {
            fail(nid, EnumSet.of(REQUESTED), "SIGNING_FAILED", "Signing or VC issuing failed", Trigger.CONNECTOR);
            callback("signed", nid, false);
            return;
        }
        JsonNode ownVc = issued.body();
        String consumerCredId = consumerVc.path("id").asText();
        String ownCredId = ownVc.path("id").asText();
        String wallet = walletError(stub.walletPut(nid, consumerCredId, consumerVc));
        if (wallet == null) {
            wallet = walletError(stub.walletPut(nid, ownCredId, ownVc));
        }
        if (wallet != null) {
            fail(nid, EnumSet.of(REQUESTED), wallet, walletMessage(wallet), Trigger.CONNECTOR);
            callback("signed", nid, false);
            return;
        }
        final JsonNode cvc = consumerVc;
        store.transition(nid, EnumSet.of(REQUESTED), SIGNED, Trigger.CONNECTOR, null, null, null, rec -> {
            rec.consumerSignatureDate = CredentialValidator.parse(cs.path("signedAt").asText());
            rec.providerSignatureDate = Instant.parse(signedAt);
            rec.usageContractHash = cs.path("usageContractHash").asText(null);
            if (rec.resourceDescriptionId == null) {
                rec.resourceDescriptionId = cs.path("resourceDescriptionId").asText(null);
            }
            rec.ownCredentialId = ownCredId;
            rec.ownCredential = ownVc.toString();
            rec.counterpartyCredentialId = consumerCredId;
            rec.counterpartyCredential = cvc.toString();
        });
        callback("signed", nid, true);
    }

    private void recordValidation(String nid, CredentialValidator.Outcome o) {
        Instant at = Instant.now();
        store.update(nid, rec -> {
            rec.validationResult = o.result();
            rec.validationReasonCode = o.reasonCode();
            rec.validatedAt = at;
        });
        store.note(nid, EntryType.VALIDATION, Trigger.CONNECTOR, null, o.reasonCode(),
                "Validation result " + o.result());
    }

    // ------------------------------------------------------------------ Consumer verification (D31, E9, E10)

    public Map<String, Object> verify(String nid, JsonNode body) {
        ContractRecord r = requireRecord(nid);
        Requests.requireObject(body);
        requireSide(r, PartyRole.CONSUMER, "Verification");
        if (isDuplicate(r)) {
            store.note(nid, EntryType.VALIDATION, Trigger.CONNECTOR, null, "DUPLICATE_OR_REPLAYED_SUBMISSION",
                    "Validation result " + CredentialValidator.IGNORED);
            return Map.of("contractNegotiationId", nid, "status", "INITIATED");
        }
        if (r.status != SIGNED) {
            throw ApiException.validation("The contract is not signed: status " + r.status);
        }
        validating.add(nid);
        JsonNode vc = body.deepCopy();
        tasks.submit("consumer verification " + nid, () -> {
            try {
                consumerVerify(nid, vc);
            } finally {
                validating.remove(nid);
            }
        });
        return Map.of("contractNegotiationId", nid, "status", "INITIATED");
    }

    void consumerVerify(String nid, JsonNode providerVc) {
        ContractRecord r = store.find(nid).orElseThrow();
        CredentialValidator.Outcome outcome = validator.validate(r, providerVc);
        recordValidation(nid, outcome);
        if (CredentialValidator.FAILED.equals(outcome.result())) {
            return;
        }
        if (!outcome.success()) {
            fail(nid, EnumSet.of(SIGNED), outcome.reasonCode(), "Provider VC validation failed", Trigger.CONNECTOR);
            callback("isValid", nid, false);
            return;
        }
        String credId = providerVc.path("id").asText();
        String wallet = walletError(stub.walletPut(nid, credId, providerVc));
        if (wallet != null) {
            fail(nid, EnumSet.of(SIGNED), wallet, walletMessage(wallet), Trigger.CONNECTOR);
            callback("isValid", nid, false);
            return;
        }
        store.transition(nid, EnumSet.of(SIGNED), ACTIVE, Trigger.CONNECTOR, null, null, null, rec -> {
            rec.providerSignatureDate = CredentialValidator.parse(providerVc.at("/credentialSubject/signedAt")
                    .asText());
            rec.counterpartyCredentialId = credId;
            rec.counterpartyCredential = providerVc.toString();
        });
        callback("isValid", nid, true);
    }

    // ------------------------------------------------------------------ Provider finalisation (D32, P3, P4)

    public Map<String, Object> agreementStatus(String aid, String did, JsonNode body) {
        ContractRecord r = requireByAgreement(aid);
        Requests.requireObject(body);
        String status = Requests.text(body, "status");
        if (!"FINALIZED".equals(status) && !"TERMINATED".equals(status)) {
            throw ApiException.validation("status must be FINALIZED or TERMINATED");
        }
        requireSide(r, PartyRole.PROVIDER, "Finalisation");
        String nid = r.contractNegotiationId;
        ContractRecord after;
        try {
            if ("FINALIZED".equals(status)) {
                after = store.transition(nid, EnumSet.of(SIGNED), ACTIVE, Trigger.CONNECTOR, null, null, null, null);
            } else {
                after = store.transition(nid, EnumSet.of(REQUESTED, SIGNED), TERMINATED, Trigger.CONNECTOR, null,
                        "COUNTERPARTY_TERMINATED", "Terminated by the connector", null);
            }
        } catch (ContractStore.TransitionRefused e) {
            throw ApiException.invalidTransition(400, "Status " + status + " is not allowed from " + e.actual);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractAgreementId", aid);
        out.put("contractDefinitionId", did);
        out.put("status", after.status.technical());
        return out;
    }

    // ------------------------------------------------------------------ connector to Consumer (D33, E11)

    public Map<String, Object> replaceOffer(String nid, JsonNode body) {
        ContractRecord r = requireRecord(nid);
        Requests.requireObject(body);
        String html = Requests.text(body, "humanReadable");
        String hash = Requests.text(body, "hash");
        if (html == null || html.isBlank() || hash == null || hash.isBlank()) {
            throw ApiException.validation("humanReadable and hash are required");
        }
        requireSide(r, PartyRole.CONSUMER, "The offer presentation");
        if (r.status != REQUESTED) {
            throw ApiException.offerNotPending("The offer is not pending: status " + r.status);
        }
        HttpResult u = uibe.postOffer(nid, html, hash);
        if (!u.ok()) {
            throw ApiException.internal("Contract UI backend did not accept the offer");
        }
        store.update(nid, rec -> {
            rec.humanReadable = html;
            rec.usageContractHash = hash;
        });
        return Map.of("contractNegotiationId", nid, "status", "INITIATED");
    }

    public Map<String, Object> offerStatusUpdate(String nid, JsonNode body) {
        ContractRecord r = requireRecord(nid);
        Requests.requireObject(body);
        String status = Requests.text(body, "status");
        String info = Requests.text(body, "additionalInfo");
        if (!"CREATED".equals(status) && !"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw ApiException.validation("status must be CREATED, APPROVED or REJECTED");
        }
        requireSide(r, PartyRole.CONSUMER, "The offer status update");
        if ("REJECTED".equals(status)) {
            try {
                store.transition(nid, EnumSet.of(REQUESTED, ACCEPTED, SIGNED), FAILED, Trigger.CONNECTOR, null,
                        "PROVIDER_REJECTED", info, null);
            } catch (ContractStore.TransitionRefused e) {
                throw ApiException.invalidTransition(400, "REJECTED is not allowed from " + e.actual);
            }
        } else {
            store.note(nid, EntryType.TRANSITION, Trigger.CONNECTOR, null, "OFFER_STATUS_" + status, info);
        }
        return Map.of("contractNegotiationId", nid, "status", "INITIATED");
    }

    // ------------------------------------------------------------------ reads (D34, A.1)

    public JsonNode ownCredential(String nid) {
        ContractRecord r = requireRecord(nid);
        if (r.ownCredential == null) {
            throw ApiException.notFound("No signed credential for this contractNegotiationId");
        }
        try {
            return mapper.readTree(r.ownCredential);
        } catch (Exception e) {
            throw ApiException.internal("Stored credential cannot be read");
        }
    }

    public ObjectNode agreement(String aid) {
        ContractRecord r = requireByAgreement(aid);
        ObjectNode o = mapper.createObjectNode();
        o.put("contractAgreementId", r.contractAgreementId);
        o.put("contractDefinitionId", r.contractDefinitionId);
        if (r.consumerSignatureDate != null) {
            o.put("consumerSignatureDate", Dates.local(r.consumerSignatureDate));
        }
        if (r.providerSignatureDate != null) {
            o.put("providerSignatureDate", Dates.local(r.providerSignatureDate));
        }
        o.put("status", r.status.technical());
        o.put("contractNegotiationId", r.contractNegotiationId);
        o.put("assetId", r.assetId);
        o.put("providerId", r.providerId);
        o.put("consumerId", r.consumerId);
        o.put("contractOfferId", r.contractOfferId);
        return o;
    }

    public String agreementFile(String aid) {
        ContractRecord r = requireByAgreement(aid);
        if (r.consumerSignatureDate == null || r.providerSignatureDate == null) {
            throw ApiException.notFound("The contract agreement is not signed by both parties");
        }
        return "CONTRACT AGREEMENT: " + nz(r.contractAgreementId) + "\n"
                + "CONTRACT DEFINITION: " + nz(r.contractDefinitionId) + "\n"
                + "ASSET: " + nz(r.assetId) + "\n"
                + "PROVIDER ID: " + nz(r.providerId) + "\n"
                + "PROVIDER SIGNATURE DATE: " + Dates.file(r.providerSignatureDate) + "\n"
                + "CONSUMER: " + nz(r.consumerId) + "\n"
                + "CONSUMER SIGNATURE DATE: " + Dates.file(r.consumerSignatureDate);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    public Map<String, Object> searchAgreements(JsonNode body) {
        Requests.requireObject(body);
        String did = Requests.optionalId(body, "contractDefinitionId");
        String assetId = Requests.optionalId(body, "assetId");
        String providerId = Requests.optionalId(body, "providerId");
        String consumerId = Requests.optionalId(body, "consumerId");
        List<Map<String, Object>> items = contracts.findByStatusInOrderByCreatedDateDesc(ContractStatus.ESTABLISHED)
                .stream()
                .filter(r -> did == null || did.equals(r.contractDefinitionId))
                .filter(r -> assetId == null || assetId.equals(r.assetId))
                .filter(r -> providerId == null || providerId.equals(r.providerId))
                .filter(r -> consumerId == null || consumerId.equals(r.consumerId))
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("contractAgreementId", r.contractAgreementId);
                    m.put("consumerId", r.consumerId);
                    return m;
                })
                .toList();
        return Map.of("items", items);
    }
}
