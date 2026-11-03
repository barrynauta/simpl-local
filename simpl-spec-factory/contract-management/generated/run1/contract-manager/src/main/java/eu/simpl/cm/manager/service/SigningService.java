package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.common.web.Times;
import eu.simpl.cm.manager.CmSettings;
import eu.simpl.cm.manager.clients.StubClient;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.PartyRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.simpl.cm.manager.service.ContractService.T_CONNECTOR;
import static eu.simpl.cm.manager.service.ContractService.T_SYSTEM;

/** Asynchronous signing (D24, D29), validation (D27, D28) and verification (D31). */
@Service
public class SigningService {
    private static final Logger log = LoggerFactory.getLogger(SigningService.class);

    /** D57 wallet errors: reason code and the exact story message. */
    public record WalletError(String code, String message) {
    }

    public static WalletError walletError(int status) {
        return switch (status) {
            case 507 -> new WalletError("WALLET_FULL", "Secure storage location is full");
            case 500 -> new WalletError("WALLET_CORRUPTED", "Secure storage location is corrupted");
            default -> new WalletError("WALLET_UNAVAILABLE", "Secure storage location unavailable");
        };
    }

    private final ContractRepository contracts;
    private final ContractService records;
    private final StubClient stub;
    private final Enricher enricher;
    private final CmSettings settings;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;
    private final CredentialValidator validator = new CredentialValidator();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public SigningService(ContractRepository contracts, ContractService records, StubClient stub, Enricher enricher,
                          CmSettings settings, TransactionTemplate tx, ObjectMapper mapper) {
        this.contracts = contracts;
        this.records = records;
        this.stub = stub;
        this.enricher = enricher;
        this.settings = settings;
        this.tx = tx;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------ duplicates (D28)
    public boolean isDuplicate(ContractRecord r) {
        return CredentialValidator.SUCCESS.equals(r.validationResult)
                || CredentialValidator.REJECTED.equals(r.validationResult);
    }

    /** C1: the stored result is not changed; IGNORED appears only in the history entry. */
    public void recordDuplicate(ContractRecord r, String trigger) {
        records.note(r, "VALIDATION", trigger, null, CredentialValidator.DUPLICATE, "Validation result IGNORED");
    }

    /** One validation per nid at a time; a second trigger while one runs is a duplicate. */
    public boolean claim(String nid) {
        return inFlight.add(nid);
    }

    private void release(String nid) {
        inFlight.remove(nid);
    }

    private ContractRecord load(String nid) {
        return contracts.findByContractNegotiationId(nid).orElseThrow();
    }

    public void callback(String nid, String endpoint, boolean value) {
        ContractRecord r = contracts.findByContractNegotiationId(nid).orElse(null);
        records.callback(r, nid, endpoint, value);
    }

    private void fail(String nid, String code, String reason, String trigger) {
        tx.executeWithoutResult(s -> {
            ContractRecord r = load(nid);
            records.transition(r, ContractStatus.FAILED, trigger, null, code, reason);
        });
    }

    private Map<String, Object> subjectFor(ContractRecord r, PartyRole signer, Instant signedAt) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", "urn:simpl:contract:" + r.contractNegotiationId);
        s.put("contractNegotiationId", r.contractNegotiationId);
        s.put("contractAgreementId", r.contractAgreementId);
        s.put("contractDefinitionId", r.contractDefinitionId);
        s.put("assetId", r.assetId);
        s.put("resourceDescriptionId", r.resourceDescriptionId);
        s.put("providerId", r.providerId);
        s.put("consumerId", r.consumerId);
        s.put("usageContractHash", r.usageContractHash);
        s.put("signerRole", signer.name());
        s.put("signedAt", Times.vc(signedAt));
        return s;
    }

    private JsonNode issue(String nid, Map<String, Object> subject, Instant signedAt) {
        Instant from = signedAt;
        Instant until = signedAt.plus(settings.vcValidity);
        HttpJson.Response res = stub.issue(nid, subject, Times.vc(from), Times.vc(until));
        if (!res.ok()) {
            return null;
        }
        JsonNode vc = stub.json(res);
        return vc != null && vc.isObject() && vc.path("id").isTextual() ? vc : null;
    }

    // ------------------------------------------------------------ D24 consumer signing
    public void consumerSign(String nid) {
        ContractRecord r = load(nid);
        if (r.status != ContractStatus.ACCEPTED) {
            return;
        }
        Instant signedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        JsonNode vc = issue(nid, subjectFor(r, PartyRole.CONSUMER, signedAt), signedAt);
        if (vc == null) {
            fail(nid, "SIGNING_FAILED", "The signer or VC issuer failed", T_SYSTEM);
            callback(nid, "signed", false);
            return;
        }
        String credId = vc.get("id").asText();
        HttpJson.Response w = stub.storeInWallet(nid, credId, vc);
        if (!w.ok()) {
            WalletError e = walletError(w.status());
            fail(nid, e.code(), e.message(), T_SYSTEM);
            callback(nid, "signed", false);
            return;
        }
        tx.executeWithoutResult(s -> {
            ContractRecord rec = load(nid);
            rec.ownCredential = vc.toString();
            rec.ownCredentialId = credId;
            rec.consumerSignatureDate = signedAt;
            records.transition(rec, ContractStatus.SIGNED, T_SYSTEM, null, null, null);
        });
        callback(nid, "signed", true);
    }

    private CredentialValidator.Ports ports(String nid) {
        return new CredentialValidator.Ports() {
            @Override
            public CredentialValidator.VerifyResult verify(JsonNode vc) {
                HttpJson.Response res = stub.verify(nid, vc);
                if (res.status() != 200) {
                    return new CredentialValidator.VerifyResult(true, List.of());
                }
                JsonNode j = stub.json(res);
                if (j == null) {
                    return new CredentialValidator.VerifyResult(true, List.of());
                }
                List<String> problems = new ArrayList<>();
                j.path("problems").forEach(p -> problems.add(p.asText()));
                if (!j.path("verified").asBoolean(false) && problems.isEmpty()) {
                    problems.add("MALFORMED");
                }
                return new CredentialValidator.VerifyResult(false, problems);
            }

            @Override
            public String counterpartyDid(String participantId) {
                return enricher.participantDid(nid, participantId);
            }

            @Override
            public Instant now() {
                return Instant.now();
            }
        };
    }

    private void storeValidation(String nid, CredentialValidator.Outcome o, String trigger) {
        tx.executeWithoutResult(s -> {
            ContractRecord rec = load(nid);
            rec.validationResult = o.result();
            rec.validationReasonCode = o.reasonCode();
            rec.validatedAt = Times.now();
            records.save(rec);
            records.note(rec, "VALIDATION", trigger, null, o.reasonCode(), "Validation result " + o.result());
        });
    }

    // ------------------------------------------------------------ D26, D27, D29 provider validation and signing
    public void providerValidateAndSign(String nid) {
        try {
            doProviderValidateAndSign(nid);
        } finally {
            release(nid);
        }
    }

    private void doProviderValidateAndSign(String nid) {
        ContractRecord r = load(nid);
        if (r.status != ContractStatus.REQUESTED) {
            return;
        }
        HttpJson.Response ex = stub.vcExchange(nid);
        JsonNode consumerVc = null;
        CredentialValidator.Outcome outcome;
        if (ex.status() == 200) {
            consumerVc = stub.json(ex);
            outcome = validator.validate(r, consumerVc, ports(nid));
        } else if (ex.status() == 404) {
            outcome = validator.validate(r, null, ports(nid));
        } else {
            outcome = new CredentialValidator.Outcome(CredentialValidator.FAILED, CredentialValidator.TECHNICAL);
        }
        storeValidation(nid, outcome, T_CONNECTOR);
        if (CredentialValidator.FAILED.equals(outcome.result())) {
            return;
        }
        if (CredentialValidator.REJECTED.equals(outcome.result())) {
            fail(nid, outcome.reasonCode(), "Consumer VC rejected: " + outcome.reasonCode(), T_CONNECTOR);
            callback(nid, "isValid", false);
            ContractRecord rec = load(nid);
            records.email(rec, rec.consumerId, "Usage contract offer could not be validated: " + nid,
                    "The signed usage contract offer " + nid + " could not be validated. Reason: " + outcome.reasonCode() + ".");
            return;
        }
        callback(nid, "isValid", true);
        JsonNode cs = consumerVc.get("credentialSubject");
        Instant signedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        ObjectNode subject = cs.deepCopy();
        subject.put("signerRole", PartyRole.PROVIDER.name());
        subject.put("signedAt", Times.vc(signedAt));
        @SuppressWarnings("unchecked")
        Map<String, Object> subjectMap = mapper.convertValue(subject, LinkedHashMap.class);
        JsonNode own = issue(nid, subjectMap, signedAt);
        if (own == null) {
            fail(nid, "SIGNING_FAILED", "The signer or VC issuer failed", T_CONNECTOR);
            callback(nid, "signed", false);
            return;
        }
        String consumerCredId = consumerVc.path("id").asText("urn:uuid:" + java.util.UUID.randomUUID());
        HttpJson.Response w1 = stub.storeInWallet(nid, consumerCredId, consumerVc);
        HttpJson.Response w2 = w1.ok() ? stub.storeInWallet(nid, own.get("id").asText(), own) : w1;
        if (!w2.ok()) {
            WalletError e = walletError(w2.status());
            fail(nid, e.code(), e.message(), T_CONNECTOR);
            callback(nid, "signed", false);
            return;
        }
        final JsonNode fConsumerVc = consumerVc;
        tx.executeWithoutResult(s -> {
            ContractRecord rec = load(nid);
            rec.ownCredential = own.toString();
            rec.ownCredentialId = own.get("id").asText();
            rec.counterpartyCredential = fConsumerVc.toString();
            rec.counterpartyCredentialId = consumerCredId;
            rec.consumerSignatureDate = Times.parse(cs.get("signedAt").asText());
            rec.providerSignatureDate = signedAt;
            rec.usageContractHash = cs.get("usageContractHash").asText();
            if (rec.resourceDescriptionId == null) {
                rec.resourceDescriptionId = cs.get("resourceDescriptionId").asText();
            }
            records.transition(rec, ContractStatus.SIGNED, T_CONNECTOR, null, null, null);
        });
        callback(nid, "signed", true);
    }

    // ------------------------------------------------------------ D31 consumer verification
    public void consumerVerify(String nid, JsonNode providerVc) {
        try {
            doConsumerVerify(nid, providerVc);
        } finally {
            release(nid);
        }
    }

    private void doConsumerVerify(String nid, JsonNode providerVc) {
        ContractRecord r = load(nid);
        if (r.status != ContractStatus.SIGNED) {
            return;
        }
        CredentialValidator.Outcome outcome = validator.validate(r, providerVc, ports(nid));
        storeValidation(nid, outcome, T_CONNECTOR);
        if (CredentialValidator.FAILED.equals(outcome.result())) {
            return;
        }
        if (CredentialValidator.REJECTED.equals(outcome.result())) {
            fail(nid, outcome.reasonCode(), "Provider VC rejected: " + outcome.reasonCode(), T_CONNECTOR);
            callback(nid, "isValid", false);
            return;
        }
        String credId = providerVc.path("id").asText("urn:uuid:" + java.util.UUID.randomUUID());
        HttpJson.Response w = stub.storeInWallet(nid, credId, providerVc);
        if (!w.ok()) {
            WalletError e = walletError(w.status());
            fail(nid, e.code(), e.message(), T_CONNECTOR);
            callback(nid, "isValid", false);
            return;
        }
        tx.executeWithoutResult(s -> {
            ContractRecord rec = load(nid);
            rec.counterpartyCredential = providerVc.toString();
            rec.counterpartyCredentialId = credId;
            rec.providerSignatureDate = Times.parse(providerVc.get("credentialSubject").get("signedAt").asText());
            records.transition(rec, ContractStatus.ACTIVE, T_CONNECTOR, null, null, null);
        });
        callback(nid, "isValid", true);
        log.info("Contract {} is ACTIVE at the consumer", nid);
    }
}
