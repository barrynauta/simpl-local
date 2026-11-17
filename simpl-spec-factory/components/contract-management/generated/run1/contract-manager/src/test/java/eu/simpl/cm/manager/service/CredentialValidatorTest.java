package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.common.proof.DataIntegrity;
import eu.simpl.cm.common.proof.DidKey;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.PartyRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** D27 check order and reason codes, with an in-process verifier that follows Appendix C.3. */
class CredentialValidatorTest {
    static final ObjectMapper M = new ObjectMapper();
    static final byte[] PROVIDER_SEED = seed(0x11);
    static final byte[] CONSUMER_SEED = seed(0x22);
    static final byte[] ROGUE_SEED = seed(0x33);
    static final String PROVIDER_DID = DidKey.did(DataIntegrity.publicKeyFromSeed(PROVIDER_SEED));
    static final String CONSUMER_DID = DidKey.did(DataIntegrity.publicKeyFromSeed(CONSUMER_SEED));
    static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

    final CredentialValidator validator = new CredentialValidator();
    ContractRecord provider;
    boolean verifierFails;
    boolean revoked;

    static byte[] seed(int b) {
        byte[] s = new byte[32];
        Arrays.fill(s, (byte) b);
        return s;
    }

    @BeforeEach
    void setUp() {
        provider = new ContractRecord();
        provider.partyRole = PartyRole.PROVIDER;
        provider.status = ContractStatus.REQUESTED;
        provider.contractNegotiationId = "nid-1";
        provider.contractAgreementId = "aid-1";
        provider.contractDefinitionId = "def-1";
        provider.assetId = "asset-1";
        provider.providerId = "provider-01";
        provider.consumerId = "consumer-01";
        verifierFails = false;
        revoked = false;
    }

    ObjectNode subject(String role) {
        ObjectNode s = M.createObjectNode();
        s.put("id", "urn:simpl:contract:nid-1");
        s.put("contractNegotiationId", "nid-1");
        s.put("contractAgreementId", "aid-1");
        s.put("contractDefinitionId", "def-1");
        s.put("assetId", "asset-1");
        s.put("resourceDescriptionId", "rd-1");
        s.put("providerId", "provider-01");
        s.put("consumerId", "consumer-01");
        s.put("usageContractHash", "hash-1");
        s.put("signerRole", role);
        s.put("signedAt", "2026-10-29T10:00:00Z");
        return s;
    }

    ObjectNode vc(byte[] seed, ObjectNode subject, String from, String until) {
        ObjectNode vc = M.createObjectNode();
        vc.putArray("@context").add("https://www.w3.org/ns/credentials/v2");
        vc.put("id", "urn:uuid:1");
        vc.putArray("type").add("VerifiableCredential").add("UsageContractCredential");
        vc.put("issuer", DidKey.did(DataIntegrity.publicKeyFromSeed(seed)));
        vc.put("validFrom", from);
        vc.put("validUntil", until);
        vc.set("credentialSubject", subject);
        return DataIntegrity.sign(vc, seed, "2026-10-29T10:00:00Z");
    }

    ObjectNode consumerVc() {
        return vc(CONSUMER_SEED, subject("CONSUMER"), "2026-10-29T10:00:00Z", "2027-10-29T10:00:00Z");
    }

    final CredentialValidator.Ports ports = new CredentialValidator.Ports() {
        @Override
        public CredentialValidator.VerifyResult verify(JsonNode vc) {
            if (verifierFails) {
                return new CredentialValidator.VerifyResult(true, List.of());
            }
            List<String> problems = new ArrayList<>();
            DataIntegrity.Outcome o = DataIntegrity.verify(vc);
            if (o == DataIntegrity.Outcome.MALFORMED) {
                problems.add("MALFORMED");
            } else if (o == DataIntegrity.Outcome.INVALID_SIGNATURE) {
                problems.add("INVALID_SIGNATURE");
            } else if (!Set.of(PROVIDER_DID, CONSUMER_DID).contains(vc.get("issuer").asText())) {
                problems.add("UNKNOWN_ISSUER");
            } else if (revoked) {
                problems.add("REVOKED_ISSUER");
            }
            return new CredentialValidator.VerifyResult(false, problems);
        }

        @Override
        public String counterpartyDid(String participantId) {
            return "consumer-01".equals(participantId) ? CONSUMER_DID : PROVIDER_DID;
        }

        @Override
        public Instant now() {
            return NOW;
        }
    };

    String code(JsonNode vc) {
        return validator.validate(provider, vc, ports).reasonCode();
    }

    @Test
    void validConsumerVcSucceeds() {
        assertThat(validator.validate(provider, consumerVc(), ports)).isEqualTo(CredentialValidator.Outcome.success());
    }

    @Test
    void missingVcOrStoredData() {
        assertThat(code(null)).isEqualTo("MISSING_CONTRACT_DATA");
        provider.contractDefinitionId = null;
        assertThat(code(consumerVc())).isEqualTo("MISSING_CONTRACT_DATA");
    }

    @Test
    void schemaFailureComesBeforeSignature() {
        ObjectNode vc = consumerVc();
        ((ObjectNode) vc.get("credentialSubject")).remove("assetId");
        assertThat(code(vc)).isEqualTo("SCHEMA_OR_CONTENT_VALIDATION_FAILURE");
        ObjectNode noProof = consumerVc();
        noProof.remove("proof");
        assertThat(code(noProof)).isEqualTo("SCHEMA_OR_CONTENT_VALIDATION_FAILURE");
    }

    @Test
    void tamperedVcIsInvalidSignatureWithPartyInTheName() {
        ObjectNode vc = consumerVc();
        ((ObjectNode) vc.get("credentialSubject")).put("assetId", "asset-2");
        assertThat(code(vc)).isEqualTo("INVALID_CONSUMER_SIGNATURE");
        provider.partyRole = PartyRole.CONSUMER;
        provider.usageContractHash = "hash-1";
        ObjectNode pvc = vc(PROVIDER_SEED, subject("PROVIDER"), "2026-10-29T10:00:00Z", "2027-10-29T10:00:00Z");
        ((ObjectNode) pvc.get("proof")).put("proofValue", "z1111");
        assertThat(code(pvc)).isEqualTo("INVALID_PROVIDER_SIGNATURE");
    }

    @Test
    void untrustedRevokedAndWrongIssuerAreTrustIssues() {
        assertThat(code(vc(ROGUE_SEED, subject("CONSUMER"), "2026-10-29T10:00:00Z", "2027-10-29T10:00:00Z")))
                .isEqualTo("CONSUMER_CREDENTIAL_TRUST_ISSUE");
        revoked = true;
        assertThat(code(consumerVc())).isEqualTo("CONSUMER_CREDENTIAL_TRUST_ISSUE");
        revoked = false;
        assertThat(code(vc(PROVIDER_SEED, subject("CONSUMER"), "2026-10-29T10:00:00Z", "2027-10-29T10:00:00Z")))
                .isEqualTo("CONSUMER_CREDENTIAL_TRUST_ISSUE");
    }

    @Test
    void verifierFailureIsTechnical() {
        verifierFails = true;
        CredentialValidator.Outcome o = validator.validate(provider, consumerVc(), ports);
        assertThat(o.result()).isEqualTo("FAILED");
        assertThat(o.reasonCode()).isEqualTo("TECHNICAL_VALIDATION_ERROR");
    }

    @Test
    void validityComesBeforePayloadIntegrity() {
        ObjectNode s = subject("CONSUMER");
        s.put("assetId", "other-asset");
        assertThat(code(vc(CONSUMER_SEED, s, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z"))).isEqualTo("CONTRACT_VALIDITY_ISSUE");
        assertThat(code(vc(CONSUMER_SEED, subject("CONSUMER"), "2027-01-01T00:00:00Z", "2028-01-01T00:00:00Z"))).isEqualTo("CONTRACT_VALIDITY_ISSUE");
    }

    @Test
    void payloadMismatchAndWrongSignerRole() {
        ObjectNode s = subject("CONSUMER");
        s.put("assetId", "other-asset");
        assertThat(code(vc(CONSUMER_SEED, s, "2026-10-29T10:00:00Z", "2027-10-29T10:00:00Z"))).isEqualTo("PAYLOAD_INTEGRITY_MISMATCH");
        assertThat(code(vc(CONSUMER_SEED, subject("PROVIDER"), "2026-10-29T10:00:00Z", "2027-10-29T10:00:00Z"))).isEqualTo("PAYLOAD_INTEGRITY_MISMATCH");
    }

    @Test
    void consumerAlsoComparesTheHash() {
        ContractRecord consumer = provider;
        consumer.partyRole = PartyRole.CONSUMER;
        consumer.usageContractHash = "another-hash";
        ObjectNode pvc = vc(PROVIDER_SEED, subject("PROVIDER"), "2026-10-29T10:00:00Z", "2027-10-29T10:00:00Z");
        assertThat(validator.validate(consumer, pvc, ports).reasonCode()).isEqualTo("PAYLOAD_INTEGRITY_MISMATCH");
        consumer.usageContractHash = "hash-1";
        assertThat(validator.validate(consumer, pvc, ports).result()).isEqualTo("SUCCESS");
    }
}
