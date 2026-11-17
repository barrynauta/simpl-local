package eu.simpl.cm.manager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/** The contract record at one party (D60). */
@Entity
@Table(name = "contract_record", indexes = {
        @Index(name = "ix_contract_aid", columnList = "contractAgreementId"),
        @Index(name = "ix_contract_asset", columnList = "assetId")})
public class ContractRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 100)
    public String contractNegotiationId;
    @Column(length = 100)
    public String contractAgreementId;
    @Column(length = 100)
    public String contractDefinitionId;
    @Column(length = 200)
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
    @Column(nullable = false, length = 20)
    public PartyRole partyRole;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    public ContractStatus status;
    @Column(length = 100)
    public String reasonCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ConsentStatus consentStatus = ConsentStatus.NOT_DEFINED;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    public PartyRole revocationRequestedBy;

    @Column(length = 20)
    public String validationResult;
    @Column(length = 100)
    public String validationReasonCode;
    public Instant validatedAt;

    @Column(columnDefinition = "text")
    public String humanReadable;
    @Column(length = 100)
    public String usageContractHash;

    @Column(length = 200)
    public String ownCredentialId;
    @Column(length = 200)
    public String counterpartyCredentialId;
    @Column(columnDefinition = "text")
    public String ownCredential;
    @Column(columnDefinition = "text")
    public String counterpartyCredential;

    public Instant consumerSignatureDate;
    public Instant providerSignatureDate;
    @Column(nullable = false)
    public Instant createdDate;
    @Column(nullable = false)
    public Instant lastUpdatedDate;

    @Version
    public long version;
}
