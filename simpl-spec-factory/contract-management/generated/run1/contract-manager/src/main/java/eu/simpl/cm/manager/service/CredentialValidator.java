package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.web.Times;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.PartyRole;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * D27: one validation algorithm at both sides. The first failing check decides the reason.
 * The duplicate check (1) is done by the caller before the trigger is accepted (D28).
 */
public class CredentialValidator {
    public static final String SUCCESS = "SUCCESS";
    public static final String REJECTED = "REJECTED";
    public static final String FAILED = "FAILED";
    public static final String IGNORED = "IGNORED";

    public static final String MISSING_CONTRACT_DATA = "MISSING_CONTRACT_DATA";
    public static final String SCHEMA = "SCHEMA_OR_CONTENT_VALIDATION_FAILURE";
    public static final String TECHNICAL = "TECHNICAL_VALIDATION_ERROR";
    public static final String VALIDITY = "CONTRACT_VALIDITY_ISSUE";
    public static final String PAYLOAD = "PAYLOAD_INTEGRITY_MISMATCH";
    public static final String DUPLICATE = "DUPLICATE_OR_REPLAYED_SUBMISSION";

    static final List<String> SUBJECT_FIELDS = List.of("id", "contractNegotiationId", "contractAgreementId",
            "contractDefinitionId", "assetId", "resourceDescriptionId", "providerId", "consumerId", "usageContractHash",
            "signerRole", "signedAt");

    public record Outcome(String result, String reasonCode) {
        public static Outcome success() {
            return new Outcome(SUCCESS, null);
        }

        static Outcome rejected(String code) {
            return new Outcome(REJECTED, code);
        }

        static Outcome failed() {
            return new Outcome(FAILED, TECHNICAL);
        }
    }

    /** Result of the stub verifier call. {@code callFailed} means no usable answer. */
    public record VerifyResult(boolean callFailed, List<String> problems) {
    }

    /** What the validator needs from outside. */
    public interface Ports {
        VerifyResult verify(JsonNode vc);

        /** DID of the counterparty from the directory; null if not known. Throws on a technical error. */
        String counterpartyDid(String participantId);

        Instant now();
    }

    public static class TechnicalError extends RuntimeException {
        public TechnicalError(String message) {
            super(message);
        }
    }

    public static String invalidSignature(PartyRole counterparty) {
        return "INVALID_" + counterparty.name() + "_SIGNATURE";
    }

    public static String trustIssue(PartyRole counterparty) {
        return counterparty.name() + "_CREDENTIAL_TRUST_ISSUE";
    }

    /**
     * @param record the record of this party
     * @param vc     the counterparty VC, null when none could be obtained
     */
    public Outcome validate(ContractRecord record, JsonNode vc, Ports ports) {
        PartyRole counterparty = record.partyRole.other();
        // (2) missing VC or missing stored contract data
        if (vc == null || vc.isNull() || vc.isMissingNode() || !storedDataComplete(record)) {
            return Outcome.rejected(MISSING_CONTRACT_DATA);
        }
        // (3) required fields of D53 and D55
        if (!schemaComplete(vc)) {
            return Outcome.rejected(SCHEMA);
        }
        // (4) technical validation by the verifier
        VerifyResult verified;
        try {
            verified = ports.verify(vc);
        } catch (RuntimeException e) {
            return Outcome.failed();
        }
        if (verified == null || verified.callFailed()) {
            return Outcome.failed();
        }
        if (verified.problems() != null && !verified.problems().isEmpty()) {
            String p = verified.problems().get(0);
            return switch (p) {
                case "MALFORMED" -> Outcome.rejected(SCHEMA);
                case "INVALID_SIGNATURE" -> Outcome.rejected(invalidSignature(counterparty));
                case "UNKNOWN_ISSUER", "REVOKED_ISSUER" -> Outcome.rejected(trustIssue(counterparty));
                default -> Outcome.rejected(SCHEMA);
            };
        }
        // (5) issuer is the counterparty
        String expectedDid;
        try {
            expectedDid = ports.counterpartyDid(record.counterpartyId());
        } catch (RuntimeException e) {
            return Outcome.failed();
        }
        if (expectedDid == null || !expectedDid.equals(vc.get("issuer").asText())) {
            return Outcome.rejected(trustIssue(counterparty));
        }
        // (6) validity period
        Instant from = Times.parse(vc.get("validFrom").asText());
        Instant until = Times.parse(vc.get("validUntil").asText());
        Instant now = ports.now();
        if (now.isBefore(from) || now.isAfter(until)) {
            return Outcome.rejected(VALIDITY);
        }
        // (7) payload integrity against the stored record
        JsonNode s = vc.get("credentialSubject");
        boolean same = eq(s, "contractNegotiationId", record.contractNegotiationId)
                && eq(s, "contractAgreementId", record.contractAgreementId)
                && eq(s, "contractDefinitionId", record.contractDefinitionId)
                && eq(s, "assetId", record.assetId)
                && eq(s, "providerId", record.providerId)
                && eq(s, "consumerId", record.consumerId)
                && (record.partyRole != PartyRole.CONSUMER || eq(s, "usageContractHash", record.usageContractHash))
                && counterparty.name().equals(s.get("signerRole").asText());
        if (!same) {
            return Outcome.rejected(PAYLOAD);
        }
        return Outcome.success();
    }

    static boolean storedDataComplete(ContractRecord r) {
        boolean base = r.contractAgreementId != null && r.contractDefinitionId != null && r.assetId != null
                && r.providerId != null && r.consumerId != null;
        if (r.partyRole == PartyRole.CONSUMER) {
            return base && r.usageContractHash != null;
        }
        return base;
    }

    static boolean schemaComplete(JsonNode vc) {
        if (!vc.isObject()) {
            return false;
        }
        if (!vc.has("@context") || !vc.has("type") || !nonBlank(vc.get("issuer")) || !vc.path("credentialSubject").isObject()
                || !nonBlank(vc.get("validFrom")) || !nonBlank(vc.get("validUntil")) || !vc.path("proof").isObject()) {
            return false;
        }
        try {
            Times.parse(vc.get("validFrom").asText());
            Times.parse(vc.get("validUntil").asText());
        } catch (Exception e) {
            return false;
        }
        JsonNode s = vc.get("credentialSubject");
        for (String f : SUBJECT_FIELDS) {
            if (!nonBlank(s.get(f))) {
                return false;
            }
        }
        return true;
    }

    private static boolean nonBlank(JsonNode n) {
        return n != null && n.isTextual() && !n.asText().isBlank();
    }

    private static boolean eq(JsonNode subject, String field, String expected) {
        return Objects.equals(subject.get(field).asText(), expected);
    }
}
