package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.manager.client.HttpResult;
import eu.simpl.cm.manager.client.StubClient;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.PartyRole;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * The validation algorithm of D27, the same at both parties. The first failing check decides.
 * The duplicate check (1) is done by the caller at trigger time (D28).
 */
@Component
public class CredentialValidator {

    public static final String SUCCESS = "SUCCESS";
    public static final String REJECTED = "REJECTED";
    public static final String FAILED = "FAILED";
    public static final String IGNORED = "IGNORED";

    public record Outcome(String result, String reasonCode) {
        public boolean success() {
            return SUCCESS.equals(result);
        }
    }

    static final List<String> VC_FIELDS = List.of("@context", "type", "id", "issuer", "validFrom", "validUntil",
            "credentialSubject", "proof");
    static final List<String> SUBJECT_FIELDS = List.of("id", "contractNegotiationId", "contractAgreementId",
            "contractDefinitionId", "assetId", "resourceDescriptionId", "providerId", "consumerId",
            "usageContractHash", "signerRole", "signedAt");

    private final StubClient stub;
    private final Directory directory;

    public CredentialValidator(StubClient stub, Directory directory) {
        this.stub = stub;
        this.directory = directory;
    }

    /**
     * Validates the counterparty VC against the stored record.
     *
     * @param record the record at this party
     * @param vc     the counterparty VC, or null when none was received
     */
    public Outcome validate(ContractRecord record, JsonNode vc) {
        PartyRole counterparty = record.partyRole.other();
        String cp = counterparty == PartyRole.CONSUMER ? "CONSUMER" : "PROVIDER";

        // (2) missing VC or contract data
        if (vc == null || !vc.isObject() || vc.isEmpty() || record.contractNegotiationId == null) {
            return new Outcome(REJECTED, "MISSING_CONTRACT_DATA");
        }
        // (3) required fields of D53 and D55
        if (!hasRequiredFields(vc)) {
            return new Outcome(REJECTED, "SCHEMA_OR_CONTENT_VALIDATION_FAILURE");
        }
        // (4) technical validation through the verifier
        HttpResult v = stub.verify(record.contractNegotiationId, vc);
        if (v.status() != 200 || v.body() == null || !v.body().has("verified")) {
            return new Outcome(FAILED, "TECHNICAL_VALIDATION_ERROR");
        }
        if (!v.body().path("verified").asBoolean(false)) {
            String problem = v.body().path("problems").path(0).asText("");
            return switch (problem) {
                case "INVALID_SIGNATURE" -> new Outcome(REJECTED, "INVALID_" + cp + "_SIGNATURE");
                case "UNKNOWN_ISSUER", "REVOKED_ISSUER" -> new Outcome(REJECTED, cp + "_CREDENTIAL_TRUST_ISSUE");
                default -> new Outcome(REJECTED, "SCHEMA_OR_CONTENT_VALIDATION_FAILURE");
            };
        }
        // (5) issuer is the counterparty's DID
        String counterpartyId = counterparty == PartyRole.CONSUMER ? record.consumerId : record.providerId;
        Directory.Participant p = directory.participant(record.contractNegotiationId, counterpartyId);
        if (p == null) {
            return new Outcome(FAILED, "TECHNICAL_VALIDATION_ERROR");
        }
        if (p.did() == null || !p.did().equals(vc.path("issuer").asText())) {
            return new Outcome(REJECTED, cp + "_CREDENTIAL_TRUST_ISSUE");
        }
        // (6) validity period
        Instant now = Instant.now();
        Instant from = parse(vc.path("validFrom").asText());
        Instant until = parse(vc.path("validUntil").asText());
        if (now.isBefore(from) || now.isAfter(until)) {
            return new Outcome(REJECTED, "CONTRACT_VALIDITY_ISSUE");
        }
        // (7) payload integrity against the stored record
        JsonNode s = vc.get("credentialSubject");
        boolean match = eq(s, "contractNegotiationId", record.contractNegotiationId)
                && eq(s, "contractAgreementId", record.contractAgreementId)
                && eq(s, "contractDefinitionId", record.contractDefinitionId)
                && eq(s, "assetId", record.assetId)
                && eq(s, "providerId", record.providerId)
                && eq(s, "consumerId", record.consumerId)
                && eq(s, "signerRole", cp);
        if (match && record.partyRole == PartyRole.CONSUMER) {
            match = eq(s, "usageContractHash", record.usageContractHash);
        }
        if (!match) {
            return new Outcome(REJECTED, "PAYLOAD_INTEGRITY_MISMATCH");
        }
        return new Outcome(SUCCESS, null);
    }

    static boolean hasRequiredFields(JsonNode vc) {
        for (String f : VC_FIELDS) {
            JsonNode n = vc.get(f);
            if (n == null || n.isNull() || (n.isTextual() && n.asText().isBlank())) {
                return false;
            }
        }
        JsonNode s = vc.get("credentialSubject");
        if (!s.isObject()) {
            return false;
        }
        for (String f : SUBJECT_FIELDS) {
            JsonNode n = s.get(f);
            if (n == null || !n.isTextual() || n.asText().isBlank()) {
                return false;
            }
        }
        return parse(vc.get("validFrom").asText()) != null && parse(vc.get("validUntil").asText()) != null
                && parse(s.get("signedAt").asText()) != null;
    }

    static Instant parse(String s) {
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (Exception e) {
            try {
                return Instant.parse(s);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static boolean eq(JsonNode subject, String field, String expected) {
        return Objects.equals(subject.path(field).asText(null), expected);
    }
}
