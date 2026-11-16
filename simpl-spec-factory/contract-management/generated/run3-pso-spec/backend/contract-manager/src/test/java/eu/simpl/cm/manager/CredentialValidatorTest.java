package eu.simpl.cm.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.manager.client.HttpResult;
import eu.simpl.cm.manager.client.StubClient;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.PartyRole;
import eu.simpl.cm.manager.service.CredentialValidator;
import eu.simpl.cm.manager.service.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** D27: the checks, their order and the reason codes. */
class CredentialValidatorTest {

    static final String CONSUMER_DID = "did:key:z6MkqGC3nWZhYieEVTVDKW5v588CiGfsDSmRVG9ZwwWTvLSK";
    static final String PROVIDER_DID = "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S";

    private final ObjectMapper mapper = new ObjectMapper();
    private StubClient stub;
    private Directory directory;
    private CredentialValidator validator;
    private ContractRecord provider;

    @BeforeEach
    void setUp() {
        stub = mock(StubClient.class);
        directory = mock(Directory.class);
        validator = new CredentialValidator(stub, directory);
        when(directory.participant(anyString(), any())).thenAnswer(inv -> {
            String id = inv.getArgument(1);
            return new Directory.Participant(id, id, id.startsWith("consumer") ? CONSUMER_DID : PROVIDER_DID, null);
        });
        verified(true, null);
        provider = new ContractRecord();
        provider.partyRole = PartyRole.PROVIDER;
        provider.contractNegotiationId = "n1";
        provider.contractAgreementId = "a1";
        provider.contractDefinitionId = "d1";
        provider.assetId = "asset-1";
        provider.providerId = "provider-01";
        provider.consumerId = "consumer-01";
    }

    private void verified(boolean ok, String problem) {
        ObjectNode body = mapper.createObjectNode();
        body.put("verified", ok);
        if (problem != null) {
            body.putArray("problems").add(problem);
        } else {
            body.putArray("problems");
        }
        when(stub.verify(anyString(), any())).thenReturn(new HttpResult(200, body));
    }

    private ObjectNode consumerVc() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ObjectNode vc = mapper.createObjectNode();
        vc.putArray("@context").add("https://www.w3.org/ns/credentials/v2");
        vc.put("id", "urn:uuid:1");
        vc.putArray("type").add("VerifiableCredential").add("UsageContractCredential");
        vc.put("issuer", CONSUMER_DID);
        vc.put("validFrom", now.minusSeconds(60).toString());
        vc.put("validUntil", now.plusSeconds(3600).toString());
        ObjectNode s = vc.putObject("credentialSubject");
        s.put("id", "urn:simpl:contract:n1");
        s.put("contractNegotiationId", "n1");
        s.put("contractAgreementId", "a1");
        s.put("contractDefinitionId", "d1");
        s.put("assetId", "asset-1");
        s.put("resourceDescriptionId", "rd-1");
        s.put("providerId", "provider-01");
        s.put("consumerId", "consumer-01");
        s.put("usageContractHash", "abc");
        s.put("signerRole", "CONSUMER");
        s.put("signedAt", now.toString());
        vc.putObject("proof").put("type", "DataIntegrityProof");
        return vc;
    }

    @Test
    void success() {
        assertThat(validator.validate(provider, consumerVc()).result()).isEqualTo("SUCCESS");
    }

    @Test
    void missingVc() {
        assertThat(validator.validate(provider, null).reasonCode()).isEqualTo("MISSING_CONTRACT_DATA");
        assertThat(validator.validate(provider, mapper.createObjectNode()).reasonCode())
                .isEqualTo("MISSING_CONTRACT_DATA");
    }

    @Test
    void schemaCheckComesBeforeTheVerifier() {
        ObjectNode vc = consumerVc();
        ((ObjectNode) vc.get("credentialSubject")).remove("assetId");
        CredentialValidator.Outcome o = validator.validate(provider, vc);
        assertThat(o.result()).isEqualTo("REJECTED");
        assertThat(o.reasonCode()).isEqualTo("SCHEMA_OR_CONTENT_VALIDATION_FAILURE");
        verify(stub, never()).verify(anyString(), any());
    }

    @Test
    void verifierProblemsMapToPartyCodes() {
        verified(false, "INVALID_SIGNATURE");
        assertThat(validator.validate(provider, consumerVc()).reasonCode()).isEqualTo("INVALID_CONSUMER_SIGNATURE");
        verified(false, "UNKNOWN_ISSUER");
        assertThat(validator.validate(provider, consumerVc()).reasonCode())
                .isEqualTo("CONSUMER_CREDENTIAL_TRUST_ISSUE");
        verified(false, "REVOKED_ISSUER");
        assertThat(validator.validate(provider, consumerVc()).reasonCode())
                .isEqualTo("CONSUMER_CREDENTIAL_TRUST_ISSUE");
        verified(false, "MALFORMED");
        assertThat(validator.validate(provider, consumerVc()).reasonCode())
                .isEqualTo("SCHEMA_OR_CONTENT_VALIDATION_FAILURE");
    }

    @Test
    void verifierFailureIsTechnical() {
        when(stub.verify(anyString(), any())).thenReturn(new HttpResult(500, null));
        CredentialValidator.Outcome o = validator.validate(provider, consumerVc());
        assertThat(o.result()).isEqualTo("FAILED");
        assertThat(o.reasonCode()).isEqualTo("TECHNICAL_VALIDATION_ERROR");
    }

    @Test
    void wrongIssuerIsATrustIssue() {
        ObjectNode vc = consumerVc();
        vc.put("issuer", PROVIDER_DID);
        assertThat(validator.validate(provider, vc).reasonCode()).isEqualTo("CONSUMER_CREDENTIAL_TRUST_ISSUE");
    }

    @Test
    void expiredBeforePayload() {
        ObjectNode vc = consumerVc();
        vc.put("validUntil", "2020-01-01T00:00:00Z");
        ((ObjectNode) vc.get("credentialSubject")).put("assetId", "other");
        assertThat(validator.validate(provider, vc).reasonCode()).isEqualTo("CONTRACT_VALIDITY_ISSUE");
    }

    @Test
    void payloadMismatchAndWrongSignerRole() {
        ObjectNode vc = consumerVc();
        ((ObjectNode) vc.get("credentialSubject")).put("assetId", "other");
        assertThat(validator.validate(provider, vc).reasonCode()).isEqualTo("PAYLOAD_INTEGRITY_MISMATCH");
        ObjectNode vc2 = consumerVc();
        ((ObjectNode) vc2.get("credentialSubject")).put("signerRole", "PROVIDER");
        assertThat(validator.validate(provider, vc2).reasonCode()).isEqualTo("PAYLOAD_INTEGRITY_MISMATCH");
    }

    @Test
    void consumerAlsoComparesTheHash() {
        ContractRecord consumer = new ContractRecord();
        consumer.partyRole = PartyRole.CONSUMER;
        consumer.contractNegotiationId = "n1";
        consumer.contractAgreementId = "a1";
        consumer.contractDefinitionId = "d1";
        consumer.assetId = "asset-1";
        consumer.providerId = "provider-01";
        consumer.consumerId = "consumer-01";
        consumer.usageContractHash = "abc";
        ObjectNode vc = consumerVc();
        vc.put("issuer", PROVIDER_DID);
        ((ObjectNode) vc.get("credentialSubject")).put("signerRole", "PROVIDER");
        assertThat(validator.validate(consumer, vc).result()).isEqualTo("SUCCESS");
        ((ObjectNode) vc.get("credentialSubject")).put("usageContractHash", "other");
        assertThat(validator.validate(consumer, vc).reasonCode()).isEqualTo("PAYLOAD_INTEGRITY_MISMATCH");
        verified(false, "INVALID_SIGNATURE");
        assertThat(validator.validate(consumer, vc).reasonCode()).isEqualTo("INVALID_PROVIDER_SIGNATURE");
    }
}
