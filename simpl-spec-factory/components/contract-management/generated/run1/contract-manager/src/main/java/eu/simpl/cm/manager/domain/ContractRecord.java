package eu.simpl.cm.manager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/** D60 contract record, one per party per nid. */
@Entity
@Table(name = "contract_record")
public class ContractRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "contract_negotiation_id", unique = true, nullable = false, length = 100)
    public String contractNegotiationId;
    @Column(length = 100)
    public String contractAgreementId;
    @Column(length = 100)
    public String contractDefinitionId;
    @Column(length = 100)
    public String contractOfferId;
    @Column(length = 100)
    public String assetId;
    @Column(length = 200)
    public String resourceDescriptionId;
    @Column(length = 500)
    public String resourceName;
    @Column(length = 100)
    public String providerId;
    @Column(length = 500)
    public String providerName;
    @Column(length = 100)
    public String consumerId;
    @Column(length = 500)
    public String consumerName;
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    public PartyRole partyRole;
    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    public ContractStatus status;
    @Column(length = 100)
    public String reasonCode;
    @Column(length = 20)
    public String consentStatus;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    public PartyRole revocationRequestedBy;
    @Column(length = 20)
    public String validationResult;
    @Column(length = 100)
    public String validationReasonCode;
    public Instant validatedAt;
    @Column(length = 1000000)
    public String humanReadable;
    @Column(length = 100)
    public String usageContractHash;
    @Column(length = 200)
    public String ownCredentialId;
    @Column(length = 200)
    public String counterpartyCredentialId;
    @Column(length = 1000000)
    public String ownCredential;
    @Column(length = 1000000)
    public String counterpartyCredential;
    public Instant consumerSignatureDate;
    public Instant providerSignatureDate;
    @Column(nullable = false)
    public Instant createdDate;
    @Column(nullable = false)
    public Instant lastUpdatedDate;
    @Version
    public long version;

    public String counterpartyId() {
        return partyRole == PartyRole.PROVIDER ? consumerId : providerId;
    }
}
